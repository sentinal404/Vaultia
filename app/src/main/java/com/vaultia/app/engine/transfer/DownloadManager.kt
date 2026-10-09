package com.vaultia.app.engine.transfer

import android.content.Context
import android.os.Environment
import com.vaultia.app.core.Constants
import com.vaultia.app.core.VaultResult
import com.vaultia.app.core.security.SecureStorageManager
import com.vaultia.app.data.local.VaultiaDatabase
import com.vaultia.app.data.local.entity.TransferEntity
import com.vaultia.app.data.remote.TelegramApiService
import com.vaultia.app.domain.model.FileChunk
import com.vaultia.app.domain.model.TransferStatus
import com.vaultia.app.domain.model.TransferTask
import com.vaultia.app.domain.model.TransferType
import com.vaultia.app.domain.model.VaultFile
import com.vaultia.app.engine.chunk.ChunkEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

class DownloadManager(
    private val context: Context,
    private val database: VaultiaDatabase,
    private val telegramApi: TelegramApiService,
    private val secureStorage: SecureStorageManager,
    private val chunkEngine: ChunkEngine,
    private val workerScheduler: WorkerScheduler,
    private val retryPolicy: RetryPolicy = RetryPolicy()
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val pausedTasks = ConcurrentHashMap.newKeySet<String>()

    private val _liveTransfers = MutableStateFlow<Map<String, TransferTask>>(emptyMap())
    val liveTransfers: StateFlow<Map<String, TransferTask>> = _liveTransfers.asStateFlow()

    private val downloadTempDir: File = File(context.cacheDir, "vaultia_downloads").apply {
        if (!exists()) mkdirs()
    }

    fun enqueueDownload(
        vaultFile: VaultFile,
        destinationDir: File? = null,
        maxConcurrency: Int = Constants.DEFAULT_MAX_PARALLEL_WORKERS
    ): String {
        val transferId = "down_${System.currentTimeMillis()}_${vaultFile.name.hashCode()}"
        val targetDir = destinationDir ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
        val destinationFile = File(targetDir, vaultFile.name)

        val job = scope.launch {
            processDownload(transferId, vaultFile, destinationFile, maxConcurrency)
        }
        activeJobs[transferId] = job
        return transferId
    }

    private suspend fun processDownload(
        transferId: String,
        vaultFile: VaultFile,
        destinationFile: File,
        maxConcurrency: Int
    ) {
        val initialTask = TransferTask(
            id = transferId,
            fileId = vaultFile.id,
            filename = vaultFile.name,
            totalSize = vaultFile.size,
            transferredBytes = 0,
            type = TransferType.DOWNLOAD,
            status = TransferStatus.PREPARING,
            progress = 0f,
            currentChunk = 0,
            totalChunks = vaultFile.totalChunks,
            localFilePath = destinationFile.absolutePath
        )
        updateTransferState(initialTask)

        val chunks = database.chunkDao().getChunksForFileList(vaultFile.id).map { it.toDomain() }
        if (chunks.isEmpty()) {
            updateTransferState(initialTask.copy(status = TransferStatus.FAILED, errorMessage = "No chunk records found for this file"))
            return
        }

        updateTransferState(initialTask.copy(status = TransferStatus.TRANSFERRING))

        val workerCount = workerScheduler.getWorkerCount()
        val effectiveConcurrency = maxOf(maxConcurrency, (workerCount * 2)).coerceIn(1, Constants.MAX_ALLOWED_WORKERS)
        val semaphore = Semaphore(effectiveConcurrency)
        val downloadedChunkFiles = ConcurrentHashMap<Int, File>()
        val chunkJobs = mutableListOf<Job>()
        val startTime = System.currentTimeMillis()

        for (chunk in chunks) {
            if (pausedTasks.contains(transferId)) {
                updateTransferState(initialTask.copy(status = TransferStatus.PAUSED))
                return
            }

            val chunkFile = File(downloadTempDir, "${vaultFile.id}_chunk_${chunk.chunkIndex}.tmp")
            if (chunkFile.exists() && chunkFile.length() == chunk.size) {
                // Already downloaded and verified
                downloadedChunkFiles[chunk.chunkIndex] = chunkFile
                continue
            }

            val job = scope.launch {
                semaphore.withPermit {
                    if (pausedTasks.contains(transferId)) return@withPermit

                    downloadSingleChunk(
                        chunk = chunk,
                        outputFile = chunkFile,
                        vaultFile = vaultFile,
                        transferId = transferId,
                        downloadedMap = downloadedChunkFiles,
                        startTime = startTime
                    )
                }
            }
            chunkJobs.add(job)
        }

        chunkJobs.forEach { it.join() }

        if (pausedTasks.contains(transferId)) return

        if (downloadedChunkFiles.size < vaultFile.totalChunks) {
            updateTransferState(initialTask.copy(status = TransferStatus.FAILED, errorMessage = "Failed to download all chunks"))
            return
        }

        // Reconstruct
        updateTransferState(
            initialTask.copy(
                status = TransferStatus.RECONSTRUCTING,
                progress = 0.95f
            )
        )

        val sortedChunkFiles = (0 until vaultFile.totalChunks).mapNotNull { downloadedChunkFiles[it] }
        val success = chunkEngine.reassembleFile(sortedChunkFiles, destinationFile, vaultFile.checksum)

        if (success) {
            updateTransferState(
                initialTask.copy(
                    status = TransferStatus.COMPLETED,
                    progress = 1.0f,
                    transferredBytes = vaultFile.size,
                    currentChunk = vaultFile.totalChunks
                )
            )
            // Cleanup temp chunk files
            sortedChunkFiles.forEach { it.delete() }
        } else {
            updateTransferState(
                initialTask.copy(
                    status = TransferStatus.FAILED,
                    errorMessage = "Checksum mismatch or file reconstruction failure"
                )
            )
        }
    }

    private suspend fun downloadSingleChunk(
        chunk: FileChunk,
        outputFile: File,
        vaultFile: VaultFile,
        transferId: String,
        downloadedMap: ConcurrentHashMap<Int, File>,
        startTime: Long
    ) {
        val remoteFileId = chunk.remoteFileId
        if (remoteFileId.isNullOrBlank()) return

        val worker = workerScheduler.getAvailableWorker()
        val token = worker?.token ?: secureStorage.botToken ?: return

        try {
            val fileDtoResult = telegramApi.getFile(token, remoteFileId)
            val filePath = fileDtoResult.getOrNull()?.filePath ?: return

            val downloadResult = retryPolicy.executeWithRetry(
                actionName = "Download chunk ${chunk.chunkIndex}",
                isRateLimited = { err ->
                    val msg = err?.message ?: ""
                    if (msg.contains("429")) 5 else null
                }
            ) {
                FileOutputStream(outputFile).use { fos ->
                    telegramApi.downloadFileChunk(token, filePath, fos)
                }
            }

            if (downloadResult is VaultResult.Success) {
                downloadedMap[chunk.chunkIndex] = outputFile
                worker?.let { workerScheduler.markSuccess(it.id) }

                val completedCount = downloadedMap.size
                val downloadedBytes = minOf(vaultFile.size, completedCount * vaultFile.chunkSize)
                val elapsedSec = ((System.currentTimeMillis() - startTime) / 1000L).coerceAtLeast(1L)
                val speed = downloadedBytes / elapsedSec
                val remainingBytes = (vaultFile.size - downloadedBytes).coerceAtLeast(0L)
                val eta = if (speed > 0) remainingBytes / speed else 0L

                val current = getCurrentTask(transferId)
                if (current != null) {
                    updateTransferState(
                        current.copy(
                            transferredBytes = downloadedBytes,
                            progress = if (vaultFile.size > 0) downloadedBytes.toFloat() / vaultFile.size else 1f,
                            currentChunk = completedCount,
                            speedBytesPerSec = speed,
                            etaSeconds = eta
                        )
                    )
                }
            }
        } catch (e: Exception) {
            outputFile.delete()
        } finally {
            workerScheduler.releaseWorker(worker?.id)
        }
    }

    fun pauseDownload(transferId: String) {
        pausedTasks.add(transferId)
        activeJobs[transferId]?.cancel()
        getCurrentTask(transferId)?.let {
            updateTransferState(it.copy(status = TransferStatus.PAUSED))
        }
    }

    fun resumeDownload(vaultFile: VaultFile, transferId: String) {
        pausedTasks.remove(transferId)
        val job = scope.launch {
            val targetDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
            val destinationFile = File(targetDir, vaultFile.name)
            processDownload(transferId, vaultFile, destinationFile, Constants.DEFAULT_MAX_PARALLEL_WORKERS)
        }
        activeJobs[transferId] = job
    }

    fun cancelDownload(transferId: String) {
        pausedTasks.remove(transferId)
        activeJobs[transferId]?.cancel()
        activeJobs.remove(transferId)
        getCurrentTask(transferId)?.let {
            updateTransferState(it.copy(status = TransferStatus.CANCELLED))
        }
    }

    private fun getCurrentTask(transferId: String): TransferTask? {
        return _liveTransfers.value[transferId]
    }

    private fun updateTransferState(task: TransferTask) {
        val updated = _liveTransfers.value.toMutableMap()
        updated[task.id] = task
        _liveTransfers.value = updated

        scope.launch {
            database.transferDao().insertTransfer(TransferEntity.fromDomain(task))
        }
    }
}
