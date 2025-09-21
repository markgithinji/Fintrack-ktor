package feature.transactions.data

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
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.temporal.IsoFields
import java.time.temporal.WeekFields

class TransactionRepository {
    fun getAllCursor(
        userId: Int,
        accountId: Int? = null,
        isIncome: Boolean? = null,
        categories: List<String>? = null,
        start: LocalDateTime? = null,
        end: LocalDateTime? = null,
        sortBy: String = "dateTime",
        order: SortOrder = SortOrder.ASC,
        limit: Int = 20,
        afterDateTime: LocalDateTime? = null,
        afterId: Int? = null
    ): List<Transaction> = transaction {
        var query = TransactionsTable.selectAll()
            .andWhere { TransactionsTable.userId eq userId }
        // Filter by account
        if (accountId != null) query = query.andWhere { TransactionsTable.accountId eq accountId }
        // Apply filters
        if (isIncome != null) query = query.andWhere { TransactionsTable.isIncome eq isIncome }
        if (!categories.isNullOrEmpty()) query = query.andWhere { TransactionsTable.category inList categories }
        if (start != null) query = query.andWhere { TransactionsTable.dateTime greaterEq start.toJavaLocalDateTime() }
        if (end != null) query = query.andWhere { TransactionsTable.dateTime lessEq end.toJavaLocalDateTime() }
        // Cursor filter
        if (afterDateTime != null && afterId != null) {
            query = query.andWhere {
                (TransactionsTable.dateTime greater afterDateTime.toJavaLocalDateTime()) or
                        ((TransactionsTable.dateTime eq afterDateTime.toJavaLocalDateTime()) and
                                (TransactionsTable.id greater afterId))
            }
        }
        // Sorting
        val orderColumn = when (sortBy) {
            "amount" -> TransactionsTable.amount
            else -> TransactionsTable.dateTime
        }

        query.orderBy(orderColumn, order)
            .orderBy(TransactionsTable.id, order) // tie-breaker
            .limit(limit)
            .map { it.toTransaction() }
    }



    fun getById(id: Int, userId: Int): Transaction = transaction {
        TransactionsTable
            .selectAll().where { (TransactionsTable.id eq id) and (TransactionsTable.userId eq userId) }
            .map { it.toTransaction() }
            .singleOrNull()
            ?: throw NoSuchElementException("Transaction with id $id not found for user $userId")
    }


    fun add(entity: Transaction): Transaction = transaction {
        try {
            entity.validate()
        } catch (e: IllegalArgumentException) {
            throw ValidationException(e.message ?: "Invalid transaction")
        }
        val inserted = TransactionsTable.insert { row ->
            row[userId] = entity.userId
            row[accountId] = entity.accountId
            row[isIncome] = entity.isIncome
            row[amount] = entity.amount
            row[category] = entity.category
            row[dateTime] = entity.dateTime.toJavaLocalDateTime()
            row[description] = entity.description
        }.resultedValues?.singleOrNull()
            ?: throw IllegalStateException("Failed to insert transaction")

        inserted.toTransaction()
    }




    fun update(id: Int, userId: Int, entity: Transaction): Transaction = transaction {
        try {
            entity.validate()
        } catch (e: IllegalArgumentException) {
            throw ValidationException(e.message ?: "Invalid transaction")
        }
        val updated = TransactionsTable.update(
            where = { (TransactionsTable.id eq id) and (TransactionsTable.userId eq userId) }
        ) { row ->
            row[accountId] = entity.accountId
            row[isIncome] = entity.isIncome
            row[amount] = entity.amount
            row[category] = entity.category
            row[dateTime] = entity.dateTime.toJavaLocalDateTime()
            row[description] = entity.description
        }

        if (updated == 0) throw NoSuchElementException("Transaction with id $id not found for user $userId")

        getById(id, userId)
    }


    fun delete(id: Int, userId: Int): Boolean = transaction {
        val deleted = TransactionsTable.deleteWhere {
            (TransactionsTable.id eq id) and (TransactionsTable.userId eq userId)
        }
        if (deleted == 0) throw NoSuchElementException("Transaction with id $id not found for user $userId")
        true
    }

    fun getAccountAggregates(userId: Int, accountId: Int? = null): AccountAggregates = transaction {
        val baseQuery = TransactionsTable
            .select(TransactionsTable.amount)
            .where { TransactionsTable.userId eq userId }

        val accountFilter = accountId?.let { TransactionsTable.accountId eq it }

        val transactions = if (accountFilter != null) {
            baseQuery.andWhere { accountFilter }
        } else {
            baseQuery
        }.map { it[TransactionsTable.amount] }

        val income = transactions.filter { it > 0 }.sumOf { it }
        val expense = transactions.filter { it < 0 }.sumOf { -it }
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


    fun clearAll(userId: Int, accountId: Int? = null): Boolean = transaction {
        val deleted = if (accountId != null) {
            TransactionsTable.deleteWhere {
                (TransactionsTable.userId eq userId) and (TransactionsTable.accountId eq accountId)
            }
        } else {
            TransactionsTable.deleteWhere { TransactionsTable.userId eq userId }
        }
        deleted > 0
    }


    /**
     * Returns an OverviewSummary containing:
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

    data class AccountAggregates(
        val income: Double = 0.0,
        val expense: Double = 0.0,
        val balance: Double = 0.0
    )
}