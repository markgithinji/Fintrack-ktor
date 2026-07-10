package feature.transaction

import com.fintrack.core.domain.*
import com.fintrack.core.toUUIDOrNull
import com.fintrack.core.userIdOrThrow
import com.fintrack.feature.budget.data.model.BulkCreateBudgetRequest
import com.fintrack.feature.budget.data.model.CreateBudgetRequest
import com.fintrack.feature.budget.data.model.UpdateBudgetRequest
import feature.budget.domain.BudgetService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import java.util.UUID

fun Route.budgetRoutes(budgetService: BudgetService) {

    route("/budgets") {

        post("/bulk") {
            val userId = call.userIdOrThrow()
            val bulkRequest = call.receive<BulkCreateBudgetRequest>()

            when (val result = budgetService.createBudgets(userId, bulkRequest.budgets)) {
                is Result.Success -> call.respond(ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        post {
            val userId = call.userIdOrThrow()
            val request = call.receive<CreateBudgetRequest>()

            when (val result = budgetService.createBudget(userId, request)) {
                is Result.Success -> call.respond(HttpStatusCode.Created, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        put("{id}") {
            val userId = call.userIdOrThrow()
            val id = call.parameters["id"]?.toUUIDOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid budget ID", "INVALID_ID"))

            val request = call.receive<UpdateBudgetRequest>()

            when (val result = budgetService.updateBudget(userId, id, request)) {
                is Result.Success -> call.respond(ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        delete("{id}") {
            val userId = call.userIdOrThrow()
            val id = call.parameters["id"]?.toUUIDOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid budget ID", "INVALID_ID"))

            when (val result = budgetService.deleteBudget(userId, id)) {
                is Result.Success -> call.respond(ApiResponse.Success("Deleted budget with id: $id"))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        delete("/clear") {
            val userId = call.userIdOrThrow()
            val accountIds = call.request.queryParameters.getAll("accountId")?.mapNotNull { it.toUUIDOrNull() }

            when (val result = budgetService.deleteAllBudgets(userId, accountIds)) {
                is Result.Success -> call.respond(ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        get {
            val userId = call.userIdOrThrow()
            val accountId = call.request.queryParameters["accountId"]?.toUUIDOrNull()
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceAtMost(100) ?: 20
            val offset = call.request.queryParameters["offset"]?.toLongOrNull() ?: 0L

            when (val result = budgetService.getAllBudgets(userId, accountId, limit, offset)) {
                is Result.Success -> call.respond(ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        get("{id}") {
            val userId = call.userIdOrThrow()
            val id = call.parameters["id"]?.toUUIDOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid budget ID", "INVALID_ID"))

            when (val result = budgetService.getBudgetById(userId, id)) {
                is Result.Success -> call.respond(ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }
    }
}
