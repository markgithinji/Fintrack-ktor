package feature.transactions

import core.AvailableMonths
import core.AvailableWeeks
import core.AvailableYears
import core.ValidationException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toKotlinLocalDateTime
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.selectAll
import java.time.temporal.WeekFields

class TransactionRepository {

    fun getAllCursor(
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


    fun getById(id: Int): Transaction = transaction {
        TransactionsTable.selectAll().where { TransactionsTable.id eq id }
            .map { it.toTransaction() }
            .singleOrNull()
            ?: throw NoSuchElementException("Transaction with id $id not found")
    }

    fun add(entity: Transaction): Transaction = transaction {
        try {
            entity.validate()
        } catch (e: IllegalArgumentException) {
            throw ValidationException(e.message ?: "Invalid transaction")
        }

        val inserted = TransactionsTable.insert { row ->
            row[isIncome] = entity.isIncome
            row[amount] = entity.amount
            row[category] = entity.category
            row[dateTime] = entity.dateTime.toJavaLocalDateTime()
            row[description] = entity.description
        }.resultedValues?.singleOrNull()
            ?: throw IllegalStateException("Failed to insert transaction")

        inserted.toTransaction()
    }


    fun update(id: Int, entity: Transaction): Transaction = transaction {
        try {
            entity.validate()
        } catch (e: IllegalArgumentException) {
            throw ValidationException(e.message ?: "Invalid transaction")
        }

        val updated = TransactionsTable.update({ TransactionsTable.id eq id }) { row ->
            row[isIncome] = entity.isIncome
            row[amount] = entity.amount
            row[category] = entity.category
            row[dateTime] = entity.dateTime.toJavaLocalDateTime()
            row[description] = entity.description
        }

        if (updated == 0) throw NoSuchElementException("Transaction with id $id not found")
        getById(id) // return updated transaction
    }


    fun delete(id: Int): Boolean = transaction {
        val deleted = TransactionsTable.deleteWhere { TransactionsTable.id eq id }
        if (deleted == 0) throw NoSuchElementException("Transaction with id $id not found")
        true
    }

    fun getHighlightsSummary(
        isIncome: Boolean? = null,
        start: LocalDateTime? = null,
        end: LocalDateTime? = null
    ): HighlightsSummary = transaction {
        var filteredQuery: Query = TransactionsTable.selectAll()
        if (isIncome != null) filteredQuery = filteredQuery.andWhere { TransactionsTable.isIncome eq isIncome }
        if (start != null) filteredQuery = filteredQuery.andWhere {
            TransactionsTable.dateTime greaterEq start.toJavaLocalDateTime()
        }
        if (end != null) filteredQuery = filteredQuery.andWhere {
            TransactionsTable.dateTime lessEq end.toJavaLocalDateTime()
        }

        val filtered = filteredQuery.map { it.toTransaction() }

        val incomeTxns = filtered.filter { it.isIncome }
        val expenseTxns = filtered.filter { !it.isIncome }

        val income = incomeTxns.sumOf { it.amount }
        val expense = expenseTxns.sumOf { it.amount }
        val balance = income - expense

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
                    val displayName = txns.firstOrNull { it.category.trim().lowercase() == entry.key }?.category
                        ?: entry.key
                    Highlight(label = displayName, value = displayName, amount = entry.value)
                }

        fun highestDay(txns: List<Transaction>) =
            txns.groupBy { it.dateTime.date }
                .mapValues { it.value.sumOf { t -> t.amount } }
                .maxByOrNull { it.value }
                ?.let { Highlight(label = it.key.toString(), value = it.key.toString(), amount = it.value) }

        fun averagePerDay(txns: List<Transaction>, total: Double): Double {
            val days = txns.groupBy { it.dateTime.date }.size.coerceAtLeast(1)
            return total / days
        }

        val incomeHighlights = Highlights(
            highestMonth = highestMonth(incomeTxns),
            highestCategory = highestCategory(incomeTxns),
            highestDay = highestDay(incomeTxns),
            averagePerDay = averagePerDay(incomeTxns, income)
        )

        val expenseHighlights = Highlights(
            highestMonth = highestMonth(expenseTxns),
            highestCategory = highestCategory(expenseTxns),
            highestDay = highestDay(expenseTxns),
            averagePerDay = averagePerDay(expenseTxns, expense)
        )

        HighlightsSummary(
            income = income,
            expense = expense,
            balance = balance,
            incomeHighlights = incomeHighlights,
            expenseHighlights = expenseHighlights
        )
    }

