package com.fintrack.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.routing.*
import kotlin.time.Duration.Companion.minutes

class RateLimitConfig {
    companion object {
        val AUTH_LIMIT = RateLimitName("auth")
        val PUBLIC_API_LIMIT = RateLimitName("public-api")
        val PROTECTED_API_LIMIT = RateLimitName("protected-api")
        val HEALTH_LIMIT = RateLimitName("health")
    }
}

fun Application.configureRateLimiting() {
    install(RateLimit) {
        // Global rate limit provider
        global {
            rateLimiter(limit = 1000, refillPeriod = 1.minutes)
        }

        // Authentication endpoints - stricter limits
        register(RateLimitConfig.AUTH_LIMIT) {
            rateLimiter(limit = 10, refillPeriod = 1.minutes) // 10 requests per minute
            requestKey { applicationCall ->
                applicationCall.request.origin.remoteHost // Limit by IP
            }
        }

        // Public API endpoints
        register(RateLimitConfig.PUBLIC_API_LIMIT) {
            rateLimiter(limit = 100, refillPeriod = 1.minutes) // 100 requests per minute
            requestKey { applicationCall ->
                applicationCall.request.origin.remoteHost
            }
        }

        // Protected API endpoints - more generous
        register(RateLimitConfig.PROTECTED_API_LIMIT) {
            rateLimiter(limit = 1000, refillPeriod = 1.minutes) // 1000 requests per minute
            requestKey { applicationCall ->
                // Use user ID for authenticated users, fallback to IP
                applicationCall.principal<UserIdPrincipal>()?.name
                    ?: applicationCall.request.origin.remoteHost
            }
        }

        // Health endpoints - very generous for monitoring
        register(RateLimitConfig.HEALTH_LIMIT) {
            rateLimiter(limit = 300, refillPeriod = 1.minutes) // 300 requests per minute
            requestKey { applicationCall ->
                applicationCall.request.origin.remoteHost
            }
        }
    }
}

// Extension functions for easy rate limiting
fun Route.withAuthRateLimit(block: Route.() -> Unit) {
    rateLimit(RateLimitConfig.AUTH_LIMIT) {
        block()
    }
}

fun Route.withPublicRateLimit(block: Route.() -> Unit) {
    rateLimit(RateLimitConfig.PUBLIC_API_LIMIT) {
        block()
    }
}

fun Route.withProtectedRateLimit(block: Route.() -> Unit) {
    rateLimit(RateLimitConfig.PROTECTED_API_LIMIT) {
        block()
    }
}

fun Route.withHealthRateLimit(block: Route.() -> Unit) {
    rateLimit(RateLimitConfig.HEALTH_LIMIT) {
        block()
    }
}