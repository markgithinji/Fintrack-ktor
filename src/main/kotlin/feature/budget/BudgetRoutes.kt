package feature.transactions

import com.fintrack.core.ApiResponse
import com.fintrack.feature.budget.data.BudgetDto
import com.fintrack.feature.budget.data.toDomain
import com.fintrack.feature.budget.data.toDto
import feature.budget.domain.BudgetService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
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
        // POST bulk budgets
        post("/bulk") {
            try {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asInt()
                val budgetDtos = call.receive<List<BudgetDto>>()
                val saved = budgetService.createBudgets(userId, budgetDtos)
                call.respond(ApiResponse.Success(saved.map { it.toDto() }))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(e.message ?: "Unknown error"))
            }
        }

        // POST a new budget
        post {
            try {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asInt()
                val budgetDto = call.receive<BudgetDto>()
                val budget = budgetService.createBudget(userId, budgetDto)
                call.respond(HttpStatusCode.Created, ApiResponse.Success(budget.toDto()))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(e.message ?: "Invalid request"))
            }
        }

        // PUT update by id
        put("{id}") {
            try {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asInt()
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Id missing"))

                val budgetDto = call.receive<BudgetDto>()
                val updatedBudget = budgetService.updateBudget(userId, id, budgetDto)

                if (updatedBudget != null) {
                    call.respond(ApiResponse.Success(updatedBudget.toDto()))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiResponse.Error("Budget not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(e.message ?: "Invalid request"))
            }
        }

        // DELETE a budget by id
        delete("{id}") {
            try {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asInt()
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Id missing"))

                val removed = budgetService.deleteBudget(userId, id)
                if (removed) {
                    call.respond(ApiResponse.Success("Deleted budget with id: $id"))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiResponse.Error("Budget not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse.Error(e.message ?: "Unknown error"))
            }
        }

        // GET all budgets (with status)
        get {
            try {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asInt()
                val accountId = call.request.queryParameters["accountId"]?.toIntOrNull()
                val budgets = budgetService.getAllBudgets(userId, accountId)
                call.respond(ApiResponse.Success(budgets.map { it.toDto() }))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse.Error(e.message ?: "Unknown error")
                )
            }
        }

        // GET budget by id (with status)
        get("{id}") {
            try {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asInt()
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Id missing"))

                val budget = budgetService.getBudgetById(userId, id)
                if (budget != null) {
                    call.respond(ApiResponse.Success(budget.toDto()))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiResponse.Error("Budget not found"))
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse.Error(e.message ?: "Unknown error")
                )
            }
        }
    }
}

