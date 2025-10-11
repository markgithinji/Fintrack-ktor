package feature.user

import com.fintrack.core.ApiResponse
import com.fintrack.core.logger
import com.fintrack.core.userIdOrThrow
import com.fintrack.core.withContext
import feature.user.data.model.UpdateUserRequest
import feature.user.domain.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.userRoutes(userService: UserService) {
    val log = logger("UserRoutes")

    route("/users") {

        get("/me") {
            val userId = call.userIdOrThrow()

            log.withContext(
                "userId" to userId,
                "endpoint" to "GET /users/me"
            ).info { "Get user profile request received" }

            val user = userService.getUserProfile(userId)
                ?: throw NoSuchElementException("User not found")

            call.respond(HttpStatusCode.OK, ApiResponse.Success(user))
        }

        // PUT /users/me - Update user profile
        put("/me") {
            val userId = call.userIdOrThrow()
            val updateRequest = call.receive<UpdateUserRequest>()

            log.withContext(
                "userId" to userId,
                "endpoint" to "PUT /users/me",
                "emailUpdate" to (updateRequest.email != null),
                "passwordUpdate" to (updateRequest.password != null)
            ).info { "Update user profile request received" }

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

            log.withContext(
                "userId" to userId,
                "endpoint" to "DELETE /users/me"
            ).warn { "Delete user account request received" }

            val success = userService.deleteUser(userId)

            if (success) {
                log.withContext("userId" to userId).warn { "User account deletion completed" }
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse.Success("User account deleted successfully")
                )
            } else {
                throw NoSuchElementException("User not found")
            }
        }
    }
}