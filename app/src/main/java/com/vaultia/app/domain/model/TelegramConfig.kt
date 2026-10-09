package com.vaultia.app.domain.model

data class TelegramConfig(
    val botToken: String,
    val botName: String,
    val botUsername: String,
    val channelId: String,
    val channelTitle: String,
    val isConfigured: Boolean,
    val isVerified: Boolean
)
