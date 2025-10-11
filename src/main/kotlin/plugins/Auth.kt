package com.fintrack.plugins

import com.fintrack.feature.auth.JwtConfig
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import java.util.UUID

fun Application.configureAuth() {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = JwtConfig.realm
            verifier(JwtConfig.createVerifier())
            validate { credential ->
                val userIdString = credential.payload.getClaim("userId").asString()
                if (userIdString != null) {
                    try {
                        UUID.fromString(userIdString)
                        JWTPrincipal(credential.payload)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                } else {
                    null
                }
            }
        }
    }
}