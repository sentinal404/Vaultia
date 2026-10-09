package com.vaultia.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vaultia.app.domain.model.WorkerBot

@Entity(tableName = "workers")
data class WorkerEntity(
    @PrimaryKey
    val id: String,
    val token: String,
    val name: String,
    val username: String,
    val isPrimary: Boolean,
    val isOnline: Boolean = true,
    val isAuthorized: Boolean = true,
    val hasChannelAccess: Boolean = true,
    val isRateLimited: Boolean = false,
    val rateLimitResetTimestamp: Long = 0L
) {
    fun toDomain(): WorkerBot {
        return WorkerBot(
            id = id,
            token = token,
            name = name,
            username = username,
            isPrimary = isPrimary,
            isOnline = isOnline,
            isAuthorized = isAuthorized,
            hasChannelAccess = hasChannelAccess,
            isRateLimited = isRateLimited,
            rateLimitResetTimestamp = rateLimitResetTimestamp,
            activeTasks = 0
        )
    }

    companion object {
        fun fromDomain(worker: WorkerBot): WorkerEntity {
            return WorkerEntity(
                id = worker.id,
                token = worker.token,
                name = worker.name,
                username = worker.username,
                isPrimary = worker.isPrimary,
                isOnline = worker.isOnline,
                isAuthorized = worker.isAuthorized,
                hasChannelAccess = worker.hasChannelAccess,
                isRateLimited = worker.isRateLimited,
                rateLimitResetTimestamp = worker.rateLimitResetTimestamp
            )
        }
    }
}
