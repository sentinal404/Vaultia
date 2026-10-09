package com.vaultia.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vaultia.app.data.local.entity.FileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {

    @Query("SELECT * FROM files ORDER BY createdAt DESC")
    fun getAllFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE mediaType = :mediaType ORDER BY createdAt DESC")
    fun getFilesByMediaType(mediaType: String): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE name LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchFiles(query: String): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE id = :id LIMIT 1")
    suspend fun getFileById(id: String): FileEntity?

    @Query("SELECT * FROM files ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentFiles(limit: Int): Flow<List<FileEntity>>

    @Query("SELECT SUM(size) FROM files")
    fun getTotalStorageUsed(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM files")
    fun getTotalFilesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM files WHERE mediaType = :mediaType")
    fun getFilesCountByMediaType(mediaType: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<FileEntity>)

    @Update
    suspend fun updateFile(file: FileEntity)

    @Query("UPDATE files SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("DELETE FROM files WHERE id = :id")
    suspend fun deleteFile(id: String)

    @Query("DELETE FROM files")
    suspend fun deleteAllFiles()
}
