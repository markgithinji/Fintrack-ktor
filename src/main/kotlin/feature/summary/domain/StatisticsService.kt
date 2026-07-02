package feature.summary.domain

import com.fintrack.feature.summary.data.model.AvailableMonthsDto
import com.fintrack.feature.summary.data.model.AvailableWeeksDto
import com.fintrack.feature.summary.data.model.AvailableYearsDto
import com.fintrack.feature.summary.data.model.CategoryComparisonDto
import com.fintrack.feature.summary.data.model.DistributionSummaryDto
import com.fintrack.feature.summary.data.model.TransactionCountSummaryDto
import core.DaySummaryDto
import core.OverviewSummaryDto
import core.StatisticsSummaryDto
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
    ): StatisticsSummaryDto

    suspend fun getDistributionSummary(
        userId: UUID,
        period: String,
        accountId: UUID?,
        isIncome: Boolean?,
        start: Instant?,
        end: Instant?
    ): DistributionSummaryDto

    suspend fun getAvailableWeeks(userId: UUID, accountId: UUID?): AvailableWeeksDto

    suspend fun getAvailableMonths(userId: UUID, accountId: UUID?): AvailableMonthsDto

    suspend fun getAvailableYears(userId: UUID, accountId: UUID?): AvailableYearsDto

    suspend fun getOverviewSummary(userId: UUID, accountId: UUID?): OverviewSummaryDto

    suspend fun getDaySummaries(
        userId: UUID,
        start: LocalDate,
        end: LocalDate,
        accountId: UUID?
    ): List<DaySummaryDto>

    suspend fun getDaySummariesByDateRange(
        userId: UUID,
        accountId: UUID?,
        startParam: String?,
        endParam: String?
    ): List<DaySummaryDto>

    suspend fun getCategoryComparisons(
        userId: UUID,
        accountId: UUID?
    ): List<CategoryComparisonDto>

    suspend fun getTransactionCountSummary(
        userId: UUID,
        accountId: UUID?,
        isIncome: Boolean? = null,
        start: Instant? = null,
        end: Instant? = null
    ): TransactionCountSummaryDto

    // Helper methods for route parameter processing
    fun parseTypeFilter(typeFilter: String?): Boolean?
    fun parseDateRange(startDate: String?, endDate: String?): Pair<Instant?, Instant?>
}
