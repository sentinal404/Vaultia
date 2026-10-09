package com.vaultia.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vaultia.app.domain.model.MediaType
import com.vaultia.app.domain.model.VaultFile

@Entity(
    tableName = "files",
    indices = [
        Index(value = ["name"]),
        Index(value = ["mediaType"]),
        Index(value = ["createdAt"]),
        Index(value = ["isFavorite"])
    ]
)
data class FileEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val size: Long,
    val mimeType: String,
    val mediaType: String,
    val totalChunks: Int,
    val chunkSize: Long,
    val checksum: String,
    val createdAt: Long,
    val modifiedAt: Long,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val remoteRootMessageId: Long? = null,
    val localUri: String? = null
) {
    fun toDomain(): VaultFile {
        val type = try {
            MediaType.valueOf(mediaType)
        } catch (e: Exception) {
            MediaType.fromMimeType(mimeType, name)
        }
        return VaultFile(
            id = id,
            name = name,
            size = size,
            mimeType = mimeType,
            mediaType = type,
            totalChunks = totalChunks,
            chunkSize = chunkSize,
            checksum = checksum,
            createdAt = createdAt,
            modifiedAt = modifiedAt,
            isFavorite = isFavorite,
            isPinned = isPinned,
            remoteRootMessageId = remoteRootMessageId,
            localUri = localUri
        )
    }

    companion object {
        fun fromDomain(file: VaultFile): FileEntity {
            return FileEntity(
                id = file.id,
                name = file.name,
                size = file.size,
                mimeType = file.mimeType,
                mediaType = file.mediaType.name,
                totalChunks = file.totalChunks,
                chunkSize = file.chunkSize,
                checksum = file.checksum,
                createdAt = file.createdAt,
                modifiedAt = file.modifiedAt,
                isFavorite = file.isFavorite,
                isPinned = file.isPinned,
                remoteRootMessageId = file.remoteRootMessageId,
                localUri = file.localUri
            )
        }
    }
}
