package com.vaultia.app.engine.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.vaultia.app.core.security.SecureStorageManager
import com.vaultia.app.data.local.VaultiaDatabase
import com.vaultia.app.data.local.entity.BackupFolderEntity
import com.vaultia.app.data.local.entity.BackupRecordEntity
import com.vaultia.app.domain.model.VaultFile
import com.vaultia.app.engine.transfer.UploadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

data class BackupSyncProgress(
    val isSyncing: Boolean = false,
    val currentFileName: String = "",
    val totalScanned: Int = 0,
    val newlyUploaded: Int = 0,
    val alreadySynced: Int = 0,
    val lastSyncMessage: String = ""
)

class AutoBackupManager(
    private val context: Context,
    private val database: VaultiaDatabase,
    private val uploadManager: UploadManager,
    private val secureStorage: SecureStorageManager
) {

    private val backupDao = database.backupDao()
    private val _syncProgress = MutableStateFlow(BackupSyncProgress())
    val syncProgress = _syncProgress.asStateFlow()

    val foldersFlow: Flow<List<BackupFolderEntity>> = backupDao.getAllFoldersFlow()
    val totalBackedUpCountFlow: Flow<Int> = backupDao.getTotalBackedUpFilesCount()

    companion object {
        private const val TAG = "AutoBackupManager"
        const val WORK_TAG = "vaultia_auto_backup_worker"
        private const val PREFS_BACKUP = "vaultia_backup_settings"
        private const val KEY_ENABLED = "backup_enabled"
        private const val KEY_WIFI_ONLY = "backup_wifi_only"
        private const val KEY_CHARGING_ONLY = "backup_charging_only"
        private const val KEY_INTERVAL_HOURS = "backup_interval_hours"
        private const val KEY_DAILY_LIMIT_BYTES = "backup_daily_limit_bytes"
        private const val KEY_TODAY_UPLOADED_BYTES = "backup_today_uploaded_bytes"
        private const val KEY_LAST_QUOTA_DATE = "backup_last_quota_date"
    }

    private val prefs = context.getSharedPreferences(PREFS_BACKUP, Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ENABLED, value).apply()
            if (value) scheduleWork() else cancelScheduledWork()
        }

    var isWifiOnly: Boolean
        get() = prefs.getBoolean(KEY_WIFI_ONLY, true)
        set(value) {
            prefs.edit().putBoolean(KEY_WIFI_ONLY, value).apply()
            if (isEnabled) scheduleWork()
        }

    var isChargingOnly: Boolean
        get() = prefs.getBoolean(KEY_CHARGING_ONLY, true)
        set(value) {
            prefs.edit().putBoolean(KEY_CHARGING_ONLY, value).apply()
            if (isEnabled) scheduleWork()
        }

    var intervalHours: Long
        get() = prefs.getLong(KEY_INTERVAL_HOURS, 24L)
        set(value) {
            prefs.edit().putLong(KEY_INTERVAL_HOURS, value).apply()
            if (isEnabled) scheduleWork()
        }

    // Daily Data Limit in Bytes (0 = Unlimited, e.g. 100MB, 500MB)
    var dailyDataLimitBytes: Long
        get() = prefs.getLong(KEY_DAILY_LIMIT_BYTES, 0L)
        set(value) = prefs.edit().putLong(KEY_DAILY_LIMIT_BYTES, value).apply()

    fun getTodayUploadedBytes(): Long {
        val today = getTodayDateKey()
        val savedDate = prefs.getString(KEY_LAST_QUOTA_DATE, "")
        return if (savedDate == today) {
            prefs.getLong(KEY_TODAY_UPLOADED_BYTES, 0L)
        } else {
            0L
        }
    }

    private fun addTodayUploadedBytes(bytes: Long) {
        val today = getTodayDateKey()
        val current = getTodayUploadedBytes()
        prefs.edit()
            .putString(KEY_LAST_QUOTA_DATE, today)
            .putLong(KEY_TODAY_UPLOADED_BYTES, current + bytes)
            .apply()
    }

    private fun getTodayDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    suspend fun addFolder(treeUri: Uri, displayName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Persist permission so background worker can access folder across reboots
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, flags)

            val folder = BackupFolderEntity(
                id = UUID.randomUUID().toString(),
                treeUri = treeUri.toString(),
                displayName = displayName,
                isEnabled = true,
                lastBackupTimestamp = 0L,
                totalFilesTracked = 0
            )
            backupDao.insertFolder(folder)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist tree permission or add folder: ${e.message}", e)
            false
        }
    }

    suspend fun removeFolder(folderId: String) = withContext(Dispatchers.IO) {
        backupDao.deleteRecordsForFolder(folderId)
        backupDao.deleteFolder(folderId)
    }

    suspend fun toggleFolder(folder: BackupFolderEntity) = withContext(Dispatchers.IO) {
        backupDao.updateFolder(folder.copy(isEnabled = !folder.isEnabled))
    }

    /**
     * Executes intelligent incremental backup:
     * - Scans all files across enabled directories
     * - Identifies files already backed up vs new/changed files
     * - Respects daily quota limit (e.g. 100MB / 500MB): stops and defers remaining files to next 24h run
     * - Uploads ONLY the new/changed files to the private Telegram vault
     * - Zero redundant re-uploads
     */
    suspend fun runIncrementalBackup(): BackupSyncProgress = withContext(Dispatchers.IO) {
        if (!secureStorage.isTelegramConfigured) {
            val msg = "Cannot backup: Telegram storage backend not configured"
            _syncProgress.value = BackupSyncProgress(lastSyncMessage = msg)
            return@withContext _syncProgress.value
        }

        _syncProgress.value = BackupSyncProgress(isSyncing = true, lastSyncMessage = "Scanning folders...")

        var scannedTotal = 0
        var uploadedTotal = 0
        var skippedTotal = 0
        var quotaExhausted = false

        val limit = dailyDataLimitBytes
        var currentTodayUsage = getTodayUploadedBytes()

        val folders = backupDao.getEnabledFoldersSync()
        for (folder in folders) {
            if (quotaExhausted) break

            try {
                val treeUri = Uri.parse(folder.treeUri)
                val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: continue
                val filesToScan = mutableListOf<DocumentFile>()
                collectFilesRecursively(rootDoc, filesToScan)

                var folderNewCount = 0
                for (doc in filesToScan) {
                    if (!doc.isFile || doc.length() <= 0) continue

                    scannedTotal++
                    val fileName = doc.name ?: "backup_${System.currentTimeMillis()}"
                    val fileSize = doc.length()
                    val lastModified = doc.lastModified()

                    _syncProgress.value = _syncProgress.value.copy(
                        currentFileName = fileName,
                        totalScanned = scannedTotal,
                        newlyUploaded = uploadedTotal,
                        alreadySynced = skippedTotal,
                        lastSyncMessage = "Inspecting $fileName"
                    )

                    // Check if already backed up previously
                    val existing = backupDao.findRecord(folder.id, fileName, fileSize, lastModified)
                    if (existing != null && existing.status == "COMPLETED") {
                        skippedTotal++
                        continue
                    }

                    // Check Daily Data Limit: if exceeded, halt uploads and wait for next 24hr backup
                    if (limit > 0 && (currentTodayUsage + fileSize) > limit) {
                        quotaExhausted = true
                        _syncProgress.value = _syncProgress.value.copy(
                            lastSyncMessage = "Daily backup limit reached (${VaultFile.formatBytes(limit)}). Remaining files saved for next 24h run."
                        )
                        break
                    }

                    // New file detected -> copy to temp cache and enqueue upload to Telegram vault
                    try {
                        val tempFolder = File(context.cacheDir, "vaultia_sync_${UUID.randomUUID()}").apply { mkdirs() }
                        val tempFile = File(tempFolder, fileName)
                        context.contentResolver.openInputStream(doc.uri)?.use { input ->
                            FileOutputStream(tempFile).use { out ->
                                input.copyTo(out)
                            }
                        }

                        if (tempFile.exists() && tempFile.length() > 0) {
                            uploadManager.enqueueUpload(tempFile, customFileName = fileName)

                            val record = BackupRecordEntity(
                                id = UUID.randomUUID().toString(),
                                folderId = folder.id,
                                fileUri = doc.uri.toString(),
                                fileName = fileName,
                                fileSize = fileSize,
                                lastModified = lastModified,
                                mimeType = doc.type ?: "application/octet-stream",
                                status = "COMPLETED",
                                uploadedAt = System.currentTimeMillis()
                            )
                            backupDao.insertRecord(record)
                            uploadedTotal++
                            folderNewCount++

                            addTodayUploadedBytes(fileSize)
                            currentTodayUsage += fileSize
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error backing up file $fileName: ${e.message}")
                    }
                }

                // Update folder sync stats
                val totalInFolder = backupDao.getFolderBackedUpCount(folder.id)
                backupDao.updateFolder(
                    folder.copy(
                        lastBackupTimestamp = System.currentTimeMillis(),
                        totalFilesTracked = totalInFolder
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed scanning folder ${folder.displayName}: ${e.message}")
            }
        }

        val summaryMessage = when {
            quotaExhausted -> {
                "Daily quota of ${VaultFile.formatBytes(limit)} reached! Uploaded $uploadedTotal new files. Remaining files deferred to tomorrow's backup."
            }
            uploadedTotal > 0 -> {
                "Backup complete! Protected $uploadedTotal new files ($skippedTotal already in vault)."
            }
            else -> {
                "All $scannedTotal files are already backed up and up-to-date in vault."
            }
        }

        val finalProgress = BackupSyncProgress(
            isSyncing = false,
            currentFileName = "",
            totalScanned = scannedTotal,
            newlyUploaded = uploadedTotal,
            alreadySynced = skippedTotal,
            lastSyncMessage = summaryMessage
        )
        _syncProgress.value = finalProgress
        finalProgress
    }

    private fun collectFilesRecursively(dir: DocumentFile, list: MutableList<DocumentFile>) {
        val children = dir.listFiles()
        for (child in children) {
            if (child.isDirectory) {
                collectFilesRecursively(child, list)
            } else if (child.isFile) {
                list.add(child)
            }
        }
    }

    fun scheduleWork() {
        val constraints = Constraints.Builder().apply {
            if (isWifiOnly) {
                setRequiredNetworkType(NetworkType.UNMETERED)
            } else {
                setRequiredNetworkType(NetworkType.CONNECTED)
            }
            if (isChargingOnly) {
                setRequiresCharging(true)
            }
        }.build()

        val hours = intervalHours.coerceAtLeast(1L)
        val workRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(hours, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag(WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun cancelScheduledWork() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_TAG)
    }
}
