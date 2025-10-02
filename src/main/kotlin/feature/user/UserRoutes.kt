package feature.user

import com.fintrack.core.ApiResponse
import feature.user.data.model.UserDto
import feature.user.data.model.UserUpdateRequest
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
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.payload.getClaim("userId").asInt()
            val user = userService.getUserProfile(userId)

            if (user == null) {
                throw NoSuchElementException("User not found")
            } else {
                call.respond(HttpStatusCode.OK, ApiResponse.Success(user))
            }
        }

        // PUT /users/me - Update user profile
        put("/me") {
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
                throw NoSuchElementException("User not found")
            }
        }

        // DELETE /users/me - Delete user account
        delete("/me") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.payload.getClaim("userId").asInt()

            val success = userService.deleteUser(userId)

            if (success) {
                call.respond(HttpStatusCode.OK, ApiResponse.Success("User account deleted successfully"))
            } else {
                throw NoSuchElementException("User not found")
            }
        }
    }
}