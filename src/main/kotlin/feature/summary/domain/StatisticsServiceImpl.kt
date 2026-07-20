package feature.summary.domain

import com.fintrack.core.domain.AppError
import com.fintrack.core.domain.Result
import com.fintrack.core.domain.TimePeriod
import com.fintrack.core.logger
import com.fintrack.core.util.calculateAverage
import com.fintrack.core.util.calculatePercentageChange
import com.fintrack.core.util.calculateRatio
import com.fintrack.core.withContext
import com.fintrack.feature.accounts.domain.AccountService
import com.fintrack.feature.user.domain.UserRepository
import feature.budget.domain.BudgetRepository
import feature.category.domain.CategoryRepository
import feature.category.domain.model.CategoryConstants
import feature.summary.data.model.AvailableMonthsDto
import feature.summary.data.model.AvailableWeeksDto
import feature.summary.data.model.AvailableYearsDto
import feature.summary.data.model.CategoryComparisonDto
import feature.summary.data.model.CategoryComparisonSummaryDto
import feature.summary.data.model.CategorySummaryDto
import feature.summary.data.model.CorrelationDto
import feature.summary.data.model.DaySummaryDto
import feature.summary.data.model.DistributionSummaryDto
import feature.summary.data.model.HighlightDto
import feature.summary.data.model.HighlightsDto
import feature.summary.data.model.OverviewSummaryDto
import feature.summary.data.model.ProfileMetricsDto
import feature.summary.data.model.StatisticsSummaryDto
import feature.summary.data.model.TransactionCountSummaryDto
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

