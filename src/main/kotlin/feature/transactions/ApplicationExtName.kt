package com.fintrack.feature.transactions

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import javax.naming.AuthenticationException

fun ApplicationCall.userIdOrThrow(): Int =
    this.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
        ?: throw AuthenticationException("User not authenticated")
