package com.vaultia.app.engine.streaming

import android.content.Context
import android.util.Log
import com.vaultia.app.core.Constants
import com.vaultia.app.core.VaultResult
import com.vaultia.app.core.security.SecureStorageManager
import com.vaultia.app.data.local.VaultiaDatabase
import com.vaultia.app.data.remote.TelegramApiService
import com.vaultia.app.domain.model.VaultFile
import com.vaultia.app.engine.transfer.WorkerScheduler
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.RandomAccessFile
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

class MediaStreamingServer(
    private val context: Context,
    private val database: VaultiaDatabase,
    private val telegramApi: TelegramApiService,
    private val secureStorage: SecureStorageManager,
    private val workerScheduler: WorkerScheduler
) {

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

    private val chunkMutexes = ConcurrentHashMap<String, Mutex>()

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

    private suspend fun handleClient(clientSocket: Socket) {
        try {
            clientSocket.use { socket ->
                socket.soTimeout = 15000
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

                // Read headers for Range
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
        val totalSize = vaultFile.size
        var startByte = 0L
        var endByte = (totalSize - 1).coerceAtLeast(0L)

        if (!rangeHeader.isNullOrBlank()) {
            if (rangeHeader.startsWith("-")) {
                // Suffix range: bytes=-N (e.g. bytes=-1048576) -> request last N bytes
                val suffixLen = rangeHeader.substring(1).toLongOrNull() ?: 0L
                startByte = (totalSize - suffixLen).coerceAtLeast(0L)
                endByte = (totalSize - 1).coerceAtLeast(0L)
            } else {
                val rangeParts = rangeHeader.split("-")
                startByte = rangeParts[0].toLongOrNull() ?: 0L
                if (rangeParts.size > 1 && rangeParts[1].isNotBlank()) {
                    endByte = rangeParts[1].toLongOrNull() ?: (totalSize - 1)
                } else {
                    endByte = totalSize - 1
                }
            }
        }

        startByte = startByte.coerceIn(0L, (totalSize - 1).coerceAtLeast(0L))
        endByte = endByte.coerceIn(startByte, (totalSize - 1).coerceAtLeast(0L))
        val contentLength = if (totalSize > 0) (endByte - startByte + 1) else 0L

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

        val chunkSize = vaultFile.chunkSize
        val startChunk = (startByte / chunkSize).toInt()
        val endChunk = (endByte / chunkSize).toInt()

        // Aggressive Parallel Prefetching: Start chunk + next 4 chunks in parallel
        val prefetchWindow = 4
        for (next in 0..prefetchWindow) {
            val prefetchChunk = startChunk + next
            if (prefetchChunk < vaultFile.totalChunks) {
                scope.launch { ensureChunkAvailable(vaultFile, prefetchChunk) }
            }
        }
        // If endChunk is at the end of the file (e.g. MP4 moov atom inspection), prefetch it in parallel
        if (endChunk == vaultFile.totalChunks - 1 && vaultFile.totalChunks > 1) {
            scope.launch { ensureChunkAvailable(vaultFile, endChunk) }
        }

        var currentOffset = startByte
        for (chunkIdx in startChunk..endChunk) {
            val chunkFile = ensureChunkAvailable(vaultFile, chunkIdx) ?: break

            // Chain prefetch: as soon as chunkIdx is available, ensure chunkIdx + prefetchWindow is downloading
            val futureChunk = chunkIdx + prefetchWindow
            if (futureChunk < vaultFile.totalChunks) {
                scope.launch { ensureChunkAvailable(vaultFile, futureChunk) }
            }

            val chunkStartByte = chunkIdx * chunkSize
            val readStartInChunk = (currentOffset - chunkStartByte).coerceAtLeast(0L)
            val readEndInChunk = minOf(chunkFile.length() - 1, endByte - chunkStartByte)
            val bytesToStreamFromThisChunk = readEndInChunk - readStartInChunk + 1

            if (bytesToStreamFromThisChunk > 0) {
                try {
                    RandomAccessFile(chunkFile, "r").use { raf ->
                        raf.seek(readStartInChunk)
                        val buffer = ByteArray(64 * 1024)
                        var remaining = bytesToStreamFromThisChunk
                        while (remaining > 0) {
                            val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                            val read = raf.read(buffer, 0, toRead)
                            if (read == -1) break
                            out.write(buffer, 0, read)
                            remaining -= read
                            currentOffset += read
                        }
                        out.flush()
                    }
                } catch (e: IOException) {
                    // Client disconnected or aborted socket (normal in media player range seeking)
                    return
                }
            }
        }
    }

    private suspend fun ensureChunkAvailable(vaultFile: VaultFile, chunkIndex: Int): File? {
        val chunkKey = "${vaultFile.id}_$chunkIndex"
        val mutex = chunkMutexes.getOrPut(chunkKey) { Mutex() }

        return mutex.withLock {
            val cachedFile = File(cacheDir, "$chunkKey.chunk")
            if (cachedFile.exists() && cachedFile.length() > 0) {
                return@withLock cachedFile
            }

            val worker = workerScheduler.acquireWorker()
            val token = worker?.token ?: secureStorage.botToken ?: run {
                workerScheduler.releaseWorker(worker?.id)
                return@withLock null
            }

            try {
                val chunkEntity = database.chunkDao().getChunkByIndex(vaultFile.id, chunkIndex) ?: run {
                    workerScheduler.releaseWorker(worker?.id)
                    return@withLock null
                }
                val remoteFileId = chunkEntity.remoteFileId ?: run {
                    workerScheduler.releaseWorker(worker?.id)
                    return@withLock null
                }

                val fileResult = telegramApi.getFile(token, remoteFileId)
                val filePath = fileResult.getOrNull()?.filePath ?: run {
                    workerScheduler.releaseWorker(worker?.id)
                    return@withLock null
                }

                val tempFile = File(cacheDir, "$chunkKey.downloading")
                val dlResult = FileOutputStream(tempFile).use { fos ->
                    telegramApi.downloadFileChunk(token, filePath, fos)
                }

                if (dlResult is VaultResult.Success) {
                    tempFile.renameTo(cachedFile)
                    worker?.let { workerScheduler.markSuccess(it.id) }
                    cachedFile
                } else {
                    tempFile.delete()
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to download chunk $chunkIndex: ${e.message}")
                null
            } finally {
                workerScheduler.releaseWorker(worker?.id)
            }
        }
    }

    private fun sendNotFound(out: OutputStream) {
        try {
            val response = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n"
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
