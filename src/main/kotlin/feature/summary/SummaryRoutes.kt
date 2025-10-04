package com.fintrack.feature.summary

import com.fintrack.core.ApiResponse
import com.fintrack.core.logger
import com.fintrack.core.userIdOrThrow
import com.fintrack.core.withContext
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
    val log = logger("SummaryRoutes")

    route("/transactions/summary") {
        get("/highlights") {
            val userId = call.userIdOrThrow()
            val accountId: Int? = call.request.queryParameters["accountId"]?.toIntOrNull()
            val typeFilter = call.request.queryParameters["type"]
            val startDate = call.request.queryParameters["start"]
            val endDate = call.request.queryParameters["end"]

            log.withContext(
                "userId" to userId,
                "endpoint" to "GET /transactions/summary/highlights",
                "accountId" to accountId,
                "typeFilter" to typeFilter,
                "startDate" to startDate,
                "endDate" to endDate
            ).info { "Highlights request received" }

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

            log.withContext(
                "userId" to userId,
                "endpoint" to "GET /transactions/summary/distribution",
                "period" to period,
                "accountId" to accountId,
                "typeFilter" to typeFilter,
                "startDate" to startDate,
                "endDate" to endDate
            ).info { "Distribution request received" }

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

            log.withContext(
                "userId" to userId,
                "endpoint" to "GET /transactions/summary/available-weeks",
                "accountId" to accountId
            ).info { "Available weeks request received" }

            val result = service.getAvailableWeeks(userId, accountId)
            call.respond(HttpStatusCode.OK, ApiResponse.Success(result.toDto()))
        }

        get("/available-months") {
            val userId = call.userIdOrThrow()
            val accountId: Int? = call.request.queryParameters["accountId"]?.toIntOrNull()

            log.withContext(
                "userId" to userId,
                "endpoint" to "GET /transactions/summary/available-months",
                "accountId" to accountId
            ).info { "Available months request received" }

            val result = service.getAvailableMonths(userId, accountId)
            call.respond(HttpStatusCode.OK, ApiResponse.Success(result.toDto()))
        }

        get("/available-years") {
            val userId = call.userIdOrThrow()
            val accountId: Int? = call.request.queryParameters["accountId"]?.toIntOrNull()

            log.withContext(
                "userId" to userId,
                "endpoint" to "GET /transactions/summary/available-years",
                "accountId" to accountId
            ).info { "Available years request received" }

            val result = service.getAvailableYears(userId, accountId)
            call.respond(HttpStatusCode.OK, ApiResponse.Success(result.toDto()))
        }

        get("/overview") {
            val userId = call.userIdOrThrow()
            val accountId: Int? = call.request.queryParameters["accountId"]?.toIntOrNull()

            log.withContext(
                "userId" to userId,
                "endpoint" to "GET /transactions/summary/overview",
                "accountId" to accountId
            ).info { "Overview request received" }

            val overview = service.getOverviewSummary(userId, accountId)
            call.respond(HttpStatusCode.OK, ApiResponse.Success(overview.toDto()))
        }

        get("/overview/range") {
            val userId = call.userIdOrThrow()
            val accountId: Int? = call.request.queryParameters["accountId"]?.toIntOrNull()
            val startParam = call.request.queryParameters["start"]
            val endParam = call.request.queryParameters["end"]

            log.withContext(
                "userId" to userId,
                "endpoint" to "GET /transactions/summary/overview/range",
                "accountId" to accountId,
                "startParam" to startParam,
                "endParam" to endParam
            ).info { "Overview range request received" }

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

            log.withContext(
                "userId" to userId,
                "endpoint" to "GET /transactions/summary/category-comparison",
                "accountId" to accountId
            ).info { "Category comparison request received" }

            val comparisons = service.getCategoryComparisons(userId, accountId)
            call.respond(HttpStatusCode.OK, ApiResponse.Success(comparisons.map { it.toDto() }))
        }

        get("/counts") {
            val userId = call.userIdOrThrow()
            val accountId = call.request.queryParameters["accountId"]?.toIntOrNull()
                ?: throw ValidationException("Missing or invalid accountId")

            log.withContext(
                "userId" to userId,
                "endpoint" to "GET /transactions/summary/counts",
                "accountId" to accountId
            ).info { "Transaction counts request received" }

            val summary = service.getTransactionCountSummary(userId, accountId)
                ?: throw NoSuchElementException("No transactions found for accountId=$accountId")

            call.respond(HttpStatusCode.OK, ApiResponse.Success(summary))
        }
    }
}