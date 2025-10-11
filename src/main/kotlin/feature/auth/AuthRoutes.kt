package com.fintrack.feature.auth

import com.fintrack.core.logger
import com.fintrack.core.withContext
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import feature.auth.domain.AuthService
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(authService: AuthService) {
    val log = logger("AuthRoutes")

    route("/auth") {

        post("/register") {
            val request = call.receive<AuthRequest>()

            log.withContext(
                "endpoint" to "POST /auth/register",
                "email" to request.email
            ).info{ "Registration request received" }

            val response = authService.register(request.email, request.password)

            log.withContext("email" to request.email).info{ "Registration completed successfully" }
            call.respond(HttpStatusCode.Created, response)
        }

        post("/login") {
            val request = call.receive<AuthRequest>()

            log.withContext(
                "endpoint" to "POST /auth/login",
                "email" to request.email
            ).info{"Login request received"}

            val response = authService.login(request.email, request.password)

            log.withContext("email" to request.email).info{ "Login completed successfully" }
            call.respond(HttpStatusCode.OK, response)
        }
    }
}