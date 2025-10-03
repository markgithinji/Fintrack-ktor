package feature.transaction

import com.fintrack.core.ApiResponse
import com.fintrack.core.userIdOrThrow
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

    route("/transactions") {
        delete("/clear") {
            val userId = call.userIdOrThrow()
            val accountId: Int? = call.request.queryParameters["accountId"]?.toIntOrNull()

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
            val saved = service.addBulk(userId, requests).map { it.toDto() }
            call.respond(HttpStatusCode.Created, ApiResponse.Success(saved))
        }

        get {
            val userId = call.userIdOrThrow()
            val transactions = service.getAllCursor(
                userId = userId,
                accountId = call.request.queryParameters["accountId"]?.toIntOrNull(),
                isIncome = when (call.request.queryParameters["type"]?.lowercase()) {
                    "income" -> true
                    "expense" -> false
                    else -> null
                },
                categories = call.request.queryParameters["category"]?.split(","),
                start = call.request.queryParameters["start"]?.let { LocalDate.parse(it).atTime(0,0,0) },
                end = call.request.queryParameters["end"]?.let { LocalDate.parse(it).atTime(23,59,59) },
                sortBy = call.request.queryParameters["sortBy"] ?: "dateTime",
                order = if (call.request.queryParameters["order"]?.uppercase() == "DESC") SortOrder.DESC else SortOrder.ASC,
                limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceAtMost(50) ?: 20,
                afterDateTime = call.request.queryParameters["afterDateTime"]?.let { LocalDateTime.parse(it) },
                afterId = call.request.queryParameters["afterId"]?.toIntOrNull()
            ).map { it.toDto() }

            val last = transactions.lastOrNull()
            val nextCursor = last?.let { "${it.dateTime}|${it.id}" }

            call.respond(ApiResponse.Success(PaginatedTransactionDto(transactions, nextCursor)))
        }

        get("{id}") {
            val userId = call.userIdOrThrow()
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ValidationException("Invalid ID")
            val transaction = service.getById(userId, id)
            call.respond(ApiResponse.Success(transaction.toDto()))
        }

        post {
            val userId = call.userIdOrThrow()
            val request = call.receive<CreateTransactionRequest>()
            val saved = service.add(userId, request)
            call.respond(HttpStatusCode.Created, ApiResponse.Success(saved.toDto()))
        }

        put("{id}") {
            val userId = call.userIdOrThrow()
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ValidationException("Invalid ID")
            val request = call.receive<UpdateTransactionRequest>()
            val updated = service.update(userId, id, request)
            call.respond(ApiResponse.Success(updated.toDto()))
        }

        delete("{id}") {
            val userId = call.userIdOrThrow()
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ValidationException("Invalid ID")
            service.delete(userId, id)
            call.respond(ApiResponse.Success(mapOf("message" to "Transaction deleted successfully")))
        }
    }
}

