package com.fintrack.feature.auth

import com.fintrack.core.info
import com.fintrack.core.logger
import com.fintrack.core.withContext
import com.fintrack.core.userIdOrThrow
import com.fintrack.feature.auth.domain.AuthValidationResponse
import feature.auth.data.model.ChangePasswordRequest
import feature.auth.data.model.RefreshRequest
import feature.auth.domain.AuthService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.origin
import io.ktor.server.request.receive
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import com.fintrack.plugins.withAuthRateLimit

fun Route.authRoutes(authService: AuthService) {
    val log = logger("AuthRoutes")

    route("/auth") {
        withAuthRateLimit {
            post("/register") {
                val request = call.receive<AuthRequest>()
                log.withContext(
                    "email" to request.email,
                    "ip" to call.request.origin.remoteHost,
                    "userAgent" to call.request.headers["User-Agent"]
                ).info { "Registration request received" }
                val response = authService.register(request.email, request.password)
                call.respond(HttpStatusCode.Created, response)
            }

            post("/login") {
                val request = call.receive<AuthRequest>()
                log.withContext(
                    "email" to request.email,
                    "ip" to call.request.origin.remoteHost,
                    "userAgent" to call.request.headers["User-Agent"]
                ).info { "Login request received" }
                val response = authService.login(request.email, request.password)
                call.respond(HttpStatusCode.OK, response)
            }

            post("/refresh") {
                val request = call.receive<RefreshRequest>()
                log.info { "Token refresh request received" }
                val response = authService.refreshToken(request.refreshToken)
                call.respond(HttpStatusCode.OK, response)
            }
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

        authenticate("auth-jwt") {
            post("/change-password") {
                val userId = call.userIdOrThrow()
                val request = call.receive<ChangePasswordRequest>()
                log.withContext(
                    "userId" to userId,
                    "ip" to call.request.origin.remoteHost,
                    "userAgent" to call.request.headers["User-Agent"]
                ).info { "Password change request received" }
                authService.changePassword(userId, request.currentPassword, request.newPassword)
                call.respond(HttpStatusCode.OK, "Password changed successfully")
            }
        }
    }
}
