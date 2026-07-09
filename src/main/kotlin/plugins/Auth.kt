package com.fintrack.plugins

import com.fintrack.feature.auth.JwtConfig
import com.fintrack.feature.auth.domain.repository.TokenBlacklistRepository
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import org.koin.ktor.ext.inject
import java.util.UUID

fun Application.configureAuth() {
    val tokenBlacklistRepository by inject<TokenBlacklistRepository>()

    install(Authentication) {
        jwt("auth-jwt") {
            realm = JwtConfig.realm
            verifier(JwtConfig.createVerifier())
            validate { credential ->
                val authHeader = this.request.headers["Authorization"]
                val token = authHeader?.removePrefix("Bearer ")?.trim()
                
                if (token != null && tokenBlacklistRepository.isTokenBlacklisted(token)) {
                    return@validate null
                }
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
