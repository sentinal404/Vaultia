package com.vaultia.app.engine.transfer

import com.vaultia.app.domain.model.WorkerBot
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class WorkerScheduler {

    private val mutex = Mutex()
    private val workers = mutableListOf<WorkerBot>()
    private val activeLoads = ConcurrentHashMap<String, AtomicInteger>()
    private var roundRobinIndex = 0

    suspend fun setWorkers(botList: List<WorkerBot>) {
        mutex.withLock {
            workers.clear()
            workers.addAll(botList)
            botList.forEach { bot ->
                activeLoads.putIfAbsent(bot.id, AtomicInteger(0))
            }
            if (roundRobinIndex >= workers.size) {
                roundRobinIndex = 0
            }
        }
    }

    suspend fun getWorkerCount(): Int {
        return mutex.withLock { workers.size }
    }

    /**
     * Intelligently dispatches work by selecting the least-loaded healthy worker bot
     * to maximize parallel upload & download throughput across multiple bots.
     */
    suspend fun acquireWorker(): WorkerBot? {
        mutex.withLock {
            if (workers.isEmpty()) return null
            val now = System.currentTimeMillis()

            val healthyWorkers = workers.filter { worker ->
                worker.isOnline && worker.isAuthorized &&
                        (!worker.isRateLimited || worker.rateLimitResetTimestamp <= now)
            }

            val candidate = if (healthyWorkers.isNotEmpty()) {
                // Select worker with lowest in-flight chunk operations
                healthyWorkers.minByOrNull { activeLoads[it.id]?.get() ?: 0 }
            } else {
                // If all temporarily throttled, pick earliest reset
                workers.minByOrNull { it.rateLimitResetTimestamp }
            }

            if (candidate != null) {
                activeLoads.getOrPut(candidate.id) { AtomicInteger(0) }.incrementAndGet()
            }
            return candidate
        }
    }

    fun releaseWorker(workerId: String?) {
        if (workerId == null) return
        activeLoads[workerId]?.let { count ->
            if (count.get() > 0) {
                count.decrementAndGet()
            }
        }
    }

    suspend fun getAvailableWorker(): WorkerBot? {
        return acquireWorker()
    }

    suspend fun markRateLimited(workerId: String, retryAfterSeconds: Int) {
        mutex.withLock {
            val idx = workers.indexOfFirst { it.id == workerId }
            if (idx != -1) {
                val resetTime = System.currentTimeMillis() + (retryAfterSeconds * 1000L)
                workers[idx] = workers[idx].copy(
                    isRateLimited = true,
                    rateLimitResetTimestamp = resetTime
                )
            }
        }
    }

    suspend fun markSuccess(workerId: String) {
        mutex.withLock {
            val idx = workers.indexOfFirst { it.id == workerId }
            if (idx != -1) {
                workers[idx] = workers[idx].copy(
                    isRateLimited = false,
                    rateLimitResetTimestamp = 0L
                )
            }
        }
    }
}
