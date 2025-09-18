package feature.transactions

import com.fintrack.core.ApiResponse
import com.fintrack.feature.transactions.userIdOrThrow
import core.AvailableWeeks
import core.PaginatedTransactionDto
import core.TransactionDto
import core.ValidationException
import core.toDto
import core.toTransaction
import core.validate
import io.ktor.http.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.temporal.IsoFields

fun Route.transactionRoutes() {
    val repo = TransactionRepository()

    route("/transactions") {
        // DELETE /transactions
        delete {
            val userId = call.userIdOrThrow()
            repo.clearAll(userId)
            call.respond(ApiResponse.Success(mapOf("message" to "All transactions cleared for user $userId")))
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
            // Type filter
            val isIncome = when (call.request.queryParameters["type"]?.lowercase()) {
                "income" -> true
                "expense" -> false
                else -> null
            }
            // Category filter
            val categories = call.request.queryParameters["category"]?.split(",")
            // Start/end filters as LocalDateTime
            val start: LocalDateTime? =
                call.request.queryParameters["start"]?.let { LocalDate.parse(it).atTime(0, 0, 0) }
            val end: LocalDateTime? =
                call.request.queryParameters["end"]?.let { LocalDate.parse(it).atTime(23, 59, 59) }
            // Sorting and limit
            val sortBy = call.request.queryParameters["sortBy"] ?: "dateTime"
            val order =
                if (call.request.queryParameters["order"]?.uppercase() == "DESC") SortOrder.DESC else SortOrder.ASC
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceAtMost(50) ?: 20
            // Cursor (dateTime + id)
            val afterDateTime: LocalDateTime? =
                call.request.queryParameters["afterDateTime"]?.let { LocalDateTime.parse(it) }
            val afterId: Int? = call.request.queryParameters["afterId"]?.toIntOrNull()
            // Fetch paginated transactions (scoped to user)
            val transactions = repo.getAllCursor(
                userId = userId,
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
            val transaction = repo.getById(userId, id)
                ?: throw NoSuchElementException("Transaction not found")

            call.respond(ApiResponse.Success(transaction.toDto()))
        }
        // POST /transactions
        post {
            val userId = call.userIdOrThrow()
            val dto = call.receive<TransactionDto>()
            dto.validate()
            // Inject userId from session, not from DTO
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
            // Inject userId from session, tie the entity to this user
            val transaction = dto.toTransaction(userId).copy(id = id)
            val updated = repo.update(userId = userId, id = id, entity = transaction)

            call.respond(ApiResponse.Success(updated.toDto()))
        }
        // DELETE /transactions/{id}
        delete("{id}") {
            val userId = call.userIdOrThrow()
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ValidationException("Invalid ID")
            // Pass userId to enforce ownership
            repo.delete(id, userId)

            call.respond(ApiResponse.Success(mapOf("message" to "Transaction deleted")))
        }
        // GET /transactions/summary/highlights?type=&start=&end=
        get("/summary/highlights") {
            val userId = call.userIdOrThrow()
            val typeFilter = call.request.queryParameters["type"]?.lowercase()
            val isIncomeFilter = when (typeFilter) {
                "income" -> true
                "expense" -> false
                else -> null
            }
            val start: LocalDateTime? = call.request.queryParameters["start"]
                ?.let { LocalDate.parse(it).atTime(0, 0, 0) }
            val end: LocalDateTime? = call.request.queryParameters["end"]
                ?.let { LocalDate.parse(it).atTime(23, 59, 59) }
            val highlights: HighlightsSummary = repo.getHighlightsSummary(
                userId = userId,
                isIncome = isIncomeFilter,
                start = start,
                end = end
            )

            call.respond(HttpStatusCode.OK, ApiResponse.Success(highlights.toDto()))
        }
        // GET /transactions/summary/distribution?period=2025-W37&type=&start=&end=
        get("/summary/distribution") {
            val userId = call.userIdOrThrow()
            val period = call.request.queryParameters["period"]
                ?: return@get call.respondText(
                    "Missing period parameter",
                    status = HttpStatusCode.BadRequest
                )
            val start: LocalDateTime? = call.request.queryParameters["start"]
                ?.let { LocalDate.parse(it).atTime(0, 0, 0) }
            val end: LocalDateTime? = call.request.queryParameters["end"]
                ?.let { LocalDate.parse(it).atTime(23, 59, 59) }
            val typeFilter = call.request.queryParameters["type"]?.lowercase()
            val isIncomeFilter = when (typeFilter) {
                "income" -> true
                "expense" -> false
                else -> null
            }
            val distribution: DistributionSummary = repo.getDistributionSummary(
                userId = userId,
                period = period,
                isIncome = isIncomeFilter,
                start = start,
                end = end
            )

            call.respond(HttpStatusCode.OK, ApiResponse.Success(distribution.toDto()))
        }


        get("/available-weeks") {
            val userId = call.userIdOrThrow()
            val result = repo.getAvailableWeeks(userId)
            call.respond(HttpStatusCode.OK, ApiResponse.Success(result.toDto()))
        }

        get("/available-months") {
            val userId = call.userIdOrThrow()
            val result = repo.getAvailableMonths(userId)
            call.respond(HttpStatusCode.OK, ApiResponse.Success(result.toDto()))
        }

        get("/available-years") {
            val userId = call.userIdOrThrow()
            val result = repo.getAvailableYears(userId)
            call.respond(HttpStatusCode.OK, ApiResponse.Success(result.toDto()))
        }

        // Overview route
        get("/overview") {
            val userId = call.userIdOrThrow()
            val overview = repo.getOverviewSummary(userId)

            call.respond(
                HttpStatusCode.OK,
                ApiResponse.Success(overview.toDto())
            )
        }

        // Day summaries for a custom date range
        get("/overview/range") {
            val userId = call.userIdOrThrow()
            val startParam = call.request.queryParameters["start"]
            val endParam = call.request.queryParameters["end"]

            if (startParam == null || endParam == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.Error("start and end query params required (yyyy-MM-dd)")
                )
                return@get
            }

            try {
                val start = kotlinx.datetime.LocalDate.parse(startParam)
                val end = kotlinx.datetime.LocalDate.parse(endParam)
                val days = repo.getDaySummaries(userId, start, end)

                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse.Success(days.map { it.toDto() })
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.Error("Invalid date format, expected yyyy-MM-dd")
                )
            }
        }

        get("/category-comparison") {
            val userId = call.userIdOrThrow()
            val comparisons = repo.getCategoryComparisons(userId)
            call.respond(
                HttpStatusCode.OK,
                ApiResponse.Success(comparisons.map { it.toDto() })
            )
        }

    }
}
