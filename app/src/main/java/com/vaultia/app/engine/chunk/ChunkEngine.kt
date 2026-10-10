package com.vaultia.app.engine.chunk

import android.content.Context
import android.net.Uri
import com.vaultia.app.core.Constants
import com.vaultia.app.domain.model.FileChunk
import com.vaultia.app.domain.model.MediaType
import com.vaultia.app.domain.model.TransferStatus
import com.vaultia.app.domain.model.VaultFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.UUID

class ChunkEngine(private val context: Context) {

    private val tempDir: File = File(context.cacheDir, "vaultia_chunks").apply {
        if (!exists()) mkdirs()
    }

    suspend fun analyzeFile(
        file: File,
        chunkSize: Long = Constants.DEFAULT_CHUNK_SIZE_BYTES.toLong(),
        overrideFileName: String? = null
    ): Pair<VaultFile, List<FileChunk>> = withContext(Dispatchers.IO) {
        val fileId = UUID.randomUUID().toString()
        val totalSize = file.length()
        val totalChunks = if (totalSize == 0L) 1 else ((totalSize + chunkSize - 1) / chunkSize).toInt()
        val fileChecksum = calculateFileChecksum(file)
        val actualName = overrideFileName ?: file.name
        val mimeType = getMimeType(actualName)
        val mediaType = MediaType.fromMimeType(mimeType, actualName)

        val vaultFile = VaultFile(
            id = fileId,
            name = actualName,
            size = totalSize,
            mimeType = mimeType,
            mediaType = mediaType,
            totalChunks = totalChunks,
            chunkSize = chunkSize,
            checksum = fileChecksum,
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis(),
            localUri = file.absolutePath
        )

        val chunks = mutableListOf<FileChunk>()
        for (i in 0 until totalChunks) {
            val chunkStart = i * chunkSize
            val remaining = totalSize - chunkStart
            val currentChunkSize = minOf(chunkSize, remaining)
            val chunkChecksum = calculateChunkChecksum(file, chunkStart, currentChunkSize)

            chunks.add(
                FileChunk(
                    id = UUID.randomUUID().toString(),
                    fileId = fileId,
                    chunkIndex = i,
                    totalChunks = totalChunks,
                    size = currentChunkSize,
                    checksum = chunkChecksum,
                    status = TransferStatus.QUEUED
                )
            )
        }

        Pair(vaultFile, chunks)
    }

    suspend fun extractChunkToTempFile(
        sourceFile: File,
        chunkIndex: Int,
        chunkSize: Long,
        totalSize: Long
    ): File = withContext(Dispatchers.IO) {
        val startOffset = chunkIndex * chunkSize
        val currentChunkSize = minOf(chunkSize, totalSize - startOffset)
        val tempChunkFile = File(tempDir, "chunk_${sourceFile.name}_$chunkIndex.tmp")

        RandomAccessFile(sourceFile, "r").use { raf ->
            raf.seek(startOffset)
            FileOutputStream(tempChunkFile).use { fos ->
                val buffer = ByteArray(64 * 1024)
                var bytesRemaining = currentChunkSize
                while (bytesRemaining > 0) {
                    val bytesToRead = minOf(buffer.size.toLong(), bytesRemaining).toInt()
                    val bytesRead = raf.read(buffer, 0, bytesToRead)
                    if (bytesRead == -1) break
                    fos.write(buffer, 0, bytesRead)
                    bytesRemaining -= bytesRead
                }
                fos.flush()
            }
        }
        tempChunkFile
    }

    suspend fun reassembleFile(
        chunkFiles: List<File>,
        targetDestinationFile: File,
        expectedFileChecksum: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (targetDestinationFile.exists()) {
            targetDestinationFile.delete()
        }

        FileOutputStream(targetDestinationFile).use { outputStream ->
            for (chunkFile in chunkFiles) {
                if (!chunkFile.exists()) return@withContext false
                FileInputStream(chunkFile).use { inputStream ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                }
            }
            outputStream.flush()
        }

        if (expectedFileChecksum != null) {
            val actualChecksum = calculateFileChecksum(targetDestinationFile)
            return@withContext actualChecksum.equals(expectedFileChecksum, ignoreCase = true)
        }

        true
    }

    fun calculateFileChecksum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return bytesToHex(digest.digest())
    }

    fun calculateChunkChecksum(file: File, offset: Long, size: Long): String {
        val digest = MessageDigest.getInstance("SHA-256")
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(offset)
            val buffer = ByteArray(64 * 1024)
            var bytesRemaining = size
            while (bytesRemaining > 0) {
                val bytesToRead = minOf(buffer.size.toLong(), bytesRemaining).toInt()
                val bytesRead = raf.read(buffer, 0, bytesToRead)
                if (bytesRead == -1) break
                digest.update(buffer, 0, bytesRead)
                bytesRemaining -= bytesRead
            }
        }
        return bytesToHex(digest.digest())
    }

    fun verifyChunkChecksum(chunkFile: File, expectedChecksum: String): Boolean {
        val actual = calculateFileChecksum(chunkFile)
        return actual.equals(expectedChecksum, ignoreCase = true)
    }

    fun cleanTempChunks() {
        tempDir.listFiles()?.forEach { it.delete() }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = java.lang.StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    private fun getMimeType(filename: String): String {
        val ext = filename.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "mov" -> "video/quicktime"
            "avi" -> "video/x-msvideo"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "flac" -> "audio/flac"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "pdf" -> "application/pdf"
            "zip" -> "application/zip"
            "tar" -> "application/x-tar"
            "gz" -> "application/gzip"
            "txt" -> "text/plain"
            "json" -> "application/json"
            else -> "application/octet-stream"
        }
    }
}
