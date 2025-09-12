package feature.transactions

import core.ValidationException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import org.jetbrains.exposed.sql.selectAll
import java.time.temporal.WeekFields

class TransactionRepository {

    fun getAllCursor(
        isIncome: Boolean? = null,
        categories: List<String>? = null,
        start: LocalDate? = null,
        end: LocalDate? = null,
        sortBy: String = "date",
        order: SortOrder = SortOrder.ASC,
        limit: Int = 20,
        afterDate: LocalDate? = null,
        afterId: Int? = null
    ): List<Transaction> = transaction {
        var query = TransactionsTable.selectAll()

        // Apply filters
        if (isIncome != null) query = query.andWhere { TransactionsTable.isIncome eq isIncome }
        if (!categories.isNullOrEmpty()) query = query.andWhere { TransactionsTable.category inList categories }
        if (start != null) query = query.andWhere { TransactionsTable.date greaterEq start.toJavaLocalDate() }
        if (end != null) query = query.andWhere { TransactionsTable.date lessEq end.toJavaLocalDate() }

        // Cursor filter
        if (afterDate != null && afterId != null) {
            query = query.andWhere {
                (TransactionsTable.date greater afterDate.toJavaLocalDate()) or
                        ((TransactionsTable.date eq afterDate.toJavaLocalDate()) and
                                (TransactionsTable.id greater afterId))
            }
        }

        // Sorting
        val orderColumn = when (sortBy) {
            "amount" -> TransactionsTable.amount
            else -> TransactionsTable.date
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
            row[date] = entity.date.toJavaLocalDate()
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
            row[date] = entity.date.toJavaLocalDate()
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
        start: LocalDate? = null,
        end: LocalDate? = null
    ): Summary = transaction {
        var filteredQuery: Query = TransactionsTable.selectAll()
        if (isIncome != null) filteredQuery = filteredQuery.andWhere { TransactionsTable.isIncome eq isIncome }
        if (start != null) filteredQuery =
            filteredQuery.andWhere { TransactionsTable.date greaterEq start.toJavaLocalDate() }
        if (end != null) filteredQuery = filteredQuery.andWhere { TransactionsTable.date lessEq end.toJavaLocalDate() }

        val filtered = filteredQuery.map { it.toTransaction() }

        val income = filtered.filter { it.isIncome }.sumOf { it.amount }
        val expense = filtered.filter { !it.isIncome }.sumOf { it.amount }
        val balance = income - expense

        // Highest month
        val monthlyTotals = filtered.groupBy { "${it.date.year}-${it.date.monthNumber.toString().padStart(2, '0')}" }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        val highestMonth = monthlyTotals.maxByOrNull { it.value }?.let {
            HighestMonth(month = it.key, amount = it.value)
        }

        // Highest category
        val categoryTotals = filtered.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        val highestCategory = categoryTotals.maxByOrNull { it.value }?.let {
            HighestCategory(category = it.key, amount = it.value)
        }

        // Highest daily spending
        val dailyTotals = filtered.groupBy { it.date }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        val highestDay = dailyTotals.maxByOrNull { it.value }?.let {
            HighestDay(date = it.key, amount = it.value)
        }

        // Average per day (only count days with transactions)
        val distinctDays = dailyTotals.size.coerceAtLeast(1)
        val averagePerDay = if (distinctDays > 0) expense / distinctDays else 0.0

        // Weekly category breakdown
        val weeklyCategorySummary = filtered.groupBy {
            val week = it.date.toJavaLocalDate()
                .get(WeekFields.ISO.weekOfYear())
            "${it.date.year}-W${week.toString().padStart(2, '0')}"
        }.mapValues { (_, txns) ->
            val total = txns.sumOf { it.amount }
            txns.groupBy { it.category }.map { (cat, list) ->
                val sum = list.sumOf { it.amount }
                CategorySummary(
                    category = cat,
                    total = sum,
                    percentage = if (total > 0) (sum / total) * 100 else 0.0
                )
            }
        }

        // Monthly category breakdown
        val monthlyCategorySummary = filtered.groupBy {
            "${it.date.year}-${it.date.monthNumber.toString().padStart(2, '0')}"
        }.mapValues { (_, txns) ->
            val total = txns.sumOf { it.amount }
            txns.groupBy { it.category }.map { (cat, list) ->
                val sum = list.sumOf { it.amount }
                CategorySummary(
                    category = cat,
                    total = sum,
                    percentage = if (total > 0) (sum / total) * 100 else 0.0
                )
            }
        }

        Summary(
            income = income,
            expense = expense,
            balance = balance,
            highestMonth = highestMonth,
            highestCategory = highestCategory,
            highestDay = highestDay,
            averagePerDay = averagePerDay,
            weeklyCategorySummary = weeklyCategorySummary,
            monthlyCategorySummary = monthlyCategorySummary
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
        date = this[TransactionsTable.date].toKotlinLocalDate(),
        description = this[TransactionsTable.description]
    )

    val LocalDate.weekOfYear: Int
        get() = this.toJavaLocalDate().get(WeekFields.ISO.weekOfYear())
}
