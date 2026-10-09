package com.vaultia.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vaultia.app.data.local.entity.WorkerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerDao {

    @Query("SELECT * FROM workers ORDER BY isPrimary DESC, name ASC")
    fun getAllWorkers(): Flow<List<WorkerEntity>>

    @Query("SELECT * FROM workers ORDER BY isPrimary DESC, name ASC")
    suspend fun getAllWorkersList(): List<WorkerEntity>

    @Query("SELECT * FROM workers WHERE isPrimary = 1 LIMIT 1")
    suspend fun getPrimaryWorker(): WorkerEntity?

    @Query("SELECT * FROM workers WHERE id = :id LIMIT 1")
    suspend fun getWorkerById(id: String): WorkerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: WorkerEntity)

    @Update
    suspend fun updateWorker(worker: WorkerEntity)

    @Query("UPDATE workers SET isRateLimited = :isRateLimited, rateLimitResetTimestamp = :resetTime WHERE id = :id")
    suspend fun updateRateLimit(id: String, isRateLimited: Boolean, resetTime: Long)

    @Query("DELETE FROM workers WHERE id = :id")
    suspend fun deleteWorker(id: String)

    @Query("DELETE FROM workers")
    suspend fun deleteAllWorkers()
}
