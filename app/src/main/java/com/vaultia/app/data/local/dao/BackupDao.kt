package com.vaultia.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vaultia.app.data.local.entity.BackupFolderEntity
import com.vaultia.app.data.local.entity.BackupRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupDao {

    @Query("SELECT * FROM backup_folders ORDER BY displayName ASC")
    fun getAllFoldersFlow(): Flow<List<BackupFolderEntity>>

    @Query("SELECT * FROM backup_folders")
    suspend fun getAllFoldersSync(): List<BackupFolderEntity>

    @Query("SELECT * FROM backup_folders WHERE isEnabled = 1")
    suspend fun getEnabledFoldersSync(): List<BackupFolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: BackupFolderEntity)

    @Update
    suspend fun updateFolder(folder: BackupFolderEntity)

    @Query("DELETE FROM backup_folders WHERE id = :folderId")
    suspend fun deleteFolder(folderId: String)

    @Query("DELETE FROM backup_records WHERE folderId = :folderId")
    suspend fun deleteRecordsForFolder(folderId: String)

    @Query("SELECT * FROM backup_records WHERE folderId = :folderId AND fileName = :fileName AND fileSize = :fileSize AND lastModified = :lastModified LIMIT 1")
    suspend fun findRecord(folderId: String, fileName: String, fileSize: Long, lastModified: Long): BackupRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: BackupRecordEntity)

    @Query("SELECT COUNT(*) FROM backup_records WHERE status = 'COMPLETED'")
    fun getTotalBackedUpFilesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM backup_records WHERE folderId = :folderId AND status = 'COMPLETED'")
    suspend fun getFolderBackedUpCount(folderId: String): Int
}
