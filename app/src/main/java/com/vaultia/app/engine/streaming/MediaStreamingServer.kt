package com.vaultia.app.engine.streaming

import android.content.Context
import android.util.Log
import com.vaultia.app.core.Constants
import com.vaultia.app.core.VaultResult
import com.vaultia.app.core.security.SecureStorageManager
import com.vaultia.app.data.local.VaultiaDatabase
import com.vaultia.app.data.local.entity.ChunkEntity
import com.vaultia.app.data.remote.TelegramApiService
import com.vaultia.app.domain.model.VaultFile
import com.vaultia.app.engine.transfer.WorkerScheduler
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.RandomAccessFile
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * High-performance, zero-buffering HTTP range-streaming server.
 * Uses direct Telegram HTTP 206 range proxying (inspired by MeriAwaaz architecture)
 * to stream video and audio instantaneously (~150ms start time) without requiring
 * full chunk or file downloads to disk or RAM.
 */
class MediaStreamingServer(
    private val context: Context,
    private val database: VaultiaDatabase,
    private val telegramApi: TelegramApiService,
    private val secureStorage: SecureStorageManager,
    private val workerScheduler: WorkerScheduler
) {

    private val mtprotoEngine = TelegramMtprotoEngine(context, secureStorage)

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.w(TAG, "Streaming server caught unhandled coroutine exception: ${throwable.message}")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    private var serverSocket: ServerSocket? = null
    var serverPort: Int = 0
        private set

    private val cacheDir: File = File(context.cacheDir, "vaultia_stream_cache").apply {
        if (!exists()) mkdirs()
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // In-memory cache for resolved Telegram download URLs (45-minute TTL, matching MeriAwaaz)
    private data class CachedTgUrl(
        val url: String,
        val fileSize: Long,
        val expiresAt: Long
    )
    private val tgUrlCache = ConcurrentHashMap<String, CachedTgUrl>()
    private val tgUrlTtlMs = 45 * 60 * 1000L

    fun start(): Int {
        if (serverSocket != null && !serverSocket!!.isClosed) {
            return serverPort
        }

        try {
            val socket = ServerSocket(0)
            serverSocket = socket
            serverPort = socket.localPort

            scope.launch {
                while (!socket.isClosed) {
                    try {
                        val client = socket.accept()
                        scope.launch {
                            handleClient(client)
                        }
                    } catch (e: Exception) {
                        if (socket.isClosed) break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MediaStreamingServer", e)
        }
        return serverPort
    }

    fun getStreamUrl(fileId: String): String {
        val port = if (serverPort == 0) start() else serverPort
        return "http://${Constants.LOCAL_STREAMING_HOST}:$port/stream?fileId=$fileId"
    }

    /**
     * Prewarm: pre-resolve CDN URL for the first chunk so playback starts instantly.
     * Uses MTProto engine's CDN resolver — does NOT call Bot API getFile here.
     */
    fun prewarmMedia(vaultFile: VaultFile) {
        scope.launch {
            try {
                val chunks = database.chunkDao().getChunksForFileList(vaultFile.id).sortedBy { it.chunkIndex }
                val first = chunks.firstOrNull() ?: return@launch
                if (!first.remoteFileId.isNullOrBlank()) {
                    // Fire-and-forget: resolve CDN URL and cache it for instant playback
                    mtprotoEngine.streamRange(first.remoteFileId, 0L, 0L, java.io.ByteArrayOutputStream())
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error in prewarmMedia: ${e.message}")
            }
        }
    }

    private suspend fun handleClient(clientSocket: Socket) {
        try {
            clientSocket.use { socket ->
                socket.soTimeout = 20000
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val out = socket.getOutputStream()

                val requestLine = reader.readLine() ?: return
                val parts = requestLine.split(" ")
                if (parts.size < 2) return

                val pathWithQuery = parts[1]
                if (!pathWithQuery.startsWith("/stream")) {
                    sendNotFound(out)
                    return
                }

                val fileId = pathWithQuery.substringAfter("fileId=").substringBefore("&")
                val fileEntity = database.fileDao().getFileById(fileId) ?: run {
                    sendNotFound(out)
                    return
                }
                val vaultFile = fileEntity.toDomain()

                // Parse Range header if present
                var rangeHeader: String? = null
                var headerLine: String?
                while (reader.readLine().also { headerLine = it } != null) {
                    if (headerLine.isNullOrBlank()) break
                    if (headerLine!!.startsWith("Range: bytes=", ignoreCase = true)) {
                        rangeHeader = headerLine!!.substringAfter("Range: bytes=").trim()
                    }
                }

                serveRange(vaultFile, rangeHeader, out)
            }
        } catch (e: IOException) {
            // Client socket disconnect or reset during streaming/seek is expected behavior
        } catch (e: Exception) {
            Log.w(TAG, "Non-fatal error in handleClient: ${e.message}")
        }
    }

    private suspend fun serveRange(vaultFile: VaultFile, rangeHeader: String?, out: OutputStream) {
        val chunks = database.chunkDao().getChunksForFileList(vaultFile.id).sortedBy { it.chunkIndex }
        val isLocal = !vaultFile.localUri.isNullOrBlank() && File(vaultFile.localUri).let { it.exists() && it.length() > 0 }

        var totalSize = vaultFile.size
        if (totalSize <= 0L && chunks.isNotEmpty()) {
            totalSize = chunks.sumOf { it.size }
        }

        var resolvedDlUrl: String? = null
        val firstChunk = chunks.firstOrNull()

        val hasMtproto = !secureStorage.apiId.isNullOrBlank() && !secureStorage.apiHash.isNullOrBlank()

        if (!isLocal) {
            if (firstChunk == null || firstChunk.remoteFileId.isNullOrBlank()) {
                Log.e(TAG, "Cannot stream: No chunks or remoteFileId is missing for file ${vaultFile.id}")
                sendNotFound(out)
                return
            }
            if (hasMtproto) {
                Log.i(TAG, "Direct MTProto active for file ${vaultFile.id}. Bypassing Bot API getFile completely to avoid 20MB limit.")
                if (totalSize <= 0L) {
                    totalSize = if (vaultFile.size > 0L) vaultFile.size else if (firstChunk.size > 0L) firstChunk.size else 500 * 1024 * 1024L
                }
            } else {
                val dlInfo = getTgDownloadUrl(firstChunk.remoteFileId, firstChunk.workerId)
                if (dlInfo == null) {
                    if (!secureStorage.customApiUrl.isNullOrBlank()) {
                        Log.i(TAG, "Bot API getFile returned null for ${firstChunk.remoteFileId}. Routing via custom bridge...")
                    } else {
                        Log.e(TAG, "Cannot stream: Failed to resolve Telegram download URL for ${firstChunk.remoteFileId}")
                        sendBadGateway(out)
                        return
                    }
                } else {
                    resolvedDlUrl = dlInfo.first
                    if (totalSize <= 0L && dlInfo.second > 0L) {
                        totalSize = dlInfo.second
                        try {
                            val entity = database.fileDao().getFileById(vaultFile.id)
                            if (entity != null) {
                                database.fileDao().updateFile(entity.copy(size = totalSize, chunkSize = totalSize))
                            }
                            database.chunkDao().updateChunk(firstChunk.copy(size = totalSize))
                        } catch (_: Exception) {}
                    }
                }
            }
        }

        if (totalSize <= 0L) {
            Log.e(TAG, "Cannot stream: totalSize is unknown/zero for file ${vaultFile.id}")
            sendBadGateway(out)
            return
        }

        var startByte = 0L
        var endByte = totalSize - 1

        if (!rangeHeader.isNullOrBlank()) {
            if (rangeHeader.startsWith("-")) {
                val suffixLen = rangeHeader.substring(1).toLongOrNull() ?: 0L
                startByte = (totalSize - suffixLen).coerceAtLeast(0L)
                endByte = totalSize - 1
            } else {
                val rangeParts = rangeHeader.split("-")
                startByte = rangeParts[0].toLongOrNull() ?: 0L
                if (rangeParts.size > 1 && rangeParts[1].isNotBlank()) {
                    endByte = (rangeParts[1].toLongOrNull() ?: (totalSize - 1)).coerceAtMost(totalSize - 1)
                } else {
                    endByte = totalSize - 1
                }
            }
        }

        startByte = startByte.coerceIn(0L, totalSize - 1)
        endByte = endByte.coerceIn(startByte, totalSize - 1)
        val contentLength = endByte - startByte + 1

        val headers = StringBuilder().apply {
            if (rangeHeader != null) {
                append("HTTP/1.1 206 Partial Content\r\n")
                append("Content-Range: bytes $startByte-$endByte/$totalSize\r\n")
            } else {
                append("HTTP/1.1 200 OK\r\n")
            }
            append("Accept-Ranges: bytes\r\n")
            append("Content-Length: $contentLength\r\n")
            append("Content-Type: ${vaultFile.mimeType.ifBlank { "video/mp4" }}\r\n")
            append("Connection: close\r\n")
            append("\r\n")
        }.toString()

        try {
            out.write(headers.toByteArray(Charsets.UTF_8))
            out.flush()
        } catch (e: IOException) {
            return
        }

        // Fast path 1: If original file exists locally on device storage
        if (isLocal) {
            val localFile = File(vaultFile.localUri!!)
            streamFromFile(localFile, startByte, endByte, out)
            return
        }

        // Fast path 2: Single-chunk file (Telegram Bot share directly bridged or MTProto)
        if (chunks.size == 1) {
            val cachedFile = File(cacheDir, "${vaultFile.id}_0.chunk")
            if (cachedFile.exists() && cachedFile.length() > 0) {
                streamFromFile(cachedFile, startByte, endByte, out)
            } else if (hasMtproto && firstChunk != null && !firstChunk.remoteFileId.isNullOrBlank()) {
                Log.i(TAG, "Streaming file ${vaultFile.id} directly via Telegram MTProto engine (zero bot limit)...")
                mtprotoEngine.streamRange(firstChunk.remoteFileId, startByte, endByte, out)
            } else if (resolvedDlUrl != null) {
                streamRangeFromTelegram(resolvedDlUrl, startByte, endByte, out)
            } else if (firstChunk != null && !firstChunk.remoteFileId.isNullOrBlank()) {
                Log.i(TAG, "Streaming single-chunk file ${vaultFile.id} via MTProto engine...")
                mtprotoEngine.streamRange(firstChunk.remoteFileId, startByte, endByte, out)
            }
            return
        }

        // Multi-chunk streaming logic (matching MeriAwaaz social.js:1366-1433)
        if (chunks.isNotEmpty()) {
            var currentByte = 0L
            for (chunk in chunks) {
                val chunkSize = if (chunk.size > 0) chunk.size else totalSize
                val chunkStart = currentByte
                val chunkEnd = currentByte + chunkSize - 1

                if (chunkEnd >= startByte && chunkStart <= endByte) {
                    val chunkOffset = (startByte - chunkStart).coerceAtLeast(0L)
                    val chunkLimit = (endByte - chunkStart).coerceAtMost(chunkSize - 1)

                    val chunkKey = "${vaultFile.id}_${chunk.chunkIndex}"
                    val cachedFile = File(cacheDir, "$chunkKey.chunk")

                    if (cachedFile.exists() && cachedFile.length() > 0) {
                        streamFromFile(cachedFile, chunkOffset, chunkLimit, out)
                    } else {
                        val remoteFileId = chunk.remoteFileId
                        if (!remoteFileId.isNullOrBlank()) {
                            if (hasMtproto) {
                                mtprotoEngine.streamRange(remoteFileId, chunkOffset, chunkLimit, out)
                            } else {
                                val chunkDlUrl = if (chunk.chunkIndex == 0 && resolvedDlUrl != null) {
                                    resolvedDlUrl
                                } else {
                                    getTgDownloadUrl(remoteFileId, chunk.workerId)?.first
                                }
                                if (chunkDlUrl != null) {
                                    streamRangeFromTelegram(chunkDlUrl, chunkOffset, chunkLimit, out)
                                } else {
                                    mtprotoEngine.streamRange(remoteFileId, chunkOffset, chunkLimit, out)
                                }
                            }
                        }
                    }
                }

                currentByte += chunkSize
                if (currentByte > endByte) break
            }
            return
        }
    }

    /**
     * Reads a byte range from a local file and writes directly to socket output stream.
     */
    private fun streamFromFile(file: File, offset: Long, limit: Long, out: OutputStream) {
        val bytesToRead = limit - offset + 1
        if (bytesToRead <= 0) return

        try {
            RandomAccessFile(file, "r").use { raf ->
                raf.seek(offset)
                val buffer = ByteArray(64 * 1024)
                var remaining = bytesToRead
                while (remaining > 0) {
                    val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                    val read = raf.read(buffer, 0, toRead)
                    if (read == -1) break
                    out.write(buffer, 0, read)
                    remaining -= read
                }
                out.flush()
            }
        } catch (_: IOException) {
            // Client closed stream / seek aborted
        }
    }

    /**
     * Directly proxies HTTP Range request to Telegram file server.
     * Telegram natively responds with HTTP 206 Partial Content, returning only the requested
     * byte slice (~100-200ms latency) instead of waiting for entire file download.
     */
    private fun streamRangeFromTelegram(
        downloadUrl: String,
        rangeStart: Long,
        rangeEnd: Long,
        out: OutputStream
    ) {
        val request = Request.Builder()
            .url(downloadUrl)
            .addHeader("Range", "bytes=$rangeStart-$rangeEnd")
            .addHeader("Accept-Encoding", "identity")
            .addHeader("User-Agent", "Vaultia-Streamer/1.0")
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body ?: return
                val inputStream = body.byteStream()

                if (response.code == 206) {
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        out.write(buffer, 0, bytesRead)
                    }
                    out.flush()
                } else if (response.code == 200) {
                    // Fallback slicing for endpoints that return 200 instead of 206
                    var bytesPassed = 0L
                    val targetBytes = rangeEnd - rangeStart + 1
                    var bytesWritten = 0L
                    val buffer = ByteArray(64 * 1024)
                    var read: Int

                    while (inputStream.read(buffer).also { read = it } != -1) {
                        var sliceStart = 0
                        var sliceEnd = read

                        if (bytesPassed < rangeStart) {
                            if (bytesPassed + read <= rangeStart) {
                                bytesPassed += read
                                continue
                            } else {
                                sliceStart = (rangeStart - bytesPassed).toInt()
                            }
                        }

                        if (bytesPassed + read > rangeEnd + 1) {
                            sliceEnd = ((rangeEnd + 1) - bytesPassed).toInt()
                        }

                        if (sliceStart < sliceEnd) {
                            val count = sliceEnd - sliceStart
                            out.write(buffer, sliceStart, count)
                            bytesWritten += count
                        }

                        bytesPassed += read
                        if (bytesPassed > rangeEnd || bytesWritten >= targetBytes) {
                            break
                        }
                    }
                    out.flush()
                } else {
                    Log.w(TAG, "Telegram returned HTTP ${response.code} for range $rangeStart-$rangeEnd")
                }
            }
        } catch (_: IOException) {
            // Client closed socket during playback or seek - expected in media players
        } catch (e: Exception) {
            Log.w(TAG, "Error in streamRangeFromTelegram: ${e.message}")
        }
    }

    /**
     * Resolves and caches the Telegram direct download URL with 45-minute TTL.
     */
    private suspend fun getTgDownloadUrl(remoteFileId: String, preferredToken: String?): Pair<String, Long>? {
        if (remoteFileId.isBlank()) return null
        val cached = tgUrlCache[remoteFileId]
        if (cached != null) {
            if (System.currentTimeMillis() < cached.expiresAt) {
                return Pair(cached.url, cached.fileSize)
            } else {
                tgUrlCache.remove(remoteFileId)
            }
        }

        val candidateTokens = mutableListOf<String>()
        if (!preferredToken.isNullOrBlank()) {
            candidateTokens.add(preferredToken)
        }
        secureStorage.botToken?.let { candidateTokens.add(it) }
        try {
            val workers = database.workerDao().getAllWorkersList().filter { it.isAuthorized }
            candidateTokens.addAll(workers.map { it.token })
        } catch (_: Exception) {}

        val uniqueTokens = candidateTokens.distinct()
        for (token in uniqueTokens) {
            try {
                val fileResult = telegramApi.getFile(token, remoteFileId)
                if (fileResult is VaultResult.Success) {
                    val fileDto = fileResult.data
                    val filePath = fileDto.filePath
                    if (!filePath.isNullOrBlank()) {
                        val baseUrl = secureStorage.customApiUrl?.trim()?.removeSuffix("/")?.takeIf { it.isNotBlank() } ?: Constants.TELEGRAM_API_BASE_URL
                        val url = "$baseUrl/file/bot$token/$filePath"
                        val size = fileDto.fileSize ?: 0L
                        tgUrlCache[remoteFileId] = CachedTgUrl(url, size, System.currentTimeMillis() + tgUrlTtlMs)
                        Log.i(TAG, "Successfully resolved download URL for $remoteFileId ($size bytes)")
                        return Pair(url, size)
                    }
                } else if (fileResult is VaultResult.Error) {
                    Log.w(TAG, "getFile failed for fileId $remoteFileId with token ${token.take(8)}...: ${fileResult.message}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Token trial failed for fileId $remoteFileId: ${e.message}")
            }
        }
        return null
    }

    private fun sendNotFound(out: OutputStream) {
        try {
            val response = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
            out.write(response.toByteArray(Charsets.UTF_8))
            out.flush()
        } catch (_: IOException) {}
    }

    private fun sendBadGateway(out: OutputStream) {
        try {
            val response = "HTTP/1.1 502 Bad Gateway\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
            out.write(response.toByteArray(Charsets.UTF_8))
            out.flush()
        } catch (_: IOException) {}
    }

    fun stop() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
    }

    companion object {
        private const val TAG = "MediaStreamingServer"
    }
}
