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

    override fun onCreate() {
        super.onCreate()
        instance = this

        createNotificationChannels()

        database = VaultiaDatabase.getInstance(this)
        secureStorage = SecureStorageManager(this)
        appLockManager = AppLockManager(secureStorage)
        telegramApi = TelegramApiService()
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
