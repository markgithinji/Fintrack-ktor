package feature.transaction

import com.fintrack.core.domain.*
import com.fintrack.core.logger
import com.fintrack.core.toUUIDOrNull
import com.fintrack.core.userIdOrThrow
import com.fintrack.core.withContext
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
    val log = logger("BudgetRoutes")

    route("/budgets") {

        post("/bulk") {
            val userId = call.userIdOrThrow()
            val bulkRequest = call.receive<BulkCreateBudgetRequest>()

            log.withContext(
                "userId" to userId,
                "endpoint" to "POST /budgets/bulk",
                "budgetCount" to bulkRequest.budgets.size
            ).info { "Bulk budget creation request received" }

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

            log.withContext(
                "userId" to userId,
                "endpoint" to "POST /budgets",
                "budgetName" to request.name,
                "limit" to request.limit
            ).info { "Budget creation request received" }

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

            log.withContext(
                "userId" to userId,
                "budgetId" to id,
                "endpoint" to "PUT /budgets/{id}"
            ).info { "Budget update request received" }

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

            log.withContext(
                "userId" to userId,
                "budgetId" to id,
                "endpoint" to "DELETE /budgets/{id}"
            ).info { "Budget deletion request received" }

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

            log.withContext(
                "userId" to userId,
                "accountIds" to accountIds,
                "endpoint" to "DELETE /budgets/clear"
            ).info { "Delete budgets request received" }

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

            log.withContext(
                "userId" to userId,
                "accountId" to accountId,
                "limit" to limit,
                "offset" to offset,
                "endpoint" to "GET /budgets"
            ).info { "Fetch budgets request received" }

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

            log.withContext(
                "userId" to userId,
                "budgetId" to id,
                "endpoint" to "GET /budgets/{id}"
            ).info { "Fetch budget by ID request received" }

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
