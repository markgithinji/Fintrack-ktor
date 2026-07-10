package com.fintrack.feature.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.config.ApplicationConfig
import core.util.IdGenerator
import java.util.Date
import java.util.UUID

object JwtConfig {
    private lateinit var config: Config

    private data class Config(
        val secret: String,
        val issuer: String,
        val audience: String,
        val realm: String
    )

    const val ACCESS_TOKEN_EXPIRATION = 15 * 60 * 1000 // 15 minutes
    const val REFRESH_TOKEN_EXPIRATION = 30L * 24 * 60 * 60 * 1000 // 30 days

    fun init(applicationConfig: ApplicationConfig) {
        config = Config(
            secret = applicationConfig.propertyOrNull("jwt.secret")?.getString()
                ?: System.getenv("JWT_SECRET")
                ?: "default-jwt-secret-change-in-production",
            issuer = applicationConfig.propertyOrNull("jwt.issuer")?.getString()
                ?: "fintrack-server",
            audience = applicationConfig.propertyOrNull("jwt.audience")?.getString()
                ?: "fintrack-client",
            realm = applicationConfig.propertyOrNull("jwt.realm")?.getString()
                ?: "FinTrack API"
        )
    }

    fun createVerifier(): JWTVerifier =
        JWT.require(Algorithm.HMAC256(config.secret))
            .withAudience(config.audience)
            .withIssuer(config.issuer)
            .build()

    fun generateAccessToken(userId: UUID): String =
        JWT.create()
            .withAudience(config.audience)
            .withIssuer(config.issuer)
            .withClaim("userId", userId.toString())
            .withExpiresAt(Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
            .sign(Algorithm.HMAC256(config.secret))

    fun generateRefreshToken(): String = IdGenerator.nextId().toString()

    val realm: String get() = config.realm
}
