package com.fintrack.feature.summary

import com.fintrack.core.ApiResponse
import com.fintrack.core.userIdOrThrow
import com.fintrack.feature.summary.data.toDto
import core.ValidationException
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
                ?: throw ValidationException("Missing period parameter")

            // Validate period format
            if (!period.matches(Regex("^(\\d{4}-W\\d{2}|\\d{4}-\\d{2}|\\d{4})$"))) {
                throw ValidationException("Period must be in format: YYYY-Www, YYYY-MM, or YYYY")
            }

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
                throw ValidationException("start and end query params required (yyyy-MM-dd)")
            }

            val (start, end) = service.parseDateRange(startParam, endParam)

            // Convert LocalDateTime? to LocalDate with null safety
            val startDate = start?.date ?: throw ValidationException("Invalid start date")
            val endDate = end?.date ?: throw ValidationException("Invalid end date")

            val days = service.getDaySummaries(userId, startDate, endDate, accountId)
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
                ?: throw ValidationException("Missing or invalid accountId")

            val summary = service.getTransactionCountSummary(userId, accountId)
                ?: throw NoSuchElementException("No transactions found for accountId=$accountId")

            call.respond(HttpStatusCode.OK, ApiResponse.Success(summary))
        }
    }
}