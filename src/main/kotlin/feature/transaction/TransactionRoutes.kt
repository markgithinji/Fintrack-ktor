package feature.transaction

import com.fintrack.core.domain.*
import com.fintrack.core.toUUIDOrNull
import com.fintrack.core.userIdOrThrow
import com.fintrack.feature.transaction.data.model.BulkCreateTransactionRequest
import com.fintrack.feature.transaction.data.model.CreateTransactionRequest
import com.fintrack.feature.transaction.data.model.UpdateTransactionRequest
import feature.transaction.domain.TransactionService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import java.util.UUID

fun Route.transactionRoutes(service: TransactionService) {
    route("/transactions") {
        delete("/clear") {
            val userId = call.userIdOrThrow()
            val accountIds = call.request.queryParameters.getAll("accountId")?.mapNotNull { it.toUUIDOrNull() }

            when (val result = service.clearAll(userId, accountIds)) {
                is Result.Success -> call.respond(ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    result.error.toApiResponse()
                )
            }
        }

        get("/recurring/detect") {
            val userId = call.userIdOrThrow()

            when (val result = service.detectRecurringBills(userId)) {
                is Result.Success -> call.respond(ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    result.error.toApiResponse()
                )
            }
        }

        post("/bulk") {
            val userId = call.userIdOrThrow()
            val bulkRequest = call.receive<BulkCreateTransactionRequest>()

            when (val result = service.addBulk(userId, bulkRequest.transactions)) {
                is Result.Success -> call.respond(HttpStatusCode.Created, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    result.error.toApiResponse()
                )
            }
        }

        post("/batch") {
            val userId = call.userIdOrThrow()
            val requests = call.receive<List<CreateTransactionRequest>>()

            when (val result = service.addBulk(userId, requests)) {
                is Result.Success -> call.respond(HttpStatusCode.Created, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    result.error.toApiResponse()
                )
            }
        }

        post("/mpesa") {
            val userId = call.userIdOrThrow()
            val requests = call.receive<List<CreateTransactionRequest>>()

            when (val result = service.addBulk(userId, requests)) {
                is Result.Success -> call.respond(HttpStatusCode.Created, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    result.error.toApiResponse()
                )
            }
        }

        post("/equity") {
            val userId = call.userIdOrThrow()
            val requests = call.receive<List<CreateTransactionRequest>>()

            when (val result = service.syncEquityTransactions(userId, requests)) {
                is Result.Success -> call.respond(HttpStatusCode.Created, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    result.error.toApiResponse()
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
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Invalid transaction ID", "INVALID_ID"))

            when (val result = service.getById(userId, id)) {
                is Result.Success -> call.respond(ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    result.error.toApiResponse()
                )
            }
        }

        post {
            val userId = call.userIdOrThrow()
            val request = call.receive<CreateTransactionRequest>()

            when (val result = service.add(userId, request)) {
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
                ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Invalid transaction ID", "INVALID_ID"))
            val request = call.receive<UpdateTransactionRequest>()

            when (val result = service.update(userId, id, request)) {
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
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Invalid transaction ID", "INVALID_ID"))

            when (val result = service.delete(userId, id)) {
                is Result.Success -> call.respond(ApiResponse.Success(mapOf("message" to "Transaction deleted successfully")))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    result.error.toApiResponse()
                )
            }
        }
    }
}
