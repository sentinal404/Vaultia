package com.vaultia.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vaultia.app.core.Constants
import com.vaultia.app.data.local.dao.BackupDao
import com.vaultia.app.data.local.dao.ChunkDao
import com.vaultia.app.data.local.dao.FileDao
import com.vaultia.app.data.local.dao.TransferDao
import com.vaultia.app.data.local.dao.WorkerDao
import com.vaultia.app.data.local.entity.BackupFolderEntity
import com.vaultia.app.data.local.entity.BackupRecordEntity
import com.vaultia.app.data.local.entity.ChunkEntity
import com.vaultia.app.data.local.entity.FileEntity
import com.vaultia.app.data.local.entity.TransferEntity
import com.vaultia.app.data.local.entity.WorkerEntity

@Database(
    entities = [
        FileEntity::class,
        ChunkEntity::class,
        TransferEntity::class,
        WorkerEntity::class,
        BackupFolderEntity::class,
        BackupRecordEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class VaultiaDatabase : RoomDatabase() {

    abstract fun fileDao(): FileDao
    abstract fun chunkDao(): ChunkDao
    abstract fun transferDao(): TransferDao
    abstract fun workerDao(): WorkerDao
    abstract fun backupDao(): BackupDao

    companion object {
        @Volatile
        private var INSTANCE: VaultiaDatabase? = null

        fun getInstance(context: Context): VaultiaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VaultiaDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
