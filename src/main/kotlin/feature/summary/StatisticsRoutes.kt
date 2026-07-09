package com.fintrack.feature.summary

import com.fintrack.core.domain.*
import com.fintrack.core.logger
import com.fintrack.core.toUUIDOrNull
import com.fintrack.core.userIdOrThrow
import com.fintrack.core.withContext
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
            val accountId: UUID? = call.request.queryParameters["accountId"]?.toUUIDOrNull()
            val typeFilter = call.request.queryParameters["type"]
            val startDate = call.request.queryParameters["start"]
            val endDate = call.request.queryParameters["end"]
            val period = call.request.queryParameters["period"]

            log.withContext(
                "userId" to userId,
                "endpoint" to "GET /transactions/summary/highlights",
                "accountId" to accountId,
                "typeFilter" to typeFilter,
                "startDate" to startDate,
                "endDate" to endDate,
                "period" to period,
            ).info { "Highlights request received" }

            val rangeResult = service.parseDateRange(startDate, endDate)
            if (rangeResult is Result.Failure) {
                call.respond(rangeResult.error.toHttpStatusCode(), ErrorResponse(rangeResult.error.message, rangeResult.error.errorCode))
                return@get
            }
            
            val (start, end) = (rangeResult as Result.Success).value
            val isIncomeFilter = service.parseTypeFilter(typeFilter)

            when (val result = service.getStatisticsSummary(
                userId = userId,
                accountId = accountId,
                isIncome = isIncomeFilter,
                start = start,
                end = end,
                period = period,
            )) {
                is Result.Success -> call.respond(HttpStatusCode.OK, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        get("/distribution") {
            val userId = call.userIdOrThrow()
            val period = call.request.queryParameters["period"]
            
            if (period == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing period parameter", "MISSING_PERIOD"))
                return@get
            }

            val accountId: UUID? = call.request.queryParameters["accountId"]?.toUUIDOrNull()
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
                "endDate" to endDate,
            ).info { "Distribution request received" }

            val rangeResult = service.parseDateRange(startDate, endDate)
            if (rangeResult is Result.Failure) {
                call.respond(rangeResult.error.toHttpStatusCode(), ErrorResponse(rangeResult.error.message, rangeResult.error.errorCode))
                return@get
            }
            
            val (start, end) = (rangeResult as Result.Success).value
            val isIncomeFilter = service.parseTypeFilter(typeFilter)

            when (val result = service.getDistributionSummary(
                userId = userId,
                accountId = accountId,
                period = period,
                isIncome = isIncomeFilter,
                start = start,
                end = end,
            )) {
                is Result.Success -> call.respond(HttpStatusCode.OK, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        get("/available-weeks") {
            val userId = call.userIdOrThrow()
            val accountId: UUID? = call.request.queryParameters["accountId"]?.toUUIDOrNull()

            log.withContext(
                "userId" to userId,
                "endpoint" to "GET /transactions/summary/available-weeks",
                "accountId" to accountId,
            ).info { "Available weeks request received" }

            when (val result = service.getAvailableWeeks(userId, accountId)) {
                is Result.Success -> call.respond(HttpStatusCode.OK, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        get("/available-months") {
            val userId = call.userIdOrThrow()
            val accountId: UUID? = call.request.queryParameters["accountId"]?.toUUIDOrNull()

            log.withContext(
                "userId" to userId,
                "endpoint" to "GET /transactions/summary/available-months",
                "accountId" to accountId,
            ).info { "Available months request received" }

            when (val result = service.getAvailableMonths(userId, accountId)) {
                is Result.Success -> call.respond(HttpStatusCode.OK, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        get("/available-years") {
            val userId = call.userIdOrThrow()
            val accountId: UUID? = call.request.queryParameters["accountId"]?.toUUIDOrNull()

            log.withContext(
                "userId" to userId,
                "endpoint" to "GET /transactions/summary/available-years",
                "accountId" to accountId,
            ).info { "Available years request received" }

            when (val result = service.getAvailableYears(userId, accountId)) {
                is Result.Success -> call.respond(HttpStatusCode.OK, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        get("/overview") {
            val userId = call.userIdOrThrow()
            val accountId: UUID? = call.request.queryParameters["accountId"]?.toUUIDOrNull()

            log.withContext(
                "userId" to userId,
                "endpoint" to "GET /transactions/summary/overview",
                "accountId" to accountId,
            ).info { "Overview request received" }

            when (val result = service.getOverviewSummary(userId, accountId)) {
                is Result.Success -> call.respond(HttpStatusCode.OK, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        get("/overview/range") {
            val userId = call.userIdOrThrow()
            val accountId: UUID? = call.request.queryParameters["accountId"]?.toUUIDOrNull()
            val startParam = call.request.queryParameters["start"]
            val endParam = call.request.queryParameters["end"]

            log.withContext(
                "userId" to userId,
                "endpoint" to "GET /transactions/summary/overview/range",
                "accountId" to accountId,
                "startParam" to startParam,
                "endParam" to endParam,
            ).info { "Overview range request received" }

            when (val result = service.getDaySummariesByDateRange(userId, accountId, startParam, endParam)) {
                is Result.Success -> call.respond(HttpStatusCode.OK, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        get("/category-comparison") {
            val userId = call.userIdOrThrow()
            val accountId: UUID? = call.request.queryParameters["accountId"]?.toUUIDOrNull()
            val period = call.request.queryParameters["period"]

            log.withContext(
                "userId" to userId,
                "endpoint" to "GET /transactions/summary/category-comparison",
                "accountId" to accountId,
                "period" to period,
            ).info { "Category comparison request received" }

            when (val result = service.getCategoryComparisons(userId, accountId, period)) {
                is Result.Success -> call.respond(HttpStatusCode.OK, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        get("/counts") {
            val userId = call.userIdOrThrow()
            val accountId = call.request.queryParameters["accountId"]?.toUUIDOrNull()
            val isIncome = call.request.queryParameters["isIncome"]?.toBooleanStrictOrNull()
            val category = call.request.queryParameters["category"]
            val hasCost = call.request.queryParameters["hasCost"]?.toBooleanStrictOrNull()
            val startDate = call.request.queryParameters["start"]
            val endDate = call.request.queryParameters["end"]

            log.withContext(
                "userId" to userId,
                "endpoint" to "GET /transactions/summary/counts",
                "accountId" to accountId,
                "isIncome" to isIncome,
                "category" to category,
                "hasCost" to hasCost,
                "startDate" to startDate,
                "endDate" to endDate,
            ).info { "Transaction counts request received" }

            val rangeResult = service.parseDateRange(startDate, endDate)
            if (rangeResult is Result.Failure) {
                call.respond(rangeResult.error.toHttpStatusCode(), ErrorResponse(rangeResult.error.message, rangeResult.error.errorCode))
                return@get
            }
            
            val (start, end) = (rangeResult as Result.Success).value
            when (val result = service.getTransactionCountSummary(
                userId = userId,
                accountId = accountId,
                isIncome = isIncome,
                category = category,
                hasCost = hasCost,
                start = start,
                end = end,
            )) {
                is Result.Success -> call.respond(HttpStatusCode.OK, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        get("/profile-metrics") {
            val userId = call.userIdOrThrow()
            log.withContext(
                "userId" to userId,
                "endpoint" to "GET /transactions/summary/profile-metrics",
            ).info { "Profile metrics request received" }

            when (val result = service.getProfileMetrics(userId)) {
                is Result.Success -> call.respond(HttpStatusCode.OK, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }
    }
}
