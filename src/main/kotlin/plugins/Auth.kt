package com.fintrack.plugins

import com.fintrack.feature.auth.JwtConfig
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt

fun Application.configureAuth() {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = JwtConfig.realm
            verifier(JwtConfig.verifier)
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asInt()
                if (userId != null) JWTPrincipal(credential.payload) else null
            }
        }
    }
}
