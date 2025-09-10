package com.fintrack.feature.transactions

import com.fintrack.core.ApiResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.sql.SortOrder

fun Route.transactionRoutes() {
    val repo = TransactionRepository()

    route("/transactions") {


        // DELETE /transactions
        delete {
            try {
                val success = repo.clearAll()
                call.respond(
                    ApiResponse.Success(mapOf("message" to "All transactions cleared"))
                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(e.message ?: "Failed to clear transactions"))
            }
        }

        // POST /transactions/bulk
        post("/bulk") {
            try {
                val transactions = call.receive<List<Transaction>>()
                val saved = transactions.map { repo.add(it) }
                call.respond(HttpStatusCode.Created, ApiResponse.Success(saved))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(e.message ?: "Failed to create transactions"))
            }
        }


        // GET /transactions?type=&category=&start=&end=&sortBy=&order=&page=&size=
        get() {
            try {
                // Convert type query param to Boolean
                val isIncome = when (call.request.queryParameters["type"]?.lowercase()) {
                    "income" -> true
                    "expense" -> false
                    else -> null
                }

                val categories = call.request.queryParameters["category"]?.split(",")
                val start = call.request.queryParameters["start"]?.let { LocalDate.parse(it) }
                val end = call.request.queryParameters["end"]?.let { LocalDate.parse(it) }
                val sortBy = call.request.queryParameters["sortBy"] ?: "date"
                val order = if (call.request.queryParameters["order"]?.uppercase() == "DESC") SortOrder.DESC else SortOrder.ASC
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20

                val transactions = repo.getAll(isIncome, categories, start, end, sortBy, order, page, size)
                call.respond(ApiResponse.Success(transactions))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(e.message ?: "Failed to fetch transactions"))
            }
        }


        // GET /transactions/{id}
        get("{id}") {
            try {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Invalid ID"))

                val transaction = repo.getById(id)
                if (transaction != null) call.respond(ApiResponse.Success(transaction))
                else call.respond(HttpStatusCode.NotFound, ApiResponse.Error("Transaction not found"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(e.message ?: "Failed to fetch transaction"))
            }
        }

        // POST /transactions
        post {
            try {
                val transaction = call.receive<Transaction>()
                transaction.validate()
                val saved = repo.add(transaction)
                call.respond(HttpStatusCode.Created, ApiResponse.Success(saved))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(e.message ?: "Failed to create transaction"))
            }
        }

        // PUT /transactions/{id}
        put("{id}") {
            try {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Invalid ID"))

                val body = call.receive<Transaction>()
                body.validate()
                val success = repo.update(id, body)
                if (success) call.respond(ApiResponse.Success(repo.getById(id)!!))
                else call.respond(HttpStatusCode.NotFound, ApiResponse.Error("Transaction not found"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(e.message ?: "Failed to update transaction"))
            }
        }

        // DELETE /transactions/{id}
        delete("{id}") {
            try {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Invalid ID"))

                val success = repo.delete(id)
                if (success) call.respond(ApiResponse.Success(mapOf("message" to "Transaction deleted")))
                else call.respond(HttpStatusCode.NotFound, ApiResponse.Error("Transaction not found"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(e.message ?: "Failed to delete transaction"))
            }
        }

        // GET /transactions/summary?type=&start=&end=&byCategory=true&monthly=true
        get("/summary") {
            try {
                // Convert type query param to Boolean
                val isIncome = when (call.request.queryParameters["type"]?.lowercase()) {
                    "income" -> true
                    "expense" -> false
                    else -> null
                }

                val start = call.request.queryParameters["start"]?.let { LocalDate.parse(it) }
                val end = call.request.queryParameters["end"]?.let { LocalDate.parse(it) }
                val byCategory = call.request.queryParameters["byCategory"]?.toBoolean() ?: false
                val monthly = call.request.queryParameters["monthly"]?.toBoolean() ?: false

                val summary = repo.getSummary(isIncome, start, end, byCategory, monthly)
                call.respond(ApiResponse.Success(summary))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(e.message ?: "Failed to fetch summary"))
            }
        }

    }
}
