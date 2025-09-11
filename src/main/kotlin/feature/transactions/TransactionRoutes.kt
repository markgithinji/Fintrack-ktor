package feature.transactions

import com.fintrack.core.ApiResponse
import core.TransactionDto
import core.ValidationException
import core.toDto
import core.toTransaction
import core.validate
import io.ktor.http.*
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
            repo.clearAll()
            call.respond(ApiResponse.Success(mapOf("message" to "All transactions cleared")))
        }

        // POST /transactions/bulk
        post("/bulk") {
            val dtos = call.receive<List<TransactionDto>>()

            val saved = dtos.map { dto ->
                dto.validate()
                repo.add(dto.toTransaction()).toDto()
            }

            call.respond(HttpStatusCode.Created, ApiResponse.Success(saved))
        }

        // GET /transactions?type=&category=&start=&end=&sortBy=&order=&page=&size=
        get {
            val isIncome = when (call.request.queryParameters["type"]?.lowercase()) {
                "income" -> true
                "expense" -> false
                else -> null
            }

            val categories = call.request.queryParameters["category"]?.split(",")
            val start = call.request.queryParameters["start"]?.let { LocalDate.parse(it) }
            val end = call.request.queryParameters["end"]?.let { LocalDate.parse(it) }
            val sortBy = call.request.queryParameters["sortBy"] ?: "date"
            val order =
                if (call.request.queryParameters["order"]?.uppercase() == "DESC") SortOrder.DESC else SortOrder.ASC
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20

            val transactions = repo.getAll(isIncome, categories, start, end, sortBy, order, page, size)
                .map { it.toDto() }

            call.respond(ApiResponse.Success(transactions))
        }

        // GET /transactions/{id}
        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ValidationException("Invalid ID")

            val transaction = repo.getById(id)
                ?: throw NoSuchElementException("Transaction not found")

            call.respond(ApiResponse.Success(transaction.toDto()))
        }

        // POST /transactions
        post {
            val dto = call.receive<TransactionDto>()
            dto.validate()

            val saved = repo.add(dto.toTransaction())
            call.respond(HttpStatusCode.Created, ApiResponse.Success(saved.toDto()))
        }

        // PUT /transactions/{id}
        put("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ValidationException("Invalid ID")

            val dto = call.receive<TransactionDto>()
            dto.validate()

            val updated = repo.update(id, dto.toTransaction().copy(id = id))
            call.respond(ApiResponse.Success(updated.toDto()))
        }

        // DELETE /transactions/{id}
        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ValidationException("Invalid ID")

            val success = repo.delete(id)
            if (!success) throw NoSuchElementException("Transaction not found")

            call.respond(ApiResponse.Success(mapOf("message" to "Transaction deleted")))
        }

        // GET /transactions/summary?type=&start=&end=&byCategory=true&monthly=true
        get("/summary") {
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
        }
    }
}
