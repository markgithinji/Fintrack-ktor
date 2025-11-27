package com.fintrack.feature.summary

import com.fintrack.core.domain.ApiResponse
import com.fintrack.core.logger
import com.fintrack.core.userIdOrThrow
import com.fintrack.core.withContext
import core.ValidationException
import feature.summary.domain.StatisticsService
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import java.util.UUID

fun Route.summaryRoutes(service: StatisticsService) {
    val log = logger("SummaryRoutes")

    route("/transactions/summary") {
        get("/highlights") {
            val userId = call.userIdOrThrow()
            val accountId: UUID? = call.request.queryParameters["accountId"]?.let { UUID.fromString(it) }
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

            call.respond(HttpStatusCode.OK, ApiResponse.Success(summary))
        }

        get("/distribution") {
            val userId = call.userIdOrThrow()
            val period = call.request.queryParameters["period"]
                ?: throw ValidationException("Missing period parameter")

            val accountId: UUID? = call.request.queryParameters["accountId"]?.let { UUID.fromString(it) }
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

            call.respond(HttpStatusCode.OK, ApiResponse.Success(distribution))
        }

        get("/available-weeks") {
            val userId = call.userIdOrThrow()
            val accountId: UUID? = call.request.queryParameters["accountId"]?.let { UUID.fromString(it) }

            log.withContext(
                "userId" to userId,
                "endpoint" to "GET /transactions/summary/available-weeks",
                "accountId" to accountId
            ).info { "Available weeks request received" }

            val result = service.getAvailableWeeks(userId, accountId)
            call.respond(HttpStatusCode.OK, ApiResponse.Success(result))
        }

        get("/available-months") {
            val userId = call.userIdOrThrow()
            val accountId: UUID? = call.request.queryParameters["accountId"]?.let { UUID.fromString(it) }

            log.withContext(
                "userId" to userId,
                "endpoint" to "GET /transactions/summary/available-months",
                "accountId" to accountId
            ).info { "Available months request received" }

            val result = service.getAvailableMonths(userId, accountId)
            call.respond(HttpStatusCode.OK, ApiResponse.Success(result))
        }

        get("/available-years") {
            val userId = call.userIdOrThrow()
            val accountId: UUID? = call.request.queryParameters["accountId"]?.let { UUID.fromString(it) }

            log.withContext(
                "userId" to userId,
                "endpoint" to "GET /transactions/summary/available-years",
                "accountId" to accountId
            ).info { "Available years request received" }

            val result = service.getAvailableYears(userId, accountId)
            call.respond(HttpStatusCode.OK, ApiResponse.Success(result))
        }

        get("/overview") {
            val userId = call.userIdOrThrow()
            val accountId: UUID? = call.request.queryParameters["accountId"]?.let { UUID.fromString(it) }

            log.withContext(
                "userId" to userId,
                "endpoint" to "GET /transactions/summary/overview",
                "accountId" to accountId
            ).info { "Overview request received" }

            val overview = service.getOverviewSummary(userId, accountId)
            call.respond(HttpStatusCode.OK, ApiResponse.Success(overview))
        }

        get("/overview/range") {
            val userId = call.userIdOrThrow()
            val accountId: UUID? = call.request.queryParameters["accountId"]?.let { UUID.fromString(it) }
            val startParam = call.request.queryParameters["start"]
            val endParam = call.request.queryParameters["end"]

            log.withContext(
                "userId" to userId,
                "endpoint" to "GET /transactions/summary/overview/range",
                "accountId" to accountId,
                "startParam" to startParam,
                "endParam" to endParam
            ).info { "Overview range request received" }

            val days = service.getDaySummariesByDateRange(userId, accountId, startParam, endParam)
            call.respond(HttpStatusCode.OK, ApiResponse.Success(days))
        }

        get("/category-comparison") {
            val userId = call.userIdOrThrow()
            val accountId: UUID? = call.request.queryParameters["accountId"]?.let { UUID.fromString(it) }

            log.withContext(
                "userId" to userId,
                "endpoint" to "GET /transactions/summary/category-comparison",
                "accountId" to accountId
            ).info { "Category comparison request received" }

            val comparisons = service.getCategoryComparisons(userId, accountId)
            call.respond(HttpStatusCode.OK, ApiResponse.Success(comparisons))
        }

        get("/counts") {
            val userId = call.userIdOrThrow()
            val accountId = call.request.queryParameters["accountId"]?.let { UUID.fromString(it) }
            val isIncome = call.request.queryParameters["isIncome"]?.toBooleanStrictOrNull()

            log.withContext(
                "userId" to userId,
                "endpoint" to "GET /transactions/summary/counts",
                "accountId" to accountId,
                "isIncome" to isIncome
            ).info { "Transaction counts request received" }

            val summary = service.getTransactionCountSummary(userId, accountId, isIncome)
            call.respond(HttpStatusCode.OK, ApiResponse.Success(summary))
        }
    }
}