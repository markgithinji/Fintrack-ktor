package com.fintrack.plugins

import com.fintrack.feature.auth.data.model.AuthRequest
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.receiveNullable
import io.ktor.server.routing.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.koin.ktor.ext.inject
import redis.clients.jedis.JedisPool
import kotlinx.datetime.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RateLimitConfig {
    companion object {
        val AUTH_IP_LIMIT = RateLimitName("auth-ip")
        val AUTH_EMAIL_LIMIT = RateLimitName("auth-email")
        val PUBLIC_API_LIMIT = RateLimitName("public-api")
        val PROTECTED_API_LIMIT = RateLimitName("protected-api")
        val HEALTH_LIMIT = RateLimitName("health")
    }
}

fun Application.configureRateLimiting() {
    val jedisPool by inject<JedisPool>()

    install(RateLimit) {
        // Global rate limit provider
        global {
            rateLimiter(limit = 1000, refillPeriod = 1.minutes)
        }

        // 1. IP-based Auth Limit (Prevent high-frequency attacks from one source)
        register(RateLimitConfig.AUTH_IP_LIMIT) {
            rateLimiter { _, key ->
                RedisRateLimiter(
                    jedisPool = jedisPool,
                    key = "ip:$key",
                    limit = 20,
                    refillPeriod = 1.minutes
                )
            }
            requestKey { it.request.origin.remoteHost }
        }

        // 2. Email-based Auth Limit (Prevent distributed brute force against a single account)
        register(RateLimitConfig.AUTH_EMAIL_LIMIT) {
            rateLimiter { _, key ->
                RedisRateLimiter(
                    jedisPool = jedisPool,
                    key = "email:$key",
                    limit = 5,
                    refillPeriod = 1.minutes
                )
            }
            requestKey { call ->
                try {
                    // Peek into the body for email (requires DoubleReceive)
                    call.receiveNullable<AuthRequest>()?.email ?: call.request.origin.remoteHost
                } catch (_: Exception) {
                    call.request.origin.remoteHost
                }
            }
        }

        // Public API endpoints
        register(RateLimitConfig.PUBLIC_API_LIMIT) {
            rateLimiter(limit = 100, refillPeriod = 1.minutes)
            requestKey { it.request.origin.remoteHost }
        }

        // Protected API endpoints
        register(RateLimitConfig.PROTECTED_API_LIMIT) {
            rateLimiter(limit = 1000, refillPeriod = 1.minutes)
            requestKey { call ->
                call.principal<UserIdPrincipal>()?.name ?: call.request.origin.remoteHost
            }
        }

        // Health endpoints
        register(RateLimitConfig.HEALTH_LIMIT) {
            rateLimiter(limit = 300, refillPeriod = 1.minutes)
            requestKey { it.request.origin.remoteHost }
        }
    }
}

/**
 * Redis-backed Rate Limiter for distributed environments.
 * Uses a simple fixed-window algorithm.
 */
class RedisRateLimiter(
    private val jedisPool: JedisPool,
    private val key: String,
    private val limit: Int,
    private val refillPeriod: Duration
) : RateLimiter {
    override suspend fun tryConsume(tokens: Int): RateLimiter.State = withContext(Dispatchers.IO) {
        jedisPool.resource.use { jedis ->
            val redisKey = "ratelimit:$key"

            // Atomic increment
            val newValue = jedis.incrBy(redisKey, tokens.toLong())

            // Set TTL only on the first increment
            if (newValue == tokens.toLong()) {
                jedis.expire(redisKey, refillPeriod.inWholeSeconds)
            }

            val ttl = jedis.ttl(redisKey).coerceAtLeast(0)
            val expiresAt = Clock.System.now().plus(ttl.seconds).toEpochMilliseconds()

            if (newValue <= limit) {
                RateLimiter.State.Available(
                    (limit - newValue).toInt(),
                    limit,
                    expiresAt
                )
            } else {
                RateLimiter.State.Exhausted(
                    ttl.seconds
                )
            }
        }
    }
}

// Extension functions for easy rate limiting
fun Route.withAuthRateLimit(block: Route.() -> Unit) {
    rateLimit(RateLimitConfig.AUTH_IP_LIMIT) {
        rateLimit(RateLimitConfig.AUTH_EMAIL_LIMIT) {
            block()
        }
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