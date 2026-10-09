package com.vaultia.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vaultia.app.domain.model.FileChunk
import com.vaultia.app.domain.model.TransferStatus

@Entity(
    tableName = "chunks",
    foreignKeys = [
        ForeignKey(
            entity = FileEntity::class,
            parentColumns = ["id"],
            childColumns = ["fileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["fileId"]),
        Index(value = ["fileId", "chunkIndex"], unique = true),
        Index(value = ["remoteMessageId"]),
        Index(value = ["status"])
    ]
)
data class ChunkEntity(
    @PrimaryKey
    val id: String,
    val fileId: String,
    val chunkIndex: Int,
    val totalChunks: Int,
    val size: Long,
    val checksum: String,
    val remoteMessageId: Long? = null,
    val remoteFileId: String? = null,
    val status: String = TransferStatus.QUEUED.name,
    val retryCount: Int = 0,
    val workerId: String? = null
) {
    fun toDomain(): FileChunk {
        return FileChunk(
            id = id,
            fileId = fileId,
            chunkIndex = chunkIndex,
            totalChunks = totalChunks,
            size = size,
            checksum = checksum,
            remoteMessageId = remoteMessageId,
            remoteFileId = remoteFileId,
            status = try { TransferStatus.valueOf(status) } catch (e: Exception) { TransferStatus.QUEUED },
            retryCount = retryCount,
            workerId = workerId
        )
    }

    companion object {
        fun fromDomain(chunk: FileChunk): ChunkEntity {
            return ChunkEntity(
                id = chunk.id,
                fileId = chunk.fileId,
                chunkIndex = chunk.chunkIndex,
                totalChunks = chunk.totalChunks,
                size = chunk.size,
                checksum = chunk.checksum,
                remoteMessageId = chunk.remoteMessageId,
                remoteFileId = chunk.remoteFileId,
                status = chunk.status.name,
                retryCount = chunk.retryCount,
                workerId = chunk.workerId
            )
        }
    }
}