class StatisticsServiceImpl(
    private val statisticsRepository: StatisticsRepository,
    private val userRepository: UserRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val accountService: AccountService
) : StatisticsService {

    private val log = logger<StatisticsServiceImpl>()

    /**
     * Set of category names considered "Essential" for financial health benchmarks (Needs vs Wants).
     * Strings are used here rather than UUIDs to provide flexibility: it allows the logic to
     * automatically include both system-default categories and user-created categories that
     * match these semantic groups (e.g., a custom category named "Rent").
     */
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
    ): Result<StatisticsSummaryDto> = coroutineScope {
        log.withContext(
            "userId" to userId,
            "accountId" to accountId,
            "isIncome" to isIncome,
            "start" to start?.toString(),
            "end" to end?.toString(),
            "period" to period
        ).debug { "Calculating statistics summary" }

        var targetPeriodString = period
        var isCurrent = true
        var parsedPeriod = targetPeriodString?.let {
            try {
                TimePeriod.parse(it)
            } catch (e: Exception) {
                null
            }
        }

        var counts = getCountsForPeriod(userId, accountId, isIncome, start, end, parsedPeriod)

        if (counts.totalCount == 0 && targetPeriodString != null && parsedPeriod != null) {
            val periodType = when (parsedPeriod) {
                is TimePeriod.Week -> "weeks"
                is TimePeriod.Month -> "months"
                is TimePeriod.Year -> "years"
            }

            val availablePeriods =
                statisticsRepository.getAvailablePeriods(userId, accountId, periodType)
            val latestPeriod = availablePeriods.firstOrNull()
            if (latestPeriod != null && latestPeriod != targetPeriodString) {
                targetPeriodString = latestPeriod
                isCurrent = false
                parsedPeriod = TimePeriod.parse(latestPeriod)
                counts = getCountsForPeriod(userId, accountId, isIncome, start, end, parsedPeriod)
            }
        }

        val finalParsedPeriod = parsedPeriod
        val yearMode = finalParsedPeriod is TimePeriod.Year
        val monthMode = finalParsedPeriod is TimePeriod.Month
        val (pStart, pEnd) = finalParsedPeriod?.toDateRange() ?: (null to null)

        log.withContext(
            "userId" to userId,
            "transactionCount" to counts.totalCount,
            "targetPeriod" to targetPeriodString,
            "isCurrent" to isCurrent,
            "yearMode" to yearMode
        ).debug { "Retrieved transaction counts for statistics summary" }

        val yearlyMetrics = if (yearMode) {
            calculateYearlyMetrics(userId, accountId, finalParsedPeriod.year)
        } else null

        val (dailyTotals, incomeCategoryTotals, expenseCategoryTotals) = coroutineScope {
            val dt = async {
                if (pStart != null && pEnd != null)
                    statisticsRepository.getDailyTotals(userId, pStart, pEnd, accountId)
                else emptyMap()
            }
            val ict = async {
                statisticsRepository.getCategoryTotals(
                    userId,
                    pStart,
                    pEnd,
                    accountId,
                    true
                )
            }
            val ect = async {
                statisticsRepository.getCategoryTotals(
                    userId,
                    pStart,
                    pEnd,
                    accountId,
                    false
                )
            }
            Triple(dt.await(), ict.await(), ect.await())
        }

        val totalIncome = incomeCategoryTotals.values.fold(BigDecimal.ZERO, BigDecimal::add)
        
        // Requirement 2 & 1 Unified: totalExpense should represent all outflows
        // We take the sum of all expense amounts, subtract the 'Transaction Fees' category amount (to avoid double counting),
        // and then add the unified totalTransactionCost.
        val transactionFeesCategoryName = "Transaction Fees" // Fallback name
        val feeAmountFromCategory = expenseCategoryTotals.entries
            .find { it.key.equals(transactionFeesCategoryName, ignoreCase = true) }?.value ?: BigDecimal.ZERO
            
        val totalExpense = expenseCategoryTotals.values.fold(BigDecimal.ZERO, BigDecimal::add) - feeAmountFromCategory + counts.totalTransactionCost

        val savingsRate = (totalIncome - totalExpense).calculateRatio(totalIncome)

        val essentialSpend = expenseCategoryTotals
            .filter { (cat, _) ->
                essentialCategories.any {
                    it.equals(
                        cat.trim(),
                        ignoreCase = true
                    )
                }
            }
            .values.fold(BigDecimal.ZERO, BigDecimal::add)

        val essentialSpendRatio = essentialSpend.calculateRatio(totalExpense)

        val projectedExceedMonth = if (yearMode && yearlyMetrics?.expenseProjectedTotal != null) {
            calculateProjectedExceedMonth(
                userId,
                accountId,
                totalExpense,
                yearlyMetrics.expenseProjectedTotal
            )
        } else null

        val incomeHighlights = assembleHighlights(
            dailyTotals = dailyTotals,
            categoryTotals = incomeCategoryTotals,
            prevCategoryTotals = yearlyMetrics?.prevIncomeByCat,
            ytdChange = yearlyMetrics?.ytdIncomeChange,
            projectedTotal = yearlyMetrics?.incomeProjectedTotal,
            savingsRate = savingsRate,
            isIncome = true
        )

        val correlationInsights = if (monthMode) {
            calculateCorrelations(userId, accountId, finalParsedPeriod)
        } else emptyList()

        val expenseHighlights = assembleHighlights(
            dailyTotals = dailyTotals,
            categoryTotals = expenseCategoryTotals,
            prevCategoryTotals = yearlyMetrics?.prevExpenseByCat,
            ytdChange = yearlyMetrics?.ytdExpenseChange,
            projectedTotal = yearlyMetrics?.expenseProjectedTotal,
            essentialSpendRatio = essentialSpendRatio,
            projectedExceedMonth = projectedExceedMonth,
            correlations = correlationInsights.distinctBy { it.target }.take(3),
            isIncome = false
        )

        Result.Success(
            StatisticsSummaryDto(
                period = targetPeriodString ?: "",
                isCurrent = isCurrent,
                income = totalIncome,
                expense = totalExpense,
                balance = totalIncome - totalExpense,
                incomeHighlights = incomeHighlights,
                expenseHighlights = expenseHighlights,
                totalTransactionCost = counts.totalTransactionCost
            )
        )
    }

    private suspend fun getCountsForPeriod(
        userId: UUID,
        accountId: UUID?,
        isIncome: Boolean?,
        start: Instant?,
        end: Instant?,
        period: TimePeriod?
    ): TransactionCounts {
        val (pStart, pEnd) = period?.toDateRange() ?: (null to null)
        val finalStart = pStart?.atStartOfDayIn(TimeZone.UTC) ?: start
        val finalEnd = pEnd?.atTime(23, 59, 59, 999_999_999)?.toInstant(TimeZone.UTC) ?: end
        return statisticsRepository.getTransactionCounts(
            userId,
            accountId,
            isIncome,
            start = finalStart,
            end = finalEnd
        )
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
            val cInc = async {
                statisticsRepository.getCategoryTotals(
                    userId,
                    currentStart,
                    currentEnd,
                    accountId,
                    true
                )
            }
            val cExp = async {
                statisticsRepository.getCategoryTotals(
                    userId,
                    currentStart,
                    currentEnd,
                    accountId,
                    false
                )
            }
            val pInc = async {
                statisticsRepository.getCategoryTotals(
                    userId,
                    prevStart,
                    prevEnd,
                    accountId,
                    true
                )
            }
            val pExp = async {
                statisticsRepository.getCategoryTotals(
                    userId,
                    prevStart,
                    prevEnd,
                    accountId,
                    false
                )
            }

            listOf(cInc.await(), cExp.await(), pInc.await(), pExp.await())
        }

        val currentIncomeTotal = currentIncByCat.values.fold(BigDecimal.ZERO, BigDecimal::add)
        val currentExpenseTotal = currentExpByCat.values.fold(BigDecimal.ZERO, BigDecimal::add)
        val prevIncomeTotal = prevIncByCat.values.fold(BigDecimal.ZERO, BigDecimal::add)
        val prevExpenseTotal = prevExpByCat.values.fold(BigDecimal.ZERO, BigDecimal::add)

        val incomeProjectedTotal = if (isCurrentYear) {
            currentIncomeTotal.multiply(BigDecimal.valueOf(12))
                .divide(
                    BigDecimal.valueOf(now.monthNumber.toLong().coerceAtLeast(1)),
                    2,
                    RoundingMode.HALF_UP
                )
        } else null

        val expenseProjectedTotal = if (isCurrentYear) {
            currentExpenseTotal.multiply(BigDecimal.valueOf(12))
                .divide(
                    BigDecimal.valueOf(now.monthNumber.toLong().coerceAtLeast(1)),
                    2,
                    RoundingMode.HALF_UP
                )
        } else null

        return YearlyMetrics(
            ytdIncomeChange = currentIncomeTotal.calculatePercentageChange(prevIncomeTotal),
            ytdExpenseChange = currentExpenseTotal.calculatePercentageChange(prevExpenseTotal),
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
        val budgets =
            budgetRepository.getAllByUser(userId, accountId, limit = Int.MAX_VALUE, offset = 0)
                .filter { it.isExpense }
        val totalMonthlyLimit = budgets.fold(BigDecimal.ZERO) { acc, b -> acc + b.limit }
        val yearlyLimit = totalMonthlyLimit.multiply(BigDecimal.valueOf(12))

        if (expenseProjectedTotal > yearlyLimit && yearlyLimit > BigDecimal.ZERO) {
            val currentMonth = Clock.System.now().toLocalDateTime(TimeZone.UTC).monthNumber
            val averagePerMonth = totalExpense.calculateAverage(currentMonth)

            for (m in (currentMonth + 1)..12) {
                val accumulatedSpend =
                    totalExpense + averagePerMonth.multiply(BigDecimal.valueOf((m - currentMonth).toLong()))
                if (accumulatedSpend > yearlyLimit) {
                    return monthNames.getOrNull(m - 1)
                }
            }
        }
        return null
    }

    private fun assembleHighlights(
        dailyTotals: Map<LocalDate, DailyTotal>,
        categoryTotals: Map<String, BigDecimal>,
        prevCategoryTotals: Map<String, BigDecimal>? = null,
        ytdChange: Double? = null,
        projectedTotal: BigDecimal? = null,
        savingsRate: Double? = null,
        essentialSpendRatio: Double? = null,
        projectedExceedMonth: String? = null,
        correlations: List<CorrelationDto>? = null,
        isIncome: Boolean
    ): HighlightsDto {
        return HighlightsDto(
            highestMonth = calculateHighestMonth(dailyTotals, isIncome),
            highestCategory = calculateHighestCategory(categoryTotals, prevCategoryTotals),
            highestDay = calculateHighestDay(dailyTotals, isIncome),
            averagePerDay = calculateAveragePerDay(dailyTotals, isIncome),
            ytdChangePercentage = ytdChange,
            projectedTotal = projectedTotal,
            savingsRate = savingsRate,
            essentialSpendRatio = essentialSpendRatio,
            projectedExceedMonth = projectedExceedMonth,
            correlations = correlations
        )
    }

    private fun calculateHighestMonth(
        dailyTotals: Map<LocalDate, DailyTotal>,
        isIncome: Boolean
    ): HighlightDto? =
        dailyTotals.entries.groupBy {
            val date = it.key
            "%04d-%02d".format(date.year, date.monthNumber)
        }
            .mapValues { (_, entries) ->
                entries.fold(BigDecimal.ZERO) { acc, entry ->
                    acc + (if (isIncome) entry.value.income else entry.value.expense)
                }
            }
            .maxByOrNull { it.value }
            ?.let { (monthStr, amount) ->
                HighlightDto(label = monthStr, value = monthStr, amount = amount)
            }

    private fun calculateHighestCategory(
        categoryTotals: Map<String, BigDecimal>,
        prevCategoryTotals: Map<String, BigDecimal>?
    ): HighlightDto? =
        categoryTotals.maxByOrNull { it.value }
            ?.let { (category, amount) ->
                val volatility = prevCategoryTotals?.let { prev ->
                    val prevAmount = prev.entries.find {
                        it.key.trim().equals(category.trim(), ignoreCase = true)
                    }?.value ?: BigDecimal.ZERO
                    amount.calculatePercentageChange(prevAmount)
                }
                HighlightDto(
                    label = category,
                    value = category,
                    amount = amount,
                    volatilityPercentage = volatility
                )
            }

    private fun calculateHighestDay(
        dailyTotals: Map<LocalDate, DailyTotal>,
        isIncome: Boolean
    ): HighlightDto? =
        dailyTotals.maxByOrNull { if (isIncome) it.value.income else it.value.expense }
            ?.let { (date, total) ->
                HighlightDto(
                    label = date.toString(),
                    value = date.toString(),
                    amount = if (isIncome) total.income else total.expense
                )
            }

    private fun calculateAveragePerDay(
        dailyTotals: Map<LocalDate, DailyTotal>,
        isIncome: Boolean
    ): Double {
        val days = dailyTotals.size.coerceAtLeast(1)
        val total =
            dailyTotals.values.fold(BigDecimal.ZERO) { acc, d -> acc + (if (isIncome) d.income else d.expense) }
        return total.calculateAverage(days).toDouble()
    }

    private suspend fun calculateCorrelations(
        userId: UUID,
        accountId: UUID?,
        period: TimePeriod.Month
    ): List<CorrelationDto> {
        val hCurrentStart = LocalDate(period.year, period.month, 1)
        val historicalStart = hCurrentStart.minus(DatePeriod(months = 6))
        val historicalEnd = hCurrentStart.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))

        val monthlyCategoryStats = statisticsRepository.getMonthlyCategoryStats(
            userId, historicalStart, historicalEnd, accountId
        )

        val monthlyData = monthlyCategoryStats.mapValues { (_, catMap) ->
            val inc = catMap.values.filter { it.isIncome }.sumOf { it.totalAmount }
            val exp = catMap.filterValues { !it.isIncome }.mapValues { it.value.totalAmount }
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
                    val incomeIncrease =
                        currMonth.first.calculatePercentageChange(prevMonth.first) / 100.0
                    if (incomeIncrease > 0.10) {
                        targetCats.forEach { cat ->
                            val prevExp = prevMonth.second[cat] ?: BigDecimal.ZERO
                            val currExp = currMonth.second[cat] ?: BigDecimal.ZERO
                            val nextExp = nextMonth?.second?.get(cat) ?: BigDecimal.ZERO

                            if (prevExp > BigDecimal.ZERO && (currExp.calculatePercentageChange(
                                    prevExp
                                ) / 100.0) > 0.15
                            ) {
                                val incPct = (incomeIncrease * 100).toInt()
                                val expPct = currExp.calculatePercentageChange(prevExp).toInt()
                                add(
                                    CorrelationDto(
                                        source = "Income",
                                        target = cat.replaceFirstChar { it.uppercase() },
                                        insight = "When your income increases by $incPct%, your '${cat.replaceFirstChar { it.uppercase() }}' spend tends to increase by $expPct% in the same month."
                                    )
                                )
                            } else if (currExp > BigDecimal.ZERO && nextExp > BigDecimal.ZERO && (nextExp.calculatePercentageChange(
                                    currExp
                                ) / 100.0) > 0.15
                            ) {
                                val incPct = (incomeIncrease * 100).toInt()
                                val expPct = nextExp.calculatePercentageChange(currExp).toInt()
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

        val (currentMonthStart, currentMonthEnd) = parsedPeriod.toDateRange()

        val historicalAverageCounts = if (parsedPeriod is TimePeriod.Month) {
            val historicalStart = currentMonthStart.minus(DatePeriod(months = 6))
            val historicalEnd = currentMonthStart.minus(DatePeriod(days = 1))

            val histStats = statisticsRepository.getMonthlyCategoryStats(
                userId,
                historicalStart,
                historicalEnd,
                accountId
            )

            histStats.values
                .flatMap { it.keys }
                .distinct()
                .associateWith { cat ->
                    val occurrences = histStats.values.count { it.containsKey(cat) }
                    val totalCount = histStats.values.sumOf { it[cat]?.count ?: 0 }
                    totalCount.toDouble() / occurrences.coerceAtLeast(1)
                }
        } else null

        val previousMonthsData = if (parsedPeriod is TimePeriod.Month) {
            val m1Start = currentMonthStart.minus(DatePeriod(months = 1))
            val m1End = currentMonthStart.minus(DatePeriod(days = 1))
            val m2Start = currentMonthStart.minus(DatePeriod(months = 2))
            val m2End = m1Start.minus(DatePeriod(days = 1))

            val totals1Deferred = async {
                statisticsRepository.getCategoryTotals(
                    userId,
                    m1Start,
                    m1End,
                    accountId,
                    isIncome
                )
            }
            val totals2Deferred = async {
                statisticsRepository.getCategoryTotals(
                    userId,
                    m2Start,
                    m2End,
                    accountId,
                    isIncome
                )
            }

            val totals1 = totals1Deferred.await().mapKeys { it.key.trim().lowercase() }
            val totals2 = totals2Deferred.await().mapKeys { it.key.trim().lowercase() }
            totals1 to totals2
        } else null

        val currentMonthStats = statisticsRepository.getMonthlyCategoryStats(
            userId,
            currentMonthStart,
            currentMonthEnd,
            accountId
        )
        val currentStats = currentMonthStats.values.firstOrNull() ?: emptyMap()
        val currentTopDescriptions = statisticsRepository.getTopDescriptions(
            userId,
            currentMonthStart,
            currentMonthEnd,
            accountId,
            isIncome
        )

        val totalIncomeAmount = currentStats.values.filter { it.isIncome }.sumOf { it.totalAmount }
        
        // Requirement 2 & 1 Unified: base total for expense distribution
        val feeAmountFromStats = currentStats.values
            .find { !it.isIncome && it.categoryId == CategoryConstants.TRANSACTION_FEES_ID }?.totalAmount ?: BigDecimal.ZERO
        
        val counts = getCountsForPeriod(userId, accountId, isIncome, start, end, parsedPeriod)
        val totalExpenseAmount = currentStats.values.filter { !it.isIncome }.sumOf { it.totalAmount } - feeAmountFromStats + counts.totalTransactionCost

        fun categorySummary(isInc: Boolean): List<CategorySummaryDto> {
            // Requirement 3: Filter out 'Transaction Fees' category to prevent duplication with the unified top-level totalTransactionCost field
            val relevantStats = currentStats.filterValues {
                it.isIncome == isInc && it.categoryId != CategoryConstants.TRANSACTION_FEES_ID
            }

            val totalAmountAll = if (isInc) totalIncomeAmount else totalExpenseAmount

            val sortedCategories = relevantStats.entries
                .sortedByDescending { it.value.totalAmount }

            val otherCategories = sortedCategories.drop(5)
            val othersInsight = if (otherCategories.isNotEmpty()) {
                otherCategories.flatMap { (cat, _) -> currentTopDescriptions[cat] ?: emptyList() }
                    .filter {
                        MerchantInsightUtils.isDescriptionMeaningful(
                            it.description,
                            "Other"
                        )
                    }
                    .groupBy { MerchantInsightUtils.cleanMerchantName(it.description) }
                    .mapValues { it.value.sumOf { dt -> dt.totalAmount } }
                    .maxByOrNull { it.value }?.key
            } else null

            return sortedCategories.map { (key, stats) ->
                val displayName = stats.name
                val sum = stats.totalAmount
                val count = stats.count

                val momentum = if (parsedPeriod is TimePeriod.Month && previousMonthsData != null) {
                    val prev1 = previousMonthsData.first[key] ?: BigDecimal.ZERO
                    val prev2 = previousMonthsData.second[key] ?: BigDecimal.ZERO
                    when {
                        sum > prev1 && prev1 > prev2 -> "UP"
                        sum < prev1 && prev1 < prev2 -> "DOWN"
                        else -> "STABLE"
                    }
                } else null

                val insights = (currentTopDescriptions[displayName] ?: emptyList())
                    .filter {
                        MerchantInsightUtils.isDescriptionMeaningful(
                            it.description,
                            displayName
                        )
                    }
                    .map { MerchantInsightUtils.cleanMerchantName(it.description) }
                    .distinct()
                    .take(3)

                CategorySummaryDto(
                    category = displayName,
                    categoryId = stats.categoryId?.toString(),
                    total = sum,
                    percentage = sum.calculateRatio(totalAmountAll) ?: 0.0,
                    transactionCount = count,
                    averageTransactionCount = historicalAverageCounts?.get(key),
                    momentumTrend = momentum,
                    topDescriptionInsights = insights
                )
            }
        }

        val incomeCategoriesFinal = if (isIncome != false) categorySummary(true) else emptyList()
        val expenseCategoriesFinal = if (isIncome != true) categorySummary(false) else emptyList()

        Result.Success(
            DistributionSummaryDto(
                period = period,
                totalTransactionCost = counts.totalTransactionCost,
                incomeCategories = incomeCategoriesFinal,
                expenseCategories = expenseCategoriesFinal,
                othersInsightSummary = null
            )
        )
    }

    override suspend fun getAvailableWeeks(
        userId: UUID,
        accountId: UUID?
    ): Result<AvailableWeeksDto> =
        Result.Success(
            AvailableWeeksDto(
                statisticsRepository.getAvailablePeriods(
                    userId,
                    accountId,
                    "weeks"
                )
            )
        )

    override suspend fun getAvailableMonths(
        userId: UUID,
        accountId: UUID?
    ): Result<AvailableMonthsDto> =
        Result.Success(
            AvailableMonthsDto(
                statisticsRepository.getAvailablePeriods(
                    userId,
                    accountId,
                    "months"
                )
            )
        )

    override suspend fun getAvailableYears(
        userId: UUID,
        accountId: UUID?
    ): Result<AvailableYearsDto> =
        Result.Success(
            AvailableYearsDto(
                statisticsRepository.getAvailablePeriods(
                    userId,
                    accountId,
                    "years"
                )
            )
        )

    override suspend fun getOverviewSummary(
        userId: UUID,
        accountId: UUID?
    ): Result<OverviewSummaryDto> = coroutineScope {
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

        Result.Success(
            OverviewSummaryDto(
                period = targetMonthCode,
                isCurrent = isCurrent,
                weeklyOverview = (weeklyResult as Result.Success).value,
                monthlyOverview = (monthlyResult as Result.Success).value
            )
        )
    }

    override suspend fun getDaySummaries(
        userId: UUID,
        start: LocalDate,
        end: LocalDate,
        accountId: UUID?
    ): Result<List<DaySummaryDto>> {
        val dailyTotals = statisticsRepository.getDailyTotals(userId, start, end, accountId)
        val dates = generateSequence(start) { current ->
            if (current < end) current.plus(DatePeriod(days = 1)) else null
        }.toList()

        val summaries = dates.map { date ->
            val total = dailyTotals[date] ?: DailyTotal(BigDecimal.ZERO, BigDecimal.ZERO)
            DaySummaryDto(date = date, income = total.income, expense = total.expense)
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

        val tmetDeferred = async {
            statisticsRepository.getCategoryTotals(
                userId,
                currentMonthStart,
                currentMonthEnd,
                accountId,
                isIncome = false
            )
        }
        val lmetDeferred = async {
            statisticsRepository.getCategoryTotals(
                userId,
                previousMonthStart,
                previousMonthEnd,
                accountId,
                isIncome = false
            )
        }
        val tmitDeferred = async {
            statisticsRepository.getCategoryTotals(
                userId,
                currentMonthStart,
                currentMonthEnd,
                accountId,
                isIncome = true
            )
        }
        val lmitDeferred = async {
            statisticsRepository.getCategoryTotals(
                userId,
                previousMonthStart,
                previousMonthEnd,
                accountId,
                isIncome = true
            )
        }
        val twtDeferred = async {
            statisticsRepository.getCategoryTotals(
                userId,
                thisWeekStart,
                thisWeekEnd,
                accountId
            )
        }
        val lwtDeferred = async {
            statisticsRepository.getCategoryTotals(
                userId,
                lastWeekStart,
                lastWeekEnd,
                accountId
            )
        }

        val thisMonthExpenseTotals = tmetDeferred.await()
        val lastMonthExpenseTotals = lmetDeferred.await()
        val thisMonthIncomeTotals = tmitDeferred.await()
        val lastMonthIncomeTotals = lmitDeferred.await()
        val thisWeekTotals = twtDeferred.await()
        val lastWeekTotals = lwtDeferred.await()

        val user = userRepository.findById(userId)
        val trackedCategoryIds = user?.trackedCategoryIds ?: emptyList()

        // Resolve category names from IDs
        val trackedCategories: List<String> = if (trackedCategoryIds.isNotEmpty()) {
            categoryRepository.getByIds(trackedCategoryIds).map { it.name }
        } else emptyList()

        val allTimeIncomeCategories = if (trackedCategories.isNotEmpty()) {
            statisticsRepository.getCategoryTotals(
                userId,
                null,
                null,
                accountId,
                isIncome = true
            ).keys
        } else emptySet()

        val comparisons = if (trackedCategories.isNotEmpty() && !isBackupMonth) {
            trackedCategories.map { category ->
                async {
                    if (category.equals("Transaction Fees", ignoreCase = true) || category.equals("Transaction Cost", ignoreCase = true)) {
                        calculateTransactionCostComparison(
                            userId,
                            accountId,
                            currentMonthStart,
                            currentMonthEnd,
                            previousMonthStart,
                            previousMonthEnd,
                            thisWeekStart,
                            thisWeekEnd,
                            lastWeekStart,
                            lastWeekEnd
                        )
                    } else {
                        val isInc =
                            allTimeIncomeCategories.contains(category) || thisMonthIncomeTotals.containsKey(
                                category
                            )
                        val currentTotals =
                            if (isInc) thisMonthIncomeTotals else thisMonthExpenseTotals
                        val previousTotals =
                            if (isInc) lastMonthIncomeTotals else lastMonthExpenseTotals

                        val cur = currentTotals[category] ?: BigDecimal.ZERO
                        val prev = previousTotals[category] ?: BigDecimal.ZERO
                        val weekCur = thisWeekTotals[category] ?: BigDecimal.ZERO
                        val weekPrev = lastWeekTotals[category] ?: BigDecimal.ZERO

                        CategoryComparisonDto(
                            category = category,
                            currentTotal = cur,
                            previousTotal = prev,
                            changePercentage = cur.calculatePercentageChange(prev),
                            isIncome = isInc,
                            period = targetPeriodString,
                            weeklyCurrentTotal = weekCur,
                            weeklyChangePercentage = weekCur.calculatePercentageChange(weekPrev)
                        )
                    }
                }
            }.awaitAll()
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

                add(
                    CategoryComparisonDto(
                        category = incomeCategory,
                        currentTotal = curInc,
                        previousTotal = prevInc,
                        changePercentage = curInc.calculatePercentageChange(prevInc),
                        isIncome = true,
                        period = targetPeriodString,
                        weeklyCurrentTotal = weekCurInc,
                        weeklyChangePercentage = weekCurInc.calculatePercentageChange(weekPrevInc)
                    )
                )

                if (topExpenseCategory != null) {
                    val curExp = thisMonthExpenseTotals[topExpenseCategory] ?: BigDecimal.ZERO
                    val prevExp = lastMonthExpenseTotals[topExpenseCategory] ?: BigDecimal.ZERO
                    val weekCurExp = thisWeekTotals[topExpenseCategory] ?: BigDecimal.ZERO
                    val weekPrevExp = lastWeekTotals[topExpenseCategory] ?: BigDecimal.ZERO

                    add(
                        CategoryComparisonDto(
                            category = topExpenseCategory,
                            currentTotal = curExp,
                            previousTotal = prevExp,
                            changePercentage = curExp.calculatePercentageChange(prevExp),
                            isIncome = false,
                            period = targetPeriodString,
                            weeklyCurrentTotal = weekCurExp,
                            weeklyChangePercentage = weekCurExp.calculatePercentageChange(
                                weekPrevExp
                            )
                        )
                    )
                } else {
                    add(
                        calculateTransactionCostComparison(
                            userId,
                            accountId,
                            currentMonthStart,
                            currentMonthEnd,
                            previousMonthStart,
                            previousMonthEnd,
                            thisWeekStart,
                            thisWeekEnd,
                            lastWeekStart,
                            lastWeekEnd
                        )
                    )
                }
            }
        }

        Result.Success(
            CategoryComparisonSummaryDto(
                period = targetPeriodString,
                isCurrent = isCurrent,
                data = comparisons
            )
        )
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
    ): CategoryComparisonDto = coroutineScope {
        val curStartInstant = currentStart.atStartOfDayIn(TimeZone.UTC)
        val curEndInstant = currentEnd.atTime(23, 59, 59, 999_999_999).toInstant(TimeZone.UTC)
        val prevStartInstant = previousStart.atStartOfDayIn(TimeZone.UTC)
        val prevEndInstant = previousEnd.atTime(23, 59, 59, 999_999_999).toInstant(TimeZone.UTC)

        val curCounts = async {
            statisticsRepository.getTransactionCounts(
                userId,
                accountId,
                start = curStartInstant,
                end = curEndInstant
            )
        }
        val prevCounts = async {
            statisticsRepository.getTransactionCounts(
                userId,
                accountId,
                start = prevStartInstant,
                end = prevEndInstant
            )
        }

        val curCost = curCounts.await().totalTransactionCost
        val prevCost = prevCounts.await().totalTransactionCost

        var weekCurCost = BigDecimal.ZERO
        var weekPrevCost = BigDecimal.ZERO
        if (thisWeekStart != null && thisWeekEnd != null && lastWeekStart != null && lastWeekEnd != null) {
            val twStart = thisWeekStart.atStartOfDayIn(TimeZone.UTC)
            val twEnd = thisWeekEnd.atTime(23, 59, 59, 999_999_999).toInstant(TimeZone.UTC)
            val lwStart = lastWeekStart.atStartOfDayIn(TimeZone.UTC)
            val lwEnd = lastWeekEnd.atTime(23, 59, 59, 999_999_999).toInstant(TimeZone.UTC)

            val twCounts = async {
                statisticsRepository.getTransactionCounts(
                    userId,
                    accountId,
                    start = twStart,
                    end = twEnd
                )
            }
            val lwCounts = async {
                statisticsRepository.getTransactionCounts(
                    userId,
                    accountId,
                    start = lwStart,
                    end = lwEnd
                )
            }

            weekCurCost = twCounts.await().totalTransactionCost
            weekPrevCost = lwCounts.await().totalTransactionCost
        }

        CategoryComparisonDto(
            category = "Transaction Fees",
            currentTotal = curCost,
            previousTotal = prevCost,
            changePercentage = curCost.calculatePercentageChange(prevCost),
            isIncome = false,
            period = currentStart.toString().substring(0, 7),
            weeklyCurrentTotal = if (thisWeekStart != null) weekCurCost else null,
            weeklyChangePercentage = if (thisWeekStart != null) weekCurCost.calculatePercentageChange(
                weekPrevCost
            ) else null
        )
    }

    override suspend fun getTransactionCountSummary(
        userId: UUID,
        accountId: UUID?,
        isIncome: Boolean?,
        categoryIds: List<UUID>?,
        hasCost: Boolean?,
        start: Instant?,
        end: Instant?
    ): Result<TransactionCountSummaryDto> {
        if (accountId == null) return Result.Failure(AppError.Validation("Account ID is required"))
        val counts = statisticsRepository.getTransactionCounts(
            userId,
            accountId,
            isIncome,
            categoryIds,
            hasCost,
            start,
            end
        )
        return Result.Success(
            TransactionCountSummaryDto(
                counts.incomeCount,
                counts.expenseCount,
                counts.totalCount,
                counts.totalTransactionCost,
                counts.totalAmount
            )
        )
    }

    override suspend fun getProfileMetrics(userId: UUID): Result<ProfileMetricsDto> {
        val user = userRepository.findById(userId)
            ?: return Result.Failure(AppError.NotFound("User not found"))
        val accountsResult = accountService.getAllAccounts(userId)
        if (accountsResult is Result.Failure) return Result.Failure(accountsResult.error)
        val accounts = (accountsResult as Result.Success).value

        val netWorth =
            accounts.fold(BigDecimal.ZERO) { acc, a -> acc + (a.balance ?: BigDecimal.ZERO) }

        val incomeTotals =
            statisticsRepository.getCategoryTotals(userId, null, null, null, isIncome = true)
        val expenseTotals =
            statisticsRepository.getCategoryTotals(userId, null, null, null, isIncome = false)

        val totalIncome = incomeTotals.values.fold(BigDecimal.ZERO, BigDecimal::add)
        val totalExpense = expenseTotals.values.fold(BigDecimal.ZERO, BigDecimal::add)

        val savingsRate = (totalIncome - totalExpense).calculateRatio(totalIncome)

        val essentialSpend = expenseTotals
            .filter { (cat, _) ->
                essentialCategories.any {
                    it.equals(
                        cat.trim(),
                        ignoreCase = true
                    )
                }
            }
            .values.fold(BigDecimal.ZERO, BigDecimal::add)
        val essentialSpendRatio = essentialSpend.calculateRatio(totalExpense)

        return Result.Success(
            ProfileMetricsDto(
                name = user.name,
                email = user.email,
                netWorth = netWorth,
                savingsRate = savingsRate,
                essentialSpendRatio = essentialSpendRatio
            )
        )
    }

    override suspend fun getDaySummariesByDateRange(
        userId: UUID, accountId: UUID?, startParam: String?, endParam: String?
    ): Result<List<DaySummaryDto>> {
        return when (val rangeResult = parseDateRange(startParam, endParam)) {
            is Result.Success -> {
                val range = rangeResult.value
                val start =
                    range.first?.toLocalDateTime(TimeZone.UTC)?.date ?: return Result.Failure(
                        AppError.Validation("Invalid start date")
                    )
                val end =
                    range.second?.toLocalDateTime(TimeZone.UTC)?.date ?: return Result.Failure(
                        AppError.Validation("Invalid end date")
                    )
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

    override fun parseDateRange(
        startDate: String?,
        endDate: String?
    ): Result<Pair<Instant?, Instant?>> {
        return try {
            val start = startDate?.let {
                LocalDate.parse(it).atTime(LocalTime(0, 0, 0)).toInstant(TimeZone.UTC)
            }
            val end = endDate?.let {
                LocalDate.parse(it).atTime(LocalTime(23, 59, 59, 999_999_999))
                    .toInstant(TimeZone.UTC)
            }
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
