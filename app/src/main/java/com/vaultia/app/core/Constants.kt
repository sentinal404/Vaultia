package com.vaultia.app.core

object Constants {
    const val TELEGRAM_API_BASE_URL = "https://api.telegram.org"
    const val DEFAULT_CHUNK_SIZE_BYTES = 20 * 1024 * 1024 // 20 MB (safe for Telegram Bot API 50MB limit)
    const val MIN_CHUNK_SIZE_BYTES = 5 * 1024 * 1024     // 5 MB
    const val MAX_CHUNK_SIZE_BYTES = 45 * 1024 * 1024    // 45 MB
    
    const val DEFAULT_MAX_PARALLEL_WORKERS = 3
    const val MAX_ALLOWED_WORKERS = 5
    const val MAX_RETRY_ATTEMPTS = 5
    const val INITIAL_RETRY_BACKOFF_MS = 2000L
    
    const val DATABASE_NAME = "vaultia_database"
    const val PREFS_SECURE_NAME = "vaultia_secure_prefs"
    const val PREFS_SETTINGS_NAME = "vaultia_settings_prefs"
    
    const val NOTIFICATION_CHANNEL_ID_TRANSFER = "vaultia_transfers"
    const val NOTIFICATION_CHANNEL_ID_PLAYER = "vaultia_media_player"
    const val NOTIFICATION_ID_TRANSFER = 1001
    const val NOTIFICATION_ID_PLAYER = 1002
    
    const val LOCAL_STREAMING_HOST = "127.0.0.1"
    
    // Telegram caption delimiter for metadata recovery
    const val METADATA_TAG = "#VAULTIA_CHUNK"
}
