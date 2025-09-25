package com.fintrack.feature.summary.data.repository

import com.fintrack.feature.summary.data.model.AccountAggregates
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
import feature.transactions.Highlight
import feature.transactions.Highlights
import feature.transactions.StatisticsSummary
import feature.transactions.data.TransactionsTable
import feature.transactions.domain.model.Transaction
import feature.transactions.validate
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.temporal.IsoFields
import java.time.temporal.WeekFields
import kotlin.math.absoluteValue

class StatisticsRepository {
    fun getAccountAggregates(userId: Int, accountId: Int? = null): AccountAggregates = transaction {
        val query = TransactionsTable
            .select(TransactionsTable.amount, TransactionsTable.isIncome)
            .where { TransactionsTable.userId eq userId }
        // Filter by account if provided
        val filteredQuery = accountId?.let { query.andWhere { TransactionsTable.accountId eq it } } ?: query
        val transactions = filteredQuery.map { it[TransactionsTable.amount] to it[TransactionsTable.isIncome] }
        val income = transactions.filter { it.second }.sumOf { it.first }
        val expense = transactions.filter { !it.second }.sumOf { it.first }
        val balance = income - expense

        AccountAggregates(income, expense, balance)
    }


    fun getStatisticsSummary(
        userId: Int,
        accountId: Int? = null,
        isIncome: Boolean? = null,
        start: LocalDateTime? = null,
        end: LocalDateTime? = null
    ): StatisticsSummary = transaction {
        var filteredQuery: Query = TransactionsTable.selectAll()
            .andWhere { TransactionsTable.userId eq userId }

        if (accountId != null) filteredQuery =
            filteredQuery.andWhere { TransactionsTable.accountId eq accountId }

        if (isIncome != null) filteredQuery =
            filteredQuery.andWhere { TransactionsTable.isIncome eq isIncome }

        if (start != null) filteredQuery =
            filteredQuery.andWhere { TransactionsTable.dateTime greaterEq start.toJavaLocalDateTime() }

        if (end != null) filteredQuery =
            filteredQuery.andWhere { TransactionsTable.dateTime lessEq end.toJavaLocalDateTime() }
        val filtered = filteredQuery.map { it.toTransaction() }
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

        StatisticsSummary(
            incomeHighlights = incomeHighlights,
            expenseHighlights = expenseHighlights
        )
    }

    // --- For a single DistributionSummary for a given period ---
    fun getDistributionSummary(
        userId: Int,
        period: String,
        accountId: Int? = null,
        isIncome: Boolean? = null,
        start: LocalDateTime? = null,
        end: LocalDateTime? = null
    ): DistributionSummary = transaction {
        // --- Filter transactions by user, type, optional account, and date ---
        var query: Query = TransactionsTable.selectAll()
            .andWhere { TransactionsTable.userId eq userId }

        if (accountId != null) query = query.andWhere { TransactionsTable.accountId eq accountId }
        if (isIncome != null) query = query.andWhere { TransactionsTable.isIncome eq isIncome }
        if (start != null) query = query.andWhere { TransactionsTable.dateTime greaterEq start.toJavaLocalDateTime() }
        if (end != null) query = query.andWhere { TransactionsTable.dateTime lessEq end.toJavaLocalDateTime() }
        val filtered = query.map { it.toTransaction() }
        val incomeTxns = filtered.filter { it.isIncome }
        val expenseTxns = filtered.filter { !it.isIncome }
        // --- Determine period type ---
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

        DistributionSummary(
            period = period,
            incomeCategories = incomeCategoriesFinal,
            expenseCategories = expenseCategoriesFinal
        )
    }


    fun getAvailableWeeks(userId: Int, accountId: Int? = null): AvailableWeeks = transaction {
        var query = TransactionsTable.selectAll().andWhere { TransactionsTable.userId eq userId }

        if (accountId != null) {
            query = query.andWhere { TransactionsTable.accountId eq accountId }
        }
        val weeks = query
            .map { it[TransactionsTable.dateTime].toLocalDate() }
            .map { date ->
                val year = date.year
                val week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                "%04d-W%02d".format(year, week)
            }
            .distinct()
            .sortedDescending()

        AvailableWeeks(weeks)
    }

    fun getAvailableMonths(userId: Int, accountId: Int? = null): AvailableMonths = transaction {
        var query = TransactionsTable.selectAll().andWhere { TransactionsTable.userId eq userId }

        if (accountId != null) {
            query = query.andWhere { TransactionsTable.accountId eq accountId }
        }
        val months = query
            .map { it[TransactionsTable.dateTime].toLocalDate() }
            .map { date ->
                val year = date.year
                val month = date.monthValue
                "%04d-%02d".format(year, month)
            }
            .distinct()
            .sortedDescending()

        AvailableMonths(months)
    }


    fun getAvailableYears(userId: Int, accountId: Int? = null): AvailableYears = transaction {
        var query = TransactionsTable.selectAll().andWhere { TransactionsTable.userId eq userId }

        if (accountId != null) {
            query = query.andWhere { TransactionsTable.accountId eq accountId }
        }
        val years = query
            .map { it[TransactionsTable.dateTime].toLocalDate().year.toString() }
            .distinct()
            .sortedDescending()

        AvailableYears(years)
    }

    /*** Returns an OverviewSummary containing:
     *  - weeklyOverview: last 7 days (today included)
     *  - monthlyOverview: last 30 days (today included)
     */
    fun getOverviewSummary(userId: Int, accountId: Int? = null): OverviewSummary {
        val today = Clock.System.now().toLocalDateTime(TimeZone.Companion.currentSystemDefault()).date
        val weeklyStart = today.minus(DatePeriod(days = 6))   // last 7 days
        val monthlyStart = today.minus(DatePeriod(days = 29)) // last 30 days
        val weekly = getDaySummaries(userId, weeklyStart, today, accountId)
        val monthly = getDaySummaries(userId, monthlyStart, today, accountId)

        return OverviewSummary(weeklyOverview = weekly, monthlyOverview = monthly)
    }

