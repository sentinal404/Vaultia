package com.vaultia.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "backup_folders")
data class BackupFolderEntity(
    @PrimaryKey
    val id: String,
    val treeUri: String,
    val displayName: String,
    val isEnabled: Boolean = true,
    val lastBackupTimestamp: Long = 0L,
    val totalFilesTracked: Int = 0
)
