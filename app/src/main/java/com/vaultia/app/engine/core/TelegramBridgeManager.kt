package com.vaultia.app.engine.core

import android.content.Context
import com.vaultia.app.core.security.SecureStorageManager
import com.vaultia.app.data.local.VaultiaDatabase
import com.vaultia.app.data.remote.TelegramApiService
import com.vaultia.app.engine.bridge.BridgeServerState
import com.vaultia.app.engine.bridge.TelegramBridgeServer
import kotlinx.coroutines.flow.StateFlow

/**
 * Isolated Telegram Bot Bridge Manager.
 * Strictly restricted to receiving incoming forwarded media when the user explicitly
 * taps "Start Server" in the UI. Never used for regular app uploads/downloads/streaming.
 */
class TelegramBridgeManager(
    private val context: Context,
    private val database: VaultiaDatabase,
    private val telegramApi: TelegramApiService,
    private val secureStorage: SecureStorageManager
) {
    private val bridgeServer = TelegramBridgeServer(context, database, telegramApi, secureStorage)

    val serverState: StateFlow<BridgeServerState> = bridgeServer.serverState

    fun startServer(durationMinutes: Int = 10) {
        bridgeServer.startServer(durationMinutes)
    }

    fun stopServer() {
        bridgeServer.stopServer()
    }

    fun isConfigured(): Boolean {
        return !secureStorage.botToken.isNullOrBlank() && !secureStorage.channelId.isNullOrBlank()
    }
}
