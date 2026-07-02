package feature.summary.domain

import com.fintrack.core.logger
import com.fintrack.core.withContext
import com.fintrack.feature.summary.data.model.AvailableMonthsDto
import com.fintrack.feature.summary.data.model.AvailableWeeksDto
import com.fintrack.feature.summary.data.model.AvailableYearsDto
import com.fintrack.feature.summary.data.model.CategoryComparisonDto
import com.fintrack.feature.summary.data.model.CategorySummaryDto
import com.fintrack.feature.summary.data.model.DistributionSummaryDto
import com.fintrack.feature.summary.data.model.TransactionCountSummaryDto
import core.DaySummaryDto
import core.HighlightDto
import core.HighlightsDto
import core.OverviewSummaryDto
import core.StatisticsSummaryDto
import core.ValidationException
import feature.transaction.domain.model.Transaction
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
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
        start: Instant?,
        end: Instant?,
        period: String?
    ): StatisticsSummaryDto {
        log.withContext(
            "userId" to userId,
            "accountId" to accountId,
            "isIncome" to isIncome,
            "start" to start?.toString(),
            "end" to end?.toString(),
            "period" to period
        ).debug { "Calculating statistics summary" }

        val filtered = statisticsRepository.getTransactions(userId, accountId, isIncome, start, end)

        val txnsForHighlights = if (period != null) {
            val weekMode = period.contains("W")
            val yearMode = !weekMode && period.length == 4
            val monthMode = !weekMode && !yearMode

            fun getPeriodString(dateTime: Instant): String {
                val dt = dateTime.toLocalDateTime(TimeZone.UTC)
                return when {
                    weekMode -> {
                        val javaDateTime = dt.toJavaLocalDateTime()
                        val week = javaDateTime.get(WeekFields.ISO.weekOfWeekBasedYear())
                        "${javaDateTime.year}-W${week.toString().padStart(2, '0')}"
                    }
                    monthMode -> "${dt.year}-${dt.monthNumber.toString().padStart(2, '0')}"
                    yearMode -> dt.year.toString()
                    else -> ""
                }
            }
            filtered.filter { getPeriodString(it.dateTime) == period }
        } else {
            filtered
        }

        log.withContext(
            "userId" to userId,
            "transactionCount" to txnsForHighlights.size
        ).debug { "Retrieved transactions for statistics highlights" }

        val incomeTxns = txnsForHighlights.filter { it.isIncome }
        val expenseTxns = txnsForHighlights.filter { !it.isIncome }

        // ... existing highlight logic using incomeTxns/expenseTxns ...
        fun highestMonth(txns: List<Transaction>) =
            txns.groupBy {
                val dt = it.dateTime.toLocalDateTime(TimeZone.UTC)
                "${dt.year}-${
                    dt.monthNumber.toString().padStart(2, '0')
                }"
            }
                .mapValues { it.value.sumOf { t -> t.totalAmount } }
                .maxByOrNull { it.value }
                ?.let { HighlightDto(label = it.key, value = it.key, amount = it.value) }

        fun highestCategory(txns: List<Transaction>) =
            txns.groupBy { it.category.trim().lowercase() }
                .mapValues { it.value.sumOf { t -> t.totalAmount } }
                .maxByOrNull { it.value }
                ?.let { entry ->
                    val displayName =
                        txns.firstOrNull { it.category.trim().lowercase() == entry.key }?.category
                            ?: entry.key
                    HighlightDto(label = displayName, value = displayName, amount = entry.value)
                }

        fun highestDay(txns: List<Transaction>) =
            txns.groupBy { it.dateTime.toLocalDateTime(TimeZone.UTC).date }
                .mapValues { it.value.sumOf { t -> t.totalAmount } }
                .maxByOrNull { it.value }
                ?.let {
                    HighlightDto(
                        label = it.key.toString(),
                        value = it.key.toString(),
                        amount = it.value
                    )
                }

        fun averagePerDay(txns: List<Transaction>): Double {
            val days = txns.groupBy { it.dateTime.toLocalDateTime(TimeZone.UTC).date }.size.coerceAtLeast(1)
            val total = txns.sumOf { it.totalAmount }
            return total / days
        }

        val incomeHighlights = HighlightsDto(
            highestMonth = highestMonth(incomeTxns),
            highestCategory = highestCategory(incomeTxns),
            highestDay = highestDay(incomeTxns),
            averagePerDay = averagePerDay(incomeTxns)
        )
        val expenseHighlights = HighlightsDto(
            highestMonth = highestMonth(expenseTxns),
            highestCategory = highestCategory(expenseTxns),
            highestDay = highestDay(expenseTxns),
            averagePerDay = averagePerDay(expenseTxns)
        )

        val summary = StatisticsSummaryDto(
            income = incomeTxns.sumOf { it.totalAmount },
            expense = expenseTxns.sumOf { it.totalAmount },
            balance = incomeTxns.sumOf { it.totalAmount } - expenseTxns.sumOf { it.totalAmount },
            incomeHighlights = incomeHighlights,
            expenseHighlights = expenseHighlights,
            totalTransactionCost = txnsForHighlights.sumOf { it.transactionCost }
        )

        return summary
    }

    override suspend fun getDistributionSummary(
        userId: UUID,
        period: String,
        accountId: UUID?,
        isIncome: Boolean?,
        start: Instant?,
        end: Instant?
    ): DistributionSummaryDto {
        if (!period.matches(Regex("^(\\d{4}-W\\d{2}|\\d{4}-\\d{2}|\\d{4})$"))) {
            throw ValidationException("Period must be in format: YYYY-Www, YYYY-MM, or YYYY")
        }

        log.withContext(
            "userId" to userId,
            "period" to period,
            "accountId" to accountId,
            "isIncome" to isIncome,
            "start" to start?.toString(),
            "end" to end?.toString()
        ).debug { "Calculating distribution summary" }

        val filtered = statisticsRepository.getTransactions(userId, accountId, isIncome, start, end)

        val weekMode = period.contains("W")
        val yearMode = !weekMode && period.length == 4
        val monthMode = !weekMode && !yearMode

        fun getPeriodString(dateTime: Instant): String {
            val dt = dateTime.toLocalDateTime(TimeZone.UTC)
            return when {
                weekMode -> {
                    val javaDateTime = dt.toJavaLocalDateTime()
                    val week = javaDateTime.get(WeekFields.ISO.weekOfWeekBasedYear())
                    "${javaDateTime.year}-W${week.toString().padStart(2, '0')}"
                }
                monthMode -> "${dt.year}-${dt.monthNumber.toString().padStart(2, '0')}"
                yearMode -> dt.year.toString()
                else -> ""
            }
        }

        val txnsInPeriod = filtered.filter { getPeriodString(it.dateTime) == period }
        val incomeTxns = txnsInPeriod.filter { it.isIncome }
        val expenseTxns = txnsInPeriod.filter { !it.isIncome }

        fun categorySummary(txns: List<Transaction>): List<CategorySummaryDto> {
            val deduped = txns.groupBy { it.category.trim().lowercase() }.map { (_, list) ->
                val sum = list.sumOf { it.totalAmount }
                val displayName = list.first().category
                CategorySummaryDto(category = displayName, total = sum, percentage = 0.0)
            }
            val totalAmount = deduped.sumOf { it.total }
            return deduped.map {
                it.copy(percentage = if (totalAmount > 0) (it.total / totalAmount) * 100 else 0.0)
            }
        }

        val incomeCategoriesFinal =
            if (isIncome != false) categorySummary(incomeTxns) else emptyList()
        val expenseCategoriesFinal =
            if (isIncome != true) categorySummary(expenseTxns) else emptyList()

        val distribution = DistributionSummaryDto(
            period = period,
            totalTransactionCost = txnsInPeriod.sumOf { it.transactionCost },
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

    override suspend fun getAvailableWeeks(userId: UUID, accountId: UUID?): AvailableWeeksDto {
        log.withContext("userId" to userId, "accountId" to accountId)
            .debug { "Fetching available weeks" }

        val weeks = statisticsRepository.getAvailablePeriods(userId, accountId, "weeks")

        log.withContext("userId" to userId, "weekCount" to weeks.size)
            .debug { "Available weeks retrieved" }
        return AvailableWeeksDto(weeks)
    }

    override suspend fun getAvailableMonths(userId: UUID, accountId: UUID?): AvailableMonthsDto {
        log.withContext("userId" to userId, "accountId" to accountId)
            .debug { "Fetching available months" }

        val months = statisticsRepository.getAvailablePeriods(userId, accountId, "months")

        log.withContext("userId" to userId, "monthCount" to months.size)
            .debug { "Available months retrieved" }
        return AvailableMonthsDto(months)
    }

    override suspend fun getAvailableYears(userId: UUID, accountId: UUID?): AvailableYearsDto {
        log.withContext("userId" to userId, "accountId" to accountId)
            .debug { "Fetching available years" }

        val years = statisticsRepository.getAvailablePeriods(userId, accountId, "years")

        log.withContext("userId" to userId, "yearCount" to years.size)
            .debug { "Available years retrieved" }
        return AvailableYearsDto(years)
    }

    override suspend fun getOverviewSummary(userId: UUID, accountId: UUID?): OverviewSummaryDto {
        log.withContext("userId" to userId, "accountId" to accountId)
            .debug { "Calculating overview summary" }

        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
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

        val overview = OverviewSummaryDto(weeklyOverview = weekly, monthlyOverview = monthly)

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
    ): List<DaySummaryDto> {
        log.withContext(
            "userId" to userId,
            "start" to start,
            "end" to end,
            "accountId" to accountId
        ).debug { "Calculating day summaries" }

        val transactions =
            statisticsRepository.getTransactionsByDateRange(userId, start, end, accountId)

        log.withContext(
            "userId" to userId,
            "transactionCount" to transactions.size
        ).debug { "Retrieved transactions for day summaries" }

        val dates = generateSequence(start) { current ->
            if (current < end) current.plus(DatePeriod(days = 1)) else null
        }.toList()

        val summaries = dates.map { date ->
            val dayTxs = transactions.filter { it.dateTime.toLocalDateTime(TimeZone.UTC).date == date }
            val income = dayTxs.filter { it.isIncome }.sumOf { it.totalAmount }
            val expense = dayTxs.filter { !it.isIncome }.sumOf { it.totalAmount }
            DaySummaryDto(date = date, income = income, expense = expense)
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
    ): List<CategoryComparisonDto> {
        log.withContext("userId" to userId, "accountId" to accountId)
            .debug { "Calculating category comparisons" }

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).date

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

        val thisWeekTotals =
            statisticsRepository.getCategoryTotals(userId, thisWeekStart, now, accountId)
        val lastWeekTotals =
            statisticsRepository.getCategoryTotals(userId, lastWeekStart, lastWeekEnd, accountId)

        val thisMonthTotals =
            statisticsRepository.getCategoryTotals(userId, thisMonthStart, now, accountId)
        val lastMonthTotals =
            statisticsRepository.getCategoryTotals(userId, lastMonthStart, lastMonthEnd, accountId)

        // Add Top Monthly Category with Weekly Context
        val topMonthCategory = thisMonthTotals.maxByOrNull { it.value }?.key
        val monthlyComparison = topMonthCategory?.let { category ->
            val monthCurrent = thisMonthTotals[category] ?: 0.0
            val monthPrevious = lastMonthTotals[category] ?: 0.0
            val weekCurrent = thisWeekTotals[category] ?: 0.0
            val weekPrevious = lastWeekTotals[category] ?: 0.0
            
            CategoryComparisonDto(
                category = category,
                currentTotal = monthCurrent,
                previousTotal = monthPrevious,
                changePercentage = if (monthPrevious != 0.0) (monthCurrent - monthPrevious) / monthPrevious * 100 else if (monthCurrent > 0) 100.0 else 0.0,
                weeklyCurrentTotal = weekCurrent,
                weeklyChangePercentage = if (weekPrevious != 0.0) (weekCurrent - weekPrevious) / weekPrevious * 100 else if (weekCurrent > 0) 100.0 else 0.0
            )
        }

        // Add Transaction Cost comparison (Merged Monthly/Weekly)
        val thisWeekTxns = statisticsRepository.getTransactionsByDateRange(userId, thisWeekStart, now, accountId)
        val lastWeekTxns = statisticsRepository.getTransactionsByDateRange(userId, lastWeekStart, lastWeekEnd, accountId)
        val thisMonthTxns = statisticsRepository.getTransactionsByDateRange(userId, thisMonthStart, now, accountId)
        val lastMonthTxns = statisticsRepository.getTransactionsByDateRange(userId, lastMonthStart, lastMonthEnd, accountId)

        val monthCostCurrent = thisMonthTxns.sumOf { it.transactionCost }
        val monthCostPrevious = lastMonthTxns.sumOf { it.transactionCost }
        val weekCostCurrent = thisWeekTxns.sumOf { it.transactionCost }
        val weekCostPrevious = lastWeekTxns.sumOf { it.transactionCost }

        val monthlyCostComparison = CategoryComparisonDto(
            category = "Transaction Cost",
            currentTotal = monthCostCurrent,
            previousTotal = monthCostPrevious,
            changePercentage = if (monthCostPrevious != 0.0) (monthCostCurrent - monthCostPrevious) / monthCostPrevious * 100 else if (monthCostCurrent > 0) 100.0 else 0.0,
            weeklyCurrentTotal = weekCostCurrent,
            weeklyChangePercentage = if (weekCostPrevious != 0.0) (weekCostCurrent - weekCostPrevious) / weekCostPrevious * 100 else if (weekCostCurrent > 0) 100.0 else 0.0
        )

        // Only return 2 entries: Top Category (with both period stats) and Transaction Fees (with both period stats)
        val comparisons = listOfNotNull(monthlyComparison, monthlyCostComparison)

        log.withContext(
            "userId" to userId,
            "comparisonCount" to comparisons.size,
            "topCategory" to monthlyComparison?.category
        ).debug { "Category comparisons calculated successfully" }

        return comparisons
    }

    override suspend fun getTransactionCountSummary(
        userId: UUID,
        accountId: UUID?,
        isIncome: Boolean?,
        start: Instant?,
        end: Instant?
    ): TransactionCountSummaryDto {
        requireNotNull(accountId) { "Account ID is required" }

        log.withContext(
            "userId" to userId,
            "accountId" to accountId,
            "isIncome" to isIncome,
            "start" to start,
            "end" to end
        ).debug { "Fetching transaction count summary" }

        val counts = statisticsRepository.getTransactionCounts(userId, accountId, isIncome, start, end)

        val result = TransactionCountSummaryDto(
            totalIncomeTransactions = counts.incomeCount,
            totalExpenseTransactions = counts.expenseCount,
            totalTransactions = counts.totalCount,
            totalTransactionCost = counts.totalTransactionCost
        )

        log.withContext(
            "userId" to userId,
            "accountId" to accountId,
            "isIncome" to isIncome,
            "totalTransactions" to counts.totalCount,
            "incomeCount" to counts.incomeCount,
            "expenseCount" to counts.expenseCount
        ).debug { "Transaction count summary retrieved" }

        return result
    }

    override suspend fun getDaySummariesByDateRange(
        userId: UUID,
        accountId: UUID?,
        startParam: String?,
        endParam: String?
    ): List<DaySummaryDto> {
        requireNotNull(startParam) { "start parameter is required" }
        requireNotNull(endParam) { "end parameter is required" }

        val range = parseDateRange(startParam, endParam)
        val start = range.first ?: throw ValidationException("Invalid start date")
        val end = range.second ?: throw ValidationException("Invalid end date")

        val startDate = start.toLocalDateTime(TimeZone.UTC).date
        val endDate = end.toLocalDateTime(TimeZone.UTC).date

        return getDaySummaries(userId, startDate, endDate, accountId)
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

    override fun parseDateRange(
        startDate: String?,
        endDate: String?
    ): Pair<Instant?, Instant?> {
        log.withContext("startDate" to startDate, "endDate" to endDate)
            .debug { "Parsing date range" }

        val start = startDate?.let {
            LocalDate.parse(it).atTime(LocalTime(0, 0, 0)).toInstant(TimeZone.UTC)
        }
        val end = endDate?.let {
            LocalDate.parse(it).atTime(LocalTime(23, 59, 59)).toInstant(TimeZone.UTC)
        }

        // Validate date range logic
        if (start != null && end != null && start > end) {
            log.withContext("start" to start, "end" to end)
                .warn { "Invalid date range - start after end" }
            throw ValidationException("Start date cannot be after end date")
        }

        log.withContext("parsedStart" to start, "parsedEnd" to end)
            .debug { "Date range parsed successfully" }
        return start to end
    }
}