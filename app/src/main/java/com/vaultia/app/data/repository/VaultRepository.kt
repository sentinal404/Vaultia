package com.vaultia.app.data.repository

import android.content.Context
import com.vaultia.app.core.Constants
import com.vaultia.app.core.VaultResult
import com.vaultia.app.core.security.SecureStorageManager
import com.vaultia.app.data.local.VaultiaDatabase
import com.vaultia.app.data.local.entity.FileEntity
import com.vaultia.app.data.local.entity.WorkerEntity
import com.vaultia.app.data.remote.TelegramApiService
import com.vaultia.app.domain.model.MediaType
import com.vaultia.app.domain.model.TelegramConfig
import com.vaultia.app.domain.model.TransferTask
import com.vaultia.app.domain.model.VaultFile
import com.vaultia.app.domain.model.WorkerBot
import com.vaultia.app.engine.chunk.ChunkEngine
import com.vaultia.app.engine.streaming.MediaStreamingServer
import com.vaultia.app.engine.transfer.DownloadManager
import com.vaultia.app.engine.transfer.UploadManager
import com.vaultia.app.engine.transfer.WorkerScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.UUID

class VaultRepository(
    private val context: Context,
    val database: VaultiaDatabase,
    val telegramApi: TelegramApiService,
    val secureStorage: SecureStorageManager,
    val chunkEngine: ChunkEngine,
    val workerScheduler: WorkerScheduler,
    val uploadManager: UploadManager,
    val downloadManager: DownloadManager,
    val streamingServer: MediaStreamingServer
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

    val allWorkers: Flow<List<WorkerBot>> = database.workerDao().getAllWorkers().map { list ->
        list.map { it.toDomain() }
    }

    val allFolders: Flow<List<com.vaultia.app.data.local.entity.FolderEntity>> = database.folderDao().getAllFolders()

    fun getFilesByFolder(folderId: String): Flow<List<VaultFile>> {
        return database.fileDao().getFilesByFolder(folderId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun createFolder(name: String, colorHex: String = "#8B5CF6") {
        val folder = com.vaultia.app.data.local.entity.FolderEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            colorHex = colorHex
        )
        database.folderDao().insertFolder(folder)
    }

    suspend fun deleteFolder(folderId: String) {
        database.folderDao().deleteFolder(folderId)
    }

    suspend fun moveFileToFolder(fileId: String, folderId: String?) {
        database.fileDao().updateFileFolder(fileId, folderId)
    }

    fun getFilesByMediaType(type: MediaType): Flow<List<VaultFile>> {
        return if (type == MediaType.ALL) {
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

    suspend fun deleteFile(fileId: String) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val channelId = secureStorage.channelId
        val token = secureStorage.botToken
        val chunks = database.chunkDao().getChunksForFileList(fileId)

        // Delete all remote chunk messages from the private Telegram Channel immediately
        if (!channelId.isNullOrBlank() && !token.isNullOrBlank()) {
            for (chunk in chunks) {
                val msgId = chunk.remoteMessageId
                if (msgId != null && msgId > 0) {
                    try {
                        val worker = workerScheduler.getAvailableWorker()
                        val deleteToken = worker?.token ?: token
                        telegramApi.deleteMessage(deleteToken, channelId, msgId)
                    } catch (_: Exception) {}
                }
            }
        }

        database.chunkDao().deleteChunksForFile(fileId)
        database.fileDao().deleteFile(fileId)
    }

    suspend fun validateTelegramConnection(token: String, channelId: String): VaultResult<TelegramConfig> {
        val meResult = telegramApi.getMe(token)
        if (meResult is VaultResult.Error) {
            return VaultResult.Error("Bot authentication failed: ${meResult.message}", code = meResult.code)
        }
        val botUser = (meResult as VaultResult.Success).data

        val chatResult = telegramApi.getChat(token, channelId)
        if (chatResult is VaultResult.Error) {
            return VaultResult.Error("Channel access failed: ${chatResult.message}. Ensure the bot is added to this channel.", code = chatResult.code)
        }
        val chat = (chatResult as VaultResult.Success).data
        if (chat.type != "channel") {
            return VaultResult.Error("The specified chat is a ${chat.type}. Vaultia requires a private Telegram Channel.")
        }

        val memberResult = telegramApi.getChatMember(token, channelId, botUser.id)
        if (memberResult is VaultResult.Error) {
            return VaultResult.Error("Could not verify bot admin status in channel: ${memberResult.message}", code = memberResult.code)
        }
        val member = (memberResult as VaultResult.Success).data
        if (member.status != "administrator" && member.status != "creator") {
            return VaultResult.Error("The bot is not an administrator. Please promote the bot to Administrator in the channel.")
        }
        if (member.canPostMessages == false) {
            return VaultResult.Error("Bot permissions missing: The bot must have permission to post messages.")
        }

        val config = TelegramConfig(
            botToken = token,
            botName = botUser.firstName,
            botUsername = botUser.username ?: "",
            channelId = channelId,
            channelTitle = chat.title ?: "Private Vault Channel",
            isConfigured = true,
            isVerified = true
        )

        // Persist securely
        secureStorage.botToken = token
        secureStorage.channelId = channelId
        secureStorage.channelTitle = config.channelTitle
        secureStorage.botName = config.botName
        secureStorage.botUsername = config.botUsername

        // Insert or update primary worker
        val primaryWorker = WorkerEntity(
            id = "worker_primary",
            token = token,
            name = botUser.firstName,
            username = botUser.username ?: "bot",
            isPrimary = true,
            isOnline = true,
            isAuthorized = true,
            hasChannelAccess = true
        )
        database.workerDao().insertWorker(primaryWorker)
        reloadWorkers()

        return VaultResult.Success(config)
    }

    suspend fun addWorkerBot(token: String): VaultResult<WorkerBot> {
        val channelId = secureStorage.channelId ?: return VaultResult.Error("No primary channel configured")

        val meResult = telegramApi.getMe(token)
        if (meResult is VaultResult.Error) {
            return VaultResult.Error("Worker token invalid: ${meResult.message}")
        }
        val botUser = (meResult as VaultResult.Success).data

        val memberResult = telegramApi.getChatMember(token, channelId, botUser.id)
        if (memberResult is VaultResult.Error) {
            return VaultResult.Error("Worker bot is not in the channel: ${memberResult.message}")
        }
        val member = (memberResult as VaultResult.Success).data
        if (member.status != "administrator" && member.status != "creator") {
            return VaultResult.Error("Worker bot is not an administrator in the private channel.")
        }

        val workerEntity = WorkerEntity(
            id = UUID.randomUUID().toString(),
            token = token,
            name = botUser.firstName,
            username = botUser.username ?: "",
            isPrimary = false,
            isOnline = true,
            isAuthorized = true,
            hasChannelAccess = true
        )
        database.workerDao().insertWorker(workerEntity)
        reloadWorkers()

        return VaultResult.Success(workerEntity.toDomain())
    }

    suspend fun removeWorker(workerId: String) {
        database.workerDao().deleteWorker(workerId)
        reloadWorkers()
    }

    suspend fun reloadWorkers() {
        val list = database.workerDao().getAllWorkersList().map { it.toDomain() }
        workerScheduler.setWorkers(list)
    }

    suspend fun wipeAllConfiguration() {
        secureStorage.clearAllCredentials()
        database.fileDao().deleteAllFiles()
        database.chunkDao().deleteAllChunks()
        database.transferDao().deleteAllTransfers()
        database.workerDao().deleteAllWorkers()
        chunkEngine.cleanTempChunks()
    }

    fun uploadFile(file: File): String {
        return uploadManager.enqueueUpload(file)
    }

    fun downloadFile(vaultFile: VaultFile, destinationDir: File? = null): String {
        return downloadManager.enqueueDownload(vaultFile, destinationDir)
    }

    fun getStreamUrl(fileId: String): String {
        return streamingServer.getStreamUrl(fileId)
    }

    fun prewarmMedia(vaultFile: VaultFile) {
        streamingServer.prewarmMedia(vaultFile)
    }
}
