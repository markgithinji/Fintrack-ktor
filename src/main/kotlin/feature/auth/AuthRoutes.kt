package com.fintrack.feature.auth

import com.fintrack.core.info
import com.fintrack.core.logger
import com.fintrack.core.withContext
import com.fintrack.feature.auth.domain.AuthValidationResponse
import feature.auth.data.model.RefreshRequest
import feature.auth.domain.AuthService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.request.receiveNullable
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
            log.withContext("email" to request.email).info { "Registration request received" }
            val response = authService.register(request.email, request.password)
            call.respond(HttpStatusCode.Created, response)
        }

        post("/login") {
            val request = call.receive<AuthRequest>()
            log.withContext("email" to request.email).info { "Login request received" }
            val response = authService.login(request.email, request.password)
            call.respond(HttpStatusCode.OK, response)
        }

        post("/refresh") {
            val request = call.receive<RefreshRequest>()
            log.info { "Token refresh request received" }
            val response = authService.refreshToken(request.refreshToken)
            call.respond(HttpStatusCode.OK, response)
        }

        get("/validate") {
            val authHeader = call.request.headers["Authorization"]
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                call.respond(HttpStatusCode.Unauthorized, AuthValidationResponse(false, null, "Missing or invalid header"))
                return@get
            }
            val token = authHeader.removePrefix("Bearer ").trim()
            val response = authService.validateToken(token)
            if (response.isValid) {
                call.respond(HttpStatusCode.OK, response)
            } else {
                call.respond(HttpStatusCode.Unauthorized, response)
            }
        }

        post("/logout") {
            val authHeader = call.request.headers["Authorization"]
            val accessToken = authHeader?.removePrefix("Bearer ")?.trim()
            val refreshRequest = call.receiveNullable<RefreshRequest>()

            if (accessToken != null) {
                authService.logout(accessToken, refreshRequest?.refreshToken)
            }

            call.respond(HttpStatusCode.OK)
        }
    }
}
