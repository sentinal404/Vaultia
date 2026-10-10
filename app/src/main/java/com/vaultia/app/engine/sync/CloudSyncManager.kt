package com.vaultia.app.engine.sync

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.vaultia.app.core.Constants
import com.vaultia.app.core.VaultResult
import com.vaultia.app.core.security.SecureStorageManager
import com.vaultia.app.data.local.VaultiaDatabase
import com.vaultia.app.data.local.entity.ChunkEntity
import com.vaultia.app.data.local.entity.FileEntity
import com.vaultia.app.data.local.entity.FolderEntity
import com.vaultia.app.data.remote.ChunkPayloadMetadata
import com.vaultia.app.data.remote.TelegramApiService
import com.vaultia.app.domain.model.MediaType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class SyncState(
    val isSyncing: Boolean = false,
    val statusMessage: String = "Idle",
    val progress: Float = 0f,
    val syncedFilesCount: Int = 0,
    val lastError: String? = null
)

data class VaultiaCloudCatalog(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @SerializedName("folders") val folders: List<FolderCatalogItem> = emptyList(),
    @SerializedName("files") val files: List<FileCatalogItem> = emptyList(),
    @SerializedName("chunks") val chunks: List<ChunkCatalogItem> = emptyList()
)

data class FolderCatalogItem(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("colorHex") val colorHex: String,
    @SerializedName("createdAt") val createdAt: Long
)

data class FileCatalogItem(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("size") val size: Long,
    @SerializedName("mimeType") val mimeType: String,
    @SerializedName("mediaType") val mediaType: String,
    @SerializedName("totalChunks") val totalChunks: Int,
    @SerializedName("chunkSize") val chunkSize: Long,
    @SerializedName("checksum") val checksum: String,
    @SerializedName("folderId") val folderId: String? = null,
    @SerializedName("isFavorite") val isFavorite: Boolean = false,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("modifiedAt") val modifiedAt: Long
)

data class ChunkCatalogItem(
    @SerializedName("id") val id: String,
    @SerializedName("fileId") val fileId: String,
    @SerializedName("chunkIndex") val chunkIndex: Int,
    @SerializedName("totalChunks") val totalChunks: Int,
    @SerializedName("size") val size: Long,
    @SerializedName("checksum") val checksum: String,
    @SerializedName("status") val status: String,
    @SerializedName("remoteMessageId") val remoteMessageId: Long? = null,
    @SerializedName("remoteFileId") val remoteFileId: String? = null,
    @SerializedName("workerId") val workerId: String? = null
)

