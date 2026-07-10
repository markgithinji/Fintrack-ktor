package com.fintrack.feature.health


import io.ktor.http.HttpStatusCode
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
        val statusCode = if (healthStatus["status"] == "UP") HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
        call.respond(statusCode, healthStatus)
    }

    // Readiness check
    get("/health/ready") {
        val isReady = healthService.checkReadiness()
        if (isReady) {
            call.respond(mapOf("status" to "READY"))
        } else {
            call.respond(HttpStatusCode.ServiceUnavailable, mapOf("status" to "NOT_READY"))
        }
    }

    // Liveness check
    get("/health/live") {
        call.respond(mapOf("status" to "ALIVE"))
    }

    // Health metrics endpoint
    get("/health/metrics") {
        call.respond(healthService.getHealthMetrics())
    }
}