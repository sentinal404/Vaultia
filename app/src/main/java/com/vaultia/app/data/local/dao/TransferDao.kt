package com.vaultia.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vaultia.app.data.local.entity.TransferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {

    @Query("SELECT * FROM transfers ORDER BY createdAt DESC")
    fun getAllTransfers(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers WHERE type = :type ORDER BY createdAt DESC")
    fun getTransfersByType(type: String): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers WHERE status IN ('QUEUED', 'PREPARING', 'TRANSFERRING', 'RETRYING', 'RECONSTRUCTING', 'VERIFYING') ORDER BY createdAt DESC")
    fun getActiveTransfers(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers WHERE id = :id LIMIT 1")
    suspend fun getTransferById(id: String): TransferEntity?

    @Query("SELECT * FROM transfers WHERE fileId = :fileId LIMIT 1")
    suspend fun getTransferByFileId(fileId: String): TransferEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: TransferEntity)

    @Update
    suspend fun updateTransfer(transfer: TransferEntity)

    @Query("UPDATE transfers SET status = :status, progress = :progress, transferredBytes = :transferredBytes, currentChunk = :currentChunk WHERE id = :id")
    suspend fun updateProgress(id: String, status: String, progress: Float, transferredBytes: Long, currentChunk: Int)

    @Query("UPDATE transfers SET status = :status, errorMessage = :error WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, error: String? = null)

    @Query("DELETE FROM transfers WHERE id = :id")
    suspend fun deleteTransfer(id: String)

    @Query("DELETE FROM transfers WHERE status IN ('COMPLETED', 'CANCELLED', 'FAILED')")
    suspend fun clearCompletedTransfers()

    @Query("DELETE FROM transfers")
    suspend fun deleteAllTransfers()
}
