package feature.transaction

import com.fintrack.core.ApiResponse
import com.fintrack.core.logger
import com.fintrack.core.userIdOrThrow
import com.fintrack.core.withContext
import com.fintrack.feature.budget.data.model.CreateBudgetRequest
import com.fintrack.feature.budget.data.model.UpdateBudgetRequest
import com.fintrack.feature.budget.data.toDto
import core.ValidationException
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
    val log = logger()

    route("/budgets") {
        // POST bulk budgets
        post("/bulk") {
            val userId = call.userIdOrThrow()
            val requests = call.receive<List<CreateBudgetRequest>>()

            log.withContext(
                "userId" to userId,
                "endpoint" to "POST /budgets/bulk",
                "budgetCount" to requests.size
            ).info("Bulk budget creation request received")

            val saved = budgetService.createBudgets(userId, requests)
            call.respond(ApiResponse.Success(saved.map { it.toDto() }))
        }

        // POST a new budget
        post {
            val userId = call.userIdOrThrow()
            val request = call.receive<CreateBudgetRequest>()

            log.withContext(
                "userId" to userId,
                "endpoint" to "POST /budgets",
                "budgetName" to request.name,
                "limit" to request.limit
            ).info("Budget creation request received")

            val budget = budgetService.createBudget(userId, request)
            call.respond(HttpStatusCode.Created, ApiResponse.Success(budget.toDto()))
        }

        // PUT update by id
        put("{id}") {
            val userId = call.userIdOrThrow()
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ValidationException("Invalid budget ID")

            val request = call.receive<UpdateBudgetRequest>()

            log.withContext(
                "userId" to userId,
                "budgetId" to id,
                "endpoint" to "PUT /budgets/{id}"
            ).info("Budget update request received")

            val updatedBudget = budgetService.updateBudget(userId, id, request)
            call.respond(ApiResponse.Success(updatedBudget.toDto()))
        }

        // DELETE a budget by id
        delete("{id}") {
            val userId = call.userIdOrThrow()
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ValidationException("Invalid budget ID")

            log.withContext(
                "userId" to userId,
                "budgetId" to id,
                "endpoint" to "DELETE /budgets/{id}"
            ).info("Budget deletion request received")

            val removed = budgetService.deleteBudget(userId, id)
            if (!removed) throw NoSuchElementException("Budget not found")

            call.respond(ApiResponse.Success("Deleted budget with id: $id"))
        }

        // GET all budgets (with status)
        get {
            val userId = call.userIdOrThrow()
            val accountId = call.request.queryParameters["accountId"]?.toIntOrNull()

            log.withContext(
                "userId" to userId,
                "accountId" to accountId,
                "endpoint" to "GET /budgets"
            ).info("Fetch budgets request received")

            val budgets = budgetService.getAllBudgets(userId, accountId)
            call.respond(ApiResponse.Success(budgets.map { it.toDto() }))
        }

        // GET budget by id (with status)
        get("{id}") {
            val userId = call.userIdOrThrow()
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ValidationException("Invalid budget ID")

            log.withContext(
                "userId" to userId,
                "budgetId" to id,
                "endpoint" to "GET /budgets/{id}"
            ).info("Fetch budget by ID request received")

            val budget = budgetService.getBudgetById(userId, id)
            call.respond(ApiResponse.Success(budget.toDto()))
        }
    }
}
