package feature.transactions

import com.fintrack.core.ApiResponse
import com.fintrack.core.userIdOrThrow
import com.fintrack.feature.summary.data.toDto
import com.fintrack.feature.summary.domain.DistributionSummary
import core.ValidationException
import feature.transactions.data.TransactionRepository
import feature.transactions.data.model.PaginatedTransactionDto
import feature.transactions.data.model.TransactionDto
import feature.transactions.data.toDto
import feature.transactions.data.toTransaction
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import org.jetbrains.exposed.sql.SortOrder


fun Route.transactionRoutes() {
    val repo = TransactionRepository()

    route("/transactions") {
        // DELETE /transactions
        delete("/clear") {
            val userId = call.userIdOrThrow()
            val accountId: Int? = call.request.queryParameters["accountId"]?.toIntOrNull()

            repo.clearAll(userId, accountId)
            call.respond(
                ApiResponse.Success(
                    mapOf(
                        "message" to if (accountId != null)
                            "All transactions cleared for account $accountId"
                        else "All transactions cleared for user $userId"
                    )
                )
            )
        }

        // POST /transactions/bulk
        post("/bulk") {
            val userId = call.userIdOrThrow()
            val dtos = call.receive<List<TransactionDto>>()
            val saved = dtos.map { dto ->
                dto.validate()
                repo.add(dto.toTransaction(userId)).toDto() // inject userId here
            }

            call.respond(HttpStatusCode.Created, ApiResponse.Success(saved))
        }
        // GET /transactions?type=&category=&start=&end=&sortBy=&order=&limit=&afterDateTime=&afterId=
        get {
            val userId = call.userIdOrThrow()

            // Account filter
            val accountId: Int? = call.request.queryParameters["accountId"]?.toIntOrNull()

            // Type filter
            val isIncome = when (call.request.queryParameters["type"]?.lowercase()) {
                "income" -> true
                "expense" -> false
                else -> null
            }

            // Category filter
            val categories = call.request.queryParameters["category"]?.split(",")

            // Start/end filters
            val start: LocalDateTime? =
                call.request.queryParameters["start"]?.let { LocalDate.parse(it).atTime(0, 0, 0) }
            val end: LocalDateTime? =
                call.request.queryParameters["end"]?.let { LocalDate.parse(it).atTime(23, 59, 59) }

            // Sorting and limit
            val sortBy = call.request.queryParameters["sortBy"] ?: "dateTime"
            val order =
                if (call.request.queryParameters["order"]?.uppercase() == "DESC") SortOrder.DESC else SortOrder.ASC
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceAtMost(50) ?: 20

            // Cursor
            val afterDateTime: LocalDateTime? =
                call.request.queryParameters["afterDateTime"]?.let { LocalDateTime.parse(it) }
            val afterId: Int? = call.request.queryParameters["afterId"]?.toIntOrNull()

            // Fetch paginated transactions (scoped to user + optional account)
            val transactions = repo.getAllCursor(
                userId = userId,
                accountId = accountId,
                isIncome = isIncome,
                categories = categories,
                start = start,
                end = end,
                sortBy = sortBy,
                order = order,
                limit = limit,
                afterDateTime = afterDateTime,
                afterId = afterId
            ).map { it.toDto() }

            // Build next cursor
            val last = transactions.lastOrNull()
            val nextCursor = last?.let { "${it.dateTime}|${it.id}" }

            call.respond(
                ApiResponse.Success(
                    PaginatedTransactionDto(
                        data = transactions,
                        nextCursor = nextCursor
                    )
                )
            )
        }

        // GET /transactions/{id}
        get("{id}") {
            val userId = call.userIdOrThrow()
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ValidationException("Invalid ID")

            val transaction = repo.getById(id, userId)

            call.respond(ApiResponse.Success(transaction.toDto()))
        }
        // POST /transactions
        post {
            val userId = call.userIdOrThrow()
            val dto = call.receive<TransactionDto>()
            dto.validate()

            // Convert DTO to domain model and inject userId
            val transaction = dto.toTransaction(userId)

            val saved = repo.add(transaction)

            call.respond(HttpStatusCode.Created, ApiResponse.Success(saved.toDto()))
        }

        // PUT /transactions/{id}
        put("{id}") {
            val userId = call.userIdOrThrow()
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ValidationException("Invalid ID")

            val dto = call.receive<TransactionDto>()
            dto.validate()

            // Convert DTO to domain model, inject userId and id
            val transaction = dto.toTransaction(userId).copy(id = id)

            val updated = repo.update(userId = userId, id = id, entity = transaction)

            call.respond(ApiResponse.Success(updated.toDto()))
        }

        // DELETE /transactions/{id}
        delete("{id}") {
            val userId = call.userIdOrThrow()
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ValidationException("Invalid ID")

            // Delete transaction with ownership check
            repo.delete(id, userId)

            call.respond(ApiResponse.Success(mapOf("message" to "Transaction deleted successfully")))
        }










        // Day summaries for a custom date range


    }
}
