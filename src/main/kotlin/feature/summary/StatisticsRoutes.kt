package com.fintrack.feature.summary

import com.fintrack.core.domain.*
import com.fintrack.core.toUUIDOrNull
import com.fintrack.core.userIdOrThrow
import feature.summary.domain.StatisticsService
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import java.util.UUID

fun Route.summaryRoutes(service: StatisticsService) {
    route("/transactions/summary") {
        get("/highlights") {
            val userId = call.userIdOrThrow()
            val accountId: UUID? = call.request.queryParameters["accountId"]?.toUUIDOrNull()
            val typeFilter = call.request.queryParameters["type"]
            val startDate = call.request.queryParameters["start"]
            val endDate = call.request.queryParameters["end"]
            val period = call.request.queryParameters["period"]

            val rangeResult = service.parseDateRange(startDate, endDate)
            if (rangeResult is Result.Failure) {
                call.respond(rangeResult.error.toHttpStatusCode(), rangeResult.error.toApiResponse())
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
                    result.error.toApiResponse()
                )
            }
        }

        get("/distribution") {
            val userId = call.userIdOrThrow()
            val period = call.request.queryParameters["period"]
            
            if (period == null) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Missing period parameter", "MISSING_PERIOD"))
                return@get
            }

            val accountId: UUID? = call.request.queryParameters["accountId"]?.toUUIDOrNull()
            val typeFilter = call.request.queryParameters["type"]
            val startDate = call.request.queryParameters["start"]
            val endDate = call.request.queryParameters["end"]

            val rangeResult = service.parseDateRange(startDate, endDate)
            if (rangeResult is Result.Failure) {
                call.respond(rangeResult.error.toHttpStatusCode(), rangeResult.error.toApiResponse())
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
                    result.error.toApiResponse()
                )
            }
        }

        get("/available-weeks") {
            val userId = call.userIdOrThrow()
            val accountId: UUID? = call.request.queryParameters["accountId"]?.toUUIDOrNull()

            when (val result = service.getAvailableWeeks(userId, accountId)) {
                is Result.Success -> call.respond(HttpStatusCode.OK, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    result.error.toApiResponse()
                )
            }
        }

        get("/available-months") {
            val userId = call.userIdOrThrow()
            val accountId: UUID? = call.request.queryParameters["accountId"]?.toUUIDOrNull()

            when (val result = service.getAvailableMonths(userId, accountId)) {
                is Result.Success -> call.respond(HttpStatusCode.OK, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    result.error.toApiResponse()
                )
            }
        }

        get("/available-years") {
            val userId = call.userIdOrThrow()
            val accountId: UUID? = call.request.queryParameters["accountId"]?.toUUIDOrNull()

            when (val result = service.getAvailableYears(userId, accountId)) {
                is Result.Success -> call.respond(HttpStatusCode.OK, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    result.error.toApiResponse()
                )
            }
        }

        get("/overview") {
            val userId = call.userIdOrThrow()
            val accountId: UUID? = call.request.queryParameters["accountId"]?.toUUIDOrNull()

            when (val result = service.getOverviewSummary(userId, accountId)) {
                is Result.Success -> call.respond(HttpStatusCode.OK, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    result.error.toApiResponse()
                )
            }
        }

        get("/overview/range") {
            val userId = call.userIdOrThrow()
            val accountId: UUID? = call.request.queryParameters["accountId"]?.toUUIDOrNull()
            val startParam = call.request.queryParameters["start"]
            val endParam = call.request.queryParameters["end"]

            when (val result = service.getDaySummariesByDateRange(userId, accountId, startParam, endParam)) {
                is Result.Success -> call.respond(HttpStatusCode.OK, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    result.error.toApiResponse()
                )
            }
        }

        get("/category-comparison") {
            val userId = call.userIdOrThrow()
            val accountId: UUID? = call.request.queryParameters["accountId"]?.toUUIDOrNull()
            val period = call.request.queryParameters["period"]

            when (val result = service.getCategoryComparisons(userId, accountId, period)) {
                is Result.Success -> call.respond(HttpStatusCode.OK, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    result.error.toApiResponse()
                )
            }
        }

        get("/counts") {
            val userId = call.userIdOrThrow()
            val accountId = call.request.queryParameters["accountId"]?.toUUIDOrNull()
            val isIncome = call.request.queryParameters["isIncome"]?.toBooleanStrictOrNull()
            val categoryIds = call.request.queryParameters["categoryId"]?.split(",")?.mapNotNull { it.toUUIDOrNull() }
            val hasCost = call.request.queryParameters["hasCost"]?.toBooleanStrictOrNull()
            val startDate = call.request.queryParameters["start"]
            val endDate = call.request.queryParameters["end"]

            val rangeResult = service.parseDateRange(startDate, endDate)
            if (rangeResult is Result.Failure) {
                call.respond(rangeResult.error.toHttpStatusCode(), rangeResult.error.toApiResponse())
                return@get
            }
            
            val (start, end) = (rangeResult as Result.Success).value
            when (val result = service.getTransactionCountSummary(
                userId = userId,
                accountId = accountId,
                isIncome = isIncome,
                categoryIds = categoryIds,
                hasCost = hasCost,
                start = start,
                end = end,
            )) {
                is Result.Success -> call.respond(HttpStatusCode.OK, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    result.error.toApiResponse()
                )
            }
        }

        get("/profile-metrics") {
            val userId = call.userIdOrThrow()
            when (val result = service.getProfileMetrics(userId)) {
                is Result.Success -> call.respond(HttpStatusCode.OK, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    result.error.toApiResponse()
                )
            }
        }
    }
}
