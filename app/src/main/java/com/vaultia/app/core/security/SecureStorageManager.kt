package com.vaultia.app.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.vaultia.app.core.Constants
import java.security.MessageDigest

class SecureStorageManager(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            Constants.PREFS_SECURE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback for devices with keystore issues during unit test or custom ROM
        context.getSharedPreferences(Constants.PREFS_SECURE_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        private const val KEY_BOT_TOKEN = "key_tg_bot_token"
        private const val KEY_CHANNEL_ID = "key_tg_channel_id"
        private const val KEY_CHANNEL_TITLE = "key_tg_channel_title"
        private const val KEY_BOT_NAME = "key_tg_bot_name"
        private const val KEY_BOT_USERNAME = "key_tg_bot_username"
        private const val KEY_APP_LOCK_PIN_HASH = "key_app_lock_pin_hash"
        private const val KEY_APP_LOCK_ENABLED = "key_app_lock_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
    }

    var botToken: String?
        get() = prefs.getString(KEY_BOT_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_BOT_TOKEN, value).apply()

    var channelId: String?
        get() = prefs.getString(KEY_CHANNEL_ID, null)
        set(value) = prefs.edit().putString(KEY_CHANNEL_ID, value).apply()

    var channelTitle: String?
        get() = prefs.getString(KEY_CHANNEL_TITLE, null)
        set(value) = prefs.edit().putString(KEY_CHANNEL_TITLE, value).apply()

    var botName: String?
        get() = prefs.getString(KEY_BOT_NAME, null)
        set(value) = prefs.edit().putString(KEY_BOT_NAME, value).apply()

    var botUsername: String?
        get() = prefs.getString(KEY_BOT_USERNAME, null)
        set(value) = prefs.edit().putString(KEY_BOT_USERNAME, value).apply()

    val isTelegramConfigured: Boolean
        get() = !botToken.isNullOrBlank() && !channelId.isNullOrBlank()

    fun setAppLockPin(pin: String) {
        val hash = hashPin(pin)
        prefs.edit()
            .putString(KEY_APP_LOCK_PIN_HASH, hash)
            .putBoolean(KEY_APP_LOCK_ENABLED, true)
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_APP_LOCK_PIN_HASH, null) ?: return false
        return storedHash == hashPin(pin)
    }

    var isAppLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, value).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    fun clearAppLock() {
        prefs.edit()
            .remove(KEY_APP_LOCK_PIN_HASH)
            .putBoolean(KEY_APP_LOCK_ENABLED, false)
            .putBoolean(KEY_BIOMETRIC_ENABLED, false)
            .apply()
    }

    fun clearAllCredentials() {
        prefs.edit().clear().apply()
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
