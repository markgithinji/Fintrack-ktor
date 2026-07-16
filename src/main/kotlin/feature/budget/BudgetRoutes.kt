package feature.budget

import com.fintrack.core.domain.ApiResponse
import com.fintrack.core.domain.Result
import com.fintrack.core.domain.toApiResponse
import com.fintrack.core.domain.toHttpStatusCode
import com.fintrack.core.toUUIDOrNull
import com.fintrack.core.userIdOrThrow
import com.fintrack.feature.budget.data.model.BulkCreateBudgetRequest
import com.fintrack.feature.budget.data.model.CreateBudgetRequest
import com.fintrack.feature.budget.data.model.UpdateBudgetRequest
import feature.budget.domain.BudgetService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.budgetRoutes(budgetService: BudgetService) {

    route("/budgets") {

        post("/bulk") {
            val userId = call.userIdOrThrow()
            val bulkRequest = call.receive<BulkCreateBudgetRequest>()

            when (val result = budgetService.createBudgets(userId, bulkRequest.budgets)) {
                is Result.Success -> call.respond(ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    result.error.toApiResponse()
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
                    result.error.toApiResponse()
                )
            }
        }

        put("{id}") {
            val userId = call.userIdOrThrow()
            val id = call.parameters["id"]?.toUUIDOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Invalid budget ID", "INVALID_ID"))

            val request = call.receive<UpdateBudgetRequest>()

            when (val result = budgetService.updateBudget(userId, id, request)) {
                is Result.Success -> call.respond(ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    result.error.toApiResponse()
                )
            }
        }

        delete("{id}") {
            val userId = call.userIdOrThrow()
            val id = call.parameters["id"]?.toUUIDOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Invalid budget ID", "INVALID_ID"))

            when (val result = budgetService.deleteBudget(userId, id)) {
                is Result.Success -> call.respond(ApiResponse.Success("Deleted budget with id: $id"))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    result.error.toApiResponse()
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
                    result.error.toApiResponse()
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
                    result.error.toApiResponse()
                )
            }
        }

        get("{id}") {
            val userId = call.userIdOrThrow()
            val id = call.parameters["id"]?.toUUIDOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Invalid budget ID", "INVALID_ID"))

            when (val result = budgetService.getBudgetById(userId, id)) {
                is Result.Success -> call.respond(ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    result.error.toApiResponse()
                )
            }
        }
    }
}
