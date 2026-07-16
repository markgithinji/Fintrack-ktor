package feature.category

import com.fintrack.core.domain.*
import com.fintrack.core.toUUIDOrNull
import com.fintrack.core.userIdOrThrow
import feature.category.data.model.CreateCategoryRequest
import feature.category.domain.CategoryRuleService
import feature.category.domain.CategoryService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.categoryRoutes(service: CategoryService, ruleService: CategoryRuleService) {
    route("/categories") {
        get {
            val userId = call.userIdOrThrow()
            
            when (val result = service.getAll(userId)) {
                is Result.Success -> call.respond(ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    result.error.toApiResponse()
                )
            }
        }

        get("/rules") {
            when (val result = ruleService.getAllRules()) {
                is Result.Success -> call.respond(ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    result.error.toApiResponse()
                )
            }
        }

        post {
            val userId = call.userIdOrThrow()
            val request = call.receive<CreateCategoryRequest>()
            
            when (val result = service.add(userId, request)) {
                is Result.Success -> call.respond(HttpStatusCode.Created, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    result.error.toApiResponse()
                )
            }
        }

        delete("{id}") {
            val userId = call.userIdOrThrow()
            val id = call.parameters["id"]?.toUUIDOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Invalid category ID", "INVALID_ID"))
                
            when (val result = service.delete(userId, id)) {
                is Result.Success -> call.respond(ApiResponse.Success(mapOf("message" to "Category deleted successfully")))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    result.error.toApiResponse()
                )
            }
        }
    }
}
