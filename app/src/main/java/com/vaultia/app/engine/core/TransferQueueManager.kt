package com.vaultia.app.engine.core

import android.content.Context
import com.vaultia.app.data.local.VaultiaDatabase
import com.vaultia.app.domain.model.TransferStatus
import com.vaultia.app.domain.model.TransferTask
import com.vaultia.app.engine.transfer.DownloadManager
import com.vaultia.app.engine.transfer.UploadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransferItemWithMetrics(
    val task: TransferTask,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = 0L
)

/**
 * Manages the persistent queue of active uploads and downloads,
 * computes live bandwidth speed & ETA metrics, and handles pause/resume/cancel/retry.
 */
class TransferQueueManager(
    private val context: Context,
    private val database: VaultiaDatabase,
    private val uploadManager: UploadManager,
    private val downloadManager: DownloadManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val lastBytesMap = mutableMapOf<String, Pair<Long, Long>>() // id -> (bytes, timestamp)

    val activeTransfersWithMetrics: StateFlow<List<TransferItemWithMetrics>> = combine(
        uploadManager.liveTransfers,
        downloadManager.liveTransfers
    ) { uploads, downloads ->
        val combined = uploads.values + downloads.values
        val now = System.currentTimeMillis()

        combined.map { task ->
            val prev = lastBytesMap[task.id]
            var speed = 0L
            if (prev != null && task.status == TransferStatus.TRANSFERRING) {
                val dtSec = (now - prev.second) / 1000.0
                if (dtSec > 0.5) {
                    val db = task.transferredBytes - prev.first
                    speed = (db / dtSec).toLong().coerceAtLeast(0L)
                    lastBytesMap[task.id] = Pair(task.transferredBytes, now)
                } else {
                    speed = 0L
                }
            } else {
                lastBytesMap[task.id] = Pair(task.transferredBytes, now)
            }

            val remainingBytes = (task.totalSize - task.transferredBytes).coerceAtLeast(0L)
            val eta = if (speed > 0) remainingBytes / speed else 0L

            TransferItemWithMetrics(
                task = task,
                speedBytesPerSec = speed,
                etaSeconds = eta
            )
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun cancelTransfer(id: String) {
        uploadManager.cancelUpload(id)
        scope.launch(Dispatchers.IO) {
            database.transferDao().updateStatus(id, TransferStatus.CANCELLED.name)
        }
    }

    fun pauseTransfer(id: String) {
        uploadManager.pauseUpload(id)
        scope.launch(Dispatchers.IO) {
            database.transferDao().updateStatus(id, TransferStatus.PAUSED.name)
        }
    }

    fun resumeTransfer(id: String) {
        uploadManager.resumeUpload(id)
        scope.launch(Dispatchers.IO) {
            database.transferDao().updateStatus(id, TransferStatus.TRANSFERRING.name)
        }
    }

    fun clearCompleted() {
        scope.launch(Dispatchers.IO) {
            database.transferDao().clearCompletedTransfers()
        }
    }
}
