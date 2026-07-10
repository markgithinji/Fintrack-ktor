package com.fintrack.feature.auth

import com.fintrack.core.domain.*
import com.fintrack.core.userIdOrThrow
import com.fintrack.feature.auth.data.model.*
import com.fintrack.feature.auth.domain.AuthService
import com.fintrack.feature.user.domain.UserService
import com.fintrack.plugins.withAuthRateLimit
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(authService: AuthService, userService: UserService) {
    route("/auth") {
        get("/verify-email-change") {
            val token = call.request.queryParameters["token"]
            
            if (token == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Token is required", "MISSING_TOKEN"))
                return@get
            }

            when (val result = userService.verifyEmailChange(token)) {
                is Result.Success -> call.respond(HttpStatusCode.OK, ApiResponse.Success("Email verified and updated successfully"))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        withAuthRateLimit {
            post("/register") {
                val request = call.receive<AuthRequest>()
                
                when (val result = authService.register(request.email, request.password)) {
                    is Result.Success -> call.respond(HttpStatusCode.Created, result.value)
                    is Result.Failure -> call.respond(
                        result.error.toHttpStatusCode(),
                        ErrorResponse(result.error.message, result.error.errorCode)
                    )
                }
            }

            post("/login") {
                val request = call.receive<AuthRequest>()

                when (val result = authService.login(request.email, request.password)) {
                    is Result.Success -> call.respond(HttpStatusCode.OK, result.value)
                    is Result.Failure -> call.respond(
                        result.error.toHttpStatusCode(),
                        ErrorResponse(result.error.message, result.error.errorCode)
                    )
                }
            }

            post("/refresh") {
                val request = call.receive<RefreshRequest>()
                
                when (val result = authService.refreshToken(request.refreshToken)) {
                    is Result.Success -> call.respond(HttpStatusCode.OK, result.value)
                    is Result.Failure -> call.respond(
                        result.error.toHttpStatusCode(),
                        ErrorResponse(result.error.message, result.error.errorCode)
                    )
                }
            }
        }

        get("/validate") {
            val authHeader = call.request.headers["Authorization"]
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Missing or invalid header", "MISSING_HEADER"))
                return@get
            }
            val token = authHeader.removePrefix("Bearer ").trim()
            
            when (val result = authService.validateToken(token)) {
                is Result.Success -> call.respond(HttpStatusCode.OK, result.value.toDto())
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        post("/logout") {
            val authHeader = call.request.headers["Authorization"]
            val accessToken = authHeader?.removePrefix("Bearer ")?.trim()
            val refreshRequest = call.receiveNullable<RefreshRequest>()

            if (accessToken != null) {
                when (val result = authService.logout(accessToken, refreshRequest?.refreshToken)) {
                    is Result.Success -> call.respond(HttpStatusCode.OK)
                    is Result.Failure -> call.respond(
                        result.error.toHttpStatusCode(),
                        ErrorResponse(result.error.message, result.error.errorCode)
                    )
                }
            } else {
                call.respond(HttpStatusCode.OK)
            }
        }

        authenticate("auth-jwt") {
            post("/change-password") {
                val userId = call.userIdOrThrow()
                val request = call.receive<ChangePasswordRequest>()
                
                when (val result = authService.changePassword(userId, request.currentPassword, request.newPassword)) {
                    is Result.Success -> call.respond(HttpStatusCode.OK, "Password changed successfully")
                    is Result.Failure -> call.respond(
                        result.error.toHttpStatusCode(),
                        ErrorResponse(result.error.message, result.error.errorCode)
                    )
                }
            }
        }
    }
}
