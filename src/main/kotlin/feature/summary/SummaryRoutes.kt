package com.fintrack.feature.summary

import com.fintrack.core.ApiResponse
import com.fintrack.core.userIdOrThrow
import com.fintrack.feature.summary.data.repository.StatisticsRepository
import com.fintrack.feature.summary.data.toDto
import com.fintrack.feature.summary.domain.DistributionSummary
import feature.summary.domain.StatisticsService
import feature.transactions.StatisticsSummary
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
fun Route.summaryRoutes(service: StatisticsService) {

    route("/transactions/summary") {
        get("/highlights") {
            val userId = call.userIdOrThrow()
            val accountId: Int? = call.request.queryParameters["accountId"]?.toIntOrNull()
            val typeFilter = call.request.queryParameters["type"]
            val startDate = call.request.queryParameters["start"]
            val endDate = call.request.queryParameters["end"]

            val (start, end) = service.parseDateRange(startDate, endDate)
            val isIncomeFilter = service.parseTypeFilter(typeFilter)

            val summary = service.getStatisticsSummary(
                userId = userId,
                accountId = accountId,
                isIncome = isIncomeFilter,
                start = start,
                end = end
            )

            call.respond(HttpStatusCode.OK, ApiResponse.Success(summary.toDto()))
        }

        get("/distribution") {
            val userId = call.userIdOrThrow()
            val period = call.request.queryParameters["period"]
                ?: return@get call.respondBadRequest("Missing period parameter")

            val accountId: Int? = call.request.queryParameters["accountId"]?.toIntOrNull()
            val typeFilter = call.request.queryParameters["type"]
            val startDate = call.request.queryParameters["start"]
            val endDate = call.request.queryParameters["end"]

            val (start, end) = service.parseDateRange(startDate, endDate)
            val isIncomeFilter = service.parseTypeFilter(typeFilter)

            val distribution = service.getDistributionSummary(
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
            val result = service.getAvailableWeeks(userId, accountId)
            call.respond(HttpStatusCode.OK, ApiResponse.Success(result.toDto()))
        }

        get("/available-months") {
            val userId = call.userIdOrThrow()
            val accountId: Int? = call.request.queryParameters["accountId"]?.toIntOrNull()
            val result = service.getAvailableMonths(userId, accountId)
            call.respond(HttpStatusCode.OK, ApiResponse.Success(result.toDto()))
        }

        get("/available-years") {
            val userId = call.userIdOrThrow()
            val accountId: Int? = call.request.queryParameters["accountId"]?.toIntOrNull()
            val result = service.getAvailableYears(userId, accountId)
            call.respond(HttpStatusCode.OK, ApiResponse.Success(result.toDto()))
        }

        get("/overview") {
            val userId = call.userIdOrThrow()
            val accountId: Int? = call.request.queryParameters["accountId"]?.toIntOrNull()
            val overview = service.getOverviewSummary(userId, accountId)
            call.respond(HttpStatusCode.OK, ApiResponse.Success(overview.toDto()))
        }

        get("/overview/range") {
            val userId = call.userIdOrThrow()
            val accountId: Int? = call.request.queryParameters["accountId"]?.toIntOrNull()
            val startParam = call.request.queryParameters["start"]
            val endParam = call.request.queryParameters["end"]

            if (startParam == null || endParam == null) {
                return@get call.respondBadRequest("start and end query params required (yyyy-MM-dd)")
            }

            try {
                val start = LocalDate.parse(startParam)
                val end = LocalDate.parse(endParam)
                val days = service.getDaySummaries(userId, start, end, accountId)
                call.respond(HttpStatusCode.OK, ApiResponse.Success(days.map { it.toDto() }))
            } catch (e: Exception) {
                call.respondBadRequest("Invalid date format, expected yyyy-MM-dd")
            }
        }

        get("/category-comparison") {
            val userId = call.userIdOrThrow()
            val accountId: Int? = call.request.queryParameters["accountId"]?.toIntOrNull()
            val comparisons = service.getCategoryComparisons(userId, accountId)
            call.respond(HttpStatusCode.OK, ApiResponse.Success(comparisons.map { it.toDto() }))
        }

        get("/counts") {
            val userId = call.userIdOrThrow()
            val accountId = call.request.queryParameters["accountId"]?.toIntOrNull()
                ?: return@get call.respondBadRequest("Missing or invalid accountId")

            val summary = service.getTransactionCountSummary(userId, accountId)
                ?: return@get call.respondNotFound("No transactions found for accountId=$accountId")

            call.respond(HttpStatusCode.OK, ApiResponse.Success(summary))
        }
    }
}

// Extension functions for cleaner error handling
private suspend fun ApplicationCall.respondBadRequest(message: String) {
    respond(HttpStatusCode.BadRequest, ApiResponse.Error(message))
}

private suspend fun ApplicationCall.respondNotFound(message: String) {
    respond(HttpStatusCode.NotFound, ApiResponse.Error(message))
}