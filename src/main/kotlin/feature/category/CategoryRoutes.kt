package feature.category

import com.fintrack.core.domain.*
import com.fintrack.core.logger
import com.fintrack.core.toUUIDOrNull
import com.fintrack.core.userIdOrThrow
import com.fintrack.core.withContext
import feature.category.data.model.CreateCategoryRequest
import feature.category.domain.CategoryService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.categoryRoutes(service: CategoryService) {
    val log = logger("CategoryRoutes")

    route("/categories") {
        get {
            val userId = call.userIdOrThrow()
            log.withContext("userId" to userId, "endpoint" to "GET /categories").info { "Get categories request received" }
            
            when (val result = service.getAll(userId)) {
                is Result.Success -> call.respond(ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        post {
            val userId = call.userIdOrThrow()
            val request = call.receive<CreateCategoryRequest>()
            log.withContext("userId" to userId, "endpoint" to "POST /categories", "name" to request.name).info { "Create category request received" }
            
            when (val result = service.add(userId, request)) {
                is Result.Success -> call.respond(HttpStatusCode.Created, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        delete("{id}") {
            val userId = call.userIdOrThrow()
            val id = call.parameters["id"]?.toUUIDOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid category ID", "INVALID_ID"))
                
            log.withContext("userId" to userId, "id" to id, "endpoint" to "DELETE /categories/{id}").info { "Delete category request received" }
            
            when (val result = service.delete(userId, id)) {
                is Result.Success -> call.respond(ApiResponse.Success(mapOf("message" to "Category deleted successfully")))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }
    }
}
