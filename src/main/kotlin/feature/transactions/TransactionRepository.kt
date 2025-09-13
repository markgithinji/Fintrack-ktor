package feature.transactions

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

    fun getSummary(
        isIncome: Boolean? = null,
        start: LocalDateTime? = null,
        end: LocalDateTime? = null
    ): Summary = transaction {

        var filteredQuery: Query = TransactionsTable.selectAll()
        if (isIncome != null) filteredQuery = filteredQuery.andWhere { TransactionsTable.isIncome eq isIncome }
        if (start != null)
            filteredQuery = filteredQuery.andWhere { TransactionsTable.dateTime greaterEq start.toJavaLocalDateTime() }
        if (end != null)
            filteredQuery = filteredQuery.andWhere { TransactionsTable.dateTime lessEq end.toJavaLocalDateTime() }

        val filtered = filteredQuery.map { it.toTransaction() }

        val incomeTxns = filtered.filter { it.isIncome }
        val expenseTxns = filtered.filter { !it.isIncome }

        val income = incomeTxns.sumOf { it.amount }
        val expense = expenseTxns.sumOf { it.amount }
        val balance = income - expense

        // Highest month
        fun highestMonth(txns: List<Transaction>) =
            txns.groupBy { "${it.dateTime.year}-${it.dateTime.monthNumber.toString().padStart(2, '0')}" }
                .mapValues { it.value.sumOf { t -> t.amount } }
                .maxByOrNull { it.value }
                ?.let { HighestMonth(month = it.key, amount = it.value) }

        val highestIncomeMonth = highestMonth(incomeTxns)
        val highestExpenseMonth = highestMonth(expenseTxns)

        // Highest category
        fun highestCategory(txns: List<Transaction>): HighestCategory? {
            val grouped = txns.groupBy { it.category.trim().lowercase() }
                .mapValues { it.value.sumOf { t -> t.amount } }
            val maxEntry = grouped.maxByOrNull { it.value } ?: return null
            val displayName = txns.firstOrNull { it.category.trim().lowercase() == maxEntry.key }?.category ?: maxEntry.key
            return HighestCategory(category = displayName, amount = maxEntry.value)
        }

        val highestIncomeCategory = highestCategory(incomeTxns)
        val highestExpenseCategory = highestCategory(expenseTxns)

        // Highest day
        fun highestDay(txns: List<Transaction>) =
            txns.groupBy { it.dateTime.date }
                .mapValues { it.value.sumOf { t -> t.amount } }
                .maxByOrNull { it.value }
                ?.let { HighestDay(date = it.key, amount = it.value) }

        val highestIncomeDay = highestDay(incomeTxns)
        val highestExpenseDay = highestDay(expenseTxns)

        // Average per day
        val expenseDays = expenseTxns.groupBy { it.dateTime.date }.size.coerceAtLeast(1)
        val averagePerDay = expense / expenseDays

        val incomeDays = incomeTxns.groupBy { it.dateTime.date }.size.coerceAtLeast(1)
        val averageIncomePerDay = income / incomeDays

        // Weekly & monthly category summary helper with deduplication
        fun categorySummary(txns: List<Transaction>, weekMode: Boolean = true): Map<String, List<CategorySummary>> {
            val grouped = if (weekMode) {
                txns.groupBy {
                    val week = it.dateTime.toJavaLocalDateTime().get(WeekFields.ISO.weekOfYear())
                    "${it.dateTime.year}-W${week.toString().padStart(2, '0')}"
                }
            } else {
                txns.groupBy { "${it.dateTime.year}-${it.dateTime.monthNumber.toString().padStart(2, '0')}" }
            }

            return grouped.mapValues { (_, txnsInPeriod) ->
                // Deduplicate categories by normalizing
                val deduped = txnsInPeriod.groupBy { it.category.trim().lowercase() }
                    .map { (_, list) ->
                        val sum = list.sumOf { it.amount }
                        val displayName = list.first().category // preserve original casing
                        CategorySummary(category = displayName, total = sum, percentage = 0.0)
                    }

                // Compute percentages
                val totalAmount = deduped.sumOf { it.total }
                deduped.map { it.copy(percentage = if (totalAmount > 0) (it.total / totalAmount) * 100 else 0.0) }
            }
        }

        val weeklyCategorySummary = categorySummary(expenseTxns, weekMode = true)
        val monthlyCategorySummary = categorySummary(expenseTxns, weekMode = false)
        val weeklyIncomeCategorySummary = categorySummary(incomeTxns, weekMode = true)
        val monthlyIncomeCategorySummary = categorySummary(incomeTxns, weekMode = false)

        Summary(
            income = income,
            expense = expense,
            balance = balance,
            highestMonth = highestExpenseMonth,
            highestCategory = highestExpenseCategory,
            highestDay = highestExpenseDay,
            averagePerDay = averagePerDay,
            highestIncomeMonth = highestIncomeMonth,
            highestIncomeCategory = highestIncomeCategory,
            highestIncomeDay = highestIncomeDay,
            averageIncomePerDay = averageIncomePerDay,
            weeklyCategorySummary = weeklyCategorySummary,
            monthlyCategorySummary = monthlyCategorySummary,
            weeklyIncomeCategorySummary = weeklyIncomeCategorySummary,
            monthlyIncomeCategorySummary = monthlyIncomeCategorySummary
        )
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
