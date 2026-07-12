package feature.summary.domain

import com.fintrack.core.domain.Result
import feature.summary.data.model.AvailableMonthsDto
import feature.summary.data.model.AvailableWeeksDto
import feature.summary.data.model.AvailableYearsDto
import feature.summary.data.model.CategoryComparisonSummaryDto
import feature.summary.data.model.DistributionSummaryDto
import feature.summary.data.model.TransactionCountSummaryDto
import feature.summary.data.model.DaySummaryDto
import feature.summary.data.model.OverviewSummaryDto
import feature.summary.data.model.StatisticsSummaryDto
import feature.summary.data.model.ProfileMetricsDto
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Instant
import java.util.UUID

interface StatisticsService {
    suspend fun getStatisticsSummary(
        userId: UUID,
        accountId: UUID?,
        isIncome: Boolean?,
        start: Instant?,
        end: Instant?,
        period: String? = null
    ): Result<StatisticsSummaryDto>

    suspend fun getDistributionSummary(
        userId: UUID,
        period: String,
        accountId: UUID?,
        isIncome: Boolean?,
        start: Instant?,
        end: Instant?
    ): Result<DistributionSummaryDto>

    suspend fun getAvailableWeeks(userId: UUID, accountId: UUID?): Result<AvailableWeeksDto>

    suspend fun getAvailableMonths(userId: UUID, accountId: UUID?): Result<AvailableMonthsDto>

    suspend fun getAvailableYears(userId: UUID, accountId: UUID?): Result<AvailableYearsDto>

    suspend fun getOverviewSummary(userId: UUID, accountId: UUID?): Result<OverviewSummaryDto>

    suspend fun getDaySummaries(
        userId: UUID,
        start: LocalDate,
        end: LocalDate,
        accountId: UUID?
    ): Result<List<DaySummaryDto>>

    suspend fun getDaySummariesByDateRange(
        userId: UUID,
        accountId: UUID?,
        startParam: String?,
        endParam: String?
    ): Result<List<DaySummaryDto>>

    suspend fun getCategoryComparisons(
        userId: UUID,
        accountId: UUID?,
        period: String? = null
    ): Result<CategoryComparisonSummaryDto>

    suspend fun getTransactionCountSummary(
        userId: UUID,
        accountId: UUID?,
        isIncome: Boolean? = null,
        categoryIds: List<UUID>? = null,
        hasCost: Boolean? = null,
        start: Instant? = null,
        end: Instant? = null
    ): Result<TransactionCountSummaryDto>

    suspend fun getProfileMetrics(userId: UUID): Result<ProfileMetricsDto>

    // Helper methods for route parameter processing
    fun parseTypeFilter(typeFilter: String?): Boolean?
    fun parseDateRange(startDate: String?, endDate: String?): Result<Pair<Instant?, Instant?>>
}
