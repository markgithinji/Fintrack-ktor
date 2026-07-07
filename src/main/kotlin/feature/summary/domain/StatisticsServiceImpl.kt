package feature.summary.domain

import com.fintrack.core.logger
import com.fintrack.core.withContext
import feature.summary.data.model.*
import core.ValidationException
import feature.transaction.domain.model.Transaction
import feature.user.domain.UserRepository
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
    private val statisticsRepository: StatisticsRepository,
    private val userRepository: UserRepository
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

        val allTransactions = statisticsRepository.getTransactions(userId, accountId, isIncome, start, end)

        var targetPeriod = period
        var isCurrent = true

        val weekMode = targetPeriod?.contains("W") == true
        val yearMode = targetPeriod != null && !weekMode && targetPeriod.length == 4
        val monthMode = targetPeriod != null && !weekMode && !yearMode

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

        var txnsForHighlights = if (targetPeriod != null) {
            allTransactions.filter { getPeriodString(it.dateTime) == targetPeriod }
        } else {
            allTransactions
        }

        // Look-back logic: if requested period is empty, find the most recent period of the same type with data
        if (txnsForHighlights.isEmpty() && targetPeriod != null) {
            val periodType = when {
                weekMode -> "weeks"
                monthMode -> "months"
                yearMode -> "years"
                else -> null
            }
            
            if (periodType != null) {
                val availablePeriods = statisticsRepository.getAvailablePeriods(userId, accountId, periodType)
                val latestPeriod = availablePeriods.firstOrNull()
                if (latestPeriod != null && latestPeriod != targetPeriod) {
                    targetPeriod = latestPeriod
                    isCurrent = false
                    txnsForHighlights = allTransactions.filter { getPeriodString(it.dateTime) == targetPeriod }
                }
            }
        }

        log.withContext(
            "userId" to userId,
            "transactionCount" to txnsForHighlights.size,
            "targetPeriod" to targetPeriod,
            "isCurrent" to isCurrent
        ).debug { "Retrieved transactions for statistics highlights" }

        val incomeTxns = txnsForHighlights.filter { it.isIncome }
        val expenseTxns = txnsForHighlights.filter { !it.isIncome }

        fun highestMonth(txns: List<Transaction>) =
            txns.groupBy {
                val dt = it.dateTime.toLocalDateTime(TimeZone.UTC)
                "${dt.year}-${dt.monthNumber.toString().padStart(2, '0')}"
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

        return StatisticsSummaryDto(
            period = targetPeriod ?: "",
            isCurrent = isCurrent,
            income = incomeTxns.sumOf { it.totalAmount },
            expense = expenseTxns.sumOf { it.totalAmount },
            balance = incomeTxns.sumOf { it.totalAmount } - expenseTxns.sumOf { it.totalAmount },
            incomeHighlights = incomeHighlights,
            expenseHighlights = expenseHighlights,
            totalTransactionCost = txnsForHighlights.sumOf { it.transactionCost }
        )
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

        val incomeCategoriesFinal = if (isIncome != false) categorySummary(incomeTxns) else emptyList()
        val expenseCategoriesFinal = if (isIncome != true) categorySummary(expenseTxns) else emptyList()

        return DistributionSummaryDto(
            period = period,
            totalTransactionCost = txnsInPeriod.sumOf { it.transactionCost },
            incomeCategories = incomeCategoriesFinal,
            expenseCategories = expenseCategoriesFinal
        )
    }

    override suspend fun getAvailableWeeks(userId: UUID, accountId: UUID?): AvailableWeeksDto =
        AvailableWeeksDto(statisticsRepository.getAvailablePeriods(userId, accountId, "weeks"))

    override suspend fun getAvailableMonths(userId: UUID, accountId: UUID?): AvailableMonthsDto =
        AvailableMonthsDto(statisticsRepository.getAvailablePeriods(userId, accountId, "months"))

    override suspend fun getAvailableYears(userId: UUID, accountId: UUID?): AvailableYearsDto =
        AvailableYearsDto(statisticsRepository.getAvailablePeriods(userId, accountId, "years"))

    override suspend fun getOverviewSummary(userId: UUID, accountId: UUID?): OverviewSummaryDto {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        val currentMonthCode = "${now.year}-${now.monthNumber.toString().padStart(2, '0')}"

        val availableMonths = statisticsRepository.getAvailablePeriods(userId, accountId, "months")
        val targetMonthCode = availableMonths.firstOrNull() ?: currentMonthCode
        val isCurrent = targetMonthCode == currentMonthCode

        // Parse target period to date ranges
        val parts = targetMonthCode.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()

        val monthStart = LocalDate(year, month, 1)
        val monthEnd = monthStart.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))

        // Determine ends for trends
        val trendEnd = if (isCurrent) now else monthEnd
        val weeklyStart = trendEnd.minus(DatePeriod(days = 6))
        val monthlyStart = trendEnd.minus(DatePeriod(days = 29))

        val weekly = getDaySummaries(userId, weeklyStart, trendEnd, accountId)
        val monthly = getDaySummaries(userId, monthlyStart, trendEnd, accountId)

        return OverviewSummaryDto(
            period = targetMonthCode,
            isCurrent = isCurrent,
            weeklyOverview = weekly,
            monthlyOverview = monthly
        )
    }

    override suspend fun getDaySummaries(
        userId: UUID,
        start: LocalDate,
        end: LocalDate,
        accountId: UUID?
    ): List<DaySummaryDto> {
        val transactions = statisticsRepository.getTransactionsByDateRange(userId, start, end, accountId)
        val dates = generateSequence(start) { current ->
            if (current < end) current.plus(DatePeriod(days = 1)) else null
        }.toList()

        return dates.map { date ->
            val dayTxs = transactions.filter { it.dateTime.toLocalDateTime(TimeZone.UTC).date == date }
            val income = dayTxs.filter { it.isIncome }.sumOf { it.totalAmount }
            val expense = dayTxs.filter { !it.isIncome }.sumOf { it.totalAmount }
            DaySummaryDto(date = date, income = income, expense = expense)
        }
    }

    override suspend fun getCategoryComparisons(
        userId: UUID,
        accountId: UUID?,
        period: String?
    ): CategoryComparisonSummaryDto {
        log.withContext("userId" to userId, "accountId" to accountId, "period" to period)
            .debug { "Calculating category comparisons with weekly data" }

        val availableMonths = statisticsRepository.getAvailablePeriods(userId, accountId, "months")
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        val currentMonthCode = "${now.year}-${now.monthNumber.toString().padStart(2, '0')}"

        val targetPeriod = period ?: availableMonths.firstOrNull() ?: currentMonthCode
        val isCurrent = targetPeriod == currentMonthCode
        val isBackupMonth = period == null && !isCurrent

        // Parse target period to date ranges
        val (year, month) = targetPeriod.split("-").map { it.toInt() }
        val currentMonthStart = LocalDate(year, month, 1)
        val currentMonthEnd = currentMonthStart.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
        val previousMonthStart = currentMonthStart.minus(DatePeriod(months = 1))
        val previousMonthEnd = currentMonthStart.minus(DatePeriod(days = 1))

        // Weekly context: Latest 7 days within the target month (or just latest 7 days if it's current month)
        val thisWeekEnd = if (isCurrent) now else currentMonthEnd
        val thisWeekStart = thisWeekEnd.minus(DatePeriod(days = 6))
        val lastWeekStart = thisWeekStart.minus(DatePeriod(days = 7))
        val lastWeekEnd = thisWeekStart.minus(DatePeriod(days = 1))

        val thisMonthExpenseTotals = statisticsRepository.getCategoryTotals(userId, currentMonthStart, currentMonthEnd, accountId, isIncome = false)
        val lastMonthExpenseTotals = statisticsRepository.getCategoryTotals(userId, previousMonthStart, previousMonthEnd, accountId, isIncome = false)
        val thisMonthIncomeTotals = statisticsRepository.getCategoryTotals(userId, currentMonthStart, currentMonthEnd, accountId, isIncome = true)
        val lastMonthIncomeTotals = statisticsRepository.getCategoryTotals(userId, previousMonthStart, previousMonthEnd, accountId, isIncome = true)

        val thisWeekTotals = statisticsRepository.getCategoryTotals(userId, thisWeekStart, thisWeekEnd, accountId)
        val lastWeekTotals = statisticsRepository.getCategoryTotals(userId, lastWeekStart, lastWeekEnd, accountId)

        val user = userRepository.findById(userId)
        val trackedCategories = user?.trackedCategories?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

        // Fetch all-time income categories to correctly identify category type in tracked branch
        val allTimeIncomeCategories = if (trackedCategories.isNotEmpty()) {
            statisticsRepository.getCategoryTotals(userId, null, null, accountId, isIncome = true).keys
        } else emptySet()

        val comparisons = if (trackedCategories.isNotEmpty() && !isBackupMonth) {
            trackedCategories.map { category ->
                if (category == "Transaction Fees" || category == "Transaction Cost") {
                    calculateTransactionCostComparison(userId, accountId, currentMonthStart, currentMonthEnd, previousMonthStart, previousMonthEnd, thisWeekStart, thisWeekEnd, lastWeekStart, lastWeekEnd)
                } else {
                    val isInc = allTimeIncomeCategories.contains(category) || thisMonthIncomeTotals.containsKey(category)
                    val currentTotals = if (isInc) thisMonthIncomeTotals else thisMonthExpenseTotals
                    val previousTotals = if (isInc) lastMonthIncomeTotals else lastMonthExpenseTotals

                    val cur = currentTotals[category] ?: 0.0
                    val prev = previousTotals[category] ?: 0.0
                    val weekCur = thisWeekTotals[category] ?: 0.0
                    val weekPrev = lastWeekTotals[category] ?: 0.0

                    CategoryComparisonDto(
                        category = category,
                        currentTotal = cur,
                        previousTotal = prev,
                        changePercentage = calculateChange(cur, prev),
                        isIncome = isInc,
                        period = targetPeriod,
                        weeklyCurrentTotal = weekCur,
                        weeklyChangePercentage = calculateChange(weekCur, weekPrev)
                    )
                }
            }
        } else {
            // Fallback: Automatic Discovery strictly within targetMonth
            val topExpenseCategory = thisMonthExpenseTotals.keys.filter { it != "Transaction Fees" && it != "Transaction Cost" }
                .maxByOrNull { thisMonthExpenseTotals[it] ?: 0.0 }
                ?: lastMonthExpenseTotals.keys.filter { it != "Transaction Fees" && it != "Transaction Cost" }
                .maxByOrNull { lastMonthExpenseTotals[it] ?: 0.0 }

            val topIncomeCategory = thisMonthIncomeTotals.keys
                .maxByOrNull { thisMonthIncomeTotals[it] ?: 0.0 }
                ?: lastMonthIncomeTotals.keys
                .maxByOrNull { lastMonthIncomeTotals[it] ?: 0.0 }

            val results = mutableListOf<CategoryComparisonDto>()

            // Ensure an Income result is always present (Snapshot requirement)
            val incomeCategory = topIncomeCategory ?: "Salary"
            val curInc = thisMonthIncomeTotals[incomeCategory] ?: 0.0
            val prevInc = lastMonthIncomeTotals[incomeCategory] ?: 0.0
            val weekCurInc = thisWeekTotals[incomeCategory] ?: 0.0
            val weekPrevInc = lastWeekTotals[incomeCategory] ?: 0.0
            
            results.add(CategoryComparisonDto(
                category = incomeCategory,
                currentTotal = curInc,
                previousTotal = prevInc,
                changePercentage = calculateChange(curInc, prevInc),
                isIncome = true,
                period = targetPeriod,
                weeklyCurrentTotal = weekCurInc,
                weeklyChangePercentage = calculateChange(weekCurInc, weekPrevInc)
            ))

            // Ensure an Expense/Cost result is always present
            if (topExpenseCategory != null) {
                val curExp = thisMonthExpenseTotals[topExpenseCategory] ?: 0.0
                val prevExp = lastMonthExpenseTotals[topExpenseCategory] ?: 0.0
                val weekCurExp = thisWeekTotals[topExpenseCategory] ?: 0.0
                val weekPrevExp = lastWeekTotals[topExpenseCategory] ?: 0.0
                
                results.add(CategoryComparisonDto(
                    category = topExpenseCategory,
                    currentTotal = curExp,
                    previousTotal = prevExp,
                    changePercentage = calculateChange(curExp, prevExp),
                    isIncome = false,
                    period = targetPeriod,
                    weeklyCurrentTotal = weekCurExp,
                    weeklyChangePercentage = calculateChange(weekCurExp, weekPrevExp)
                ))
            } else {
                results.add(calculateTransactionCostComparison(
                    userId, accountId, currentMonthStart, currentMonthEnd, previousMonthStart, previousMonthEnd, 
                    thisWeekStart, thisWeekEnd, lastWeekStart, lastWeekEnd
                ))
            }

            results
        }

        return CategoryComparisonSummaryDto(
            period = targetPeriod,
            isCurrent = isCurrent,
            data = comparisons
        )
    }

    private fun calculateChange(current: Double, previous: Double): Double =
        if (previous != 0.0) (current - previous) / previous * 100 else if (current > 0) 100.0 else 0.0

    private suspend fun calculateTransactionCostComparison(
        userId: UUID,
        accountId: UUID?,
        currentStart: LocalDate,
        currentEnd: LocalDate,
        previousStart: LocalDate,
        previousEnd: LocalDate,
        thisWeekStart: LocalDate? = null,
        thisWeekEnd: LocalDate? = null,
        lastWeekStart: LocalDate? = null,
        lastWeekEnd: LocalDate? = null
    ): CategoryComparisonDto {
        val curTxns = statisticsRepository.getTransactionsByDateRange(userId, currentStart, currentEnd, accountId)
        val prevTxns = statisticsRepository.getTransactionsByDateRange(userId, previousStart, previousEnd, accountId)

        val curCost = curTxns.sumOf { it.transactionCost }
        val prevCost = prevTxns.sumOf { it.transactionCost }

        var weekCurCost = 0.0
        var weekPrevCost = 0.0
        if (thisWeekStart != null && thisWeekEnd != null && lastWeekStart != null && lastWeekEnd != null) {
            weekCurCost = statisticsRepository.getTransactionsByDateRange(userId, thisWeekStart, thisWeekEnd, accountId).sumOf { it.transactionCost }
            weekPrevCost = statisticsRepository.getTransactionsByDateRange(userId, lastWeekStart, lastWeekEnd, accountId).sumOf { it.transactionCost }
        }

        return CategoryComparisonDto(
            category = "Transaction Fees",
            currentTotal = curCost,
            previousTotal = prevCost,
            changePercentage = calculateChange(curCost, prevCost),
            isIncome = false,
            period = currentStart.toString().substring(0, 7),
            weeklyCurrentTotal = if (thisWeekStart != null) weekCurCost else null,
            weeklyChangePercentage = if (thisWeekStart != null) calculateChange(weekCurCost, weekPrevCost) else null
        )
    }

    override suspend fun getTransactionCountSummary(
        userId: UUID, accountId: UUID?, isIncome: Boolean?, category: String?, hasCost: Boolean?, start: Instant?, end: Instant?
    ): TransactionCountSummaryDto {
        requireNotNull(accountId) { "Account ID is required" }
        val counts = statisticsRepository.getTransactionCounts(userId, accountId, isIncome, category, hasCost, start, end)
        return TransactionCountSummaryDto(counts.incomeCount, counts.expenseCount, counts.totalCount, counts.totalTransactionCost)
    }

    override suspend fun getDaySummariesByDateRange(
        userId: UUID, accountId: UUID?, startParam: String?, endParam: String?
    ): List<DaySummaryDto> {
        val range = parseDateRange(startParam, endParam)
        val start = range.first?.toLocalDateTime(TimeZone.UTC)?.date ?: throw ValidationException("Invalid start date")
        val end = range.second?.toLocalDateTime(TimeZone.UTC)?.date ?: throw ValidationException("Invalid end date")
        return getDaySummaries(userId, start, end, accountId)
    }

    override fun parseTypeFilter(typeFilter: String?): Boolean? = when (typeFilter?.lowercase()) {
        "income" -> true
        "expense" -> false
        else -> null
    }

    override fun parseDateRange(startDate: String?, endDate: String?): Pair<Instant?, Instant?> {
        val start = startDate?.let { LocalDate.parse(it).atTime(LocalTime(0, 0, 0)).toInstant(TimeZone.UTC) }
        val end = endDate?.let { LocalDate.parse(it).atTime(LocalTime(23, 59, 59)).toInstant(TimeZone.UTC) }
        if (start != null && end != null && start > end) throw ValidationException("Start date cannot be after end date")
        return start to end
    }
}
