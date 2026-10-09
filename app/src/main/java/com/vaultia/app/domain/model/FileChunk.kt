package com.vaultia.app.domain.model

data class FileChunk(
    val id: String,
    val fileId: String,
    val chunkIndex: Int,
    val totalChunks: Int,
    val size: Long,
    val checksum: String,
    val remoteMessageId: Long? = null,
    val remoteFileId: String? = null,
    val status: TransferStatus = TransferStatus.QUEUED,
    val retryCount: Int = 0,
    val workerId: String? = null
)
