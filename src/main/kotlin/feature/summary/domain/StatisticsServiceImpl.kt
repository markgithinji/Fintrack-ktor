package feature.summary.domain

import com.fintrack.feature.summary.data.model.TransactionCountSummaryDto
import com.fintrack.feature.summary.domain.CategoryComparison
import com.fintrack.feature.summary.domain.CategorySummary
import com.fintrack.feature.summary.domain.DaySummary
import com.fintrack.feature.summary.domain.DistributionSummary
import com.fintrack.feature.summary.domain.OverviewSummary
import core.AvailableMonths
import core.AvailableWeeks
import core.AvailableYears
import core.ValidationException
import feature.transaction.Highlight
import feature.transaction.Highlights
import feature.transaction.StatisticsSummary
import feature.transaction.domain.model.Transaction
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.temporal.WeekFields

class StatisticsServiceImpl(
    private val statisticsRepository: StatisticsRepository
) : StatisticsService {
    override suspend fun getStatisticsSummary(
        userId: Int,
        accountId: Int?,
        isIncome: Boolean?,
        start: LocalDateTime?,
        end: LocalDateTime?
    ): StatisticsSummary {
        val filtered = statisticsRepository.getTransactions(userId, accountId, isIncome, start, end)
        val incomeTxns = filtered.filter { it.isIncome }
        val expenseTxns = filtered.filter { !it.isIncome }

        fun highestMonth(txns: List<Transaction>) =
            txns.groupBy { "${it.dateTime.year}-${it.dateTime.monthNumber.toString().padStart(2, '0')}" }
                .mapValues { it.value.sumOf { t -> t.amount } }
                .maxByOrNull { it.value }
                ?.let { Highlight(label = it.key, value = it.key, amount = it.value) }

        fun highestCategory(txns: List<Transaction>) =
            txns.groupBy { it.category.trim().lowercase() }
                .mapValues { it.value.sumOf { t -> t.amount } }
                .maxByOrNull { it.value }
                ?.let { entry ->
                    val displayName =
                        txns.firstOrNull { it.category.trim().lowercase() == entry.key }?.category ?: entry.key
                    Highlight(label = displayName, value = displayName, amount = entry.value)
                }

        fun highestDay(txns: List<Transaction>) =
            txns.groupBy { it.dateTime.date }
                .mapValues { it.value.sumOf { t -> t.amount } }
                .maxByOrNull { it.value }
                ?.let { Highlight(label = it.key.toString(), value = it.key.toString(), amount = it.value) }

        fun averagePerDay(txns: List<Transaction>): Double {
            val days = txns.groupBy { it.dateTime.date }.size.coerceAtLeast(1)
            val total = txns.sumOf { it.amount }
            return total / days
        }

        val incomeHighlights = Highlights(
            highestMonth = highestMonth(incomeTxns),
            highestCategory = highestCategory(incomeTxns),
            highestDay = highestDay(incomeTxns),
            averagePerDay = averagePerDay(incomeTxns)
        )
        val expenseHighlights = Highlights(
            highestMonth = highestMonth(expenseTxns),
            highestCategory = highestCategory(expenseTxns),
            highestDay = highestDay(expenseTxns),
            averagePerDay = averagePerDay(expenseTxns)
        )

        return StatisticsSummary(
            incomeHighlights = incomeHighlights,
            expenseHighlights = expenseHighlights
        )
    }

    override suspend fun getDistributionSummary(
        userId: Int,
        period: String,
        accountId: Int?,
        isIncome: Boolean?,
        start: LocalDateTime?,
        end: LocalDateTime?
    ): DistributionSummary {
        val filtered = statisticsRepository.getTransactions(userId, accountId, isIncome, start, end)
        val incomeTxns = filtered.filter { it.isIncome }
        val expenseTxns = filtered.filter { !it.isIncome }

        val weekMode = period.contains("W")
        val yearMode = !weekMode && period.length == 4
        val monthMode = !weekMode && !yearMode

        fun categorySummary(txns: List<Transaction>): List<CategorySummary> {
            val grouped = when {
                weekMode -> txns.groupBy {
                    val javaDateTime = it.dateTime.toJavaLocalDateTime()
                    val week = javaDateTime.get(WeekFields.ISO.weekOfWeekBasedYear())
                    "${javaDateTime.year}-W${week.toString().padStart(2, '0')}"
                }
                monthMode -> txns.groupBy {
                    "${it.dateTime.year}-${it.dateTime.monthNumber.toString().padStart(2, '0')}"
                }
                yearMode -> txns.groupBy { it.dateTime.year.toString() }
                else -> emptyMap()
            }
            val txnsInPeriod = grouped[period].orEmpty()
            val deduped = txnsInPeriod.groupBy { it.category.trim().lowercase() }.map { (_, list) ->
                val sum = list.sumOf { it.amount }
                val displayName = list.first().category
                CategorySummary(category = displayName, total = sum, percentage = 0.0)
            }
            val totalAmount = deduped.sumOf { it.total }
            return deduped.map {
                it.copy(percentage = if (totalAmount > 0) (it.total / totalAmount) * 100 else 0.0)
            }
        }

        val incomeCategoriesFinal = if (isIncome != false) categorySummary(incomeTxns) else emptyList()
        val expenseCategoriesFinal = if (isIncome != true) categorySummary(expenseTxns) else emptyList()

        return DistributionSummary(
            period = period,
            incomeCategories = incomeCategoriesFinal,
            expenseCategories = expenseCategoriesFinal
        )
    }

    override suspend fun getAvailableWeeks(userId: Int, accountId: Int?): AvailableWeeks {
        val weeks = statisticsRepository.getAvailablePeriods(userId, accountId, "weeks")
        return AvailableWeeks(weeks)
    }

    override suspend fun getAvailableMonths(userId: Int, accountId: Int?): AvailableMonths {
        val months = statisticsRepository.getAvailablePeriods(userId, accountId, "months")
        return AvailableMonths(months)
    }

    override suspend fun getAvailableYears(userId: Int, accountId: Int?): AvailableYears {
        val years = statisticsRepository.getAvailablePeriods(userId, accountId, "years")
        return AvailableYears(years)
    }

    override suspend fun getOverviewSummary(userId: Int, accountId: Int?): OverviewSummary {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val weeklyStart = today.minus(DatePeriod(days = 6))
        val monthlyStart = today.minus(DatePeriod(days = 29))
        val weekly = getDaySummaries(userId, weeklyStart, today, accountId)
        val monthly = getDaySummaries(userId, monthlyStart, today, accountId)

        return OverviewSummary(weeklyOverview = weekly, monthlyOverview = monthly)
    }

    override suspend fun getDaySummaries(
        userId: Int,
        start: LocalDate,
        end: LocalDate,
        accountId: Int?
    ): List<DaySummary> {
        val transactions = statisticsRepository.getTransactionsByDateRange(userId, start, end, accountId)

        val dates = generateSequence(start) { current ->
            if (current < end) current.plus(DatePeriod(days = 1)) else null
        }.toList()

        return dates.map { date ->
            val dayTxs = transactions.filter { it.dateTime.date == date }
            val income = dayTxs.filter { it.isIncome }.sumOf { it.amount }
            val expense = dayTxs.filter { !it.isIncome }.sumOf { it.amount }
            DaySummary(date = date, income = income, expense = expense)
        }
    }

    override suspend fun getCategoryComparisons(
        userId: Int,
        accountId: Int?
    ): List<CategoryComparison> {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        val thisWeekStart = now.minus(DatePeriod(days = 6))
        val lastWeekStart = thisWeekStart.minus(DatePeriod(days = 7))
        val lastWeekEnd = thisWeekStart.minus(DatePeriod(days = 1))

        val thisMonthStart = now.minus(DatePeriod(days = 29))
        val lastMonthStart = thisMonthStart.minus(DatePeriod(days = 30))
        val lastMonthEnd = thisMonthStart.minus(DatePeriod(days = 1))

        val thisWeekTotals = statisticsRepository.getCategoryTotals(userId, thisWeekStart, now, accountId)
        val lastWeekTotals = statisticsRepository.getCategoryTotals(userId, lastWeekStart, lastWeekEnd, accountId)

        val thisMonthTotals = statisticsRepository.getCategoryTotals(userId, thisMonthStart, now, accountId)
        val lastMonthTotals = statisticsRepository.getCategoryTotals(userId, lastMonthStart, lastMonthEnd, accountId)

        val topWeekCategory = thisWeekTotals.maxByOrNull { it.value }?.key
        val weeklyComparison = topWeekCategory?.let { category ->
            val current = thisWeekTotals[category] ?: 0.0
            val previous = lastWeekTotals[category] ?: 0.0
            CategoryComparison(
                period = "weekly",
                category = category,
                currentTotal = current,
                previousTotal = previous,
                changePercentage = if (previous != 0.0) (current - previous) / previous * 100 else 100.0
            )
        }

        val topMonthCategory = thisMonthTotals.maxByOrNull { it.value }?.key
        val monthlyComparison = topMonthCategory?.let { category ->
            val current = thisMonthTotals[category] ?: 0.0
            val previous = lastMonthTotals[category] ?: 0.0
            CategoryComparison(
                period = "monthly",
                category = category,
                currentTotal = current,
                previousTotal = previous,
                changePercentage = if (previous != 0.0) (current - previous) / previous * 100 else 100.0
            )
        }

        return listOfNotNull(weeklyComparison, monthlyComparison)
    }

    override suspend fun getTransactionCountSummary(
        userId: Int,
        accountId: Int
    ): TransactionCountSummaryDto? {
        val counts = statisticsRepository.getTransactionCounts(userId, accountId)

        return if (counts.totalCount == 0) {
            null
        } else {
            TransactionCountSummaryDto(
                totalIncomeTransactions = counts.incomeCount,
                totalExpenseTransactions = counts.expenseCount,
                totalTransactions = counts.totalCount
            )
        }
    }

    // ---- Helper methods for route parameter processing ----
    override fun parseTypeFilter(typeFilter: String?): Boolean? = when (typeFilter?.lowercase()) {
        "income" -> true
        "expense" -> false
        "" -> null
        null -> null
        else -> throw ValidationException("Type filter must be 'income' or 'expense'")
    }

    override fun parseDateRange(startDate: String?, endDate: String?): Pair<LocalDateTime?, LocalDateTime?> {
        val start = startDate?.let {
            try {
                LocalDate.parse(it).atTime(LocalTime(0, 0, 0))
            } catch (e: Exception) {
                throw ValidationException("Invalid start date format. Use yyyy-MM-dd")
            }
        }
        val end = endDate?.let {
            try {
                LocalDate.parse(it).atTime(LocalTime(23, 59, 59))
            } catch (e: Exception) {
                throw ValidationException("Invalid end date format. Use yyyy-MM-dd")
            }
        }

        // Validate date range logic
        if (start != null && end != null && start > end) {
            throw ValidationException("Start date cannot be after end date")
        }

        return start to end
    }
}