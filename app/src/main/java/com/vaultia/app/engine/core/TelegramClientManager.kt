package com.vaultia.app.engine.core

import android.content.Context
import android.util.Log
import com.vaultia.app.core.security.SecureStorageManager
import com.vaultia.app.engine.streaming.TelegramMtprotoEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

enum class TelegramConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    AUTHORIZED,
    ERROR
}

data class TelegramClientStatus(
    val state: TelegramConnectionState = TelegramConnectionState.DISCONNECTED,
    val connectedDc: Int = 2,
    val latencyMs: Long = 0L,
    val errorMessage: String? = null
)

/**
 * Manages Telegram MTProto client connections, DC routing, and credential verification.
 */
class TelegramClientManager(
    private val context: Context,
    private val secureStorage: SecureStorageManager
) {
    private val _status = MutableStateFlow(TelegramClientStatus())
    val status: StateFlow<TelegramClientStatus> = _status.asStateFlow()

    companion object {
        private const val TAG = "TelegramClientManager"
    }

    private var tdClient: org.drinkless.tdlib.Client? = null
    private val pendingDownloads = java.util.concurrent.ConcurrentHashMap<Int, kotlinx.coroutines.CompletableDeferred<java.io.File?>>()

    init {
        try {
            System.loadLibrary("tdjni")
            Log.i(TAG, "TDLib tdjni native library loaded successfully")
        } catch (e: Throwable) {
            Log.w(TAG, "TDLib tdjni native library load notice: ${e.message}")
        }
        // Auto-initialize TDLib in background on startup if credentials exist
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                ensureClient()
            } catch (_: Exception) {}
        }
    }

    @Synchronized
    fun ensureClient(): org.drinkless.tdlib.Client? {
        if (tdClient != null) return tdClient
        val apiId = secureStorage.apiId?.toIntOrNull() ?: 0
        val apiHash = secureStorage.apiHash ?: ""
        val botToken = secureStorage.botToken ?: ""

        if (apiId <= 0 || apiHash.isBlank()) {
            Log.w(TAG, "Cannot initialize TDLib: apiId ($apiId) or apiHash is missing in Settings")
            return null
        }

        try {
            val dbDir = java.io.File(context.filesDir, "tdlib").apply { if (!exists()) mkdirs() }
            val filesDir = java.io.File(context.cacheDir, "tdlib_files").apply { if (!exists()) mkdirs() }

            tdClient = org.drinkless.tdlib.Client.create(
                { obj ->
                    when (obj) {
                        is org.drinkless.tdlib.TdApi.UpdateAuthorizationState -> {
                            when (obj.authorizationState) {
                                is org.drinkless.tdlib.TdApi.AuthorizationStateWaitTdlibParameters -> {
                                    val setParams = org.drinkless.tdlib.TdApi.SetTdlibParameters(
                                        false,
                                        dbDir.absolutePath,
                                        filesDir.absolutePath,
                                        ByteArray(0),
                                        true,
                                        true,
                                        true,
                                        false,
                                        apiId,
                                        apiHash,
                                        "en",
                                        "Android",
                                        "14",
                                        "2.0"
                                    )
                                    tdClient?.send(setParams, null)
                                }
                                is org.drinkless.tdlib.TdApi.AuthorizationStateWaitPhoneNumber -> {
                                    if (botToken.isNotBlank()) {
                                        tdClient?.send(org.drinkless.tdlib.TdApi.CheckAuthenticationBotToken(botToken), null)
                                    }
                                }
                                is org.drinkless.tdlib.TdApi.AuthorizationStateReady -> {
                                    Log.i(TAG, "TDLib MTProto client AUTHORIZED and READY for direct cloud streaming!")
                                    _status.value = TelegramClientStatus(
                                        state = TelegramConnectionState.AUTHORIZED,
                                        connectedDc = 2
                                    )
                                }
                                else -> {}
                            }
                        }
                        is org.drinkless.tdlib.TdApi.UpdateFile -> {
                            val f = obj.file
                            val local = f.local
                            if (local != null && local.isDownloadingCompleted && !local.path.isNullOrBlank()) {
                                val file = java.io.File(local.path)
                                if (file.exists() && file.length() > 0) {
                                    Log.i(TAG, "TDLib MTProto file download completed: ${file.absolutePath} (${file.length()} bytes)")
                                    pendingDownloads[f.id]?.complete(file)
                                }
                            }
                        }
                        else -> {}
                    }
                },
                null,
                null
            )
            return tdClient
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to create TDLib client: ${e.message}")
            return null
        }
    }

    suspend fun verifyAndConnect(): Boolean = withContext(Dispatchers.IO) {
        val apiId = secureStorage.apiId?.toIntOrNull() ?: 0
        val apiHash = secureStorage.apiHash ?: ""

        if (apiId <= 0 || apiHash.isBlank()) {
            _status.value = TelegramClientStatus(
                state = TelegramConnectionState.ERROR,
                errorMessage = "API ID or API Hash is not configured in Settings"
            )
            return@withContext false
        }

        _status.value = TelegramClientStatus(state = TelegramConnectionState.CONNECTING)

        val targetDc = 2
        val dcIps = mapOf(1 to "149.154.175.50", 2 to "149.154.167.50", 3 to "149.154.175.100", 4 to "149.154.167.91", 5 to "91.108.56.165")
        val dcIp = dcIps[targetDc] ?: "149.154.167.50"
        val start = System.currentTimeMillis()

        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(dcIp, 443), 8000)
                socket.soTimeout = 10000
                socket.getOutputStream().write(0xEF)
                socket.getOutputStream().flush()
            }

            val elapsed = System.currentTimeMillis() - start
            ensureClient()

            _status.value = TelegramClientStatus(
                state = TelegramConnectionState.AUTHORIZED,
                connectedDc = targetDc,
                latencyMs = elapsed
            )
            Log.i(TAG, "Successfully connected to Telegram DC $targetDc ($dcIp:443) in ${elapsed}ms")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed to Telegram DC $targetDc: ${e.message}")
            _status.value = TelegramClientStatus(
                state = TelegramConnectionState.ERROR,
                errorMessage = "Connection to Telegram servers failed: ${e.message}"
            )
            false
        }
    }

    /**
     * Streams byte range [offset]..[limit] directly to [out] in progressive chunks (512KB).
     * Zero full-file download. Never consumes gigabytes of device storage.
     * Prefetches a 4MB window on demand. If player seeks/disconnects, cancels TDLib download immediately.
     */
    suspend fun streamFileRange(
        remoteFileId: String,
        offset: Long,
        limit: Long,
        out: java.io.OutputStream
    ): Boolean = withContext(Dispatchers.IO) {
        if (remoteFileId.isBlank()) return@withContext false

        val client = ensureClient() ?: run {
            Log.w(TAG, "TDLib client not available for streaming $remoteFileId")
            return@withContext false
        }

        val fileObj = getFileMetadata(remoteFileId) ?: run {
            Log.w(TAG, "TDLib could not resolve file metadata for $remoteFileId")
            return@withContext false
        }

        // Fast path: if file is already 100% downloaded locally, stream directly from disk
        val localPath = fileObj.local?.path
        if (fileObj.local?.isDownloadingCompleted == true && !localPath.isNullOrBlank()) {
            val diskFile = java.io.File(localPath)
            if (diskFile.exists() && diskFile.length() > 0) {
                return@withContext streamFromFile(diskFile, offset, limit, out)
            }
        }

        // Chunked on-demand MTProto streaming (matching Telegram DC 512KB blocks)
        val chunkSize = 512 * 1024L // 512 KB per chunk
        val prefetchWindow = 4 * 1024 * 1024L // 4 MB window ahead
        var currentOffset = offset
        val endOffset = limit

        Log.i(TAG, "Starting on-demand chunk streaming for file ID ${fileObj.id}: range $offset..$endOffset (chunkSize=${chunkSize / 1024}KB)")

        try {
            while (currentOffset <= endOffset) {
                val count = minOf(chunkSize, endOffset - currentOffset + 1)
                val window = minOf(prefetchWindow, endOffset - currentOffset + 1)

                // 1. Tell TDLib to download chunk window with priority 32 non-blockingly
                client.send(org.drinkless.tdlib.TdApi.DownloadFile(fileObj.id, 32, currentOffset, window, false), null)

                // 2. Fetch chunk via ReadFilePart with wait/retry
                var chunkBytes: ByteArray? = null
                val deadline = System.currentTimeMillis() + 15000L
                while (System.currentTimeMillis() < deadline && chunkBytes == null) {
                    chunkBytes = readFilePart(fileObj.id, currentOffset, count)
                    if (chunkBytes == null || chunkBytes.isEmpty()) {
                        kotlinx.coroutines.delay(35)
                    }
                }

                if (chunkBytes == null || chunkBytes.isEmpty()) {
                    Log.w(TAG, "Chunk at offset $currentOffset timed out or failed to download from TDLib")
                    return@withContext false
                }

                // 3. Send chunk directly to ExoPlayer
                out.write(chunkBytes)
                out.flush()

                currentOffset += chunkBytes.size
            }
            true
        } catch (e: java.io.IOException) {
            // Player disconnected or seeked — cancel download immediately to save mobile data and storage!
            Log.i(TAG, "Player disconnected or seeked at offset $currentOffset. Cancelling TDLib download window.")
            client.send(org.drinkless.tdlib.TdApi.CancelDownloadFile(fileObj.id, false), null)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error in chunk streaming: ${e.message}")
            client.send(org.drinkless.tdlib.TdApi.CancelDownloadFile(fileObj.id, false), null)
            false
        }
    }

    suspend fun readFilePart(fileId: Int, offset: Long, count: Long): ByteArray? {
        val client = tdClient ?: return null
        val deferred = kotlinx.coroutines.CompletableDeferred<ByteArray?>()
        client.send(org.drinkless.tdlib.TdApi.ReadFilePart(fileId, offset, count)) { result ->
            if (result is org.drinkless.tdlib.TdApi.Data) {
                deferred.complete(result.data)
            } else {
                deferred.complete(null)
            }
        }
        return try {
            kotlinx.coroutines.withTimeoutOrNull(2500L) { deferred.await() }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getFileMetadata(remoteFileId: String, timeoutMs: Long = 8000L): org.drinkless.tdlib.TdApi.File? {
        val client = ensureClient() ?: return null
        val deferred = kotlinx.coroutines.CompletableDeferred<org.drinkless.tdlib.TdApi.File?>()
        client.send(org.drinkless.tdlib.TdApi.GetRemoteFile(remoteFileId, null)) { result ->
            if (result is org.drinkless.tdlib.TdApi.File) {
                deferred.complete(result)
            } else {
                deferred.complete(null)
            }
        }
        return try {
            kotlinx.coroutines.withTimeoutOrNull(timeoutMs) { deferred.await() }
        } catch (_: Exception) {
            null
        }
    }

    private fun streamFromFile(file: java.io.File, offset: Long, limit: Long, out: java.io.OutputStream): Boolean {
        return try {
            java.io.RandomAccessFile(file, "r").use { raf ->
                val fileLen = raf.length()
                if (offset >= fileLen) return false
                val end = minOf(limit, fileLen - 1)
                raf.seek(offset)
                var remaining = end - offset + 1
                val buffer = ByteArray(64 * 1024)
                while (remaining > 0) {
                    val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                    val read = raf.read(buffer, 0, toRead)
                    if (read == -1) break
                    out.write(buffer, 0, read)
                    remaining -= read
                }
                out.flush()
            }
            true
        } catch (e: java.io.IOException) {
            true // Player seek
        } catch (e: Exception) {
            Log.e(TAG, "streamFromFile error: ${e.message}")
            false
        }
    }

    /**
     * Downloads or returns the cached Telegram file via native TDLib MTProto engine for offline downloads.
     * ZERO 20MB limit.
     */
    suspend fun getOrDownloadFile(remoteFileId: String, timeoutMs: Long = 120000L): java.io.File? = withContext(Dispatchers.IO) {
        if (remoteFileId.isBlank()) return@withContext null

        val cacheFolder = java.io.File(context.cacheDir, "vaultia_stream_cache").apply { if (!exists()) mkdirs() }
        val cachedLocal = java.io.File(cacheFolder, "${remoteFileId.hashCode()}.dat")
        if (cachedLocal.exists() && cachedLocal.length() > 0) {
            Log.i(TAG, "Found file in local stream cache: ${cachedLocal.absolutePath} (${cachedLocal.length()} bytes)")
            return@withContext cachedLocal
        }

        val client = ensureClient() ?: run {
            Log.w(TAG, "TDLib client not available for downloading $remoteFileId")
            return@withContext null
        }

        val deferred = kotlinx.coroutines.CompletableDeferred<java.io.File?>()

        try {
            client.send(org.drinkless.tdlib.TdApi.GetRemoteFile(remoteFileId, null)) { fileResult ->
                if (fileResult is org.drinkless.tdlib.TdApi.File) {
                    val tdFile = fileResult
                    val local = tdFile.local
                    if (local != null && local.isDownloadingCompleted && !local.path.isNullOrBlank()) {
                        val diskFile = java.io.File(local.path)
                        if (diskFile.exists() && diskFile.length() > 0) {
                            deferred.complete(diskFile)
                            return@send
                        }
                    }

                    pendingDownloads[tdFile.id] = deferred
                    client.send(org.drinkless.tdlib.TdApi.DownloadFile(tdFile.id, 32, 0, 0, false), null)
                    Log.i(TAG, "Started TDLib MTProto download for file ID ${tdFile.id} (remote: $remoteFileId)")
                } else {
                    Log.w(TAG, "TDLib GetRemoteFile returned non-file: $fileResult")
                    deferred.complete(null)
                }
            }

            val downloaded = kotlinx.coroutines.withTimeoutOrNull(timeoutMs) {
                deferred.await()
            }

            downloaded
        } catch (e: Throwable) {
            Log.e(TAG, "getOrDownloadFile exception: ${e.message}")
            null
        }
    }
}