    /**
     * Returns list of DaySummary for the inclusive date range [start. .end].
     * This function opens a transaction (Exposed).
     */
    fun getDaySummaries(
        userId: Int,
        start: LocalDate,
        end: LocalDate,
        accountId: Int? = null
    ): List<DaySummary> = transaction {
        val startJ = java.time.LocalDateTime.of(start.year, start.monthNumber, start.dayOfMonth, 0, 0, 0, 0)
        val endJ = java.time.LocalDateTime.of(end.year, end.monthNumber, end.dayOfMonth, 23, 59, 59, 999_999_999)
        // Query rows in the range for the user (and optional account)
        var query = TransactionsTable.selectAll().where {
            (TransactionsTable.userId eq userId) and
                    (TransactionsTable.dateTime greaterEq startJ) and
                    (TransactionsTable.dateTime lessEq endJ)
        }

        if (accountId != null) {
            query = query.andWhere { TransactionsTable.accountId eq accountId }
        }
        val rows = query.map { row ->
            val jdt = row[TransactionsTable.dateTime]
            val jDate = jdt.toLocalDate()
            val kDate = LocalDate(jDate.year, jDate.monthValue, jDate.dayOfMonth)
            Triple(kDate, row[TransactionsTable.isIncome], row[TransactionsTable.amount])
        }
        val dates = generateSequence(start) { current ->
            if (current < end) current.plus(DatePeriod(days = 1)) else null
        }.toList()

        dates.map { date ->
            val dayTxs = rows.filter { it.first == date }
            val income = dayTxs.filter { it.second }.sumOf { it.third }
            val expense = dayTxs.filter { !it.second }.sumOf { it.third }
            DaySummary(date = date, income = income, expense = expense)
        }
    }

    fun getCategoryComparisons(
        userId: Int,
        accountId: Int? = null
    ): List<CategoryComparison> = transaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.Companion.currentSystemDefault()).date
        // --- Period boundaries ---
        val thisWeekStart = now.minus(DatePeriod(days = 6))
        val lastWeekStart = thisWeekStart.minus(DatePeriod(days = 7))
        val lastWeekEnd = thisWeekStart.minus(DatePeriod(days = 1))
        val thisMonthStart = now.minus(DatePeriod(days = 29))
        val lastMonthStart = thisMonthStart.minus(DatePeriod(days = 30))
        val lastMonthEnd = thisMonthStart.minus(DatePeriod(days = 1))

        // --- Helper to get category totals for a range ---
        fun totalsByCategory(start: LocalDate, end: LocalDate): Map<String, Double> {
            val startJ = java.time.LocalDateTime.of(start.year, start.monthNumber, start.dayOfMonth, 0, 0)
            val endJ = java.time.LocalDateTime.of(end.year, end.monthNumber, end.dayOfMonth, 23, 59, 59)
            var query = TransactionsTable.selectAll().where {
                (TransactionsTable.userId eq userId) and
                        (TransactionsTable.dateTime greaterEq startJ) and
                        (TransactionsTable.dateTime lessEq endJ)
            }

            if (accountId != null) {
                query = query.andWhere { TransactionsTable.accountId eq accountId }
            }

            return query
                .groupBy { it[TransactionsTable.category] }
                .mapValues { (_, rows) -> rows.sumOf { it[TransactionsTable.amount] } }
        }
        // --- Weekly comparison ---
        val thisWeekTotals = totalsByCategory(thisWeekStart, now)
        val lastWeekTotals = totalsByCategory(lastWeekStart, lastWeekEnd)
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
        // --- Monthly comparison ---
        val thisMonthTotals = totalsByCategory(thisMonthStart, now)
        val lastMonthTotals = totalsByCategory(lastMonthStart, lastMonthEnd)
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

        listOfNotNull(weeklyComparison, monthlyComparison)
    }

    fun getTransactionCountSummary(
        userId: Int,
        accountId: Int
    ): TransactionCountSummaryDto? {
        return transaction {
            val incomeCount = TransactionsTable
                .selectAll()
                .where {
                    (TransactionsTable.userId eq userId) and
                            (TransactionsTable.accountId eq accountId) and
                            (TransactionsTable.isIncome eq true)
                }
                .count()
            val expenseCount = TransactionsTable
                .selectAll()
                .where {
                    (TransactionsTable.userId eq userId) and
                            (TransactionsTable.accountId eq accountId) and
                            (TransactionsTable.isIncome eq false)
                }
                .count()
            val totalCount = TransactionsTable
                .selectAll()
                .where {
                    (TransactionsTable.userId eq userId) and
                            (TransactionsTable.accountId eq accountId)
                }
                .count()

            if (totalCount == 0L) {
                null
            } else {
                TransactionCountSummaryDto(
                    totalIncomeTransactions = incomeCount.toInt(),
                    totalExpenseTransactions = expenseCount.toInt(),
                    totalTransactions = totalCount.toInt()
                )
            }
        }
    }


    private fun ResultRow.toTransaction() = Transaction(
        id = this[TransactionsTable.id],
        userId = this[TransactionsTable.userId],
        isIncome = this[TransactionsTable.isIncome],
        amount = this[TransactionsTable.amount],
        category = this[TransactionsTable.category],
        dateTime = this[TransactionsTable.dateTime].toKotlinLocalDateTime(),
        description = this[TransactionsTable.description],
        accountId = this[TransactionsTable.accountId]
    )
}
