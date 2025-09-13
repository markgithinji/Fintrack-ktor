package feature.transactions

import com.fintrack.core.ApiResponse
import core.AvailableWeeks
import core.PaginatedTransactionDto
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

        // GET /transactions?type=&category=&start=&end=&sortBy=&order=&limit=&afterDate=&afterId=
        get {
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

            // Fetch paginated transactions
            val transactions = repo.getAllCursor(
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


        // GET /transactions/summary/highlights?type=&start=&end=
        get("/summary/highlights") {
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
                isIncome = isIncomeFilter,
                start = start,
                end = end
            )

            call.respond(HttpStatusCode.OK, ApiResponse.Success(highlights.toDto()))
        }

        // GET /transactions/summary/distribution?period=2025-W37&type=&start=&end=
        get("/summary/distribution") {
            val period = call.request.queryParameters["period"]
                ?: return@get call.respondText(
                    "Missing period parameter",
                    status = HttpStatusCode.BadRequest
                )

            val start: LocalDateTime? = call.request.queryParameters["start"]
                ?.let { LocalDate.parse(it).atTime(0, 0, 0) }
            val end: LocalDateTime? = call.request.queryParameters["end"]
                ?.let { LocalDate.parse(it).atTime(23, 59, 59) }

            // Optional filter by type (income/expense)
            val typeFilter = call.request.queryParameters["type"]?.lowercase()
            val isIncomeFilter = when (typeFilter) {
                "income" -> true
                "expense" -> false
                else -> null
            }

            val distribution: DistributionSummary = repo.getDistributionSummary(
                period = period,
                isIncome = isIncomeFilter,
                start = start,
                end = end
            )

            call.respond(HttpStatusCode.OK, ApiResponse.Success(distribution.toDto()))
        }

        get("/available-weeks") {
            val weeks = transaction {
                TransactionsTable
                    .selectAll()
                    .map { it[TransactionsTable.dateTime].toLocalDate() }
                    .map { date ->
                        val year = date.year
                        val week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                        "%04d-W%02d".format(year, week)
                    }
                    .distinct()
                    .sortedDescending()
            }

            val result = AvailableWeeks(weeks)
            call.respond(HttpStatusCode.OK, ApiResponse.Success(result.toDto()))
        }
    }
}
