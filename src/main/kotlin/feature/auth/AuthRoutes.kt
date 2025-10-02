package com.fintrack.feature.auth

import com.fintrack.core.ApiResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import com.fintrack.feature.user.data.UserRepository
import feature.auth.data.model.AuthResponse
import feature.auth.domain.AuthService
import feature.auth.domain.AuthenticationException
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.mindrot.jbcrypt.BCrypt
fun Route.authRoutes(authService: AuthService) {
    route("/auth") {
        // Register route
        post("/register") {
            try {
                val request = call.receive<AuthRequest>()
                val response = authService.register(request.email, request.password)
                call.respond(HttpStatusCode.Created, response)
            } catch (e: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.Error("Registration failed: ${e.message}")
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.Error("Registration failed: ${e.message}")
                )
            }
        }

        // Login route
        post("/login") {
            try {
                val request = call.receive<AuthRequest>()
                val response = authService.login(request.email, request.password)
                call.respond(HttpStatusCode.OK, response)
            } catch (e: AuthenticationException) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse.Error("Invalid credentials")
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.Error("Login failed: ${e.message}")
                )
            }
        }
    }
}
