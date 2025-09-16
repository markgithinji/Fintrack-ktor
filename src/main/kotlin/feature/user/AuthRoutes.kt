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
        post("/register") {
            try {
                val request = call.receive<AuthRequest>()
                val userId = userRepo.createUser(request.username, request.password)
                call.respond(
                    HttpStatusCode.Created,
                    ApiResponse.Success(mapOf("userId" to userId))
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.Error("Registration failed: ${e.message}")
                )
            }
        }

        post("/login") {
            val request = call.receive<AuthRequest>()
            val user = userRepo.findByUsername(request.username)

            if (user == null || !BCrypt.checkpw(request.password, user.passwordHash)) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse.Error("Invalid credentials"))
                return@post
            }

            val token = JwtConfig.generateToken(user.id)
            call.respond(ApiResponse.Success(mapOf("token" to token)))
        }

        authenticate("auth-jwt") {
            get("/me") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val user = userRepo.findById(userId)

                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, ApiResponse.Error("User not found"))
                } else {
                    call.respond(
                        ApiResponse.Success(
                            mapOf(
                                "id" to user.id,
                                "username" to user.username
                            )
                        )
                    )
                }
            }
        }
    }
}
