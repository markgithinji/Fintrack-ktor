package com.fintrack.feature.health

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics

class RateLimitMetrics {
    companion object {
        val requestsTotal: Counter = Counter.builder("ratelimit.requests.total")
            .description("Total requests processed by rate limiter")
            .register(Metrics.globalRegistry)

        val blockedRequests: Counter = Counter.builder("ratelimit.requests.blocked")
            .description("Requests blocked by rate limiter")
            .register(Metrics.globalRegistry)

        val allowedRequests: Counter = Counter.builder("ratelimit.requests.allowed")
            .description("Requests allowed by rate limiter")
            .register(Metrics.globalRegistry)
    }

    fun getMetrics(): Map<String, Any> {
        val total = requestsTotal.count()
        val blocked = blockedRequests.count()
        val allowed = allowedRequests.count()

        val blockRate = if (total > 0) {
            (blocked / total) * 100
        } else {
            0.0
        }

        return mapOf(
            "totalRequests" to total,
            "blockedRequests" to blocked,
            "allowedRequests" to allowed,
            "blockRate" to "%.2f".format(blockRate)
        )
    }
}