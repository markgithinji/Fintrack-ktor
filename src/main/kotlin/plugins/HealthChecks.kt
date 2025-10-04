package com.fintrack.plugins

import com.fintrack.feature.health.DatabaseHealthIndicator
import com.fintrack.feature.health.MemoryHealthIndicator
import com.fintrack.core.logger
import com.fintrack.core.withContext
import io.ktor.server.application.Application
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import org.koin.ktor.ext.inject


fun Application.configureHealthChecks() {
    val log = logger<Application>()

    log.withContext("step" to "health_checks_setup").info{ "Configuring Micrometer health checks" }

    // Get health indicators from DI
    val databaseHealthIndicator by inject<DatabaseHealthIndicator>()
    val memoryHealthIndicator by inject<MemoryHealthIndicator>()

    // Bind health indicators to registry
    databaseHealthIndicator.bindTo(appMicrometerRegistry)
    memoryHealthIndicator.bindTo(appMicrometerRegistry)

    // Add additional health-related metrics
    ClassLoaderMetrics().bindTo(appMicrometerRegistry)
    JvmMemoryMetrics().bindTo(appMicrometerRegistry)
    JvmGcMetrics().bindTo(appMicrometerRegistry)
    JvmThreadMetrics().bindTo(appMicrometerRegistry)
    ProcessorMetrics().bindTo(appMicrometerRegistry)
    UptimeMetrics().bindTo(appMicrometerRegistry)

    log.withContext("healthIndicators" to 2).info{ "Micrometer health indicators registered" }
}