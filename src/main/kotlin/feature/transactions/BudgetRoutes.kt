package feature.transactions

import com.fintrack.core.ApiResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.budgetRoutes() {
    val repository = BudgetRepository()

    route("/budgets") {
        // POST bulk
        post("/bulk") {
            try {
                val budgetDtos = call.receive<List<BudgetDto>>()
                val domainBudgets = budgetDtos.map { it.toDomain() }
                val saved = repository.addAll(domainBudgets)
                call.respond(ApiResponse.Success(saved.map { it.toDto() }))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(e.message ?: "Unknown error"))
            }
        }
        // GET all budgets
        get {
            try {
                val budgets = repository.getAll()
                call.respond(ApiResponse.Success(budgets.map { it.toDto() }))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse.Error(e.message ?: "Unknown error")
                )
            }
        }

        // GET budget by id
        get("{id}") {
            try {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Id missing"))

                val budget = repository.getById(id)
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

        // POST a new budget
        post {
            try {
                val budgetDto = call.receive<BudgetDto>()
                val budget = repository.add(budgetDto.toDomain())
                call.respond(HttpStatusCode.Created, ApiResponse.Success(budget.toDto()))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.Error(e.message ?: "Invalid request")
                )
            }
        }
        // PUT update by id
        put("{id}") {
            try {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Id missing"))
                val budgetDto = call.receive<BudgetDto>()
                val updated = repository.update(id, budgetDto.toDomain())
                if (updated) {
                    call.respond(ApiResponse.Success("Updated budget with id: $id"))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiResponse.Error("Budget not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(e.message ?: "Invalid request"))
            }
        }
        // DELETE by id
        delete("{id}") {
            try {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse.Error("Id missing")
                    )
                val removed = repository.delete(id)
                if (removed) {
                    call.respond(ApiResponse.Success("Deleted budget with id: $id"))
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


