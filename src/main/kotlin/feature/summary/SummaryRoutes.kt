package com.fintrack.feature.summary

import com.fintrack.core.ApiResponse
import com.fintrack.core.userIdOrThrow
import com.fintrack.feature.summary.data.toDto
import feature.summary.domain.StatisticsService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.datetime.LocalDate

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
                ?: throw IllegalArgumentException("Missing period parameter")

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
                throw IllegalArgumentException("start and end query params required (yyyy-MM-dd)")
            }

            val start = LocalDate.parse(startParam)
            val end = LocalDate.parse(endParam)
            val days = service.getDaySummaries(userId, start, end, accountId)
            call.respond(HttpStatusCode.OK, ApiResponse.Success(days.map { it.toDto() }))
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
                ?: throw IllegalArgumentException("Missing or invalid accountId")

            val summary = service.getTransactionCountSummary(userId, accountId)
                ?: throw NoSuchElementException("No transactions found for accountId=$accountId")

            call.respond(HttpStatusCode.OK, ApiResponse.Success(summary))
        }
    }
}