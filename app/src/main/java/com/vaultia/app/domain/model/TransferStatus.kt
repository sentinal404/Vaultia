package com.vaultia.app.domain.model

enum class TransferStatus {
    QUEUED,
    PREPARING,
    TRANSFERRING,
    PAUSED,
    RETRYING,
    RECONSTRUCTING,
    VERIFYING,
    COMPLETED,
    FAILED,
    CANCELLED;

    val isActive: Boolean
        get() = this in listOf(QUEUED, PREPARING, TRANSFERRING, RETRYING, RECONSTRUCTING, VERIFYING)

    val isFinished: Boolean
        get() = this in listOf(COMPLETED, FAILED, CANCELLED)
}

enum class TransferType {
    UPLOAD,
    DOWNLOAD
}
