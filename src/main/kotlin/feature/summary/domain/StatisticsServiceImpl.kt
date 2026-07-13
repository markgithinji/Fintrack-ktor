package feature.summary.domain

import com.fintrack.core.*
import com.fintrack.core.domain.AppError
import com.fintrack.core.domain.Result
import com.fintrack.core.domain.TimePeriod
import com.fintrack.feature.accounts.domain.AccountService
import com.fintrack.feature.user.domain.UserRepository
import feature.budget.domain.BudgetRepository
import feature.summary.data.model.*
import feature.transaction.domain.model.Transaction
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.*
import java.util.UUID

class StatisticsServiceImpl(
    private val statisticsRepository: StatisticsRepository,
    private val userRepository: UserRepository,
    private val budgetRepository: BudgetRepository,
    private val accountService: AccountService
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
    ): Result<StatisticsSummaryDto> {
        log.withContext(
            "userId" to userId,
            "accountId" to accountId,
            "isIncome" to isIncome,
            "start" to start?.toString(),
            "end" to end?.toString(),
            "period" to period
        ).debug { "Calculating statistics summary" }

        val allTransactions = statisticsRepository.getTransactions(userId, accountId, isIncome, start, end)

        var targetPeriodString = period
        var isCurrent = true

        val parsedPeriod = targetPeriodString?.let { TimePeriod.parse(it) }

        var txnsForHighlights = if (parsedPeriod != null) {
            allTransactions.filter { parsedPeriod.matches(it.dateTime) }
        } else {
            allTransactions
        }

        if (txnsForHighlights.isEmpty() && targetPeriodString != null && parsedPeriod != null) {
            val periodType = when (parsedPeriod) {
                is TimePeriod.Week -> "weeks"
                is TimePeriod.Month -> "months"
                is TimePeriod.Year -> "years"
            }
            
            val availablePeriods = statisticsRepository.getAvailablePeriods(userId, accountId, periodType)
            val latestPeriod = availablePeriods.firstOrNull()
            if (latestPeriod != null && latestPeriod != targetPeriodString) {
                targetPeriodString = latestPeriod
                isCurrent = false
                val newParsedPeriod = TimePeriod.parse(latestPeriod)
                txnsForHighlights = allTransactions.filter { newParsedPeriod.matches(it.dateTime) }
            }
        }

        val finalParsedPeriod = targetPeriodString?.let { TimePeriod.parse(it) }
        val weekMode = finalParsedPeriod is TimePeriod.Week
        val yearMode = finalParsedPeriod is TimePeriod.Year
        val monthMode = finalParsedPeriod is TimePeriod.Month

        log.withContext(
            "userId" to userId,
            "transactionCount" to txnsForHighlights.size,
            "targetPeriod" to targetPeriodString,
            "isCurrent" to isCurrent,
            "yearMode" to yearMode
        ).debug { "Retrieved transactions for statistics highlights" }

        var ytdIncomeChange: Double? = null
        var ytdExpenseChange: Double? = null
        var incomeProjectedTotal: Double? = null
        var expenseProjectedTotal: Double? = null
        var prevIncomeByCat: Map<String, Double>? = null
        var prevExpenseByCat: Map<String, Double>? = null

        if (yearMode) {
            val requestedYear = finalParsedPeriod.year
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

            val (currentIncomeByCat, currentExpenseByCat, prevIncomeByCatAsync, prevExpenseByCatAsync) = coroutineScope {
                val currentInc = async { statisticsRepository.getCategoryTotals(userId, currentStart, currentEnd, accountId, true) }
                val currentExp = async { statisticsRepository.getCategoryTotals(userId, currentStart, currentEnd, accountId, false) }
                val prevInc = async { statisticsRepository.getCategoryTotals(userId, prevStart, prevEnd, accountId, true) }
                val prevExp = async { statisticsRepository.getCategoryTotals(userId, prevStart, prevEnd, accountId, false) }
                
                listOf(currentInc.await(), currentExp.await(), prevInc.await(), prevExp.await())
            }

            prevIncomeByCat = prevIncomeByCatAsync
            prevExpenseByCat = prevExpenseByCatAsync

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

        val incomeTxns = txnsForHighlights.filter { it.isIncome }
        val expenseTxns = txnsForHighlights.filter { !it.isIncome }

        fun highestMonth(txns: List<Transaction>) =
            txns.groupBy {
                TimePeriod.fromInstant(it.dateTime, "month").toString()
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
        if (yearMode && expenseProjectedTotal != null) {
            val budgets = budgetRepository.getAllByUser(userId, accountId, limit = Int.MAX_VALUE, offset = 0).filter { it.isExpense }
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

        val correlationInsights = if (monthMode && finalParsedPeriod is TimePeriod.Month) {
            val hCurrentStart = LocalDate(finalParsedPeriod.year, finalParsedPeriod.month, 1)

            val historicalStart = hCurrentStart.minus(DatePeriod(months = 6))
            val historicalEnd = hCurrentStart.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))

            val hTxns = statisticsRepository.getTransactions(
                userId, accountId, null,
                historicalStart.atStartOfDayIn(TimeZone.UTC),
                historicalEnd.atTime(23, 59, 59, 999_999_999).toInstant(TimeZone.UTC)
            )

            val monthlyData = hTxns.groupBy {
                TimePeriod.fromInstant(it.dateTime, "month").toString()
            }.mapValues { (_, mTxns) ->
                val inc = mTxns.asSequence().filter { it.isIncome }.sumOf { it.totalAmount }
                val exp = mTxns.asSequence().filter { !it.isIncome }.groupBy { it.category.trim().lowercase() }
                    .mapValues { it.value.sumOf { t -> t.totalAmount } }
                Pair(inc, exp)
            }.toSortedMap()

            val targetCats = setOf("shopping", "entertainment", "dining out")
            val keys = monthlyData.keys.toList()
            buildList {
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
                                    add(
                                        CorrelationDto(
                                            source = "Income",
                                            target = cat.replaceFirstChar { it.uppercase() },
                                            insight = "When your income increases by $incPct%, your '${cat.replaceFirstChar { it.uppercase() }}' spend tends to increase by $expPct% in the same month."
                                        )
                                    )
                                } else if (currExp > 0 && nextExp > 0 && (nextExp - currExp) / currExp > 0.15) {
                                    val incPct = (incomeIncrease * 100).toInt()
                                    val expPct = (((nextExp - currExp) / currExp) * 100).toInt()
                                    add(
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
        } else emptyList()

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

        return Result.Success(StatisticsSummaryDto(
            period = targetPeriodString ?: "",
            isCurrent = isCurrent,
            income = incomeTxns.sumOf { it.totalAmount },
            expense = expenseTxns.sumOf { it.totalAmount },
            balance = incomeTxns.sumOf { it.totalAmount } - expenseTxns.sumOf { it.totalAmount },
            incomeHighlights = incomeHighlights,
            expenseHighlights = expenseHighlights,
            totalTransactionCost = txnsForHighlights.sumOf { it.transactionCost }
        ))
    }

    override suspend fun getDistributionSummary(
        userId: UUID,
        period: String,
        accountId: UUID?,
        isIncome: Boolean?,
        start: Instant?,
        end: Instant?
    ): Result<DistributionSummaryDto> = coroutineScope {
        val parsedPeriod = try {
            TimePeriod.parse(period)
        } catch (e: Exception) {
            return@coroutineScope Result.Failure(AppError.Validation("Period must be in format: YYYY-Www, YYYY-MM, or YYYY"))
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

        val txnsInPeriod = filtered.filter { parsedPeriod.matches(it.dateTime) }

        val (currentMonthStart, _) = parsedPeriod.toDateRange()

        val historicalCounts = if (parsedPeriod is TimePeriod.Month) {
            val historicalStart = currentMonthStart.minus(DatePeriod(months = 6))
            val historicalEnd = currentMonthStart.minus(DatePeriod(days = 1))

            val histTxns = statisticsRepository.getTransactions(
                userId, accountId, isIncome,
                historicalStart.atStartOfDayIn(TimeZone.UTC),
                historicalEnd.atTime(23, 59, 59, 999_999_999).toInstant(TimeZone.UTC)
            )

            histTxns.groupBy { it.category.trim().lowercase() }
                .mapValues { (_, txns) ->
                    val monthsWithData = txns.groupBy {
                        TimePeriod.fromInstant(it.dateTime, "month").toString()
                    }.size
                    txns.size.toDouble() / monthsWithData.coerceAtLeast(1)
                }
        } else null

        val previousMonthsData = if (parsedPeriod is TimePeriod.Month) {
            val m1Start = currentMonthStart.minus(DatePeriod(months = 1))
            val m1End = currentMonthStart.minus(DatePeriod(days = 1))
            val m2Start = currentMonthStart.minus(DatePeriod(months = 2))
            val m2End = m1Start.minus(DatePeriod(days = 1))

            val totals1Deferred = async { statisticsRepository.getCategoryTotals(userId, m1Start, m1End, accountId, isIncome) }
            val totals2Deferred = async { statisticsRepository.getCategoryTotals(userId, m2Start, m2End, accountId, isIncome) }
            
            val totals1 = totals1Deferred.await().mapKeys { it.key.trim().lowercase() }
            val totals2 = totals2Deferred.await().mapKeys { it.key.trim().lowercase() }
            totals1 to totals2
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

            val otherCategories = sortedCategories.drop(5)

            if (otherCategories.isNotEmpty()) {
                val othersTxns = otherCategories.flatMap { it.second }
                othersInsight = othersTxns
                    .mapNotNull { txn ->
                        if (MerchantInsightUtils.isDescriptionMeaningful(txn.description, txn.category)) {
                            MerchantInsightUtils.cleanMerchantName(txn.description!!) to txn.totalAmount
                        } else null
                    }
                    .groupBy({ it.first }, { it.second })
                    .mapValues { it.value.sum() }
                    .maxByOrNull { it.value }
                    ?.key
            }

            val deduped = sortedCategories.map { (key, list, sum) ->
                val displayName = list.first().category

                val count = list.size
                val momentum = if (parsedPeriod is TimePeriod.Month && previousMonthsData != null) {
                    val prev1 = previousMonthsData.first[key] ?: 0.0
                    val prev2 = previousMonthsData.second[key] ?: 0.0
                    when {
                        sum > prev1 && prev1 > prev2 -> "UP"
                        sum < prev1 && prev1 < prev2 -> "DOWN"
                        else -> "STABLE"
                    }
                } else null

                val insights = list
                    .mapNotNull { txn ->
                        if (MerchantInsightUtils.isDescriptionMeaningful(txn.description, displayName)) {
                            MerchantInsightUtils.cleanMerchantName(txn.description!!) to txn.totalAmount
                        } else null
                    }
                    .groupBy({ it.first }, { it.second })
                    .mapValues { it.value.sum() }
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

        Result.Success(DistributionSummaryDto(
            period = period,
            totalTransactionCost = txnsInPeriod.sumOf { it.transactionCost },
            incomeCategories = incomeCategoriesFinal,
            expenseCategories = expenseCategoriesFinal,
            othersInsightSummary = othersInsight
        ))
    }

    override suspend fun getAvailableWeeks(userId: UUID, accountId: UUID?): Result<AvailableWeeksDto> =
        Result.Success(AvailableWeeksDto(statisticsRepository.getAvailablePeriods(userId, accountId, "weeks")))

    override suspend fun getAvailableMonths(userId: UUID, accountId: UUID?): Result<AvailableMonthsDto> =
        Result.Success(AvailableMonthsDto(statisticsRepository.getAvailablePeriods(userId, accountId, "months")))

    override suspend fun getAvailableYears(userId: UUID, accountId: UUID?): Result<AvailableYearsDto> =
        Result.Success(AvailableYearsDto(statisticsRepository.getAvailablePeriods(userId, accountId, "years")))

    override suspend fun getOverviewSummary(userId: UUID, accountId: UUID?): Result<OverviewSummaryDto> = coroutineScope {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        val currentPeriod = TimePeriod.Month(now.year, now.monthNumber)
        val currentMonthCode = currentPeriod.toString()

        val availableMonths = statisticsRepository.getAvailablePeriods(userId, accountId, "months")
        val targetMonthCode = availableMonths.firstOrNull() ?: currentMonthCode
        val isCurrent = targetMonthCode == currentMonthCode

        val parsedPeriod = TimePeriod.parse(targetMonthCode)
        val (monthStart, monthEnd) = parsedPeriod.toDateRange()

        val trendEnd = if (isCurrent) now else monthEnd
        val weeklyStart = trendEnd.minus(DatePeriod(days = 6))
        val monthlyStart = trendEnd.minus(DatePeriod(days = 29))

        val weeklyDeferred = async { getDaySummaries(userId, weeklyStart, trendEnd, accountId) }
        val monthlyDeferred = async { getDaySummaries(userId, monthlyStart, trendEnd, accountId) }

        val weeklyResult = weeklyDeferred.await()
        val monthlyResult = monthlyDeferred.await()

        Result.Success(OverviewSummaryDto(
            period = targetMonthCode,
            isCurrent = isCurrent,
            weeklyOverview = (weeklyResult as Result.Success).value,
            monthlyOverview = (monthlyResult as Result.Success).value
        ))
    }

    override suspend fun getDaySummaries(
        userId: UUID,
        start: LocalDate,
        end: LocalDate,
        accountId: UUID?
    ): Result<List<DaySummaryDto>> {
        val transactions = statisticsRepository.getTransactionsByDateRange(userId, start, end, accountId)
        val dates = generateSequence(start) { current ->
            if (current < end) current.plus(DatePeriod(days = 1)) else null
        }.toList()

        val summaries = dates.map { date ->
            val dayTxs = transactions.filter { it.dateTime.toLocalDateTime(TimeZone.UTC).date == date }
            val income = dayTxs.filter { it.isIncome }.sumOf { it.totalAmount }
            val expense = dayTxs.filter { !it.isIncome }.sumOf { it.totalAmount }
            DaySummaryDto(date = date, income = income, expense = expense)
        }
        return Result.Success(summaries)
    }

    override suspend fun getCategoryComparisons(
        userId: UUID,
        accountId: UUID?,
        period: String?
    ): Result<CategoryComparisonSummaryDto> = coroutineScope {
        log.withContext("userId" to userId, "accountId" to accountId, "period" to period)
            .debug { "Calculating category comparisons with weekly data" }

        val availableMonths = statisticsRepository.getAvailablePeriods(userId, accountId, "months")
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        val currentPeriod = TimePeriod.Month(now.year, now.monthNumber)
        val currentMonthCode = currentPeriod.toString()

        val targetPeriodString = period ?: availableMonths.firstOrNull() ?: currentMonthCode
        val isCurrent = targetPeriodString == currentMonthCode
        val isBackupMonth = period == null && !isCurrent

        val parsedPeriod = TimePeriod.parse(targetPeriodString)
        val (currentMonthStart, currentMonthEnd) = parsedPeriod.toDateRange()
        val previousMonthStart = currentMonthStart.minus(DatePeriod(months = 1))
        val previousMonthEnd = currentMonthStart.minus(DatePeriod(days = 1))

        val thisWeekEnd = if (isCurrent) now else currentMonthEnd
        val thisWeekStart = thisWeekEnd.minus(DatePeriod(days = 6))
        val lastWeekStart = thisWeekStart.minus(DatePeriod(days = 7))
        val lastWeekEnd = thisWeekStart.minus(DatePeriod(days = 1))

        val tmetDeferred = async { statisticsRepository.getCategoryTotals(userId, currentMonthStart, currentMonthEnd, accountId, isIncome = false) }
        val lmetDeferred = async { statisticsRepository.getCategoryTotals(userId, previousMonthStart, previousMonthEnd, accountId, isIncome = false) }
        val tmitDeferred = async { statisticsRepository.getCategoryTotals(userId, currentMonthStart, currentMonthEnd, accountId, isIncome = true) }
        val lmitDeferred = async { statisticsRepository.getCategoryTotals(userId, previousMonthStart, previousMonthEnd, accountId, isIncome = true) }
        val twtDeferred = async { statisticsRepository.getCategoryTotals(userId, thisWeekStart, thisWeekEnd, accountId) }
        val lwtDeferred = async { statisticsRepository.getCategoryTotals(userId, lastWeekStart, lastWeekEnd, accountId) }

        val thisMonthExpenseTotals = tmetDeferred.await()
        val lastMonthExpenseTotals = lmetDeferred.await()
        val thisMonthIncomeTotals = tmitDeferred.await()
        val lastMonthIncomeTotals = lmitDeferred.await()
        val thisWeekTotals = twtDeferred.await()
        val lastWeekTotals = lwtDeferred.await()

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
                        period = targetPeriodString,
                        weeklyCurrentTotal = weekCur,
                        weeklyChangePercentage = calculateChange(weekCur, weekPrev)
                    )
                }
            }
        } else {
            val topExpenseCategory = thisMonthExpenseTotals.keys.asSequence()
                .filter { it != "Transaction Fees" && it != "Transaction Cost" }
                .maxByOrNull { thisMonthExpenseTotals[it] ?: 0.0 }
                ?: lastMonthExpenseTotals.keys.asSequence()
                .filter { it != "Transaction Fees" && it != "Transaction Cost" }
                .maxByOrNull { lastMonthExpenseTotals[it] ?: 0.0 }

            val topIncomeCategory = thisMonthIncomeTotals.keys.asSequence()
                .maxByOrNull { thisMonthIncomeTotals[it] ?: 0.0 }
                ?: lastMonthIncomeTotals.keys.asSequence()
                .maxByOrNull { lastMonthIncomeTotals[it] ?: 0.0 }

            buildList {
                val incomeCategory = topIncomeCategory ?: "Salary"
                val curInc = thisMonthIncomeTotals[incomeCategory] ?: 0.0
                val prevInc = lastMonthIncomeTotals[incomeCategory] ?: 0.0
                val weekCurInc = thisWeekTotals[incomeCategory] ?: 0.0
                val weekPrevInc = lastWeekTotals[incomeCategory] ?: 0.0
                
                add(CategoryComparisonDto(
                    category = incomeCategory,
                    currentTotal = curInc,
                    previousTotal = prevInc,
                    changePercentage = calculateChange(curInc, prevInc),
                    isIncome = true,
                    period = targetPeriodString,
                    weeklyCurrentTotal = weekCurInc,
                    weeklyChangePercentage = calculateChange(weekCurInc, weekPrevInc)
                ))

                if (topExpenseCategory != null) {
                    val curExp = thisMonthExpenseTotals[topExpenseCategory] ?: 0.0
                    val prevExp = lastMonthExpenseTotals[topExpenseCategory] ?: 0.0
                    val weekCurExp = thisWeekTotals[topExpenseCategory] ?: 0.0
                    val weekPrevExp = lastWeekTotals[topExpenseCategory] ?: 0.0
                    
                    add(CategoryComparisonDto(
                        category = topExpenseCategory,
                        currentTotal = curExp,
                        previousTotal = prevExp,
                        changePercentage = calculateChange(curExp, prevExp),
                        isIncome = false,
                        period = targetPeriodString,
                        weeklyCurrentTotal = weekCurExp,
                        weeklyChangePercentage = calculateChange(weekCurExp, weekPrevExp)
                    ))
                } else {
                    add(calculateTransactionCostComparison(
                        userId, accountId, currentMonthStart, currentMonthEnd, previousMonthStart, previousMonthEnd, 
                        thisWeekStart, thisWeekEnd, lastWeekStart, lastWeekEnd
                    ))
                }
            }
        }

        Result.Success(CategoryComparisonSummaryDto(
            period = targetPeriodString,
            isCurrent = isCurrent,
            data = comparisons
        ))
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
        userId: UUID, accountId: UUID?, isIncome: Boolean?, categoryIds: List<UUID>?, hasCost: Boolean?, start: Instant?, end: Instant?
    ): Result<TransactionCountSummaryDto> {
        if (accountId == null) return Result.Failure(AppError.Validation("Account ID is required"))
        val counts = statisticsRepository.getTransactionCounts(userId, accountId, isIncome, categoryIds, hasCost, start, end)
        return Result.Success(TransactionCountSummaryDto(counts.incomeCount, counts.expenseCount, counts.totalCount, counts.totalTransactionCost))
    }

    override suspend fun getProfileMetrics(userId: UUID): Result<ProfileMetricsDto> {
        val user = userRepository.findById(userId) ?: return Result.Failure(AppError.NotFound("User not found"))
        val accountsResult = accountService.getAllAccounts(userId)
        if (accountsResult is Result.Failure) return Result.Failure(accountsResult.error)
        val accounts = (accountsResult as Result.Success).value
        
        val netWorth = accounts.sumOf { it.balance ?: 0.0 }

        // Use global summaries from repository instead of fetching ALL transactions
        val incomeTotals = statisticsRepository.getCategoryTotals(userId, null, null, null, isIncome = true)
        val expenseTotals = statisticsRepository.getCategoryTotals(userId, null, null, null, isIncome = false)

        val totalIncome = incomeTotals.values.sum()
        val totalExpense = expenseTotals.values.sum()

        val savingsRate = if (totalIncome > 0) ((totalIncome - totalExpense) / totalIncome) * 100 else null

        val essentialSpend = expenseTotals
            .filter { (cat, _) -> essentialCategories.any { it.equals(cat.trim(), ignoreCase = true) } }
            .values.sum()
        val essentialSpendRatio = if (totalExpense > 0) (essentialSpend / totalExpense) * 100 else null

        return Result.Success(ProfileMetricsDto(
            name = user.name,
            email = user.email,
            netWorth = netWorth,
            savingsRate = savingsRate,
            essentialSpendRatio = essentialSpendRatio
        ))
    }

    override suspend fun getDaySummariesByDateRange(
        userId: UUID, accountId: UUID?, startParam: String?, endParam: String?
    ): Result<List<DaySummaryDto>> {
        return when (val rangeResult = parseDateRange(startParam, endParam)) {
            is Result.Success -> {
                val range = rangeResult.value
                val start = range.first?.toLocalDateTime(TimeZone.UTC)?.date ?: return Result.Failure(AppError.Validation("Invalid start date"))
                val end = range.second?.toLocalDateTime(TimeZone.UTC)?.date ?: return Result.Failure(AppError.Validation("Invalid end date"))
                getDaySummaries(userId, start, end, accountId)
            }
            is Result.Failure -> Result.Failure(rangeResult.error)
        }
    }

    override fun parseTypeFilter(typeFilter: String?): Boolean? = when (typeFilter?.lowercase()) {
        "income" -> true
        "expense" -> false
        else -> null
    }

    override fun parseDateRange(startDate: String?, endDate: String?): Result<Pair<Instant?, Instant?>> {
        return try {
            val start = startDate?.let { LocalDate.parse(it).atTime(LocalTime(0, 0, 0)).toInstant(TimeZone.UTC) }
            val end = endDate?.let { LocalDate.parse(it).atTime(LocalTime(23, 59, 59, 999_999_999)).toInstant(TimeZone.UTC) }
            if (start != null && end != null && start > end) {
                Result.Failure(AppError.Validation("Start date cannot be after end date"))
            } else {
                Result.Success(start to end)
            }
        } catch (e: Exception) {
            Result.Failure(AppError.Validation("Invalid date format. Use YYYY-MM-DD"))
        }
    }
}
