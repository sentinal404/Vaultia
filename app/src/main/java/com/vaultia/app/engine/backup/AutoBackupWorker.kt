package com.vaultia.app.engine.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vaultia.app.VaultiaApplication

class AutoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? VaultiaApplication ?: return Result.failure()
        return try {
            app.autoBackupManager.runIncrementalBackup()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
