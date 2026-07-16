package com.fintrack.core

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import java.util.UUID
import javax.naming.AuthenticationException

fun ApplicationCall.userIdOrThrow(): UUID {
    val principal = this.principal<JWTPrincipal>()
        ?: throw AuthenticationException("Missing JWT principal")

    val userIdString = principal.payload.getClaim("userId").asString()
        ?: throw AuthenticationException("Missing userId claim in JWT")

    return UUID.fromString(userIdString)
}

fun String.toUUIDOrNull(): UUID? = try {
    UUID.fromString(this)
} catch (e: Exception) {
    null
}