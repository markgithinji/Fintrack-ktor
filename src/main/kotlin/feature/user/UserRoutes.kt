package feature.user

import com.fintrack.core.ApiResponse
import com.fintrack.feature.user.data.UserDto
import com.fintrack.feature.user.data.UserRepository
import feature.user.domain.UserService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoutes(userService: UserService) {
    route("/users") {
        get("/me") {
            try {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asInt()
                val user = userService.getUserProfile(userId)

                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, ApiResponse.Error("User not found"))
                } else {
                    call.respond(HttpStatusCode.OK, ApiResponse.Success(user))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse.Error("Failed to retrieve user profile"))
            }
        }

        // PUT /users/me - Update user profile
        put("/me") {
            try {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asInt()
                val updateRequest = call.receive<UserUpdateRequest>()

                val success = userService.updateUser(
                    userId = userId,
                    username = updateRequest.username,
                    password = updateRequest.password
                )

                if (success) {
                    call.respond(HttpStatusCode.OK, ApiResponse.Success("User updated successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiResponse.Error("User not found"))
                }
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(e.message ?: "Invalid request"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse.Error("Failed to update user"))
            }
        }

        // DELETE /users/me - Delete user account
        delete("/me") {
            try {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asInt()

                val success = userService.deleteUser(userId)

                if (success) {
                    call.respond(HttpStatusCode.OK, ApiResponse.Success("User account deleted successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiResponse.Error("User not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse.Error("Failed to delete user account"))
            }
        }
    }
}

// DTO for user update requests TODO: Refactor location
data class UserUpdateRequest(
    val username: String? = null,
    val password: String? = null
)