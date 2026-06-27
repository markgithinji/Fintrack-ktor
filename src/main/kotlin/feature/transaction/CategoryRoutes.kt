package feature.transaction

import com.fintrack.core.domain.ApiResponse
import com.fintrack.core.logger
import com.fintrack.core.userIdOrThrow
import com.fintrack.core.withContext
import feature.transaction.data.model.CreateCategoryRequest
import feature.transaction.domain.CategoryService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import java.util.UUID

fun Route.categoryRoutes(service: CategoryService) {
    val log = logger("CategoryRoutes")

    route("/categories") {
        get {
            val userId = call.userIdOrThrow()
            log.withContext("userId" to userId, "endpoint" to "GET /categories").info { "Get categories request received" }
            val categories = service.getAll(userId)
            call.respond(ApiResponse.Success(categories))
        }

        post {
            val userId = call.userIdOrThrow()
            val request = call.receive<CreateCategoryRequest>()
            log.withContext("userId" to userId, "endpoint" to "POST /categories", "name" to request.name).info { "Create category request received" }
            val saved = service.add(userId, request)
            call.respond(HttpStatusCode.Created, ApiResponse.Success(saved))
        }

        delete("{id}") {
            val userId = call.userIdOrThrow()
            val id = call.parameters["id"]?.let { UUID.fromString(it) } ?: throw IllegalArgumentException("Invalid ID")
            log.withContext("userId" to userId, "id" to id, "endpoint" to "DELETE /categories/{id}").info { "Delete category request received" }
            service.delete(userId, id)
            call.respond(ApiResponse.Success(mapOf("message" to "Category deleted successfully")))
        }
    }
}
