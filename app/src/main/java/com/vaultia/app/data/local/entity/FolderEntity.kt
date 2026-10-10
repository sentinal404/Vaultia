package com.vaultia.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val colorHex: String = "#8B5CF6",
    val createdAt: Long = System.currentTimeMillis()
)
