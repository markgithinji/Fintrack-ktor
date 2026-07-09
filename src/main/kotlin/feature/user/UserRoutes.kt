package feature.user

import com.fintrack.core.domain.*
import com.fintrack.core.logger
import com.fintrack.core.userIdOrThrow
import com.fintrack.core.withContext
import feature.user.data.model.TrackedCategoriesRequest
import feature.user.data.model.UpdateUserRequest
import feature.user.domain.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.userRoutes(userService: UserService) {
    val log = logger("UserRoutes")

    route("/users") {

        get("/me") {
            val userId = call.userIdOrThrow()

            log.withContext(
                "userId" to userId,
                "endpoint" to "GET /users/me"
            ).info { "Get user profile request received" }

            when (val result = userService.getUserProfile(userId)) {
                is Result.Success -> call.respond(HttpStatusCode.OK, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        put("/me") {
            val userId = call.userIdOrThrow()
            val updateRequest = call.receive<UpdateUserRequest>()

            log.withContext(
                "userId" to userId,
                "endpoint" to "PUT /users/me",
                "emailUpdate" to (updateRequest.email != null),
                "passwordUpdate" to (updateRequest.password != null)
            ).info { "Update user profile request received" }

            when (val result = userService.updateUser(userId, updateRequest)) {
                is Result.Success -> call.respond(HttpStatusCode.OK, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        put("/preferences/tracked-categories") {
            val userId = call.userIdOrThrow()
            val request = call.receive<TrackedCategoriesRequest>()

            log.withContext(
                "userId" to userId,
                "categories" to request.categories
            ).info { "Update tracked categories request received" }

            when (val result = userService.updateTrackedCategories(userId, request.categories)) {
                is Result.Success -> call.respond(HttpStatusCode.OK, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        delete("/me") {
            val userId = call.userIdOrThrow()

            log.withContext(
                "userId" to userId,
                "endpoint" to "DELETE /users/me"
            ).warn { "Delete user account request received" }

            when (val result = userService.deleteUser(userId)) {
                is Result.Success -> {
                    log.withContext("userId" to userId).warn { "User account deletion completed" }
                    call.respond(HttpStatusCode.OK, ApiResponse.Success("User account deleted successfully"))
                }
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }
    }
}
