package com.vaultia.app.engine.transfer

import android.content.Context
import com.google.gson.Gson
import com.vaultia.app.core.Constants
import com.vaultia.app.core.VaultResult
import com.vaultia.app.core.security.SecureStorageManager
import com.vaultia.app.data.local.VaultiaDatabase
import com.vaultia.app.data.local.entity.ChunkEntity
import com.vaultia.app.data.local.entity.FileEntity
import com.vaultia.app.data.local.entity.TransferEntity
import com.vaultia.app.data.remote.ChunkPayloadMetadata
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
import java.util.concurrent.ConcurrentHashMap

class UploadManager(
    private val context: Context,
    private val database: VaultiaDatabase,
    private val telegramApi: TelegramApiService,
    private val secureStorage: SecureStorageManager,
    private val chunkEngine: ChunkEngine,
    private val workerScheduler: WorkerScheduler,
    private val retryPolicy: RetryPolicy = RetryPolicy()
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val pausedTasks = ConcurrentHashMap.newKeySet<String>()

    private val _liveTransfers = MutableStateFlow<Map<String, TransferTask>>(emptyMap())
    val liveTransfers: StateFlow<Map<String, TransferTask>> = _liveTransfers.asStateFlow()

    fun enqueueUpload(
        file: File,
        customFileName: String? = null,
        maxConcurrency: Int = Constants.DEFAULT_MAX_PARALLEL_WORKERS
    ): String {
        val transferId = "up_${System.currentTimeMillis()}_${file.name.hashCode()}"
        val channelId = secureStorage.channelId ?: return ""
        val primaryToken = secureStorage.botToken ?: return ""

        val job = scope.launch {
            processUpload(transferId, file, channelId, primaryToken, maxConcurrency, customFileName)
        }
        activeJobs[transferId] = job
        return transferId
    }

    private suspend fun processUpload(
        transferId: String,
        sourceFile: File,
        channelId: String,
        primaryToken: String,
        maxConcurrency: Int,
        customFileName: String? = null
    ) {
        // Step 1: Analyze file or retrieve existing metadata
        val (vaultFile, chunks) = chunkEngine.analyzeFile(sourceFile, overrideFileName = customFileName)
        val fileId = vaultFile.id

        // Persist file and chunks
        database.fileDao().insertFile(FileEntity.fromDomain(vaultFile))
        database.chunkDao().insertChunks(chunks.map { ChunkEntity.fromDomain(it) })

        val initialTask = TransferTask(
            id = transferId,
            fileId = fileId,
            filename = vaultFile.name,
            totalSize = vaultFile.size,
            transferredBytes = 0,
            type = TransferType.UPLOAD,
            status = TransferStatus.PREPARING,
            progress = 0f,
            currentChunk = 0,
            totalChunks = vaultFile.totalChunks,
            localFilePath = sourceFile.absolutePath
        )
        updateTransferState(initialTask)

        val workerCount = workerScheduler.getWorkerCount()
        val effectiveConcurrency = maxOf(maxConcurrency, (workerCount * 2)).coerceIn(1, Constants.MAX_ALLOWED_WORKERS)
        val semaphore = Semaphore(effectiveConcurrency)
        val startTime = System.currentTimeMillis()
        var completedChunksCount = database.chunkDao().getCompletedChunksCountSync(fileId)
        val completedBytes = completedChunksCount * vaultFile.chunkSize

        updateTransferState(
            initialTask.copy(
                status = TransferStatus.TRANSFERRING,
                transferredBytes = completedBytes,
                progress = if (vaultFile.size > 0) completedBytes.toFloat() / vaultFile.size else 1f,
                currentChunk = completedChunksCount
            )
        )

        val pendingChunks = database.chunkDao().getPendingChunksForFile(fileId)

        val chunkJobs = mutableListOf<Job>()
        for (chunkEntity in pendingChunks) {
            if (pausedTasks.contains(transferId)) {
                updateTransferState(
                    getCurrentTask(transferId)?.copy(status = TransferStatus.PAUSED)
                        ?: initialTask.copy(status = TransferStatus.PAUSED)
                )
                return
            }

            val chunkJob = scope.launch {
                semaphore.withPermit {
                    if (pausedTasks.contains(transferId)) return@withPermit

                    uploadSingleChunk(
                        transferId = transferId,
                        sourceFile = sourceFile,
                        chunkEntity = chunkEntity,
                        vaultFile = vaultFile,
                        channelId = channelId,
                        primaryToken = primaryToken,
                        startTime = startTime
                    )
                }
            }
            chunkJobs.add(chunkJob)
        }

        chunkJobs.forEach { it.join() }

        // Final verification
        val remainingPending = database.chunkDao().getPendingChunksForFile(fileId)
        if (remainingPending.isEmpty()) {
            val finalTask = getCurrentTask(transferId)?.copy(
                status = TransferStatus.COMPLETED,
                progress = 1.0f,
                transferredBytes = vaultFile.size,
                currentChunk = vaultFile.totalChunks
            ) ?: initialTask.copy(status = TransferStatus.COMPLETED)
            updateTransferState(finalTask)
            chunkEngine.cleanTempChunks()
            try {
                com.vaultia.app.VaultiaApplication.instance.cloudSyncManager.syncCatalogToCloud()
            } catch (_: Exception) {}
        } else if (!pausedTasks.contains(transferId)) {
            val failedTask = getCurrentTask(transferId)?.copy(
                status = TransferStatus.FAILED,
                errorMessage = "Failed to upload all chunks"
            ) ?: initialTask.copy(status = TransferStatus.FAILED)
            updateTransferState(failedTask)
        }
    }

    private suspend fun uploadSingleChunk(
        transferId: String,
        sourceFile: File,
        chunkEntity: ChunkEntity,
        vaultFile: VaultFile,
        channelId: String,
        primaryToken: String,
        startTime: Long
    ) {
        val worker = workerScheduler.getAvailableWorker()
        val token = worker?.token ?: primaryToken
        val workerName = worker?.name ?: "Primary Bot"

        database.chunkDao().updateChunkStatus(chunkEntity.id, TransferStatus.TRANSFERRING.name)

        var tempChunkFile: File? = null
        try {
            tempChunkFile = chunkEngine.extractChunkToTempFile(
                sourceFile = sourceFile,
                chunkIndex = chunkEntity.chunkIndex,
                chunkSize = vaultFile.chunkSize,
                totalSize = vaultFile.size
            )

            val payloadMeta = ChunkPayloadMetadata(
                fileId = vaultFile.id,
                filename = vaultFile.name,
                chunkIndex = chunkEntity.chunkIndex,
                totalChunks = vaultFile.totalChunks,
                chunkSize = chunkEntity.size,
                checksum = chunkEntity.checksum
            )
            val caption = "${Constants.METADATA_TAG} ${gson.toJson(payloadMeta)}"

            val remoteChunkName = if (vaultFile.totalChunks == 1) {
                vaultFile.name
            } else {
                "${vaultFile.name}.part${chunkEntity.chunkIndex + 1}"
            }

            val result = retryPolicy.executeWithRetry(
                actionName = "Upload chunk ${chunkEntity.chunkIndex}",
                isRateLimited = { err ->
                    val msg = err?.message ?: ""
                    if (msg.contains("429")) 5 else null
                }
            ) { attempt ->
                telegramApi.sendDocumentChunk(
                    token = token,
                    chatId = channelId,
                    chunkFile = tempChunkFile,
                    fileName = remoteChunkName,
                    caption = caption
                )
            }

            when (result) {
                is VaultResult.Success -> {
                    val msgDto = result.data
                    val remoteDoc = msgDto.document
                    database.chunkDao().updateChunkRemote(
                        chunkId = chunkEntity.id,
                        status = TransferStatus.COMPLETED.name,
                        remoteMessageId = msgDto.messageId,
                        remoteFileId = remoteDoc?.fileId,
                        workerId = worker?.token ?: primaryToken
                    )
                    worker?.let { workerScheduler.markSuccess(it.id) }

                    // Recalculate overall progress
                    val completedCount = database.chunkDao().getCompletedChunksCountSync(vaultFile.id)
                    val uploadedBytes = minOf(vaultFile.size, completedCount * vaultFile.chunkSize)
                    val elapsedSec = ((System.currentTimeMillis() - startTime) / 1000L).coerceAtLeast(1L)
                    val speed = uploadedBytes / elapsedSec
                    val remainingBytes = (vaultFile.size - uploadedBytes).coerceAtLeast(0L)
                    val eta = if (speed > 0) remainingBytes / speed else 0L

                    val current = getCurrentTask(transferId)
                    if (current != null) {
                        updateTransferState(
                            current.copy(
                                transferredBytes = uploadedBytes,
                                progress = if (vaultFile.size > 0) uploadedBytes.toFloat() / vaultFile.size else 1f,
                                currentChunk = completedCount,
                                speedBytesPerSec = speed,
                                etaSeconds = eta,
                                activeWorkerName = workerName
                            )
                        )
                    }
                }
                is VaultResult.Error -> {
                    database.chunkDao().updateChunkStatus(chunkEntity.id, TransferStatus.FAILED.name)
                    if (result.code == 429 && worker != null) {
                        workerScheduler.markRateLimited(worker.id, 10)
                    }
                }
                else -> Unit
            }
        } catch (e: Exception) {
            database.chunkDao().updateChunkStatus(chunkEntity.id, TransferStatus.FAILED.name)
        } finally {
            workerScheduler.releaseWorker(worker?.id)
            tempChunkFile?.delete()
        }
    }

    fun pauseUpload(transferId: String) {
        pausedTasks.add(transferId)
        activeJobs[transferId]?.cancel()
        activeJobs.remove(transferId)
        getCurrentTask(transferId)?.let {
            updateTransferState(it.copy(status = TransferStatus.PAUSED))
        } ?: run {
            scope.launch {
                database.transferDao().updateStatus(transferId, TransferStatus.PAUSED.name)
            }
        }
    }

    fun resumeUpload(transferId: String, maxConcurrency: Int = Constants.DEFAULT_MAX_PARALLEL_WORKERS) {
        pausedTasks.remove(transferId)
        scope.launch {
            val task = getCurrentTask(transferId)
                ?: database.transferDao().getTransferById(transferId)?.toDomain()
                ?: return@launch
            val localPath = task.localFilePath ?: return@launch
            val file = File(localPath)
            if (!file.exists()) return@launch

            val channelId = secureStorage.channelId ?: return@launch
            val primaryToken = secureStorage.botToken ?: return@launch

            val job = scope.launch {
                processUpload(transferId, file, channelId, primaryToken, maxConcurrency, customFileName = task.filename)
            }
            activeJobs[transferId] = job
        }
    }

    fun retryUpload(transferId: String) {
        resumeUpload(transferId)
    }

    fun cancelUpload(transferId: String) {
        pausedTasks.remove(transferId)
        activeJobs[transferId]?.cancel()
        activeJobs.remove(transferId)
        getCurrentTask(transferId)?.let {
            updateTransferState(it.copy(status = TransferStatus.CANCELLED))
        }
        scope.launch {
            database.transferDao().updateStatus(transferId, TransferStatus.CANCELLED.name)
        }
    }

    fun clearAllFinishedOrStuck() {
        val updated = _liveTransfers.value.toMutableMap()
        val toRemove = updated.filter {
            it.value.status.isFinished || it.value.status == TransferStatus.FAILED || it.value.status == TransferStatus.CANCELLED
        }.keys
        toRemove.forEach { updated.remove(it) }
        _liveTransfers.value = updated
        scope.launch {
            database.transferDao().clearCompletedTransfers()
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
