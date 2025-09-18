package com.fintrack.feature.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import com.fintrack.feature.auth.JwtConfig
import com.fintrack.feature.user.data.UserRepository
import com.fintrack.feature.user.data.UserDto
import feature.auth.AuthResponse
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.mindrot.jbcrypt.BCrypt
fun Route.authRoutes() {
    val userRepo = UserRepository()

    route("/auth") {

        // Register route
        post("/register") {
            try {
                val request = call.receive<AuthRequest>()
                val userId = userRepo.createUser(request.email, request.password) // use email as username for now
                val token = JwtConfig.generateToken(userId)

                val response = AuthResponse(
                    token = token
                )

                call.respond(HttpStatusCode.Created, response)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Registration failed: ${e.message}")
                )
            }
        }

        // Login route
        post("/login") {
            try {
                val request = call.receive<AuthRequest>()
                val user = userRepo.findByUsername(request.email) // lookup by email

                if (user == null || !BCrypt.checkpw(request.password, user.passwordHash)) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
                    return@post
                }

                val token = JwtConfig.generateToken(user.id)
                val response = AuthResponse(
                    token = token
                )

                call.respond(response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Login failed: ${e.message}"))
            }
        }
    }
}
