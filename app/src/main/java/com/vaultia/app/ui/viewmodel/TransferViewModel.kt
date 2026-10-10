package com.vaultia.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultia.app.data.local.VaultiaDatabase
import com.vaultia.app.domain.model.TransferTask
import com.vaultia.app.domain.model.VaultFile
import com.vaultia.app.engine.transfer.DownloadManager
import com.vaultia.app.engine.transfer.UploadManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransferViewModel(
    private val database: VaultiaDatabase,
    private val uploadManager: UploadManager,
    private val downloadManager: DownloadManager
) : ViewModel() {

    val transfers: StateFlow<List<TransferTask>> = combine(
        uploadManager.liveTransfers,
        downloadManager.liveTransfers,
        database.transferDao().getAllTransfers()
    ) { liveUps, liveDowns, dbTransfers ->
        val mergedMap = mutableMapOf<String, TransferTask>()

        // Database history
        for (item in dbTransfers) {
            val domain = item.toDomain()
            mergedMap[domain.id] = domain
        }

        // Overwrite with live in-memory updates
        for ((k, v) in liveUps) {
            mergedMap[k] = v
        }
        for ((k, v) in liveDowns) {
            mergedMap[k] = v
        }

        mergedMap.values.sortedByDescending { it.id }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun pauseTransfer(transferId: String) {
        if (transferId.startsWith("up_")) {
            uploadManager.pauseUpload(transferId)
        } else {
            downloadManager.pauseDownload(transferId)
        }
    }

    fun resumeTransfer(transferId: String, file: VaultFile? = null) {
        if (transferId.startsWith("up_")) {
            uploadManager.resumeUpload(transferId)
        } else {
            downloadManager.resumeDownload(file, transferId)
        }
    }

    fun cancelTransfer(transferId: String) {
        if (transferId.startsWith("up_")) {
            uploadManager.cancelUpload(transferId)
        } else {
            downloadManager.cancelDownload(transferId)
        }
    }

    fun clearCompleted() {
        uploadManager.clearAllFinishedOrStuck()
        downloadManager.clearAllFinishedOrStuck()
    }
}
