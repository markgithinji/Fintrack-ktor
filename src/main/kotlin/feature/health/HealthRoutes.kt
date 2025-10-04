package com.fintrack.feature.health


import com.fintrack.core.logger
import com.fintrack.core.withContext
import io.ktor.server.plugins.origin
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get

fun Routing.healthRoutes(healthService: HealthService) {
    val log = logger("HealthRoutes")

    // Simple health check
    get("/health") {
        log.withContext(
            "endpoint" to "/health",
            "clientIp" to call.request.origin.remoteHost
        ).debug { "Health check request" }

        call.respond(mapOf(
            "status" to "UP",
            "service" to "fintrack-api",
            "timestamp" to System.currentTimeMillis()
        ))
    }

    // Detailed health check with metrics
    get("/health/detailed") {
        log.withContext(
            "endpoint" to "/health/detailed",
            "clientIp" to call.request.origin.remoteHost
        ).info { "Detailed health check request" }

        val healthStatus = healthService.getDetailedHealthStatus()

        log.withContext(
            "endpoint" to "/health/detailed",
            "overallStatus" to healthStatus["status"]
        ).debug { "Detailed health status generated" }

        call.respond(healthStatus)
    }

    // Readiness check
    get("/health/ready") {
        log.withContext(
            "endpoint" to "/health/ready",
            "clientIp" to call.request.origin.remoteHost
        ).info { "Readiness check request" }

        val isReady = healthService.checkReadiness()
        val status = if (isReady) "READY" else "NOT_READY"

        log.withContext(
            "endpoint" to "/health/ready",
            "status" to status
        ).debug { "Readiness check completed" }

        call.respond(mapOf("status" to status))
    }

    // Liveness check
    get("/health/live") {
        log.withContext(
            "endpoint" to "/health/live",
            "clientIp" to call.request.origin.remoteHost
        ).debug { "Liveness check request" }

        call.respond(mapOf("status" to "ALIVE"))
    }

    // Health metrics endpoint (alternative to /metrics for health-focused metrics)
    get("/health/metrics") {
        log.withContext(
            "endpoint" to "/health/metrics",
            "clientIp" to call.request.origin.remoteHost
        ).info { "Health metrics request" }

        val metrics = healthService.getHealthMetrics()
        call.respond(metrics)
    }
}