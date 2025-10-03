package com.fintrack.feature.health


import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get

fun Routing.healthRoutes(healthService: HealthService) {

    // Simple health check
    get("/health") {
        call.respond(mapOf(
            "status" to "UP",
            "service" to "fintrack-api",
            "timestamp" to System.currentTimeMillis()
        ))
    }

    // Detailed health check with metrics
    get("/health/detailed") {
        val healthStatus = healthService.getDetailedHealthStatus()
        call.respond(healthStatus)
    }

    // Readiness check
    get("/health/ready") {
        val isReady = healthService.checkReadiness()
        val status = if (isReady) "READY" else "NOT_READY"
        call.respond(mapOf("status" to status))
    }

    // Liveness check
    get("/health/live") {
        call.respond(mapOf("status" to "ALIVE"))
    }

    // Health metrics endpoint (alternative to /metrics for health-focused metrics)
    get("/health/metrics") {
        call.respond(healthService.getHealthMetrics())
    }
}