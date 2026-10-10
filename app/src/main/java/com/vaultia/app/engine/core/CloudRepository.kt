package com.vaultia.app.engine.core

import com.vaultia.app.core.security.SecureStorageManager
import com.vaultia.app.data.local.VaultiaDatabase
import com.vaultia.app.domain.model.MediaType
import com.vaultia.app.domain.model.VaultFile
import com.vaultia.app.engine.chunk.ChunkEngine
import com.vaultia.app.engine.transfer.DownloadManager
import com.vaultia.app.engine.transfer.UploadManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

/**
 * Primary cloud repository for Vaultia.
 * Orchestrates cloud files, folders, search, streaming, and manifest synchronization.
 */
class CloudRepository(
    private val database: VaultiaDatabase,
    private val secureStorage: SecureStorageManager,
    private val clientManager: TelegramClientManager,
    private val manifestRepository: CloudManifestRepository,
    private val streamingManager: MediaStreamingManager,
    private val uploadManager: UploadManager,
    private val downloadManager: DownloadManager,
    private val chunkEngine: ChunkEngine
) {
    val allFiles: Flow<List<VaultFile>> = database.fileDao().getAllFiles().map { list ->
        list.map { it.toDomain() }
    }

    val recentFiles: Flow<List<VaultFile>> = database.fileDao().getRecentFiles(10).map { list ->
        list.map { it.toDomain() }
    }

    val favoriteFiles: Flow<List<VaultFile>> = database.fileDao().getFavoriteFiles().map { list ->
        list.map { it.toDomain() }
    }

    val totalStorageUsed: Flow<Long> = database.fileDao().getTotalStorageUsed().map { it ?: 0L }
    val totalFilesCount: Flow<Int> = database.fileDao().getTotalFilesCount()

    fun getFilesByMediaType(type: MediaType): Flow<List<VaultFile>> {
        return if (type == MediaType.OTHER) {
            allFiles
        } else {
            database.fileDao().getFilesByMediaType(type.name).map { list ->
                list.map { it.toDomain() }
            }
        }
    }

    fun searchFiles(query: String): Flow<List<VaultFile>> {
        return database.fileDao().searchFiles(query).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getFileById(fileId: String): VaultFile? {
        return database.fileDao().getFileById(fileId)?.toDomain()
    }

    suspend fun toggleFavorite(fileId: String, currentStatus: Boolean) {
        database.fileDao().setFavorite(fileId, !currentStatus)
    }

    suspend fun deleteFile(fileId: String) {
        database.fileDao().deleteFile(fileId)
        database.chunkDao().deleteChunksForFile(fileId)
    }

    fun uploadFile(file: File): String {
        return uploadManager.enqueueUpload(file)
    }

    fun downloadFile(vaultFile: VaultFile, destinationDir: File? = null): String {
        return downloadManager.enqueueDownload(vaultFile, destinationDir)
    }

    fun getStreamUrl(fileId: String): String {
        return streamingManager.getStreamUrl(fileId)
    }

    fun prewarmMedia(vaultFile: VaultFile) {
        streamingManager.prewarmMedia(vaultFile)
    }

    suspend fun syncManifest() {
        val manifest = manifestRepository.generateCurrentManifest()
        manifestRepository.saveManifestLocally(manifest)
    }

    suspend fun restoreLibrary(): Int {
        val manifest = manifestRepository.generateCurrentManifest()
        return manifestRepository.restoreLibraryFromManifest(manifest)
    }
}
