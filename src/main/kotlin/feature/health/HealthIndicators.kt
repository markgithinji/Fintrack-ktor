package com.fintrack.feature.health

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import java.util.concurrent.atomic.AtomicReference
import java.lang.Runtime

class DatabaseHealthIndicator : MeterBinder {
    private val databaseStatus = AtomicReference(0.0) // 1.0 = healthy, 0.0 = unhealthy

    override fun bindTo(registry: MeterRegistry) {
        Gauge.builder("database.health") { databaseStatus.get() }
            .tag("component", "database")
            .register(registry)
    }

    fun setHealthy(healthy: Boolean) {
        databaseStatus.set(if (healthy) 1.0 else 0.0)
    }
}

class MemoryHealthIndicator : MeterBinder {
    private val memoryHealthStatus = AtomicReference(0.0)

    override fun bindTo(registry: MeterRegistry) {
        // Memory health gauge (1.0 = healthy, 0.0 = unhealthy)
        Gauge.builder("memory.health") { memoryHealthStatus.get() }
            .tag("component", "memory")
            .register(registry)

        // Memory usage percentage gauge
        Gauge.builder("memory.usage.percent") { getMemoryUsagePercent() }
            .tag("component", "memory")
            .register(registry)
    }

    private fun getMemoryUsagePercent(): Double {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        return (usedMemory.toDouble() / runtime.maxMemory()) * 100.0
    }

    fun updateHealth() {
        val usage = getMemoryUsagePercent()
        memoryHealthStatus.set(if (usage < 90.0) 1.0 else 0.0) // Healthy if < 90% usage
    }
}