package com.fintrack.feature.auth

import com.fintrack.core.logger
import com.fintrack.core.warn
import com.fintrack.core.withContext
import com.fintrack.feature.auth.domain.AuthValidationResponse
import feature.auth.domain.AuthService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authRoutes(authService: AuthService) {
    val log = logger("AuthRoutes")

    route("/auth") {

        post("/register") {
            val request = call.receive<AuthRequest>()

            log.withContext(
                "endpoint" to "POST /auth/register",
                "email" to request.email
            ).info { "Registration request received" }

            val response = authService.register(request.email, request.password)

            log.withContext("email" to request.email).info { "Registration completed successfully" }
            call.respond(HttpStatusCode.Created, response)
        }

        post("/login") {
            val request = call.receive<AuthRequest>()

            log.withContext(
                "endpoint" to "POST /auth/login",
                "email" to request.email
            ).info { "Login request received" }

            val response = authService.login(request.email, request.password)

            log.withContext("email" to request.email).info { "Login completed successfully" }
            call.respond(HttpStatusCode.OK, response)
        }

        get("/validate") {
            val authHeader = call.request.headers["Authorization"]

            log.withContext(
                "endpoint" to "GET /auth/validate",
                "hasAuthHeader" to (authHeader != null)
            ).info { "Token validation request received" }

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn { "Missing or invalid Authorization header" }
                call.respond(
                    HttpStatusCode.Unauthorized, AuthValidationResponse(
                        isValid = false,
                        userId = null,
                        message = "Missing or invalid Authorization header"
                    )
                )
                return@get
            }

            val token = authHeader.removePrefix("Bearer ").trim()

            val response = authService.validateToken(token)

            if (response.isValid) {
                log.withContext("userId" to response.userId)
                    .info { "Token validation successful" }
                call.respond(HttpStatusCode.OK, response)
            } else {
                log.withContext("userId" to response.userId).warn { "Token validation failed" }
                call.respond(HttpStatusCode.Unauthorized, response)
            }
        }
    }
}