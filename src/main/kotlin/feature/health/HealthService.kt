package com.fintrack.feature.health

import com.fintrack.core.logger
import core.DatabaseFactory
import java.lang.management.ManagementFactory
import java.lang.Runtime

class HealthService(
    private val databaseHealthIndicator: DatabaseHealthIndicator,
    private val memoryHealthIndicator: MemoryHealthIndicator
) {
    private val log = logger<HealthService>()

    fun getDetailedHealthStatus(): Map<String, Any> {
        // Check database health
        val dbHealthy = DatabaseFactory.checkConnection()
        databaseHealthIndicator.setHealthy(dbHealthy)

        // Update memory health
        memoryHealthIndicator.updateHealth()

        val runtime = Runtime.getRuntime()
        val memoryUsage = (runtime.totalMemory() - runtime.freeMemory()).toDouble() / runtime.maxMemory()

        val checks = mapOf(
            "database" to mapOf(
                "status" to if (dbHealthy) "UP" else "DOWN",
                "details" to DatabaseFactory.getPoolStats()
            ),
            "memory" to mapOf(
                "status" to if (memoryUsage < 0.9) "UP" else "DOWN",
                "details" to mapOf(
                    "usagePercent" to "%.2f".format(memoryUsage * 100),
                    "usedMB" to (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024,
                    "maxMB" to runtime.maxMemory() / 1024 / 1024
                )
            ),
            "application" to mapOf(
                "status" to "UP",
                "details" to mapOf(
                    "uptime" to ManagementFactory.getRuntimeMXBean().uptime
                )
            )
        )

        val overallStatus = if (dbHealthy && memoryUsage < 0.9) "UP" else "DOWN"

        return mapOf(
            "status" to overallStatus,
            "timestamp" to System.currentTimeMillis(),
            "service" to "fintrack-api",
            "version" to "1.0.0",
            "checks" to checks
        )
    }

    fun checkReadiness(): Boolean {
        val dbHealthy = DatabaseFactory.checkConnection()
        databaseHealthIndicator.setHealthy(dbHealthy)

        val runtime = Runtime.getRuntime()
        val memoryUsage = (runtime.totalMemory() - runtime.freeMemory()).toDouble() / runtime.maxMemory()

        return dbHealthy && memoryUsage < 0.9
    }

    fun getHealthMetrics(): Map<String, Any> {
        val runtime = Runtime.getRuntime()
        val memoryUsage = (runtime.totalMemory() - runtime.freeMemory()).toDouble() / runtime.maxMemory()

        return mapOf(
            "health" to mapOf(
                "database" to if (DatabaseFactory.checkConnection()) "healthy" else "unhealthy",
                "memory" to if (memoryUsage < 0.9) "healthy" else "unhealthy"
            ),
            "resources" to mapOf(
                "memory" to mapOf(
                    "usedMB" to (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024,
                    "maxMB" to runtime.maxMemory() / 1024 / 1024,
                    "usagePercent" to "%.2f".format(memoryUsage * 100)
                ),
                "processors" to runtime.availableProcessors()
            ),
            "database" to DatabaseFactory.getPoolStats()
        )
    }
}