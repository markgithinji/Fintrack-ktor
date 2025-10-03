package com.fintrack.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import org.slf4j.event.Level

fun Application.configureLogging() {
    install(CallLogging) {
        level = Level.INFO
        format { call ->
            val status = call.response.status()?.value ?: "UNKNOWN"
            val httpMethod = call.request.httpMethod.value
            val path = call.request.uri
            val userAgent = call.request.headers["User-Agent"] ?: "Unknown"
            val clientIp = call.request.origin.remoteHost

            "HTTP $status - $httpMethod $path - Client: $clientIp - Agent: $userAgent"
        }
        // Filter out health checks and other noisy endpoints
        filter { call ->
            val path = call.request.uri
            !path.startsWith("/health") &&
                    !path.startsWith("/metrics") &&
                    !path.contains("favicon")
        }
    }
}