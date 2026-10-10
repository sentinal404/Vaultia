package com.vaultia.app.engine.core

import android.content.Context
import com.vaultia.app.core.security.SecureStorageManager
import com.vaultia.app.data.local.VaultiaDatabase
import com.vaultia.app.data.remote.TelegramApiService
import com.vaultia.app.domain.model.VaultFile
import com.vaultia.app.engine.streaming.MediaStreamingServer
import com.vaultia.app.engine.transfer.WorkerScheduler
import java.io.File

/**
 * Manages HTTP 206 Partial Content range-streaming for ExoPlayer media playback.
 * Proxies requested slices from Telegram MTProto / cloud cache with zero buffering.
 */
class MediaStreamingManager(
    private val context: Context,
    private val database: VaultiaDatabase,
    private val telegramApi: TelegramApiService,
    private val secureStorage: SecureStorageManager,
    private val workerScheduler: WorkerScheduler
) {
    private val streamingServer = MediaStreamingServer(
        context = context,
        database = database,
        telegramApi = telegramApi,
        secureStorage = secureStorage,
        workerScheduler = workerScheduler
    )

    fun start(): Int = streamingServer.start()

    fun stop() = streamingServer.stop()

    fun getStreamUrl(fileId: String): String = streamingServer.getStreamUrl(fileId)

    fun prewarmMedia(vaultFile: VaultFile) = streamingServer.prewarmMedia(vaultFile)

    val serverPort: Int
        get() = streamingServer.serverPort

    fun cleanCache() {
        try {
            val cacheDir = File(context.cacheDir, "vaultia_stream_cache")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { it.delete() }
            }
        } catch (_: Exception) {}
    }

    fun getCacheSizeBytes(): Long {
        return try {
            val cacheDir = File(context.cacheDir, "vaultia_stream_cache")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
            } else 0L
        } catch (_: Exception) {
            0L
        }
    }
}
