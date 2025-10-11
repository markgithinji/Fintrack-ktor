package feature.summary.domain

import com.fintrack.core.logger
import com.fintrack.core.withContext
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
import java.util.UUID

class StatisticsServiceImpl(
    private val statisticsRepository: StatisticsRepository
) : StatisticsService {

    private val log = logger<StatisticsServiceImpl>()

    override suspend fun getStatisticsSummary(
        userId: UUID,
        accountId: UUID?,
        isIncome: Boolean?,
        start: LocalDateTime?,
        end: LocalDateTime?
    ): StatisticsSummary {
        log.withContext(
            "userId" to userId,
            "accountId" to accountId,
            "isIncome" to isIncome,
            "start" to start?.toString(),
            "end" to end?.toString()
        ).debug { "Calculating statistics summary" }

        val filtered = statisticsRepository.getTransactions(userId, accountId, isIncome, start, end)

        log.withContext(
            "userId" to userId,
            "transactionCount" to filtered.size
        ).debug { "Retrieved transactions for statistics" }

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

        val summary = StatisticsSummary(
            incomeHighlights = incomeHighlights,
            expenseHighlights = expenseHighlights
        )

        log.withContext(
            "userId" to userId,
            "incomeTransactionCount" to incomeTxns.size,
            "expenseTransactionCount" to expenseTxns.size,
            "highestIncomeMonth" to incomeHighlights.highestMonth?.amount,
            "highestExpenseMonth" to expenseHighlights.highestMonth?.amount
        ).debug { "Statistics summary calculated successfully" }

        return summary
    }

    override suspend fun getDistributionSummary(
        userId: UUID,
        period: String,
        accountId: UUID?,
        isIncome: Boolean?,
        start: LocalDateTime?,
        end: LocalDateTime?
    ): DistributionSummary {
        log.withContext(
            "userId" to userId,
            "period" to period,
            "accountId" to accountId,
            "isIncome" to isIncome,
            "start" to start?.toString(),
            "end" to end?.toString()
        ).debug { "Calculating distribution summary" }

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

        val distribution = DistributionSummary(
            period = period,
            incomeCategories = incomeCategoriesFinal,
            expenseCategories = expenseCategoriesFinal
        )

        log.withContext(
            "userId" to userId,
            "period" to period,
            "incomeCategoryCount" to incomeCategoriesFinal.size,
            "expenseCategoryCount" to expenseCategoriesFinal.size
        ).debug { "Distribution summary calculated successfully" }

        return distribution
    }

    override suspend fun getAvailableWeeks(userId: UUID, accountId: UUID?): AvailableWeeks {
        log.withContext("userId" to userId, "accountId" to accountId)
            .debug { "Fetching available weeks" }

        val weeks = statisticsRepository.getAvailablePeriods(userId, accountId, "weeks")

        log.withContext("userId" to userId, "weekCount" to weeks.size)
            .debug { "Available weeks retrieved" }
        return AvailableWeeks(weeks)
    }

    override suspend fun getAvailableMonths(userId: UUID, accountId: UUID?): AvailableMonths {
        log.withContext("userId" to userId, "accountId" to accountId)
            .debug { "Fetching available months" }

        val months = statisticsRepository.getAvailablePeriods(userId, accountId, "months")

        log.withContext("userId" to userId, "monthCount" to months.size)
            .debug { "Available months retrieved" }
        return AvailableMonths(months)
    }

    override suspend fun getAvailableYears(userId: UUID, accountId: UUID?): AvailableYears {
        log.withContext("userId" to userId, "accountId" to accountId)
            .debug { "Fetching available years" }

        val years = statisticsRepository.getAvailablePeriods(userId, accountId, "years")

        log.withContext("userId" to userId, "yearCount" to years.size)
            .debug { "Available years retrieved" }
        return AvailableYears(years)
    }

    override suspend fun getOverviewSummary(userId: UUID, accountId: UUID?): OverviewSummary {
        log.withContext("userId" to userId, "accountId" to accountId)
            .debug { "Calculating overview summary" }

        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val weeklyStart = today.minus(DatePeriod(days = 6))
        val monthlyStart = today.minus(DatePeriod(days = 29))

        log.withContext(
            "userId" to userId,
            "weeklyStart" to weeklyStart,
            "monthlyStart" to monthlyStart,
            "today" to today
        ).debug { "Date ranges calculated for overview" }

        val weekly = getDaySummaries(userId, weeklyStart, today, accountId)
        val monthly = getDaySummaries(userId, monthlyStart, today, accountId)

        val overview = OverviewSummary(weeklyOverview = weekly, monthlyOverview = monthly)

        log.withContext(
            "userId" to userId,
            "weeklyDataPoints" to weekly.size,
            "monthlyDataPoints" to monthly.size
        ).debug { "Overview summary calculated successfully" }

        return overview
    }

    override suspend fun getDaySummaries(
        userId: UUID,
        start: LocalDate,
        end: LocalDate,
        accountId: UUID?
    ): List<DaySummary> {
        log.withContext(
            "userId" to userId,
            "start" to start,
            "end" to end,
            "accountId" to accountId
        ).debug { "Calculating day summaries" }

        val transactions = statisticsRepository.getTransactionsByDateRange(userId, start, end, accountId)

        log.withContext(
            "userId" to userId,
            "transactionCount" to transactions.size
        ).debug { "Retrieved transactions for day summaries" }

        val dates = generateSequence(start) { current ->
            if (current < end) current.plus(DatePeriod(days = 1)) else null
        }.toList()

        val summaries = dates.map { date ->
            val dayTxs = transactions.filter { it.dateTime.date == date }
            val income = dayTxs.filter { it.isIncome }.sumOf { it.amount }
            val expense = dayTxs.filter { !it.isIncome }.sumOf { it.amount }
            DaySummary(date = date, income = income, expense = expense)
        }

        log.withContext(
            "userId" to userId,
            "dayCount" to summaries.size,
            "dateRange" to "${start} to ${end}"
        ).debug { "Day summaries calculated successfully" }

        return summaries
    }

    override suspend fun getCategoryComparisons(
        userId: UUID,
        accountId: UUID?
    ): List<CategoryComparison> {
        log.withContext("userId" to userId, "accountId" to accountId)
            .debug { "Calculating category comparisons" }

        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        val thisWeekStart = now.minus(DatePeriod(days = 6))
        val lastWeekStart = thisWeekStart.minus(DatePeriod(days = 7))
        val lastWeekEnd = thisWeekStart.minus(DatePeriod(days = 1))

        val thisMonthStart = now.minus(DatePeriod(days = 29))
        val lastMonthStart = thisMonthStart.minus(DatePeriod(days = 30))
        val lastMonthEnd = thisMonthStart.minus(DatePeriod(days = 1))

        log.withContext(
            "userId" to userId,
            "thisWeek" to "${thisWeekStart} to ${now}",
            "lastWeek" to "${lastWeekStart} to ${lastWeekEnd}",
            "thisMonth" to "${thisMonthStart} to ${now}",
            "lastMonth" to "${lastMonthStart} to ${lastMonthEnd}"
        ).debug { "Date ranges calculated for category comparisons" }

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

        val comparisons = listOfNotNull(weeklyComparison, monthlyComparison)

        log.withContext(
            "userId" to userId,
            "comparisonCount" to comparisons.size,
            "weeklyCategory" to weeklyComparison?.category,
            "monthlyCategory" to monthlyComparison?.category
        ).debug { "Category comparisons calculated successfully" }

        return comparisons
    }

    override suspend fun getTransactionCountSummary(
        userId: UUID,
        accountId: UUID?
    ): TransactionCountSummaryDto? {
        log.withContext("userId" to userId, "accountId" to accountId)
            .debug { "Fetching transaction count summary" }

        val counts = statisticsRepository.getTransactionCounts(userId, accountId)

        val result = if (counts.totalCount == 0) {
            log.withContext("userId" to userId, "accountId" to accountId)
                .debug { "No transactions found for account" }
            null
        } else {
            TransactionCountSummaryDto(
                totalIncomeTransactions = counts.incomeCount,
                totalExpenseTransactions = counts.expenseCount,
                totalTransactions = counts.totalCount
            )
        }

        log.withContext(
            "userId" to userId,
            "accountId" to accountId,
            "totalTransactions" to counts.totalCount,
            "incomeCount" to counts.incomeCount,
            "expenseCount" to counts.expenseCount
        ).debug { "Transaction count summary retrieved" }

        return result
    }

    // ---- Helper methods for route parameter processing ----
    override fun parseTypeFilter(typeFilter: String?): Boolean? {
        log.withContext("typeFilter" to typeFilter).debug { "Parsing type filter" }
        return when (typeFilter?.lowercase()) {
            "income" -> true
            "expense" -> false
            "" -> null
            null -> null
            else -> throw ValidationException("Type filter must be 'income' or 'expense'")
        }
    }

    override fun parseDateRange(startDate: String?, endDate: String?): Pair<LocalDateTime?, LocalDateTime?> {
        log.withContext("startDate" to startDate, "endDate" to endDate).debug { "Parsing date range" }

        val start = startDate?.let {
            LocalDate.parse(it).atTime(LocalTime(0, 0, 0))
        }
        val end = endDate?.let {
            LocalDate.parse(it).atTime(LocalTime(23, 59, 59))
        }

        // Validate date range logic
        if (start != null && end != null && start > end) {
            log.withContext("start" to start, "end" to end).warn { "Invalid date range - start after end" }
            throw ValidationException("Start date cannot be after end date")
        }

        log.withContext("parsedStart" to start, "parsedEnd" to end).debug { "Date range parsed successfully" }
        return start to end
    }
}