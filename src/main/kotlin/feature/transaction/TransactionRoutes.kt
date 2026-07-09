package feature.transaction

import com.fintrack.core.domain.*
import com.fintrack.core.logger
import com.fintrack.core.toUUIDOrNull
import com.fintrack.core.userIdOrThrow
import com.fintrack.core.withContext
import com.fintrack.feature.transaction.data.model.BulkCreateTransactionRequest
import com.fintrack.feature.transactions.data.model.CreateTransactionRequest
import com.fintrack.feature.transactions.data.model.UpdateTransactionRequest
import feature.transaction.domain.TransactionService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import java.util.UUID

fun Route.transactionRoutes(service: TransactionService) {
    val log = logger("TransactionRoutes")

    route("/transactions") {
        delete("/clear") {
            val userId = call.userIdOrThrow()
            val accountIds = call.request.queryParameters.getAll("accountId")?.mapNotNull { it.toUUIDOrNull() }

            log.withContext(
                "userId" to userId,
                "accountIds" to accountIds,
                "endpoint" to "DELETE /transactions/clear"
            ).warn { "Clear transactions request received" }

            when (val result = service.clearAll(userId, accountIds)) {
                is Result.Success -> call.respond(ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        get("/recurring/detect") {
            val userId = call.userIdOrThrow()
            log.withContext(
                "userId" to userId,
                "endpoint" to "GET /transactions/recurring/detect"
            ).info { "Detect recurring bills request received" }

            when (val result = service.detectRecurringBills(userId)) {
                is Result.Success -> call.respond(ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        post("/bulk") {
            val userId = call.userIdOrThrow()
            val bulkRequest = call.receive<BulkCreateTransactionRequest>()

            log.withContext(
                "userId" to userId,
                "endpoint" to "POST /transactions/bulk",
                "transactionCount" to bulkRequest.transactions.size
            ).info { "Bulk transaction creation request received" }

            when (val result = service.addBulk(userId, bulkRequest.transactions)) {
                is Result.Success -> call.respond(HttpStatusCode.Created, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        post("/batch") {
            val userId = call.userIdOrThrow()
            val requests = call.receive<List<CreateTransactionRequest>>()

            log.withContext(
                "userId" to userId,
                "endpoint" to "POST /transactions/batch",
                "transactionCount" to requests.size
            ).info { "Batch transaction creation request received" }

            when (val result = service.addBulk(userId, requests)) {
                is Result.Success -> call.respond(HttpStatusCode.Created, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        post("/mpesa") {
            val userId = call.userIdOrThrow()
            val requests = call.receive<List<CreateTransactionRequest>>()

            log.withContext(
                "userId" to userId,
                "endpoint" to "POST /transactions/mpesa",
                "transactionCount" to requests.size
            ).info { "M-Pesa batch transaction creation request received" }

            when (val result = service.addBulk(userId, requests)) {
                is Result.Success -> call.respond(HttpStatusCode.Created, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        post("/equity") {
            val userId = call.userIdOrThrow()
            val requests = call.receive<List<CreateTransactionRequest>>()

            log.withContext(
                "userId" to userId,
                "endpoint" to "POST /transactions/equity",
                "transactionCount" to requests.size
            ).info { "Equity batch transaction creation request received" }

            when (val result = service.syncEquityTransactions(userId, requests)) {
                is Result.Success -> call.respond(HttpStatusCode.Created, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        get {
            val userId = call.userIdOrThrow()
            val accountId = call.request.queryParameters["accountId"]?.toUUIDOrNull()
            val typeFilter = call.request.queryParameters["type"]
            val isIncomeParam = call.request.queryParameters["isIncome"]?.toBooleanStrictOrNull()
            val categories = call.request.queryParameters["category"]?.split(",")
            val startDate = call.request.queryParameters["start"]
            val endDate = call.request.queryParameters["end"]
            val hasCost = call.request.queryParameters["hasCost"]?.toBooleanStrictOrNull()
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
                "isIncomeParam" to isIncomeParam,
                "categoryCount" to categories?.size,
                "startDate" to startDate,
                "endDate" to endDate,
                "hasCost" to hasCost,
                "sortBy" to sortBy,
                "order" to order,
                "limit" to limit,
                "afterDateTime" to afterDateTime,
                "afterId" to afterId
            ).info { "Transaction list request received" }

            when (val result = service.getAllCursor(
                userId = userId,
                accountId = accountId,
                typeFilter = typeFilter,
                isIncome = isIncomeParam,
                categories = categories,
                startDate = startDate,
                endDate = endDate,
                sortBy = sortBy,
                order = order,
                limit = limit,
                afterDateTime = afterDateTime,
                afterId = afterId,
                hasTransactionCost = hasCost
            )) {
                is Result.Success -> {
                    log.withContext(
                        "userId" to userId,
                        "transactionCount" to result.value.data.size,
                        "hasNextCursor" to (result.value.nextCursor != null)
                    ).debug { "Transaction list retrieved successfully" }
                    call.respond(ApiResponse.Success(result.value))
                }
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        get("{id}") {
            val userId = call.userIdOrThrow()
            val id = call.parameters["id"]?.toUUIDOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid transaction ID", "INVALID_ID"))

            log.withContext(
                "userId" to userId,
                "transactionId" to id,
                "endpoint" to "GET /transactions/{id}"
            ).info { "Get transaction by ID request received" }

            when (val result = service.getById(userId, id)) {
                is Result.Success -> call.respond(ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
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

            when (val result = service.add(userId, request)) {
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
                ?: return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid transaction ID", "INVALID_ID"))
            val request = call.receive<UpdateTransactionRequest>()

            log.withContext(
                "userId" to userId,
                "transactionId" to id,
                "endpoint" to "PUT /transactions/{id}",
                "accountId" to request.accountId,
                "amount" to request.amount
            ).info { "Update transaction request received" }

            when (val result = service.update(userId, id, request)) {
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
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid transaction ID", "INVALID_ID"))

            log.withContext(
                "userId" to userId,
                "transactionId" to id,
                "endpoint" to "DELETE /transactions/{id}"
            ).info { "Delete transaction request received" }

            when (val result = service.delete(userId, id)) {
                is Result.Success -> call.respond(ApiResponse.Success(mapOf("message" to "Transaction deleted successfully")))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }
    }
}
