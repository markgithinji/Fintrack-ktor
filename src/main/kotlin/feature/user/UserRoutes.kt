package com.fintrack.feature.user

import com.fintrack.core.domain.*
import com.fintrack.core.userIdOrThrow
import com.fintrack.feature.user.data.model.TrackedCategoriesRequest
import com.fintrack.feature.user.data.model.UpdateUserRequest
import com.fintrack.feature.user.domain.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.userRoutes(userService: UserService) {
    route("/users") {

        get("/me") {
            val userId = call.userIdOrThrow()

            when (val result = userService.getUserProfile(userId)) {
                is Result.Success -> call.respond(HttpStatusCode.OK, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    result.error.toApiResponse()
                )
            }
        }

        put("/me") {
            val userId = call.userIdOrThrow()
            val updateRequest = call.receive<UpdateUserRequest>()

            when (val result = userService.updateUser(userId, updateRequest)) {
                is Result.Success -> call.respond(HttpStatusCode.OK, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    result.error.toApiResponse()
                )
            }
        }

        put("/preferences/tracked-categories") {
            val userId = call.userIdOrThrow()
            val request = call.receive<TrackedCategoriesRequest>()

            when (val result = userService.updateTrackedCategories(userId, request.categories)) {
                is Result.Success -> call.respond(HttpStatusCode.OK, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    result.error.toApiResponse()
                )
            }
        }

        delete("/me") {
            val userId = call.userIdOrThrow()

            when (val result = userService.deleteUser(userId)) {
                is Result.Success -> {
                    call.respond(HttpStatusCode.OK, ApiResponse.Success("User account deleted successfully"))
                }
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    result.error.toApiResponse()
                )
            }
        }
    }
}
