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
import java.math.BigDecimal
import java.math.RoundingMode
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
        val yearMode = finalParsedPeriod is TimePeriod.Year
        val monthMode = finalParsedPeriod is TimePeriod.Month

        log.withContext(
            "userId" to userId,
            "transactionCount" to txnsForHighlights.size,
            "targetPeriod" to targetPeriodString,
            "isCurrent" to isCurrent,
            "yearMode" to yearMode
        ).debug { "Retrieved transactions for statistics highlights" }

        val yearlyMetrics = if (yearMode) {
            calculateYearlyMetrics(userId, accountId, finalParsedPeriod.year)
        } else null

        val incomeTxns = txnsForHighlights.filter { it.isIncome }
        val expenseTxns = txnsForHighlights.filter { !it.isIncome }

        val totalIncome = incomeTxns.fold(BigDecimal.ZERO) { acc, t -> acc + t.totalAmount }
        val totalExpense = expenseTxns.fold(BigDecimal.ZERO) { acc, t -> acc + t.totalAmount }

        val savingsRate = if (totalIncome > BigDecimal.ZERO) {
            (totalIncome - totalExpense).divide(totalIncome, 4, RoundingMode.HALF_UP).toDouble() * 100
        } else null

        val essentialSpend = expenseTxns
            .filter { txn -> essentialCategories.any { it.equals(txn.category.trim(), ignoreCase = true) } }
            .fold(BigDecimal.ZERO) { acc, t -> acc + t.totalAmount }
        val essentialSpendRatio = if (totalExpense > BigDecimal.ZERO) {
            essentialSpend.divide(totalExpense, 4, RoundingMode.HALF_UP).toDouble() * 100
        } else null

        val projectedExceedMonth = if (yearMode && yearlyMetrics?.expenseProjectedTotal != null) {
            calculateProjectedExceedMonth(userId, accountId, totalExpense, yearlyMetrics.expenseProjectedTotal)
        } else null

        val incomeHighlights = assembleHighlights(
            txns = incomeTxns,
            prevTotals = yearlyMetrics?.prevIncomeByCat,
            ytdChange = yearlyMetrics?.ytdIncomeChange,
            projectedTotal = yearlyMetrics?.incomeProjectedTotal,
            savingsRate = savingsRate
        )

        val correlationInsights = if (monthMode) {
            calculateCorrelations(userId, accountId, finalParsedPeriod)
        } else emptyList()

        val expenseHighlights = assembleHighlights(
            txns = expenseTxns,
            prevTotals = yearlyMetrics?.prevExpenseByCat,
            ytdChange = yearlyMetrics?.ytdExpenseChange,
            projectedTotal = yearlyMetrics?.expenseProjectedTotal,
            essentialSpendRatio = essentialSpendRatio,
            projectedExceedMonth = projectedExceedMonth,
            correlations = correlationInsights.distinctBy { it.target }.take(3)
        )

        return Result.Success(StatisticsSummaryDto(
            period = targetPeriodString ?: "",
            isCurrent = isCurrent,
            income = totalIncome,
            expense = totalExpense,
            balance = totalIncome - totalExpense,
            incomeHighlights = incomeHighlights,
            expenseHighlights = expenseHighlights,
            totalTransactionCost = txnsForHighlights.fold(BigDecimal.ZERO) { acc, t -> acc + t.transactionCost }
        ))
    }

    private suspend fun calculateYearlyMetrics(
        userId: UUID,
        accountId: UUID?,
        requestedYear: Int
    ): YearlyMetrics {
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

        val (currentIncByCat, currentExpByCat, prevIncByCat, prevExpByCat) = coroutineScope {
            val cInc = async { statisticsRepository.getCategoryTotals(userId, currentStart, currentEnd, accountId, true) }
            val cExp = async { statisticsRepository.getCategoryTotals(userId, currentStart, currentEnd, accountId, false) }
            val pInc = async { statisticsRepository.getCategoryTotals(userId, prevStart, prevEnd, accountId, true) }
            val pExp = async { statisticsRepository.getCategoryTotals(userId, prevStart, prevEnd, accountId, false) }

            listOf(cInc.await(), cExp.await(), pInc.await(), pExp.await())
        }

        val currentIncomeTotal = currentIncByCat.values.fold(BigDecimal.ZERO, BigDecimal::add)
        val currentExpenseTotal = currentExpByCat.values.fold(BigDecimal.ZERO, BigDecimal::add)
        val prevIncomeTotal = prevIncByCat.values.fold(BigDecimal.ZERO, BigDecimal::add)
        val prevExpenseTotal = prevExpByCat.values.fold(BigDecimal.ZERO, BigDecimal::add)

        val incomeProjectedTotal = if (isCurrentYear) {
            currentIncomeTotal.multiply(BigDecimal.valueOf(12))
                .divide(BigDecimal.valueOf(now.monthNumber.toLong().coerceAtLeast(1)), 2, RoundingMode.HALF_UP)
        } else null

        val expenseProjectedTotal = if (isCurrentYear) {
            currentExpenseTotal.multiply(BigDecimal.valueOf(12))
                .divide(BigDecimal.valueOf(now.monthNumber.toLong().coerceAtLeast(1)), 2, RoundingMode.HALF_UP)
        } else null

        return YearlyMetrics(
            ytdIncomeChange = calculateChange(currentIncomeTotal, prevIncomeTotal),
            ytdExpenseChange = calculateChange(currentExpenseTotal, prevExpenseTotal),
            incomeProjectedTotal = incomeProjectedTotal,
            expenseProjectedTotal = expenseProjectedTotal,
            prevIncomeByCat = prevIncByCat,
            prevExpenseByCat = prevExpByCat
        )
    }

    private suspend fun calculateProjectedExceedMonth(
        userId: UUID,
        accountId: UUID?,
        totalExpense: BigDecimal,
        expenseProjectedTotal: BigDecimal
    ): String? {
        val budgets = budgetRepository.getAllByUser(userId, accountId, limit = Int.MAX_VALUE, offset = 0).filter { it.isExpense }
        val totalMonthlyLimit = budgets.fold(BigDecimal.ZERO) { acc, b -> acc + b.limit }
        val yearlyLimit = totalMonthlyLimit.multiply(BigDecimal.valueOf(12))

        if (expenseProjectedTotal > yearlyLimit && yearlyLimit > BigDecimal.ZERO) {
            val currentMonth = Clock.System.now().toLocalDateTime(TimeZone.UTC).monthNumber
            val averagePerMonth = totalExpense.divide(BigDecimal.valueOf(currentMonth.toLong()), 4, RoundingMode.HALF_UP)

            for (m in (currentMonth + 1)..12) {
                val accumulatedSpend = totalExpense + averagePerMonth.multiply(BigDecimal.valueOf((m - currentMonth).toLong()))
                if (accumulatedSpend > yearlyLimit) {
                    return monthNames.getOrNull(m - 1)
                }
            }
        }
        return null
    }

    private fun assembleHighlights(
        txns: List<Transaction>,
        prevTotals: Map<String, BigDecimal>? = null,
        ytdChange: Double? = null,
        projectedTotal: BigDecimal? = null,
        savingsRate: Double? = null,
        essentialSpendRatio: Double? = null,
        projectedExceedMonth: String? = null,
        correlations: List<CorrelationDto>? = null
    ): HighlightsDto {
        return HighlightsDto(
            highestMonth = calculateHighestMonth(txns),
            highestCategory = calculateHighestCategory(txns, prevTotals),
            highestDay = calculateHighestDay(txns),
            averagePerDay = calculateAveragePerDay(txns),
            ytdChangePercentage = ytdChange,
            projectedTotal = projectedTotal,
            savingsRate = savingsRate,
            essentialSpendRatio = essentialSpendRatio,
            projectedExceedMonth = projectedExceedMonth,
            correlations = correlations
        )
    }

    private fun calculateHighestMonth(txns: List<Transaction>): HighlightDto? =
        txns.groupBy { TimePeriod.fromInstant(it.dateTime, "month").toString() }
            .mapValues { it.value.fold(BigDecimal.ZERO) { acc, t -> acc + t.totalAmount } }
            .maxByOrNull { it.value }
            ?.let { HighlightDto(label = it.key, value = it.key, amount = it.value) }

    private fun calculateHighestCategory(txns: List<Transaction>, prevTotals: Map<String, BigDecimal>?): HighlightDto? =
        txns.groupBy { it.category.trim().lowercase() }
            .mapValues { it.value.fold(BigDecimal.ZERO) { acc, t -> acc + t.totalAmount } }
            .maxByOrNull { it.value }
            ?.let { entry ->
                val displayName = txns.firstOrNull { it.category.trim().lowercase() == entry.key }?.category ?: entry.key
                val volatility = prevTotals?.let { totals ->
                    val prevAmount = totals.entries.find { it.key.trim().lowercase() == entry.key }?.value ?: BigDecimal.ZERO
                    calculateChange(entry.value, prevAmount)
                }
                HighlightDto(label = displayName, value = displayName, amount = entry.value, volatilityPercentage = volatility)
            }

    private fun calculateHighestDay(txns: List<Transaction>): HighlightDto? =
        txns.groupBy { it.dateTime.toLocalDateTime(TimeZone.UTC).date }
            .mapValues { it.value.fold(BigDecimal.ZERO) { acc, t -> acc + t.totalAmount } }
            .maxByOrNull { it.value }
            ?.let { HighlightDto(label = it.key.toString(), value = it.key.toString(), amount = it.value) }

    private fun calculateAveragePerDay(txns: List<Transaction>): Double {
        val days = txns.groupBy { it.dateTime.toLocalDateTime(TimeZone.UTC).date }.size.coerceAtLeast(1)
        val total = txns.fold(BigDecimal.ZERO) { acc, t -> acc + t.totalAmount }
        return total.divide(BigDecimal.valueOf(days.toLong()), 4, RoundingMode.HALF_UP).toDouble()
    }

    private suspend fun calculateCorrelations(
        userId: UUID,
        accountId: UUID?,
        period: TimePeriod.Month
    ): List<CorrelationDto> {
        val hCurrentStart = LocalDate(period.year, period.month, 1)
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
            val inc = mTxns.asSequence().filter { it.isIncome }.fold(BigDecimal.ZERO) { acc, t -> acc + t.totalAmount }
            val exp = mTxns.asSequence().filter { !it.isIncome }.groupBy { it.category.trim().lowercase() }
                .mapValues { it.value.fold(BigDecimal.ZERO) { acc, t -> acc + t.totalAmount } }
            Pair(inc, exp)
        }.toSortedMap()

        val targetCats = setOf("shopping", "entertainment", "dining out")
        val keys = monthlyData.keys.toList()
        return buildList {
            for (i in 1 until keys.size) {
                val prevMonth = monthlyData[keys[i - 1]]!!
                val currMonth = monthlyData[keys[i]]!!
                val nextMonth = if (i + 1 < keys.size) monthlyData[keys[i + 1]] else null

                if (prevMonth.first > BigDecimal.ZERO) {
                    val incomeIncrease = (currMonth.first - prevMonth.first).divide(prevMonth.first, 4, RoundingMode.HALF_UP).toDouble()
                    if (incomeIncrease > 0.10) {
                        targetCats.forEach { cat ->
                            val prevExp = prevMonth.second[cat] ?: BigDecimal.ZERO
                            val currExp = currMonth.second[cat] ?: BigDecimal.ZERO
                            val nextExp = nextMonth?.second?.get(cat) ?: BigDecimal.ZERO

                            if (prevExp > BigDecimal.ZERO && (currExp - prevExp).divide(prevExp, 4, RoundingMode.HALF_UP).toDouble() > 0.15) {
                                val incPct = (incomeIncrease * 100).toInt()
                                val expPct = ((currExp - prevExp).divide(prevExp, 4, RoundingMode.HALF_UP).toDouble() * 100).toInt()
                                add(CorrelationDto(
                                    source = "Income",
                                    target = cat.replaceFirstChar { it.uppercase() },
                                    insight = "When your income increases by $incPct%, your '${cat.replaceFirstChar { it.uppercase() }}' spend tends to increase by $expPct% in the same month."
                                ))
                            } else if (currExp > BigDecimal.ZERO && nextExp > BigDecimal.ZERO && (nextExp - currExp).divide(currExp, 4, RoundingMode.HALF_UP).toDouble() > 0.15) {
                                val incPct = (incomeIncrease * 100).toInt()
                                val expPct = ((nextExp - currExp).divide(currExp, 4, RoundingMode.HALF_UP).toDouble() * 100).toInt()
                                add(CorrelationDto(
                                    source = "Income",
                                    target = cat.replaceFirstChar { it.uppercase() },
                                    insight = "Following a $incPct% income increase, your '${cat.replaceFirstChar { it.uppercase() }}' spend tended to increase by $expPct% the next month."
                                ))
                            }
                        }
                    }
                }
            }
        }
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
            val totalAmountAll = txns.fold(BigDecimal.ZERO) { acc, t -> acc + t.totalAmount }
            val grouped = txns.groupBy { it.category.trim().lowercase() }
            
            val sortedCategories = grouped.map { (key, list) ->
                val sum = list.fold(BigDecimal.ZERO) { acc, t -> acc + t.totalAmount }
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
                    .mapValues { it.value.fold(BigDecimal.ZERO, BigDecimal::add) }
                    .maxByOrNull { it.value }
                    ?.key
            }

            val deduped = sortedCategories.map { (key, list, sum) ->
                val displayName = list.first().category

                val count = list.size
                val momentum = if (parsedPeriod is TimePeriod.Month && previousMonthsData != null) {
                    val prev1 = previousMonthsData.first[key] ?: BigDecimal.ZERO
                    val prev2 = previousMonthsData.second[key] ?: BigDecimal.ZERO
                    when {
                        sum.compareTo(prev1) > 0 && prev1.compareTo(prev2) > 0 -> "UP"
                        sum.compareTo(prev1) < 0 && prev1.compareTo(prev2) < 0 -> "DOWN"
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
                    .mapValues { it.value.fold(BigDecimal.ZERO, BigDecimal::add) }
                    .entries
                    .sortedByDescending { it.value }
                    .take(3)
                    .map { it.key }

                CategorySummaryDto(
                    category = displayName,
                    total = sum,
                    percentage = if (totalAmountAll.compareTo(BigDecimal.ZERO) > 0) {
                        sum.divide(totalAmountAll, 4, RoundingMode.HALF_UP).toDouble() * 100
                    } else 0.0,
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
            totalTransactionCost = txnsInPeriod.fold(BigDecimal.ZERO) { acc, t -> acc + t.transactionCost },
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
            val income = dayTxs.filter { it.isIncome }.fold(BigDecimal.ZERO) { acc, t -> acc + t.totalAmount }
            val expense = dayTxs.filter { !it.isIncome }.fold(BigDecimal.ZERO) { acc, t -> acc + t.totalAmount }
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

                    val cur = currentTotals[category] ?: BigDecimal.ZERO
                    val prev = previousTotals[category] ?: BigDecimal.ZERO
                    val weekCur = thisWeekTotals[category] ?: BigDecimal.ZERO
                    val weekPrev = lastWeekTotals[category] ?: BigDecimal.ZERO

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
            val topExpenseCategory = thisMonthExpenseTotals.keys
                .filter { it != "Transaction Fees" && it != "Transaction Cost" }
                .maxByOrNull { thisMonthExpenseTotals[it] ?: BigDecimal.ZERO }
                ?: lastMonthExpenseTotals.keys
                .filter { it != "Transaction Fees" && it != "Transaction Cost" }
                .maxByOrNull { lastMonthExpenseTotals[it] ?: BigDecimal.ZERO }

            val topIncomeCategory = thisMonthIncomeTotals.keys
                .maxByOrNull { thisMonthIncomeTotals[it] ?: BigDecimal.ZERO }
                ?: lastMonthIncomeTotals.keys
                .maxByOrNull { lastMonthIncomeTotals[it] ?: BigDecimal.ZERO }

            buildList {
                val incomeCategory = topIncomeCategory ?: "Salary"
                val curInc = thisMonthIncomeTotals[incomeCategory] ?: BigDecimal.ZERO
                val prevInc = lastMonthIncomeTotals[incomeCategory] ?: BigDecimal.ZERO
                val weekCurInc = thisWeekTotals[incomeCategory] ?: BigDecimal.ZERO
                val weekPrevInc = lastWeekTotals[incomeCategory] ?: BigDecimal.ZERO
                
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
                    val curExp = thisMonthExpenseTotals[topExpenseCategory] ?: BigDecimal.ZERO
                    val prevExp = lastMonthExpenseTotals[topExpenseCategory] ?: BigDecimal.ZERO
                    val weekCurExp = thisWeekTotals[topExpenseCategory] ?: BigDecimal.ZERO
                    val weekPrevExp = lastWeekTotals[topExpenseCategory] ?: BigDecimal.ZERO
                    
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

    private fun calculateChange(current: BigDecimal, previous: BigDecimal): Double =
        if (previous.compareTo(BigDecimal.ZERO) != 0) {
            (current - previous).divide(previous, 4, RoundingMode.HALF_UP).toDouble() * 100
        } else if (current.compareTo(BigDecimal.ZERO) > 0) {
            100.0
        } else {
            0.0
        }

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

        val curCost = curTxns.fold(BigDecimal.ZERO) { acc, t -> acc + t.transactionCost }
        val prevCost = prevTxns.fold(BigDecimal.ZERO) { acc, t -> acc + t.transactionCost }

        var weekCurCost = BigDecimal.ZERO
        var weekPrevCost = BigDecimal.ZERO
        if (thisWeekStart != null && thisWeekEnd != null && lastWeekStart != null && lastWeekEnd != null) {
            weekCurCost = statisticsRepository.getTransactionsByDateRange(userId, thisWeekStart, thisWeekEnd, accountId).fold(BigDecimal.ZERO) { acc, t -> acc + t.transactionCost }
            weekPrevCost = statisticsRepository.getTransactionsByDateRange(userId, lastWeekStart, lastWeekEnd, accountId).fold(BigDecimal.ZERO) { acc, t -> acc + t.transactionCost }
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
        
        val netWorth = accounts.fold(BigDecimal.ZERO) { acc, a -> acc + (a.balance ?: BigDecimal.ZERO) }

        // Use global summaries from repository instead of fetching ALL transactions
        val incomeTotals = statisticsRepository.getCategoryTotals(userId, null, null, null, isIncome = true)
        val expenseTotals = statisticsRepository.getCategoryTotals(userId, null, null, null, isIncome = false)

        val totalIncome = incomeTotals.values.fold(BigDecimal.ZERO, BigDecimal::add)
        val totalExpense = expenseTotals.values.fold(BigDecimal.ZERO, BigDecimal::add)

        val savingsRate = if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            (totalIncome - totalExpense).divide(totalIncome, 4, RoundingMode.HALF_UP).toDouble() * 100
        } else null

        val essentialSpend = expenseTotals
            .filter { (cat, _) -> essentialCategories.any { it.equals(cat.trim(), ignoreCase = true) } }
            .values.fold(BigDecimal.ZERO, BigDecimal::add)
        val essentialSpendRatio = if (totalExpense.compareTo(BigDecimal.ZERO) > 0) {
            essentialSpend.divide(totalExpense, 4, RoundingMode.HALF_UP).toDouble() * 100
        } else null

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

    private data class YearlyMetrics(
        val ytdIncomeChange: Double?,
        val ytdExpenseChange: Double?,
        val incomeProjectedTotal: BigDecimal?,
        val expenseProjectedTotal: BigDecimal?,
        val prevIncomeByCat: Map<String, BigDecimal>?,
        val prevExpenseByCat: Map<String, BigDecimal>?
    )
}
