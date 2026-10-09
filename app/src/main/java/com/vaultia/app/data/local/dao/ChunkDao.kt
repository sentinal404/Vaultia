package com.vaultia.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vaultia.app.data.local.entity.ChunkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChunkDao {

    @Query("SELECT * FROM chunks WHERE fileId = :fileId ORDER BY chunkIndex ASC")
    fun getChunksForFile(fileId: String): Flow<List<ChunkEntity>>

    @Query("SELECT * FROM chunks WHERE fileId = :fileId ORDER BY chunkIndex ASC")
    suspend fun getChunksForFileList(fileId: String): List<ChunkEntity>

    @Query("SELECT * FROM chunks WHERE fileId = :fileId AND chunkIndex = :index LIMIT 1")
    suspend fun getChunkByIndex(fileId: String, index: Int): ChunkEntity?

    @Query("SELECT * FROM chunks WHERE fileId = :fileId AND status != 'COMPLETED' ORDER BY chunkIndex ASC")
    suspend fun getPendingChunksForFile(fileId: String): List<ChunkEntity>

    @Query("SELECT COUNT(*) FROM chunks WHERE fileId = :fileId AND status = 'COMPLETED'")
    fun getCompletedChunksCount(fileId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM chunks WHERE fileId = :fileId AND status = 'COMPLETED'")
    suspend fun getCompletedChunksCountSync(fileId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunk(chunk: ChunkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<ChunkEntity>)

    @Update
    suspend fun updateChunk(chunk: ChunkEntity)

    @Query("UPDATE chunks SET status = :status, remoteMessageId = :remoteMessageId, remoteFileId = :remoteFileId WHERE id = :chunkId")
    suspend fun updateChunkRemote(chunkId: String, status: String, remoteMessageId: Long?, remoteFileId: String?)

    @Query("UPDATE chunks SET status = :status WHERE id = :chunkId")
    suspend fun updateChunkStatus(chunkId: String, status: String)

    @Query("DELETE FROM chunks WHERE fileId = :fileId")
    suspend fun deleteChunksForFile(fileId: String)

    @Query("DELETE FROM chunks")
    suspend fun deleteAllChunks()
}
