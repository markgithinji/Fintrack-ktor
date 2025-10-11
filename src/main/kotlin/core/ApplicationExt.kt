package com.fintrack.core

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.util.*
import javax.naming.AuthenticationException

import io.ktor.server.application.*
import java.util.UUID

fun ApplicationCall.userIdOrThrow(): UUID {
    val principal = this.principal<JWTPrincipal>()
        ?: throw AuthenticationException("Missing JWT principal")

    val userIdString = principal.payload.getClaim("userId").asString()
        ?: throw AuthenticationException("Missing userId claim in JWT")

    return UUID.fromString(userIdString)
}