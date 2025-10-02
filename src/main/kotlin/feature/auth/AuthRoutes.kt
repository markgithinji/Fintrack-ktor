package com.fintrack.feature.auth

import com.fintrack.core.ApiResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import feature.auth.data.model.AuthResponse
import feature.auth.domain.AuthService
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.mindrot.jbcrypt.BCrypt
fun Route.authRoutes(authService: AuthService) {
    route("/auth") {
        // Register route
        post("/register") {
            val request = call.receive<AuthRequest>()
            val response = authService.register(request.email, request.password)
            call.respond(HttpStatusCode.Created, response)
        }

        // Login route
        post("/login") {
            val request = call.receive<AuthRequest>()
            val response = authService.login(request.email, request.password)
            call.respond(HttpStatusCode.OK, response)
        }
    }
}