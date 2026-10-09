package com.vaultia.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vaultia.app.domain.model.TransferStatus
import com.vaultia.app.domain.model.TransferTask
import com.vaultia.app.domain.model.TransferType

@Entity(
    tableName = "transfers",
    indices = [
        Index(value = ["fileId"]),
        Index(value = ["status"]),
        Index(value = ["type"])
    ]
)
data class TransferEntity(
    @PrimaryKey
    val id: String,
    val fileId: String,
    val filename: String,
    val totalSize: Long,
    val transferredBytes: Long,
    val type: String,
    val status: String,
    val progress: Float,
    val currentChunk: Int,
    val totalChunks: Int,
    val activeWorkerName: String? = null,
    val errorMessage: String? = null,
    val localFilePath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): TransferTask {
        return TransferTask(
            id = id,
            fileId = fileId,
            filename = filename,
            totalSize = totalSize,
            transferredBytes = transferredBytes,
            type = try { TransferType.valueOf(type) } catch (e: Exception) { TransferType.UPLOAD },
            status = try { TransferStatus.valueOf(status) } catch (e: Exception) { TransferStatus.QUEUED },
            progress = progress,
            speedBytesPerSec = 0,
            etaSeconds = 0,
            currentChunk = currentChunk,
            totalChunks = totalChunks,
            activeWorkerName = activeWorkerName,
            errorMessage = errorMessage,
            localFilePath = localFilePath
        )
    }

    companion object {
        fun fromDomain(task: TransferTask): TransferEntity {
            return TransferEntity(
                id = task.id,
                fileId = task.fileId,
                filename = task.filename,
                totalSize = task.totalSize,
                transferredBytes = task.transferredBytes,
                type = task.type.name,
                status = task.status.name,
                progress = task.progress,
                currentChunk = task.currentChunk,
                totalChunks = task.totalChunks,
                activeWorkerName = task.activeWorkerName,
                errorMessage = task.errorMessage,
                localFilePath = task.localFilePath
            )
        }
    }
}
