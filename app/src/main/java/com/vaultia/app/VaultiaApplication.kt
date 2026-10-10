package com.vaultia.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.vaultia.app.core.Constants
import com.vaultia.app.core.security.AppLockManager
import com.vaultia.app.core.security.SecureStorageManager
import com.vaultia.app.data.local.VaultiaDatabase
import com.vaultia.app.data.remote.TelegramApiService
import com.vaultia.app.data.repository.SettingsRepository
import com.vaultia.app.data.repository.VaultRepository
import com.vaultia.app.engine.chunk.ChunkEngine
import com.vaultia.app.engine.streaming.MediaStreamingServer
import com.vaultia.app.engine.transfer.DownloadManager
import com.vaultia.app.engine.transfer.UploadManager
import com.vaultia.app.engine.transfer.WorkerScheduler
import com.vaultia.app.player.VideoPlayerManager
import kotlinx.coroutines.launch

class VaultiaApplication : Application() {

    lateinit var database: VaultiaDatabase
        private set
    lateinit var secureStorage: SecureStorageManager
        private set
    lateinit var appLockManager: AppLockManager
        private set
    lateinit var telegramApi: TelegramApiService
        private set
    lateinit var chunkEngine: ChunkEngine
        private set
    lateinit var workerScheduler: WorkerScheduler
        private set
    lateinit var uploadManager: UploadManager
        private set
    lateinit var downloadManager: DownloadManager
        private set
    lateinit var streamingServer: MediaStreamingServer
        private set
    lateinit var vaultRepository: VaultRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var videoPlayerManager: VideoPlayerManager
        private set
    lateinit var autoBackupManager: com.vaultia.app.engine.backup.AutoBackupManager
        private set
    lateinit var bridgeServer: com.vaultia.app.engine.bridge.TelegramBridgeServer
        private set
    lateinit var cloudSyncManager: com.vaultia.app.engine.sync.CloudSyncManager
        private set

    // New Clean Service Architecture
    lateinit var telegramClientManager: com.vaultia.app.engine.core.TelegramClientManager
        private set
    lateinit var cloudManifestRepository: com.vaultia.app.engine.core.CloudManifestRepository
        private set
    lateinit var mediaStreamingManager: com.vaultia.app.engine.core.MediaStreamingManager
        private set
    lateinit var telegramBridgeManager: com.vaultia.app.engine.core.TelegramBridgeManager
        private set
    lateinit var transferQueueManager: com.vaultia.app.engine.core.TransferQueueManager
        private set
    lateinit var cloudRepository: com.vaultia.app.engine.core.CloudRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        createNotificationChannels()

        database = VaultiaDatabase.getInstance(this)
        secureStorage = SecureStorageManager(this)
        appLockManager = AppLockManager(secureStorage)
        telegramApi = TelegramApiService().apply {
            customBaseUrl = secureStorage.customApiUrl
        }
        chunkEngine = ChunkEngine(this)
        workerScheduler = WorkerScheduler()

        uploadManager = UploadManager(
            context = this,
            database = database,
            telegramApi = telegramApi,
            secureStorage = secureStorage,
            chunkEngine = chunkEngine,
            workerScheduler = workerScheduler
        )

        downloadManager = DownloadManager(
            context = this,
            database = database,
            telegramApi = telegramApi,
            secureStorage = secureStorage,
            chunkEngine = chunkEngine,
            workerScheduler = workerScheduler
        )

        streamingServer = MediaStreamingServer(
            context = this,
            database = database,
            telegramApi = telegramApi,
            secureStorage = secureStorage,
            workerScheduler = workerScheduler
        )
        streamingServer.start()

        vaultRepository = VaultRepository(
            context = this,
            database = database,
            telegramApi = telegramApi,
            secureStorage = secureStorage,
            chunkEngine = chunkEngine,
            workerScheduler = workerScheduler,
            uploadManager = uploadManager,
            downloadManager = downloadManager,
            streamingServer = streamingServer
        )

        settingsRepository = SettingsRepository(this)
        videoPlayerManager = VideoPlayerManager(this)

        autoBackupManager = com.vaultia.app.engine.backup.AutoBackupManager(
            context = this,
            database = database,
            uploadManager = uploadManager,
            secureStorage = secureStorage
        )
        if (autoBackupManager.isEnabled) {
            autoBackupManager.scheduleWork()
        }

        bridgeServer = com.vaultia.app.engine.bridge.TelegramBridgeServer(
            context = this,
            database = database,
            telegramApi = telegramApi,
            secureStorage = secureStorage
        )

        cloudSyncManager = com.vaultia.app.engine.sync.CloudSyncManager(
            context = this,
            database = database,
            telegramApi = telegramApi,
            secureStorage = secureStorage
        )

        // New Clean Service Architecture Singletons
        telegramClientManager = com.vaultia.app.engine.core.TelegramClientManager(
            context = this,
            secureStorage = secureStorage
        )

        cloudManifestRepository = com.vaultia.app.engine.core.CloudManifestRepository(
            context = this,
            database = database,
            secureStorage = secureStorage
        )

        mediaStreamingManager = com.vaultia.app.engine.core.MediaStreamingManager(
            context = this,
            database = database,
            telegramApi = telegramApi,
            secureStorage = secureStorage,
            workerScheduler = workerScheduler
        )

        telegramBridgeManager = com.vaultia.app.engine.core.TelegramBridgeManager(
            context = this,
            database = database,
            telegramApi = telegramApi,
            secureStorage = secureStorage
        )

        transferQueueManager = com.vaultia.app.engine.core.TransferQueueManager(
            context = this,
            database = database,
            uploadManager = uploadManager,
            downloadManager = downloadManager
        )

        cloudRepository = com.vaultia.app.engine.core.CloudRepository(
            database = database,
            secureStorage = secureStorage,
            clientManager = telegramClientManager,
            manifestRepository = cloudManifestRepository,
            streamingManager = mediaStreamingManager,
            uploadManager = uploadManager,
            downloadManager = downloadManager,
            chunkEngine = chunkEngine
        )

        // Clean old streaming cache and temp files to ensure device storage is never filled
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val streamCacheDir = java.io.File(cacheDir, "vaultia_stream_cache")
                if (streamCacheDir.exists()) {
                    streamCacheDir.listFiles()?.forEach { file ->
                        if (System.currentTimeMillis() - file.lastModified() > 6 * 3600 * 1000L) {
                            file.delete()
                        }
                    }
                }
                val tdlibFilesDir = java.io.File(cacheDir, "tdlib_files")
                if (tdlibFilesDir.exists()) {
                    tdlibFilesDir.listFiles()?.forEach { file ->
                        if (System.currentTimeMillis() - file.lastModified() > 6 * 3600 * 1000L) {
                            file.delete()
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val transferChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID_TRANSFER,
                "Vault Transfers",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time upload and download progress"
            }

            val playerChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID_PLAYER,
                "Media Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Media playback controls"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(transferChannel)
            notificationManager.createNotificationChannel(playerChannel)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        streamingServer.stop()
        videoPlayerManager.release()
    }

    companion object {
        lateinit var instance: VaultiaApplication
            private set
    }
}
