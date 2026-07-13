package com.fintrack.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.hsts.*

fun Application.configureSecurity() {
    install(DefaultHeaders) {
        // Protect against MIME type sniffing
        header("X-Content-Type-Options", "nosniff")
        // Prevent the site from being rendered in an iframe (clickjacking protection)
        header("X-Frame-Options", "DENY")
        // Content Security Policy to prevent XSS and other injections
        header("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none';")
        // Control how much referrer information should be included with requests
        header("Referrer-Policy", "strict-origin-when-cross-origin")
    }

    install(HSTS) {
        includeSubDomains = true
        maxAgeInSeconds = 31536000 // 1 year
    }
}
