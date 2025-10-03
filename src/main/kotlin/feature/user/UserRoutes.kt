package feature.user

import com.fintrack.core.ApiResponse
import com.fintrack.core.userIdOrThrow
import feature.user.data.model.UpdateUserRequest
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
            val userId = call.userIdOrThrow()
            val user = userService.getUserProfile(userId)
                ?: throw NoSuchElementException("User not found")

            call.respond(HttpStatusCode.OK, ApiResponse.Success(user))
        }

        // PUT /users/me - Update user profile
        put("/me") {
            val userId = call.userIdOrThrow()
            val updateRequest = call.receive<UpdateUserRequest>()

            val success = userService.updateUser(userId, updateRequest)

            if (success) {
                call.respond(HttpStatusCode.OK, ApiResponse.Success("User updated successfully"))
            } else {
                throw NoSuchElementException("User not found")
            }
        }

        // DELETE /users/me - Delete user account
        delete("/me") {
            val userId = call.userIdOrThrow()

            val success = userService.deleteUser(userId)

            if (success) {
                call.respond(HttpStatusCode.OK, ApiResponse.Success("User account deleted successfully"))
            } else {
                throw NoSuchElementException("User not found")
            }
        }
    }
}