    // --- For a single DistributionSummary for a given period ---
    fun getDistributionSummary(
        period: String,
        isIncome: Boolean? = null,
        start: LocalDateTime? = null,
        end: LocalDateTime? = null
    ): DistributionSummary = transaction {
        // --- Filter transactions by type and optional date ---
        var query: Query = TransactionsTable.selectAll()

        if (isIncome != null) {
            query = query.andWhere { TransactionsTable.isIncome eq isIncome }
        }
        if (start != null) {
            query = query.andWhere { TransactionsTable.dateTime greaterEq start.toJavaLocalDateTime() }
        }
        if (end != null) {
            query = query.andWhere { TransactionsTable.dateTime lessEq end.toJavaLocalDateTime() }
        }

        val filtered = query.map { it.toTransaction() }

        val incomeTxns = filtered.filter { it.isIncome }
        val expenseTxns = filtered.filter { !it.isIncome }

        // --- Determine period type ---
        val weekMode = period.contains("W")
        val yearMode = !weekMode && period.length == 4
        val monthMode = !weekMode && !yearMode

        fun categorySummary(txns: List<Transaction>): List<CategorySummary> {
            // Group transactions by the period
            val grouped = when {
                weekMode -> txns.groupBy {
                    val week = it.dateTime.toJavaLocalDateTime()
                        .get(WeekFields.ISO.weekOfWeekBasedYear())
                    "${it.dateTime.year}-W${week.toString().padStart(2, '0')}"
                }
                monthMode -> txns.groupBy {
                    "${it.dateTime.year}-${it.dateTime.monthNumber.toString().padStart(2, '0')}"
                }
                yearMode -> txns.groupBy { it.dateTime.year.toString() }
                else -> emptyMap()
            }

            // --- Get transactions for the requested period ---
            val txnsInPeriod = if (yearMode) {
                grouped[period].orEmpty()
            } else if (monthMode || weekMode) {
                grouped[period].orEmpty()
            } else {
                emptyList()
            }

            // --- Aggregate by category ---
            val deduped = txnsInPeriod.groupBy { it.category.trim().lowercase() }.map { (_, list) ->
                val sum = list.sumOf { it.amount }
                val displayName = list.first().category
                CategorySummary(category = displayName, total = sum, percentage = 0.0)
            }

            // --- Compute percentages ---
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


    fun getAvailableWeeks(): AvailableWeeks = transaction {
        val weeks = TransactionsTable
            .selectAll()
            .map { it[TransactionsTable.dateTime].toLocalDate() }
            .map { date ->
                val year = date.year
                val week = date.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                "%04d-W%02d".format(year, week)
            }
            .distinct()
            .sortedDescending()

        AvailableWeeks(weeks)
    }

    fun getAvailableMonths(): AvailableMonths = transaction {
        val months = TransactionsTable
            .selectAll()
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

    fun getAvailableYears(): AvailableYears = transaction {
        val years = TransactionsTable
            .selectAll()
            .map { it[TransactionsTable.dateTime].toLocalDate().year.toString() }
            .distinct()
            .sortedDescending()

        AvailableYears(years)
    }


    fun clearAll(): Boolean = transaction {
        TransactionsTable.deleteAll() > 0
    }

    private fun ResultRow.toTransaction() = Transaction(
        id = this[TransactionsTable.id],
        isIncome = this[TransactionsTable.isIncome],
        amount = this[TransactionsTable.amount],
        category = this[TransactionsTable.category],
        dateTime = this[TransactionsTable.dateTime].toKotlinLocalDateTime(),
        description = this[TransactionsTable.description]
    )
}
