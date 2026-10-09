package com.vaultia.app.domain.model

data class TransferTask(
    val id: String,
    val fileId: String,
    val filename: String,
    val totalSize: Long,
    val transferredBytes: Long = 0,
    val type: TransferType,
    val status: TransferStatus = TransferStatus.QUEUED,
    val progress: Float = 0f,
    val speedBytesPerSec: Long = 0,
    val etaSeconds: Long = 0,
    val currentChunk: Int = 0,
    val totalChunks: Int = 0,
    val activeWorkerName: String? = null,
    val errorMessage: String? = null,
    val localFilePath: String? = null
) {
    val formattedSpeed: String
        get() = "${VaultFile.formatBytes(speedBytesPerSec)}/s"

    val formattedEta: String
        get() {
            if (etaSeconds <= 0) return "--"
            val mins = etaSeconds / 60
            val secs = etaSeconds % 60
            return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
        }
}
