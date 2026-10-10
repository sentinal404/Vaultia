package com.vaultia.app.engine.bridge

import android.content.Context
import android.util.Log
import com.vaultia.app.core.VaultResult
import com.vaultia.app.core.security.SecureStorageManager
import com.vaultia.app.data.local.VaultiaDatabase
import com.vaultia.app.data.local.entity.ChunkEntity
import com.vaultia.app.data.local.entity.FileEntity
import com.vaultia.app.data.remote.TelegramApiService
import com.vaultia.app.domain.model.MediaType
import com.vaultia.app.domain.model.TransferStatus
import com.vaultia.app.domain.model.VaultFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class BridgeServerState(
    val isRunning: Boolean = false,
    val remainingSeconds: Int = 0,
    val receivedFilesCount: Int = 0,
    val lastReceivedFileName: String? = null,
    val listeningBotNames: List<String> = emptyList()
)

class TelegramBridgeServer(
    private val context: Context,
    private val database: VaultiaDatabase,
    private val telegramApi: TelegramApiService,
    private val secureStorage: SecureStorageManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverJob: Job? = null
    private var timerJob: Job? = null

    private val _serverState = MutableStateFlow(BridgeServerState())
    val serverState: StateFlow<BridgeServerState> = _serverState.asStateFlow()

    private val lastOffsets = ConcurrentHashMap<String, Long>()
    private val processedMessageIds = ConcurrentHashMap.newKeySet<String>()

    fun startServer(durationMinutes: Int = 10) {
        if (_serverState.value.isRunning) return

        val primaryToken = secureStorage.botToken ?: return
        val channelId = secureStorage.channelId ?: return

        val totalSeconds = durationMinutes * 60
        _serverState.value = _serverState.value.copy(
            isRunning = true,
            remainingSeconds = totalSeconds,
            lastReceivedFileName = null
        )

        // Countdown timer job
        timerJob = scope.launch {
            var remaining = totalSeconds
            while (isActive && remaining > 0) {
                delay(1000L)
                remaining--
                _serverState.value = _serverState.value.copy(remainingSeconds = remaining)
            }
            if (isActive && remaining <= 0) {
                stopServer()
            }
        }

        // Server worker polling loop
        serverJob = scope.launch {
            // Gather all bot tokens (Primary + Worker bots)
            val workers = database.workerDao().getAllWorkersList().filter { it.isAuthorized }
            val botTokens = mutableListOf(Pair("Primary Bot", primaryToken))
            workers.forEach { worker ->
                botTokens.add(Pair(worker.name, worker.token))
            }

            // DEDUPLICATE: Make sure each token is polled at most once to prevent duplicate processing!
            val uniqueBots = botTokens.distinctBy { it.second }

            _serverState.value = _serverState.value.copy(
                listeningBotNames = uniqueBots.map { it.first }
            )

            // Spawn parallel listener for each unique bot
            uniqueBots.forEach { (botName, token) ->
                launch {
                    runBotListener(botName, token, channelId)
                }
            }
        }
    }

    private suspend fun runBotListener(botName: String, token: String, channelId: String) {
        while (scope.isActive && _serverState.value.isRunning) {
            try {
                val currentOffset = lastOffsets[token]
                val updatesResult = telegramApi.getUpdates(
                    token = token,
                    offset = if (currentOffset != null) currentOffset + 1 else null,
                    limit = 50,
                    timeout = 15
                )

                if (updatesResult is VaultResult.Success) {
                    val updates = updatesResult.data
                    for (update in updates) {
                        lastOffsets[token] = update.updateId

                        val msg = update.message ?: update.channelPost ?: continue
                        // Prevent duplicate handling of the same incoming message
                        val dedupeKey = "${msg.chat.id}_${msg.messageId}"
                        if (!processedMessageIds.add(dedupeKey)) continue

                        processIncomingMessage(token, channelId, msg)
                    }
                } else {
                    delay(3000L)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error in bot listener ($botName): ${e.message}")
                delay(4000L)
            }
        }
    }

    private suspend fun processIncomingMessage(
        token: String,
        channelId: String,
        msg: com.vaultia.app.data.remote.MessageDto
    ) {
        val chatId = msg.chat.id
        // Ignore messages originating from the destination channel to prevent feedback loops
        if (msg.chat.type == "channel") return

        val doc = msg.document
        val video = msg.video
        val anim = msg.animation
        val audio = msg.audio
        val photo = msg.photo?.maxByOrNull { it.width * it.height }
        val voice = msg.voice

        // Check if message contains a downloadable media/file
        val (fileId, fileName, rawFileSize, mimeType) = when {
            doc != null -> {
                val name = doc.fileName ?: "doc_${System.currentTimeMillis()}.${doc.mimeType?.substringAfter('/') ?: "bin"}"
                Quadruple(doc.fileId, name, doc.fileSize ?: 0L, doc.mimeType ?: "application/octet-stream")
            }
            video != null -> {
                val name = video.fileName ?: "video_${System.currentTimeMillis()}.mp4"
                Quadruple(video.fileId, name, video.fileSize ?: 0L, video.mimeType ?: "video/mp4")
            }
            anim != null -> {
                val name = anim.fileName ?: "animation_${System.currentTimeMillis()}.mp4"
                Quadruple(anim.fileId, name, anim.fileSize ?: 0L, anim.mimeType ?: "video/mp4")
            }
            audio != null -> {
                val name = audio.fileName ?: "${audio.title ?: "audio"}_${System.currentTimeMillis()}.mp3"
                Quadruple(audio.fileId, name, audio.fileSize ?: 0L, audio.mimeType ?: "audio/mpeg")
            }
            photo != null -> {
                val name = "IMG_${System.currentTimeMillis()}.jpg"
                Quadruple(photo.fileId, name, photo.fileSize ?: 0L, "image/jpeg")
            }
            voice != null -> {
                val name = "VOICE_${System.currentTimeMillis()}.ogg"
                Quadruple(voice.fileId, name, voice.fileSize ?: 0L, voice.mimeType ?: "audio/ogg")
            }
            else -> return // Text message only or non-media
        }

        var fileSize = rawFileSize
        if (fileSize <= 0L) {
            try {
                val fileInfo = telegramApi.getFile(token, fileId).getOrNull()
                if (fileInfo?.fileSize != null && fileInfo.fileSize > 0L) {
                    fileSize = fileInfo.fileSize
                }
            } catch (_: Exception) {}
        }

        try {
            val cleanCaption = "📁 Vaultia Cloud • $fileName (${VaultFile.formatBytes(fileSize)})"
            // Forward / Copy message to user's private channel with clean Vaultia caption (strips external ad captions!)
            val copyResult = telegramApi.copyMessage(
                token = token,
                chatId = channelId,
                fromChatId = chatId,
                messageId = msg.messageId,
                caption = cleanCaption
            )

            if (copyResult is VaultResult.Success) {
                val remoteMessageId = copyResult.data.messageId
                val mediaType = MediaType.fromMimeType(mimeType, fileName)
                val fileIdUnique = UUID.randomUUID().toString()

                val vaultFile = VaultFile(
                    id = fileIdUnique,
                    name = fileName,
                    size = fileSize,
                    mimeType = mimeType,
                    mediaType = mediaType,
                    totalChunks = 1,
                    chunkSize = if (fileSize > 0) fileSize else 1024L,
                    checksum = "",
                    createdAt = System.currentTimeMillis(),
                    modifiedAt = System.currentTimeMillis()
                )

                // Persist into database with token as workerId so downloading knows which bot owns the file
                database.fileDao().insertFile(FileEntity.fromDomain(vaultFile))
                database.chunkDao().insertChunks(
                    listOf(
                        ChunkEntity(
                            id = UUID.randomUUID().toString(),
                            fileId = fileIdUnique,
                            chunkIndex = 0,
                            totalChunks = 1,
                            size = fileSize,
                            checksum = "",
                            status = TransferStatus.COMPLETED.name,
                            remoteMessageId = remoteMessageId,
                            remoteFileId = fileId,
                            workerId = token
                        )
                    )
                )

                // Update server state counter
                val updatedCount = _serverState.value.receivedFilesCount + 1
                _serverState.value = _serverState.value.copy(
                    receivedFilesCount = updatedCount,
                    lastReceivedFileName = fileName
                )

                // Prewarm media streaming URL immediately
                try {
                    com.vaultia.app.VaultiaApplication.instance.streamingServer.prewarmMedia(vaultFile)
                } catch (_: Exception) {}

                // Send confirmation reply back to the Telegram sender
                val replyText = if (fileSize > 20 * 1024 * 1024 && secureStorage.apiId.isNullOrBlank() && secureStorage.customApiUrl.isNullOrBlank()) {
                    "📁 File '$fileName' (${VaultFile.formatBytes(fileSize)}) saved to your Vaultia channel!\n\n💡 Tip: To enable instant zero-buffering streaming of large files (> 20MB) forwarded directly to your bot, configure your Telegram API ID & Hash in Vaultia Settings."
                } else {
                    "✅ File '$fileName' (${VaultFile.formatBytes(fileSize)}) received and ready to stream in Vaultia!"
                }
                telegramApi.sendMessage(
                    token = token,
                    chatId = chatId,
                    text = replyText
                )
                // Sync manifest to Telegram cloud so newly received file is backed up in index
                try {
                    com.vaultia.app.VaultiaApplication.instance.cloudSyncManager.syncCatalogToCloud()
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bridge incoming message: ${e.message}", e)
        }
    }

    fun extendServer(additionalMinutes: Int = 10) {
        val currentRemaining = _serverState.value.remainingSeconds
        _serverState.value = _serverState.value.copy(
            remainingSeconds = currentRemaining + (additionalMinutes * 60)
        )
    }

    fun stopServer() {
        timerJob?.cancel()
        timerJob = null
        serverJob?.cancel()
        serverJob = null

        _serverState.value = _serverState.value.copy(
            isRunning = false,
            remainingSeconds = 0
        )
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    companion object {
        private const val TAG = "TelegramBridgeServer"
    }
}
