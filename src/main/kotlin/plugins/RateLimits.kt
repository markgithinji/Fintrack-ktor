package com.fintrack.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.routing.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.koin.ktor.ext.inject
import redis.clients.jedis.JedisPool
import com.fintrack.feature.auth.AuthRequest
import io.ktor.server.request.receiveNullable
import kotlinx.datetime.Clock

class RateLimitConfig {
    companion object {
        val AUTH_LIMIT = RateLimitName("auth")
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

        // Authentication endpoints - stricter limits with Redis and Account-based peeking
        register(RateLimitConfig.AUTH_LIMIT) {
            rateLimiter { _, key ->
                RedisRateLimiter(
                    jedisPool = jedisPool,
                    key = key.toString(),
                    limit = 10,
                    refillPeriod = 1.minutes
                )
            }
            requestKey { call ->
                val ip = call.request.origin.remoteHost
                val email = try {
                    // Peek into the body for email (requires DoubleReceive)
                    call.receiveNullable<AuthRequest>()?.email
                } catch (_: Exception) {
                    null
                }
                
                if (email != null) "auth:$ip:$email" else "auth:$ip"
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
    override suspend fun tryConsume(tokens: Int): RateLimiter.State {
        return jedisPool.resource.use { jedis ->
            val redisKey = "ratelimit:$key"
            val current = jedis[redisKey]?.toLong() ?: 0L
            
            if ((current + tokens) <= limit) {
                val newValue = jedis.incrBy(redisKey, tokens.toLong())
                if (newValue == tokens.toLong()) {
                    jedis.expire(redisKey, refillPeriod.inWholeSeconds)
                }
                val ttl = jedis.ttl(redisKey).coerceAtLeast(0)
                RateLimiter.State.Available(
                    (limit - newValue).toInt(),
                    limit,
                    Clock.System.now().plus(ttl.seconds).toEpochMilliseconds()
                )
            } else {
                val ttl = jedis.ttl(redisKey).coerceAtLeast(0)
                RateLimiter.State.Exhausted(
                    ttl.seconds
                )
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