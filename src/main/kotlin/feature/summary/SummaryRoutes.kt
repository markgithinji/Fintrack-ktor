package com.fintrack.feature.summary

import com.fintrack.core.ApiResponse
import com.fintrack.core.userIdOrThrow
import com.fintrack.feature.summary.data.repository.StatisticsRepository
import com.fintrack.feature.summary.data.toDto
import com.fintrack.feature.summary.domain.DistributionSummary
import core.ValidationException
import feature.transactions.StatisticsSummary
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

fun Route.summaryRoutes() {
    val repo = StatisticsRepository()

    route("/transactions/summary") {
        get("/highlights") {
            val userId = call.userIdOrThrow()

            // Optional account filter
            val accountId: Int? = call.request.queryParameters["accountId"]?.toIntOrNull()

            val typeFilter = call.request.queryParameters["type"]?.lowercase()
            val isIncomeFilter = when (typeFilter) {
                "income" -> true
                "expense" -> false
                else -> null
            }

            val start: LocalDateTime? = call.request.queryParameters["start"]
                ?.let { LocalDate.parse(it) }
                ?.let { date -> LocalDateTime(date, LocalTime(0, 0, 0)) }

            val end: LocalDateTime? = call.request.queryParameters["end"]
                ?.let { LocalDate.parse(it) }
                ?.let { date -> LocalDateTime(date, LocalTime(23, 59, 59)) }

            val summary: StatisticsSummary = repo.getStatisticsSummary(
                userId = userId,
                accountId = accountId,
                isIncome = isIncomeFilter,
                start = start,
                end = end
            )

            call.respond(
                HttpStatusCode.OK,
                ApiResponse.Success(summary.toDto())
            )
        }

        // GET /transactions/summary/distribution?period=2025-W37&type=&start=&end=
        get("/distribution") {
            val userId = call.userIdOrThrow()
            val period = call.request.queryParameters["period"]
                ?: return@get call.respondText("Missing period parameter", status = HttpStatusCode.BadRequest)

            // Optional account filter
            val accountId: Int? = call.request.queryParameters["accountId"]?.toIntOrNull()

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
                accountId = accountId,
                period = period,
                isIncome = isIncomeFilter,
                start = start,
                end = end
            )

            call.respond(HttpStatusCode.OK, ApiResponse.Success(distribution.toDto()))
        }

        get("/available-weeks") {
            val userId = call.userIdOrThrow()
            val accountId: Int? = call.request.queryParameters["accountId"]?.toIntOrNull()

            val result = repo.getAvailableWeeks(userId, accountId)
            call.respond(HttpStatusCode.OK, ApiResponse.Success(result.toDto()))
        }

        get("/available-months") {
            val userId = call.userIdOrThrow()
            val accountId: Int? = call.request.queryParameters["accountId"]?.toIntOrNull()

            val result = repo.getAvailableMonths(userId, accountId)
            call.respond(HttpStatusCode.OK, ApiResponse.Success(result.toDto()))
        }


        get("/available-years") {
            val userId = call.userIdOrThrow()
            val accountId: Int? = call.request.queryParameters["accountId"]?.toIntOrNull()

            val result = repo.getAvailableYears(userId, accountId)
            call.respond(HttpStatusCode.OK, ApiResponse.Success(result.toDto()))
        }

        get("/overview") {
            val userId = call.userIdOrThrow()
            val accountId: Int? = call.request.queryParameters["accountId"]?.toIntOrNull()

            val overview = repo.getOverviewSummary(userId, accountId)

            call.respond(
                HttpStatusCode.OK,
                ApiResponse.Success (overview.toDto())
            )
        }
        get("/overview/range") {
            val userId = call.userIdOrThrow()
            val accountId: Int? = call.request.queryParameters["accountId"]?.toIntOrNull()
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
                val start = LocalDate.parse(startParam)
                val end = LocalDate.parse(endParam)
                val days = repo.getDaySummaries(userId, start, end, accountId)

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
            val accountId: Int? = call.request.queryParameters["accountId"]?.toIntOrNull()
            val comparisons = repo.getCategoryComparisons(userId, accountId)

            call.respond(
                HttpStatusCode.OK,
                ApiResponse.Success(comparisons.map { it.toDto() })
            )
        }
    }
}
