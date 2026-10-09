package com.vaultia.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "backup_records",
    indices = [
        Index(value = ["folderId", "fileName", "fileSize", "lastModified"], unique = true)
    ]
)
data class BackupRecordEntity(
    @PrimaryKey
    val id: String,
    val folderId: String,
    val fileUri: String,
    val fileName: String,
    val fileSize: Long,
    val lastModified: Long,
    val mimeType: String,
    val status: String, // "COMPLETED", "PENDING", "FAILED"
    val uploadedAt: Long = System.currentTimeMillis()
)
