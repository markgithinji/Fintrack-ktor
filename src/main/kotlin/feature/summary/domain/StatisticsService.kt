package feature.summary.domain

import com.fintrack.feature.summary.data.model.TransactionCountSummaryDto
import com.fintrack.feature.summary.domain.CategoryComparison
import com.fintrack.feature.summary.domain.DaySummary
import com.fintrack.feature.summary.domain.DistributionSummary
import com.fintrack.feature.summary.domain.OverviewSummary
import core.AvailableMonths
import core.AvailableWeeks
import core.AvailableYears
import feature.transaction.StatisticsSummary
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

import java.util.UUID

interface StatisticsService {
    suspend fun getStatisticsSummary(
        userId: UUID,
        accountId: UUID?,
        isIncome: Boolean?,
        start: LocalDateTime?,
        end: LocalDateTime?
    ): StatisticsSummary

    suspend fun getDistributionSummary(
        userId: UUID,
        period: String,
        accountId: UUID?,
        isIncome: Boolean?,
        start: LocalDateTime?,
        end: LocalDateTime?
    ): DistributionSummary

    suspend fun getAvailableWeeks(userId: UUID, accountId: UUID?): AvailableWeeks
    suspend fun getAvailableMonths(userId: UUID, accountId: UUID?): AvailableMonths
    suspend fun getAvailableYears(userId: UUID, accountId: UUID?): AvailableYears
    suspend fun getOverviewSummary(userId: UUID, accountId: UUID?): OverviewSummary
    suspend fun getDaySummaries(userId: UUID, start: LocalDate, end: LocalDate, accountId: UUID?): List<DaySummary>
    suspend fun getCategoryComparisons(userId: UUID, accountId: UUID?): List<CategoryComparison>
    suspend fun getTransactionCountSummary(userId: UUID, accountId: UUID?): TransactionCountSummaryDto?

    fun parseTypeFilter(typeFilter: String?): Boolean?
    fun parseDateRange(startDate: String?, endDate: String?): Pair<LocalDateTime?, LocalDateTime?>
}