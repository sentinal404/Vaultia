package com.vaultia.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.vaultia.app.core.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

data class UserSettings(
    val maxWorkers: Int = Constants.DEFAULT_MAX_PARALLEL_WORKERS,
    val chunkSizeMb: Int = 20,
    val wifiOnlyUploads: Boolean = false,
    val wifiOnlyDownloads: Boolean = false,
    val autoResume: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isAppLockEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false
)

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(Constants.PREFS_SETTINGS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private fun loadSettings(): UserSettings {
        return UserSettings(
            maxWorkers = prefs.getInt("pref_max_workers", Constants.DEFAULT_MAX_PARALLEL_WORKERS),
            chunkSizeMb = prefs.getInt("pref_chunk_size_mb", 20),
            wifiOnlyUploads = prefs.getBoolean("pref_wifi_only_uploads", false),
            wifiOnlyDownloads = prefs.getBoolean("pref_wifi_only_downloads", false),
            autoResume = prefs.getBoolean("pref_auto_resume", true),
            themeMode = try {
                ThemeMode.valueOf(prefs.getString("pref_theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
            } catch (e: Exception) {
                ThemeMode.SYSTEM
            },
            isAppLockEnabled = prefs.getBoolean("pref_app_lock", false),
            isBiometricEnabled = prefs.getBoolean("pref_biometric", false)
        )
    }

    fun updateMaxWorkers(count: Int) {
        val safe = count.coerceIn(1, Constants.MAX_ALLOWED_WORKERS)
        prefs.edit().putInt("pref_max_workers", safe).apply()
        _settings.value = _settings.value.copy(maxWorkers = safe)
    }

    fun updateChunkSizeMb(sizeMb: Int) {
        val safe = sizeMb.coerceIn(5, 45)
        prefs.edit().putInt("pref_chunk_size_mb", safe).apply()
        _settings.value = _settings.value.copy(chunkSizeMb = safe)
    }

    fun setWifiOnlyUploads(enabled: Boolean) {
        prefs.edit().putBoolean("pref_wifi_only_uploads", enabled).apply()
        _settings.value = _settings.value.copy(wifiOnlyUploads = enabled)
    }

    fun setWifiOnlyDownloads(enabled: Boolean) {
        prefs.edit().putBoolean("pref_wifi_only_downloads", enabled).apply()
        _settings.value = _settings.value.copy(wifiOnlyDownloads = enabled)
    }

    fun setAutoResume(enabled: Boolean) {
        prefs.edit().putBoolean("pref_auto_resume", enabled).apply()
        _settings.value = _settings.value.copy(autoResume = enabled)
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("pref_theme_mode", mode.name).apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun setAppLock(enabled: Boolean) {
        prefs.edit().putBoolean("pref_app_lock", enabled).apply()
        _settings.value = _settings.value.copy(isAppLockEnabled = enabled)
    }

    fun setBiometric(enabled: Boolean) {
        prefs.edit().putBoolean("pref_biometric", enabled).apply()
        _settings.value = _settings.value.copy(isBiometricEnabled = enabled)
    }
}
