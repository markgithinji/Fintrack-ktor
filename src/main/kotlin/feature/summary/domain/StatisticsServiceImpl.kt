package feature.summary.domain

import com.fintrack.core.*
import feature.summary.data.model.*
import core.ValidationException
import feature.budget.domain.BudgetRepository
import feature.transaction.domain.model.Transaction
import feature.user.domain.UserRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
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
    private val userRepository: UserRepository,
    private val budgetRepository: BudgetRepository
) : StatisticsService {

    private val log = logger<StatisticsServiceImpl>()

    private val essentialCategories = setOf(
        "Rent", "Groceries", "Transport", "Bills", "Health", "Education", "Utilities", "Insurance"
    )

    private val monthNames = listOf(
        "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
        "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"
    )

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

        val initialWeekMode = targetPeriod?.contains("W") == true
        val initialYearMode = targetPeriod != null && !initialWeekMode && targetPeriod.length == 4
        val initialMonthMode = targetPeriod != null && !initialWeekMode && !initialYearMode

        fun getPeriodString(dateTime: Instant, week: Boolean, month: Boolean, year: Boolean): String {
            val dt = dateTime.toLocalDateTime(TimeZone.UTC)
            return when {
                week -> {
                    val javaDateTime = dt.toJavaLocalDateTime()
                    val w = javaDateTime.get(WeekFields.ISO.weekOfWeekBasedYear())
                    "${javaDateTime.year}-W${w.toString().padStart(2, '0')}"
                }
                month -> "${dt.year}-${dt.monthNumber.toString().padStart(2, '0')}"
                year -> dt.year.toString()
                else -> ""
            }
        }

        var txnsForHighlights = if (targetPeriod != null) {
            allTransactions.filter { getPeriodString(it.dateTime, initialWeekMode, initialMonthMode, initialYearMode) == targetPeriod }
        } else {
            allTransactions
        }

        if (txnsForHighlights.isEmpty() && targetPeriod != null) {
            val periodType = when {
                initialWeekMode -> "weeks"
                initialMonthMode -> "months"
                initialYearMode -> "years"
                else -> null
            }
            
            if (periodType != null) {
                val availablePeriods = statisticsRepository.getAvailablePeriods(userId, accountId, periodType)
                val latestPeriod = availablePeriods.firstOrNull()
                if (latestPeriod != null && latestPeriod != targetPeriod) {
                    targetPeriod = latestPeriod
                    isCurrent = false
                    txnsForHighlights = allTransactions.filter { 
                        getPeriodString(it.dateTime, initialWeekMode, initialMonthMode, initialYearMode) == targetPeriod 
                    }
                }
            }
        }

        val weekMode = targetPeriod?.contains("W") == true
        val yearMode = targetPeriod != null && !weekMode && targetPeriod.length == 4
        val monthMode = targetPeriod != null && !weekMode && !yearMode

        log.withContext(
            "userId" to userId,
            "transactionCount" to txnsForHighlights.size,
            "targetPeriod" to targetPeriod,
            "isCurrent" to isCurrent,
            "yearMode" to yearMode
        ).debug { "Retrieved transactions for statistics highlights" }

        var ytdIncomeChange: Double? = null
        var ytdExpenseChange: Double? = null
        var incomeProjectedTotal: Double? = null
        var expenseProjectedTotal: Double? = null
        var prevIncomeByCat: Map<String, Double>? = null
        var prevExpenseByCat: Map<String, Double>? = null

        if (yearMode && targetPeriod != null) {
            val requestedYear = targetPeriod.toIntOrNull()
            if (requestedYear != null) {
                val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
                val isCurrentYear = requestedYear == now.year
                val lastYear = requestedYear - 1

                val (currentStart, currentEnd) = if (isCurrentYear) {
                    LocalDate(requestedYear, 1, 1) to now.date
                } else {
                    LocalDate(requestedYear, 1, 1) to LocalDate(requestedYear, 12, 31)
                }

                val (prevStart, prevEnd) = if (isCurrentYear) {
                    val month = now.monthNumber
                    val day = now.dayOfMonth
                    val lastYearDay = if (month == 2 && day == 29) 28 else day
                    LocalDate(lastYear, 1, 1) to LocalDate(lastYear, month, lastYearDay)
                } else {
                    LocalDate(lastYear, 1, 1) to LocalDate(lastYear, 12, 31)
                }

                val currentIncomeByCat = statisticsRepository.getCategoryTotals(userId, currentStart, currentEnd, accountId, true)
                val currentExpenseByCat = statisticsRepository.getCategoryTotals(userId, currentStart, currentEnd, accountId, false)

                prevIncomeByCat = statisticsRepository.getCategoryTotals(userId, prevStart, prevEnd, accountId, true)
                prevExpenseByCat = statisticsRepository.getCategoryTotals(userId, prevStart, prevEnd, accountId, false)

                val currentIncomeTotal = currentIncomeByCat.values.sum()
                val currentExpenseTotal = currentExpenseByCat.values.sum()
                val prevIncomeTotal = prevIncomeByCat.values.sum()
                val prevExpenseTotal = prevExpenseByCat.values.sum()

                ytdIncomeChange = calculateChange(currentIncomeTotal, prevIncomeTotal)
                ytdExpenseChange = calculateChange(currentExpenseTotal, prevExpenseTotal)

                if (isCurrentYear) {
                    val monthNumber = now.monthNumber.coerceAtLeast(1)
                    incomeProjectedTotal = (currentIncomeTotal / monthNumber) * 12
                    expenseProjectedTotal = (currentExpenseTotal / monthNumber) * 12
                }
            }
        }

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

        fun highestCategory(txns: List<Transaction>, prevTotals: Map<String, Double>?) =
            txns.groupBy { it.category.trim().lowercase() }
                .mapValues { it.value.sumOf { t -> t.totalAmount } }
                .maxByOrNull { it.value }
                ?.let { entry ->
                    val displayName =
                        txns.firstOrNull { it.category.trim().lowercase() == entry.key }?.category
                            ?: entry.key
                    
                    val volatility = prevTotals?.let { totals ->
                        val prevAmount = totals.entries.find { it.key.trim().lowercase() == entry.key }?.value ?: 0.0
                        calculateChange(entry.value, prevAmount)
                    }

                    HighlightDto(
                        label = displayName,
                        value = displayName,
                        amount = entry.value,
                        volatilityPercentage = volatility
                    )
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

        val totalIncome = incomeTxns.sumOf { it.totalAmount }
        val totalExpense = expenseTxns.sumOf { it.totalAmount }

        val savingsRate = if (totalIncome > 0) ((totalIncome - totalExpense) / totalIncome) * 100 else null

        val essentialSpend = expenseTxns
            .filter { txn -> essentialCategories.any { it.equals(txn.category.trim(), ignoreCase = true) } }
            .sumOf { it.totalAmount }
        val essentialSpendRatio = if (totalExpense > 0) (essentialSpend / totalExpense) * 100 else null

        var projectedExceedMonth: String? = null
        if (yearMode && expenseProjectedTotal != null && targetPeriod != null) {
            val budgets = budgetRepository.getAllByUser(userId, accountId).filter { it.isExpense }
            val totalMonthlyLimit = budgets.sumOf { it.limit }
            val yearlyLimit = totalMonthlyLimit * 12

            if (expenseProjectedTotal > yearlyLimit && yearlyLimit > 0) {
                val currentMonth = Clock.System.now().toLocalDateTime(TimeZone.UTC).monthNumber
                val averagePerMonth = totalExpense / currentMonth

                for (m in (currentMonth + 1)..12) {
                    val accumulatedSpend = totalExpense + averagePerMonth * (m - currentMonth)
                    if (accumulatedSpend > yearlyLimit) {
                        projectedExceedMonth = monthNames.getOrNull(m - 1)
                        break
                    }
                }
            }
        }

        val incomeHighlights = HighlightsDto(
            highestMonth = highestMonth(incomeTxns),
            highestCategory = highestCategory(incomeTxns, prevIncomeByCat),
            highestDay = highestDay(incomeTxns),
            averagePerDay = averagePerDay(incomeTxns),
            ytdChangePercentage = ytdIncomeChange,
            projectedTotal = incomeProjectedTotal,
            savingsRate = savingsRate
        )

        val correlationInsights = mutableListOf<CorrelationDto>()
        if (monthMode && targetPeriod != null) {
            val hParts = targetPeriod.split("-")
            val hYear = hParts[0].toInt()
            val hMonth = hParts[1].toInt()
            val hCurrentStart = LocalDate(hYear, hMonth, 1)

            val historicalStart = hCurrentStart.minus(DatePeriod(months = 6))
            val historicalEnd = hCurrentStart.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))

            val hTxns = statisticsRepository.getTransactions(
                userId, accountId, null,
                historicalStart.atStartOfDayIn(TimeZone.UTC),
                historicalEnd.atTime(23, 59, 59).toInstant(TimeZone.UTC)
            )

            val monthlyData = hTxns.groupBy {
                val dt = it.dateTime.toLocalDateTime(TimeZone.UTC)
                "${dt.year}-${dt.monthNumber.toString().padStart(2, '0')}"
            }.mapValues { (_, mTxns) ->
                val inc = mTxns.filter { it.isIncome }.sumOf { it.totalAmount }
                val exp = mTxns.filter { !it.isIncome }.groupBy { it.category.trim().lowercase() }
                    .mapValues { it.value.sumOf { t -> t.totalAmount } }
                Pair(inc, exp)
            }.toSortedMap()

            val targetCats = setOf("shopping", "entertainment", "dining out")
            val keys = monthlyData.keys.toList()
            for (i in 1 until keys.size) {
                val prevMonth = monthlyData[keys[i - 1]]!!
                val currMonth = monthlyData[keys[i]]!!
                val nextMonth = if (i + 1 < keys.size) monthlyData[keys[i + 1]] else null

                if (prevMonth.first > 0) {
                    val incomeIncrease = (currMonth.first - prevMonth.first) / prevMonth.first
                    if (incomeIncrease > 0.10) {
                        targetCats.forEach { cat ->
                            val prevExp = prevMonth.second[cat] ?: 0.0
                            val currExp = currMonth.second[cat] ?: 0.0
                            val nextExp = nextMonth?.second?.get(cat) ?: 0.0

                            if (prevExp > 0 && (currExp - prevExp) / prevExp > 0.15) {
                                val incPct = (incomeIncrease * 100).toInt()
                                val expPct = (((currExp - prevExp) / prevExp) * 100).toInt()
                                correlationInsights.add(
                                    CorrelationDto(
                                        source = "Income",
                                        target = cat.replaceFirstChar { it.uppercase() },
                                        insight = "When your income increases by $incPct%, your '${cat.replaceFirstChar { it.uppercase() }}' spend tends to increase by $expPct% in the same month."
                                    )
                                )
                            }
                            else if (currExp > 0 && nextExp > 0 && (nextExp - currExp) / currExp > 0.15) {
                                val incPct = (incomeIncrease * 100).toInt()
                                val expPct = (((nextExp - currExp) / currExp) * 100).toInt()
                                correlationInsights.add(
                                    CorrelationDto(
                                        source = "Income",
                                        target = cat.replaceFirstChar { it.uppercase() },
                                        insight = "Following a $incPct% income increase, your '${cat.replaceFirstChar { it.uppercase() }}' spend tended to increase by $expPct% the next month."
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        val expenseHighlights = HighlightsDto(
            highestMonth = highestMonth(expenseTxns),
            highestCategory = highestCategory(expenseTxns, prevExpenseByCat),
            highestDay = highestDay(expenseTxns),
            averagePerDay = averagePerDay(expenseTxns),
            ytdChangePercentage = ytdExpenseChange,
            projectedTotal = expenseProjectedTotal,
            essentialSpendRatio = essentialSpendRatio,
            projectedExceedMonth = projectedExceedMonth,
            correlations = correlationInsights.distinctBy { it.target }.take(3)
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

        val parts = period.split("-")
        val year = if (monthMode || weekMode) parts[0].toInt() else period.toInt()
        val month = if (monthMode) parts[1].toInt() else 1
        val currentMonthStart = LocalDate(year, month, 1)

        val historicalCounts = if (monthMode) {
            val historicalStart = currentMonthStart.minus(DatePeriod(months = 6))
            val historicalEnd = currentMonthStart.minus(DatePeriod(days = 1))

            val histTxns = statisticsRepository.getTransactions(
                userId, accountId, isIncome,
                historicalStart.atStartOfDayIn(TimeZone.UTC),
                historicalEnd.atTime(23, 59, 59).toInstant(TimeZone.UTC)
            )

            histTxns.groupBy { it.category.trim().lowercase() }
                .mapValues { (_, txns) ->
                    val monthsWithData = txns.groupBy {
                        val dt = it.dateTime.toLocalDateTime(TimeZone.UTC)
                        "${dt.year}-${dt.monthNumber}"
                    }.size
                    txns.size.toDouble() / monthsWithData.coerceAtLeast(1)
                }
        } else null

        val previousMonthsData = if (monthMode) {
            val m1Start = currentMonthStart.minus(DatePeriod(months = 1))
            val m1End = currentMonthStart.minus(DatePeriod(days = 1))
            val m2Start = currentMonthStart.minus(DatePeriod(months = 2))
            val m2End = m1Start.minus(DatePeriod(days = 1))

            val totals1 = statisticsRepository.getCategoryTotals(userId, m1Start, m1End, accountId, isIncome)
                .mapKeys { it.key.trim().lowercase() }
            val totals2 = statisticsRepository.getCategoryTotals(userId, m2Start, m2End, accountId, isIncome)
                .mapKeys { it.key.trim().lowercase() }
            Pair(totals1, totals2)
        } else null

        val incomeTxns = txnsInPeriod.filter { it.isIncome }
        val expenseTxns = txnsInPeriod.filter { !it.isIncome }

        var othersInsight: String? = null

        fun categorySummary(txns: List<Transaction>): List<CategorySummaryDto> {
            val totalAmountAll = txns.sumOf { it.totalAmount }
            val grouped = txns.groupBy { it.category.trim().lowercase() }
            
            val sortedCategories = grouped.map { (key, list) ->
                val sum = list.sumOf { it.totalAmount }
                Triple(key, list, sum)
            }.sortedByDescending { it.third }

            val topCategories = sortedCategories.take(5)
            val otherCategories = sortedCategories.drop(5)

            if (otherCategories.isNotEmpty()) {
                val othersTxns = otherCategories.flatMap { it.second }
                othersInsight = othersTxns
                    .mapNotNull { if (isDescriptionMeaningful(it.description, it.category)) cleanMerchantName(it.description!!) else null }
                    .groupBy { it }
                    .maxByOrNull { it.value.size }
                    ?.key
            }

            val deduped = sortedCategories.map { (key, list, sum) ->
                val displayName = list.first().category

                val count = list.size
                val momentum = if (monthMode && previousMonthsData != null) {
                    val prev1 = previousMonthsData.first[key] ?: 0.0
                    val prev2 = previousMonthsData.second[key] ?: 0.0
                    when {
                        sum > prev1 && prev1 > prev2 -> "UP"
                        sum < prev1 && prev1 < prev2 -> "DOWN"
                        else -> "STABLE"
                    }
                } else null

                val insights = list
                    .mapNotNull { if (isDescriptionMeaningful(it.description, displayName)) cleanMerchantName(it.description!!) else null }
                    .groupBy { it }
                    .mapValues { it.value.size }
                    .entries
                    .sortedByDescending { it.value }
                    .take(3)
                    .map { it.key }

                CategorySummaryDto(
                    category = displayName,
                    total = sum,
                    percentage = if (totalAmountAll > 0) (sum / totalAmountAll) * 100 else 0.0,
                    transactionCount = count,
                    averageTransactionCount = historicalCounts?.get(key),
                    momentumTrend = momentum,
                    topDescriptionInsights = insights
                )
            }
            return deduped
        }

        val incomeCategoriesFinal = if (isIncome != false) categorySummary(incomeTxns) else emptyList()
        val expenseCategoriesFinal = if (isIncome != true) categorySummary(expenseTxns) else emptyList()

        return DistributionSummaryDto(
            period = period,
            totalTransactionCost = txnsInPeriod.sumOf { it.transactionCost },
            incomeCategories = incomeCategoriesFinal,
            expenseCategories = expenseCategoriesFinal,
            othersInsightSummary = othersInsight
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

        val parts = targetMonthCode.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()

        val monthStart = LocalDate(year, month, 1)
        val monthEnd = monthStart.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))

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

        val (year, month) = targetPeriod.split("-").map { it.toInt() }
        val currentMonthStart = LocalDate(year, month, 1)
        val currentMonthEnd = currentMonthStart.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
        val previousMonthStart = currentMonthStart.minus(DatePeriod(months = 1))
        val previousMonthEnd = currentMonthStart.minus(DatePeriod(days = 1))

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
            val topExpenseCategory = thisMonthExpenseTotals.keys.filter { it != "Transaction Fees" && it != "Transaction Cost" }
                .maxByOrNull { thisMonthExpenseTotals[it] ?: 0.0 }
                ?: lastMonthExpenseTotals.keys.filter { it != "Transaction Fees" && it != "Transaction Cost" }
                .maxByOrNull { lastMonthExpenseTotals[it] ?: 0.0 }

            val topIncomeCategory = thisMonthIncomeTotals.keys
                .maxByOrNull { thisMonthIncomeTotals[it] ?: 0.0 }
                ?: lastMonthIncomeTotals.keys
                .maxByOrNull { lastMonthIncomeTotals[it] ?: 0.0 }

            val results = mutableListOf<CategoryComparisonDto>()

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

    private fun isDescriptionMeaningful(description: String?, categoryName: String): Boolean {
        if (description.isNullOrBlank()) return false
        val cleaned = cleanMerchantName(description)

        if (cleaned.length < 3) return false

        if (cleaned.all { it.isDigit() || it == '-' || it == '.' || it == ':' || it == '#' || it == '/' || it == ' ' }) return false

        val normalizedCategory = categoryName.lowercase().replace(" ", "").replace("-", "")
        val normalizedDesc = cleaned.lowercase().replace(" ", "").replace("-", "")

        if (normalizedDesc == normalizedCategory) return false

        if (normalizedDesc.contains(normalizedCategory) || normalizedCategory.contains(normalizedDesc)) {
            val lengthDiff = kotlin.math.abs(normalizedDesc.length - normalizedCategory.length)
            if (lengthDiff <= 3) return false
        }

        return true
    }

    private fun cleanMerchantName(description: String): String {
        // 1. Remove common reference labels like "Ref:", "Reference:", etc.
        var cleaned = description.replace(Regex("(?i)\\b(Ref|Reference|Ref No|Ref#|Ref ID)[:.]?\\s*"), "").trim()

        // 2. Remove transaction codes (e.g., OAG8123456), potentially in parentheses
        cleaned = cleaned.replace(Regex("\\(?\\s*[A-Z0-9]{10}\\s*\\)?"), "").trim()
        
        // 3. Remove common boilerplate words
        val noiseWords = listOf("Confirmed", "Ksh", "Paid to", "Sent to", "on", "at")
        noiseWords.forEach { word ->
            cleaned = cleaned.replace(Regex("(?i)\\b$word\\b"), "")
        }

        // 4. Remove trailing numbers/IDs (e.g., UBER *1234 -> UBER)
        cleaned = cleaned.split("*", "#", " - ").first().trim()

        // 5. Final numeric cleanup (remove 4+ digit codes)
        cleaned = cleaned.replace(Regex("[0-9]{4,}"), "").trim()

        // 6. Remove empty parentheses ()
        cleaned = cleaned.replace(Regex("\\(\\s*\\)"), "").trim()

        // 7. Final strip of leading/trailing parentheses if they wrap the name
        return cleaned.removePrefix("(").removeSuffix(")").trim()
    }
}
