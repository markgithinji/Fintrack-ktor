package feature.transaction

import com.fintrack.core.ApiResponse
import com.fintrack.core.logger
import com.fintrack.core.userIdOrThrow
import com.fintrack.core.withContext
import com.fintrack.feature.transactions.data.model.CreateTransactionRequest
import com.fintrack.feature.transactions.data.model.UpdateTransactionRequest
import core.ValidationException
import feature.transaction.data.model.PaginatedTransactionDto
import feature.transaction.data.model.toDto
import feature.transaction.domain.TransactionService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime
import org.jetbrains.exposed.sql.SortOrder

fun Route.transactionRoutes(service: TransactionService) {
    val log = logger()

    route("/transactions") {
        delete("/clear") {
            val userId = call.userIdOrThrow()
            val accountId: Int? = call.request.queryParameters["accountId"]?.toIntOrNull()

            log.withContext(
                "userId" to userId,
                "accountId" to accountId,
                "endpoint" to "DELETE /transactions/clear"
            ).warn("Clear all transactions request received")

            service.clearAll(userId, accountId)

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

        post("/bulk") {
            val userId = call.userIdOrThrow()
            val requests = call.receive<List<CreateTransactionRequest>>()

            log.withContext(
                "userId" to userId,
                "endpoint" to "POST /transactions/bulk",
                "transactionCount" to requests.size
            ).info("Bulk transaction creation request received")

            val saved = service.addBulk(userId, requests).map { it.toDto() }
            call.respond(HttpStatusCode.Created, ApiResponse.Success(saved))
        }

        get {
            val userId = call.userIdOrThrow()
            val accountId = call.request.queryParameters["accountId"]?.toIntOrNull()
            val typeFilter = call.request.queryParameters["type"]
            val categories = call.request.queryParameters["category"]?.split(",")
            val startDate = call.request.queryParameters["start"]
            val endDate = call.request.queryParameters["end"]
            val sortBy = call.request.queryParameters["sortBy"] ?: "dateTime"
            val order = call.request.queryParameters["order"]?.uppercase()
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceAtMost(50) ?: 20
            val afterDateTime = call.request.queryParameters["afterDateTime"]
            val afterId = call.request.queryParameters["afterId"]?.toIntOrNull()

            log.withContext(
                "userId" to userId,
                "endpoint" to "GET /transactions",
                "accountId" to accountId,
                "typeFilter" to typeFilter,
                "categoryCount" to categories?.size,
                "startDate" to startDate,
                "endDate" to endDate,
                "sortBy" to sortBy,
                "order" to order,
                "limit" to limit,
                "afterDateTime" to afterDateTime,
                "afterId" to afterId
            ).info("Transaction list request received")

            val transactions = service.getAllCursor(
                userId = userId,
                accountId = accountId,
                isIncome = when (typeFilter?.lowercase()) {
                    "income" -> true
                    "expense" -> false
                    else -> null
                },
                categories = categories,
                start = startDate?.let { LocalDate.parse(it).atTime(0, 0, 0) },
                end = endDate?.let { LocalDate.parse(it).atTime(23, 59, 59) },
                sortBy = sortBy,
                order = if (order == "DESC") SortOrder.DESC else SortOrder.ASC,
                limit = limit,
                afterDateTime = afterDateTime?.let { LocalDateTime.parse(it) },
                afterId = afterId
            ).map { it.toDto() }

            val last = transactions.lastOrNull()
            val nextCursor = last?.let { "${it.dateTime}|${it.id}" }

            log.withContext(
                "userId" to userId,
                "transactionCount" to transactions.size,
                "hasNextCursor" to (nextCursor != null)
            ).debug("Transaction list retrieved successfully")

            call.respond(ApiResponse.Success(PaginatedTransactionDto(transactions, nextCursor)))
        }

        get("{id}") {
            val userId = call.userIdOrThrow()
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ValidationException("Invalid ID")

            log.withContext(
                "userId" to userId,
                "transactionId" to id,
                "endpoint" to "GET /transactions/{id}"
            ).info("Get transaction by ID request received")

            val transaction = service.getById(userId, id)
            call.respond(ApiResponse.Success(transaction.toDto()))
        }

        post {
            val userId = call.userIdOrThrow()
            val request = call.receive<CreateTransactionRequest>()

            log.withContext(
                "userId" to userId,
                "endpoint" to "POST /transactions",
                "accountId" to request.accountId,
                "amount" to request.amount,
                "isIncome" to request.isIncome,
                "category" to request.category
            ).info("Create transaction request received")

            val saved = service.add(userId, request)
            call.respond(HttpStatusCode.Created, ApiResponse.Success(saved.toDto()))
        }

        put("{id}") {
            val userId = call.userIdOrThrow()
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ValidationException("Invalid ID")
            val request = call.receive<UpdateTransactionRequest>()

            log.withContext(
                "userId" to userId,
                "transactionId" to id,
                "endpoint" to "PUT /transactions/{id}",
                "accountId" to request.accountId,
                "amount" to request.amount
            ).info("Update transaction request received")

            val updated = service.update(userId, id, request)
            call.respond(ApiResponse.Success(updated.toDto()))
        }

        delete("{id}") {
            val userId = call.userIdOrThrow()
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ValidationException("Invalid ID")

            log.withContext(
                "userId" to userId,
                "transactionId" to id,
                "endpoint" to "DELETE /transactions/{id}"
            ).info("Delete transaction request received")

            service.delete(userId, id)
            call.respond(ApiResponse.Success(mapOf("message" to "Transaction deleted successfully")))
        }
    }
}
