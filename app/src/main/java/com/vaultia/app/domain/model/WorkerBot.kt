package com.vaultia.app.domain.model

data class WorkerBot(
    val id: String,
    val token: String,
    val name: String,
    val username: String,
    val isPrimary: Boolean = false,
    val isOnline: Boolean = true,
    val isAuthorized: Boolean = true,
    val hasChannelAccess: Boolean = true,
    val isRateLimited: Boolean = false,
    val rateLimitResetTimestamp: Long = 0L,
    val activeTasks: Int = 0
)
