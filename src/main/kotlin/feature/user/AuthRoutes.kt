package com.fintrack.feature.user

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import com.fintrack.core.ApiResponse
import core.AvailableWeeks
import core.PaginatedTransactionDto
import core.TransactionDto
import core.ValidationException
import core.toDto
import core.toTransaction
import core.validate
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime
import org.jetbrains.exposed.sql.SortOrder
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

                val response = UserDto(
                    id = userId.toString(),
                    name = request.email, // using email as name for now
                    email = request.email,
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
                val response = UserDto(
                    id = user.id.toString(),
                    name = user.username,
                    email = user.username,
                    token = token
                )

                call.respond(response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Login failed: ${e.message}"))
            }
        }

        // Get current user
        authenticate("auth-jwt") {
            get("/me") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val user = userRepo.findById(userId)

                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                } else {
                    val response = UserDto(
                        id = user.id.toString(),
                        name = user.username,
                        email = user.username,
                        token = "" // token not needed here
                    )
                    call.respond(response)
                }
            }
        }
    }
}
