package feature.summary.domain

import com.fintrack.feature.summary.data.model.TransactionCountSummaryDto
import com.fintrack.feature.summary.domain.CategoryComparison
import com.fintrack.feature.summary.domain.DaySummary
import com.fintrack.feature.summary.domain.DistributionSummary
import com.fintrack.feature.summary.domain.OverviewSummary
import core.AvailableMonths
import core.AvailableWeeks
import core.AvailableYears
import feature.transactions.StatisticsSummary
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

interface StatisticsService {
    suspend fun getStatisticsSummary(
        userId: Int,
        accountId: Int? = null,
        isIncome: Boolean? = null,
        start: LocalDateTime? = null,
        end: LocalDateTime? = null
    ): StatisticsSummary

    suspend fun getDistributionSummary(
        userId: Int,
        period: String,
        accountId: Int? = null,
        isIncome: Boolean? = null,
        start: LocalDateTime? = null,
        end: LocalDateTime? = null
    ): DistributionSummary

    suspend fun getAvailableWeeks(userId: Int, accountId: Int? = null): AvailableWeeks
    suspend fun getAvailableMonths(userId: Int, accountId: Int? = null): AvailableMonths
    suspend fun getAvailableYears(userId: Int, accountId: Int? = null): AvailableYears
    suspend fun getOverviewSummary(userId: Int, accountId: Int? = null): OverviewSummary

    suspend fun getDaySummaries(
        userId: Int,
        start: LocalDate,
        end: LocalDate,
        accountId: Int? = null
    ): List<DaySummary>

    suspend fun getCategoryComparisons(
        userId: Int,
        accountId: Int? = null
    ): List<CategoryComparison>

    suspend fun getTransactionCountSummary(
        userId: Int,
        accountId: Int
    ): TransactionCountSummaryDto?

    fun parseTypeFilter(typeFilter: String?): Boolean?
    fun parseDateRange(startDate: String?, endDate: String?): Pair<LocalDateTime?, LocalDateTime?>
}