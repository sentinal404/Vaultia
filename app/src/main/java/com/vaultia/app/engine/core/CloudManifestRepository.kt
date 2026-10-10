package com.vaultia.app.engine.core

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.vaultia.app.core.security.SecureStorageManager
import com.vaultia.app.data.local.VaultiaDatabase
import com.vaultia.app.data.local.entity.ChunkEntity
import com.vaultia.app.data.local.entity.FileEntity
import com.vaultia.app.data.local.entity.FolderEntity
import com.vaultia.app.domain.model.TransferStatus
import com.vaultia.app.domain.model.VaultFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class ManifestFileEntry(
    val id: String,
    val name: String,
    val size: Long,
    val mimeType: String,
    val mediaType: String,
    val totalChunks: Int,
    val chunkSize: Long,
    val checksum: String,
    val createdAt: Long,
    val modifiedAt: Long,
    val folderId: String? = null,
    val remoteFileId: String? = null,
    val remoteMessageId: Long? = null
)

data class ManifestFolderEntry(
    val id: String,
    val name: String,
    val colorHex: String = "#8B5CF6",
    val createdAt: Long = System.currentTimeMillis()
)

data class VaultiaCloudManifest(
    val version: Int = 2,
    val vaultId: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val files: List<ManifestFileEntry> = emptyList(),
    val folders: List<ManifestFolderEntry> = emptyList()
)

/**
 * Manages versioned cloud manifests, integrity validation, and library restoration.
 */
class CloudManifestRepository(
    private val context: Context,
    private val database: VaultiaDatabase,
    private val secureStorage: SecureStorageManager
) {
    private val gson = Gson()
    private val manifestLocalFile = File(context.filesDir, "vaultia_manifest_v2.json")

    companion object {
        private const val TAG = "CloudManifestRepository"
        const val MANIFEST_VERSION = 2
    }

    suspend fun generateCurrentManifest(): VaultiaCloudManifest = withContext(Dispatchers.IO) {
        val files = database.fileDao().getAllFilesList()
        val folders = database.folderDao().getAllFoldersList()
        val allChunks = database.chunkDao().getAllChunksList().groupBy { it.fileId }

        val fileEntries = files.map { file ->
            val firstChunk = allChunks[file.id]?.firstOrNull()
            ManifestFileEntry(
                id = file.id,
                name = file.name,
                size = file.size,
                mimeType = file.mimeType,
                mediaType = file.mediaType,
                totalChunks = file.totalChunks,
                chunkSize = file.chunkSize,
                checksum = file.checksum,
                createdAt = file.createdAt,
                modifiedAt = file.modifiedAt,
                folderId = file.folderId,
                remoteFileId = firstChunk?.remoteFileId,
                remoteMessageId = firstChunk?.remoteMessageId
            )
        }

        val folderEntries = folders.map { folder ->
            ManifestFolderEntry(
                id = folder.id,
                name = folder.name,
                colorHex = folder.colorHex,
                createdAt = folder.createdAt
            )
        }

        VaultiaCloudManifest(
            version = MANIFEST_VERSION,
            vaultId = secureStorage.channelId ?: "local_vault",
            updatedAt = System.currentTimeMillis(),
            files = fileEntries,
            folders = folderEntries
        )
    }

    suspend fun saveManifestLocally(manifest: VaultiaCloudManifest) = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(manifest)
            manifestLocalFile.writeText(json)
            Log.i(TAG, "Saved manifest locally (${manifest.files.size} files, ${manifest.folders.size} folders)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving manifest locally: ${e.message}")
        }
    }

    suspend fun restoreLibraryFromManifest(manifest: VaultiaCloudManifest): Int = withContext(Dispatchers.IO) {
        var restoredCount = 0
        try {
            // Restore folders first
            manifest.folders.forEach { f ->
                database.folderDao().insertFolder(
                    FolderEntity(
                        id = f.id,
                        name = f.name,
                        colorHex = f.colorHex,
                        createdAt = f.createdAt
                    )
                )
            }

            // Restore files idempotently without overriding existing completed files
            manifest.files.forEach { fe ->
                val existing = database.fileDao().getFileById(fe.id)
                if (existing == null) {
                    database.fileDao().insertFile(
                        FileEntity(
                            id = fe.id,
                            name = fe.name,
                            size = fe.size,
                            mimeType = fe.mimeType,
                            mediaType = fe.mediaType,
                            totalChunks = fe.totalChunks,
                            chunkSize = fe.chunkSize,
                            checksum = fe.checksum,
                            createdAt = fe.createdAt,
                            modifiedAt = fe.modifiedAt,
                            folderId = fe.folderId
                        )
                    )

                    // Insert matching chunk record if remoteFileId exists
                    if (!fe.remoteFileId.isNullOrBlank()) {
                        database.chunkDao().insertChunks(
                            listOf(
                                ChunkEntity(
                                    id = "chunk_${fe.id}_0",
                                    fileId = fe.id,
                                    chunkIndex = 0,
                                    totalChunks = fe.totalChunks,
                                    size = fe.size,
                                    checksum = fe.checksum,
                                    status = TransferStatus.COMPLETED.name,
                                    remoteMessageId = fe.remoteMessageId,
                                    remoteFileId = fe.remoteFileId,
                                    workerId = secureStorage.botToken
                                )
                            )
                        )
                    }
                    restoredCount++
                }
            }
            Log.i(TAG, "Successfully restored $restoredCount files from manifest")
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring from manifest: ${e.message}")
        }
        restoredCount
    }
}
