package com.fintrack.plugins

import com.fintrack.core.logger
import com.fintrack.core.withContext
import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry


// Global reference to the metrics registry
lateinit var appMicrometerRegistry: PrometheusMeterRegistry

fun Application.configureMetrics() {
    val log = logger<Application>()
    // Create Prometheus registry
    appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    // Install Micrometer Metrics
    install(MicrometerMetrics) {
        registry = appMicrometerRegistry
        // Add JVM metrics binders
        meterBinders = listOf(
            ClassLoaderMetrics(),
            JvmMemoryMetrics(),
            JvmGcMetrics(),
            ProcessorMetrics(),
            JvmThreadMetrics(),
            UptimeMetrics()
        )
    }

    log.withContext(
        "endpoint" to "/metrics",
        "registryType" to (appMicrometerRegistry::class.simpleName ?: "Unknown")
    ).info{ "Micrometer metrics configured successfully" }
}