class CloudSyncManager(
    private val context: Context,
    private val database: VaultiaDatabase,
    private val telegramApi: TelegramApiService,
    private val secureStorage: SecureStorageManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    private var syncJob: Job? = null

    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    fun performFullSync(forceDeepScan: Boolean = false) {
        if (_syncState.value.isSyncing) return

        syncJob = scope.launch {
            val token = secureStorage.botToken
            val channelId = secureStorage.channelId

            if (token.isNullOrBlank() || channelId.isNullOrBlank()) {
                _syncState.value = _syncState.value.copy(
                    isSyncing = false,
                    lastError = "Telegram credentials not configured"
                )
                return@launch
            }

            _syncState.value = SyncState(
                isSyncing = true,
                statusMessage = "Connecting to Telegram Cloud...",
                progress = 0.05f
            )

            try {
                // Step 1: Check Pinned Catalog for instant fast sync
                var catalogRestored = false
                if (!forceDeepScan) {
                    catalogRestored = tryRestoreFromPinnedCatalog(token, channelId)
                }

                // Step 2: If no catalog was found or forceDeepScan is requested, run Deep Channel Scan
                if (!catalogRestored) {
                    runDeepChannelScan(token, channelId)
                }

                // Step 3: Refresh cloud catalog and pin it so all future syncs are instant
                syncCatalogToCloud()

                val totalFiles = database.fileDao().getAllFilesList().size
                _syncState.value = SyncState(
                    isSyncing = false,
                    statusMessage = "Synced $totalFiles files",
                    progress = 1.0f,
                    syncedFilesCount = totalFiles
                )
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed: ${e.message}", e)
                _syncState.value = SyncState(
                    isSyncing = false,
                    statusMessage = "Sync failed",
                    lastError = e.localizedMessage ?: "Unknown sync error"
                )
            }
        }
    }

    private suspend fun tryRestoreFromPinnedCatalog(token: String, channelId: String): Boolean {
        _syncState.value = _syncState.value.copy(
            statusMessage = "Checking Cloud Manifest...",
            progress = 0.15f
        )

        val chatResult = telegramApi.getChat(token, channelId)
        if (chatResult !is VaultResult.Success) return false

        val pinnedMsg = chatResult.data.pinnedMessage ?: return false
        val doc = pinnedMsg.document ?: return false

        val isCatalogDoc = doc.fileName?.contains("vaultia_catalog", ignoreCase = true) == true ||
                pinnedMsg.caption?.contains("[VAULTIA_CATALOG]", ignoreCase = true) == true

        if (!isCatalogDoc) return false

        _syncState.value = _syncState.value.copy(
            statusMessage = "Restoring Cloud Index...",
            progress = 0.35f
        )

        val fileResult = telegramApi.getFile(token, doc.fileId)
        val filePath = fileResult.getOrNull()?.filePath ?: return false

        val tempFile = File(context.cacheDir, "catalog_restore.json")
        val dlResult = FileOutputStream(tempFile).use { fos ->
            telegramApi.downloadFileChunk(token, filePath, fos)
        }

        if (dlResult !is VaultResult.Success || !tempFile.exists() || tempFile.length() == 0L) {
            return false
        }

        val jsonString = tempFile.readText(Charsets.UTF_8)
        val catalog = try {
            gson.fromJson(jsonString, VaultiaCloudCatalog::class.java)
        } catch (e: Exception) {
            null
        } ?: return false

        // Apply folders
        val folderEntities = catalog.folders.map {
            FolderEntity(id = it.id, name = it.name, colorHex = it.colorHex, createdAt = it.createdAt)
        }
        folderEntities.forEach { database.folderDao().insertFolder(it) }

        // Apply files
        val fileEntities = catalog.files.map {
            FileEntity(
                id = it.id,
                name = it.name,
                size = it.size,
                mimeType = it.mimeType,
                mediaType = it.mediaType,
                totalChunks = it.totalChunks,
                chunkSize = it.chunkSize,
                checksum = it.checksum,
                isFavorite = it.isFavorite,
                folderId = it.folderId,
                createdAt = it.createdAt,
                modifiedAt = it.modifiedAt
            )
        }
        database.fileDao().insertFiles(fileEntities)

        // Apply chunks
        val chunkEntities = catalog.chunks.map {
            ChunkEntity(
                id = it.id,
                fileId = it.fileId,
                chunkIndex = it.chunkIndex,
                totalChunks = it.totalChunks,
                size = it.size,
                checksum = it.checksum,
                status = it.status,
                remoteMessageId = it.remoteMessageId,
                remoteFileId = it.remoteFileId,
                workerId = it.workerId
            )
        }
        database.chunkDao().insertChunks(chunkEntities)

        tempFile.delete()
        return true
    }

    private suspend fun runDeepChannelScan(token: String, channelId: String) {
        _syncState.value = _syncState.value.copy(
            statusMessage = "Probing Channel History...",
            progress = 0.2f
        )

        // Send a probe ping message to get the latest channel messageId
        val pingResult = telegramApi.sendMessage(token, channelId, "🔄 Vaultia Sync Indexing...")
        val maxMessageId = if (pingResult is VaultResult.Success) {
            val pingId = pingResult.data.messageId
            telegramApi.deleteMessage(token, channelId, pingId)
            pingId
        } else {
            // Fallback upper bound probe
            150L
        }

        val startId = (maxMessageId - 1).coerceAtLeast(1L)
        val endId = 1L
        val totalToScan = (startId - endId + 1).coerceAtLeast(1L)

        var scannedCount = 0
        var consecutiveMisses = 0

        for (msgId in startId downTo endId) {
            val progressVal = 0.2f + (0.7f * (scannedCount.toFloat() / totalToScan)).coerceIn(0f, 0.7f)
            _syncState.value = _syncState.value.copy(
                statusMessage = "Scanning message #$msgId...",
                progress = progressVal
            )

            // Attempt to forward the message within the channel to inspect its contents
            val fwdResult = telegramApi.forwardMessage(token, channelId, channelId, msgId)
            if (fwdResult is VaultResult.Success) {
                consecutiveMisses = 0
                val msg = fwdResult.data
                // Immediately delete the forwarded clone to keep channel clean
                telegramApi.deleteMessage(token, channelId, msg.messageId)

                parseAndPersistScannedMessage(token, msgId, msg)
            } else {
                consecutiveMisses++
                // If more than 80 consecutive messages are absent, we likely reached the beginning
                if (consecutiveMisses > 80 && scannedCount > 100) {
                    break
                }
            }

            scannedCount++
            // 40ms interval ensures compliance with Telegram Bot API rate limits (30 req/sec)
            delay(40L)
        }
    }

    private suspend fun parseAndPersistScannedMessage(
        token: String,
        originalMsgId: Long,
        msg: com.vaultia.app.data.remote.MessageDto
    ) {
        val caption = msg.caption ?: ""

        // Check 1: Is this a chunked Vaultia file?
        if (caption.contains(Constants.METADATA_TAG) && msg.document != null) {
            try {
                val jsonPart = caption.substringAfter(Constants.METADATA_TAG).trim()
                val meta = gson.fromJson(jsonPart, ChunkPayloadMetadata::class.java)
                val doc = msg.document

                val existingFile = database.fileDao().getFileById(meta.fileId)
                if (existingFile == null) {
                    val mediaType = MediaType.fromMimeType(doc.mimeType ?: "", meta.filename)
                    val newFile = FileEntity(
                        id = meta.fileId,
                        name = meta.filename,
                        size = meta.chunkSize * meta.totalChunks,
                        mimeType = doc.mimeType ?: "application/octet-stream",
                        mediaType = mediaType.name,
                        totalChunks = meta.totalChunks,
                        chunkSize = meta.chunkSize,
                        checksum = meta.checksum,
                        createdAt = msg.date * 1000L,
                        modifiedAt = msg.date * 1000L
                    )
                    database.fileDao().insertFile(newFile)
                }

                val chunk = ChunkEntity(
                    id = "${meta.fileId}_${meta.chunkIndex}",
                    fileId = meta.fileId,
                    chunkIndex = meta.chunkIndex,
                    totalChunks = meta.totalChunks,
                    size = meta.chunkSize,
                    checksum = meta.checksum,
                    status = "COMPLETED",
                    remoteMessageId = originalMsgId,
                    remoteFileId = doc.fileId,
                    workerId = token
                )
                database.chunkDao().insertChunk(chunk)
                return
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing chunk metadata: ${e.message}")
            }
        }

        // Check 2: Standalone media (received via Bot or uploaded directly)
        val doc = msg.document
        val video = msg.video
        val audio = msg.audio
        val photo = msg.photo?.maxByOrNull { it.width * it.height }

        val (fileId, fileName, fileSize, mimeType, mediaType) = when {
            doc != null -> {
                val name = doc.fileName ?: "doc_$originalMsgId.bin"
                val size = doc.fileSize ?: 0L
                val mime = doc.mimeType ?: "application/octet-stream"
                Quintuple(doc.fileId, name, size, mime, MediaType.fromMimeType(mime, name))
            }
            video != null -> {
                val name = video.fileName ?: "video_$originalMsgId.mp4"
                val size = video.fileSize ?: 0L
                val mime = video.mimeType ?: "video/mp4"
                Quintuple(video.fileId, name, size, mime, MediaType.VIDEOS)
            }
            audio != null -> {
                val name = audio.fileName ?: "${audio.title ?: "audio"}_$originalMsgId.mp3"
                val size = audio.fileSize ?: 0L
                val mime = audio.mimeType ?: "audio/mpeg"
                Quintuple(audio.fileId, name, size, mime, MediaType.AUDIO)
            }
            photo != null -> {
                val name = "IMG_$originalMsgId.jpg"
                val size = photo.fileSize ?: 0L
                val mime = "image/jpeg"
                Quintuple(photo.fileId, name, size, mime, MediaType.IMAGES)
            }
            else -> return
        }

        val uniqueFileId = "recovered_$originalMsgId"
        val existing = database.fileDao().getFileById(uniqueFileId)
        if (existing == null) {
            val fileEntity = FileEntity(
                id = uniqueFileId,
                name = fileName,
                size = fileSize,
                mimeType = mimeType,
                mediaType = mediaType.name,
                totalChunks = 1,
                chunkSize = if (fileSize > 0) fileSize else 1024L,
                checksum = "",
                createdAt = msg.date * 1000L,
                modifiedAt = msg.date * 1000L
            )
            database.fileDao().insertFile(fileEntity)

            val chunk = ChunkEntity(
                id = "c_$uniqueFileId",
                fileId = uniqueFileId,
                chunkIndex = 0,
                totalChunks = 1,
                size = fileSize,
                checksum = "",
                status = "COMPLETED",
                remoteMessageId = originalMsgId,
                remoteFileId = fileId,
                workerId = token
            )
            database.chunkDao().insertChunk(chunk)
        }
    }

    suspend fun syncCatalogToCloud() = withContext(Dispatchers.IO) {
        val token = secureStorage.botToken ?: return@withContext
        val channelId = secureStorage.channelId ?: return@withContext

        try {
            val allFolders = database.folderDao().getAllFoldersList()
            val allFiles = database.fileDao().getAllFilesList()
            val allChunks = database.chunkDao().getAllChunksList()

            val catalog = VaultiaCloudCatalog(
                version = 1,
                timestamp = System.currentTimeMillis(),
                folders = allFolders.map { FolderCatalogItem(it.id, it.name, it.colorHex, it.createdAt) },
                files = allFiles.map {
                    FileCatalogItem(
                        id = it.id,
                        name = it.name,
                        size = it.size,
                        mimeType = it.mimeType,
                        mediaType = it.mediaType,
                        totalChunks = it.totalChunks,
                        chunkSize = it.chunkSize,
                        checksum = it.checksum,
                        folderId = it.folderId,
                        isFavorite = it.isFavorite,
                        createdAt = it.createdAt,
                        modifiedAt = it.modifiedAt
                    )
                },
                chunks = allChunks.map {
                    ChunkCatalogItem(
                        id = it.id,
                        fileId = it.fileId,
                        chunkIndex = it.chunkIndex,
                        totalChunks = it.totalChunks,
                        size = it.size,
                        checksum = it.checksum,
                        status = it.status,
                        remoteMessageId = it.remoteMessageId,
                        remoteFileId = it.remoteFileId,
                        workerId = it.workerId
                    )
                }
            )

            val jsonContent = gson.toJson(catalog)
            val catalogFile = File(context.cacheDir, "vaultia_catalog.json")
            catalogFile.writeText(jsonContent, Charsets.UTF_8)

            val uploadResult = telegramApi.sendDocumentChunk(
                token = token,
                chatId = channelId,
                chunkFile = catalogFile,
                fileName = "vaultia_catalog.json",
                caption = "[VAULTIA_CATALOG] Vaultia Cloud Manifest • ${allFiles.size} items"
            )

            if (uploadResult is VaultResult.Success) {
                val messageId = uploadResult.data.messageId
                telegramApi.pinChatMessage(token, channelId, messageId, disableNotification = true)
                Log.d(TAG, "Vaultia Cloud Manifest updated & pinned successfully (Message #$messageId)")
            }
            catalogFile.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync catalog to cloud: ${e.message}")
        }
    }

    private data class Quintuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

    companion object {
        private const val TAG = "CloudSyncManager"
    }
}
