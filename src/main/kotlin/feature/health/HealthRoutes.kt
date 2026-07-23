package com.fintrack.feature.health

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val service: String,
    val timestamp: Long
)

@Serializable
data class SimpleStatusResponse(
    val status: String
)

fun Routing.healthRoutes(healthService: HealthService) {
    // Simple health check
    get("/health") {
        call.respond(HealthResponse(
            status = "UP",
            service = "fintrack-api",
            timestamp = System.currentTimeMillis()
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
            call.respond(SimpleStatusResponse(status = "READY"))
        } else {
            call.respond(HttpStatusCode.ServiceUnavailable, SimpleStatusResponse(status = "NOT_READY"))
        }
    }

    // Liveness check
    get("/health/live") {
        call.respond(SimpleStatusResponse(status = "ALIVE"))
    }

    // Health metrics endpoint
    get("/health/metrics") {
        call.respond(healthService.getHealthMetrics())
    }
}
