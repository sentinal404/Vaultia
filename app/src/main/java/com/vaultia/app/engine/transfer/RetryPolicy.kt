package com.vaultia.app.engine.transfer

import com.vaultia.app.core.Constants
import kotlinx.coroutines.delay

class RetryPolicy(
    private val maxRetries: Int = Constants.MAX_RETRY_ATTEMPTS,
    private val initialBackoffMs: Long = Constants.INITIAL_RETRY_BACKOFF_MS
) {

    suspend fun <T> executeWithRetry(
        actionName: String,
        isRateLimited: (throwable: Throwable?) -> Int?,
        block: suspend (attempt: Int) -> T
    ): T {
        var currentAttempt = 0
        var backoffMs = initialBackoffMs

        while (true) {
            try {
                return block(currentAttempt)
            } catch (e: Exception) {
                currentAttempt++
                val rateLimitWaitSec = isRateLimited(e)

                if (rateLimitWaitSec != null && rateLimitWaitSec > 0) {
                    // Telegram rate limit encountered
                    val waitTime = (rateLimitWaitSec * 1000L).coerceAtLeast(1000L)
                    delay(waitTime)
                    continue
                }

                if (currentAttempt >= maxRetries || isPermanentError(e)) {
                    throw e
                }

                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(30000L) // exponential backoff with 30s cap
            }
        }
    }

    private fun isPermanentError(e: Throwable): Boolean {
        val msg = e.message?.lowercase() ?: ""
        return msg.contains("unauthorized") ||
                msg.contains("forbidden") ||
                msg.contains("chat not found") ||
                msg.contains("bot was blocked") ||
                msg.contains("not an administrator")
    }
}
