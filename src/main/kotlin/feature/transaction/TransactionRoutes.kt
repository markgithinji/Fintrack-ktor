package feature.transaction

import com.fintrack.core.domain.ApiResponse
import com.fintrack.core.logger
import com.fintrack.core.userIdOrThrow
import com.fintrack.core.withContext
import com.fintrack.feature.transactions.data.model.CreateTransactionRequest
import com.fintrack.feature.transactions.data.model.UpdateTransactionRequest
import core.ValidationException
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
import java.util.UUID

fun Route.transactionRoutes(service: TransactionService) {
    val log = logger("TransactionRoutes")

    route("/transactions") {
        delete("/clear") {
            val userId = call.userIdOrThrow()
            val accountId: UUID? =
                call.request.queryParameters["accountId"]?.let { UUID.fromString(it) }

            log.withContext(
                "userId" to userId,
                "accountId" to accountId,
                "endpoint" to "DELETE /transactions/clear"
            ).warn { "Clear all transactions request received" }

            val result = service.clearAll(userId, accountId)
            call.respond(ApiResponse.Success(result))
        }

        post("/bulk") {
            val userId = call.userIdOrThrow()
            val requests = call.receive<List<CreateTransactionRequest>>()

            log.withContext(
                "userId" to userId,
                "endpoint" to "POST /transactions/bulk",
                "transactionCount" to requests.size
            ).info { "Bulk transaction creation request received" }

            val saved = service.addBulk(userId, requests)
            call.respond(HttpStatusCode.Created, ApiResponse.Success(saved))
        }

        get {
            val userId = call.userIdOrThrow()
            val accountId = call.request.queryParameters["accountId"]?.let { UUID.fromString(it) }
            val typeFilter = call.request.queryParameters["type"]
            val categories = call.request.queryParameters["category"]?.split(",")
            val startDate = call.request.queryParameters["start"]
            val endDate = call.request.queryParameters["end"]
            val sortBy = call.request.queryParameters["sortBy"] ?: "dateTime"
            val order = call.request.queryParameters["order"]?.uppercase()
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceAtMost(50) ?: 20
            val afterDateTime = call.request.queryParameters["afterDateTime"]
            val afterId = call.request.queryParameters["afterId"]?.let { UUID.fromString(it) }

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
            ).info { "Transaction list request received" }

            val result = service.getAllCursor(
                userId = userId,
                accountId = accountId,
                typeFilter = typeFilter,
                categories = categories,
                startDate = startDate,
                endDate = endDate,
                sortBy = sortBy,
                order = order,
                limit = limit,
                afterDateTime = afterDateTime,
                afterId = afterId
            )

            log.withContext(
                "userId" to userId,
                "transactionCount" to result.data.size,
                "hasNextCursor" to (result.nextCursor != null)
            ).debug { "Transaction list retrieved successfully" }

            call.respond(ApiResponse.Success(result))
        }

        get("{id}") {
            val userId = call.userIdOrThrow()
            val id = call.parameters["id"]?.let { UUID.fromString(it) }
                ?: throw ValidationException("Invalid ID")

            log.withContext(
                "userId" to userId,
                "transactionId" to id,
                "endpoint" to "GET /transactions/{id}"
            ).info { "Get transaction by ID request received" }

            val transaction = service.getById(userId, id)
            call.respond(ApiResponse.Success(transaction))
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
            ).info { "Create transaction request received" }

            val saved = service.add(userId, request)
            call.respond(HttpStatusCode.Created, ApiResponse.Success(saved))
        }

        put("{id}") {
            val userId = call.userIdOrThrow()
            val id = call.parameters["id"]?.let { UUID.fromString(it) }
                ?: throw ValidationException("Invalid ID")
            val request = call.receive<UpdateTransactionRequest>()

            log.withContext(
                "userId" to userId,
                "transactionId" to id,
                "endpoint" to "PUT /transactions/{id}",
                "accountId" to request.accountId,
                "amount" to request.amount
            ).info { "Update transaction request received" }

            val updated = service.update(userId, id, request)
            call.respond(ApiResponse.Success(updated))
        }

        delete("{id}") {
            val userId = call.userIdOrThrow()
            val id = call.parameters["id"]?.let { UUID.fromString(it) }
                ?: throw ValidationException("Invalid ID")

            log.withContext(
                "userId" to userId,
                "transactionId" to id,
                "endpoint" to "DELETE /transactions/{id}"
            ).info { "Delete transaction request received" }

            service.delete(userId, id)
            call.respond(ApiResponse.Success(mapOf("message" to "Transaction deleted successfully")))
        }
    }
}