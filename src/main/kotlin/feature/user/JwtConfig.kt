package com.fintrack.feature.user

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

object JwtConfig {
    private const val secret = "super_secret_key" // move to ENV
    private const val issuer = "fintrack-server"
    private const val audience = "fintrack-client"
    const val realm = "Access to Fintrack"

    val algorithm = Algorithm.HMAC256(secret)

    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    fun generateToken(userId: Int): String = JWT.create()
        .withAudience(audience)
        .withIssuer(issuer)
        .withClaim("userId", userId)
        .withExpiresAt(Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)) // 24h
        .sign(algorithm)
}
