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
        if (start != null) filteredQuery =
            filteredQuery.andWhere { TransactionsTable.dateTime greaterEq start.toJavaLocalDateTime() }
        if (end != null) filteredQuery =
            filteredQuery.andWhere { TransactionsTable.dateTime lessEq end.toJavaLocalDateTime() }

        val filtered = filteredQuery.map { it.toTransaction() }

        val incomeTxns = filtered.filter { it.isIncome }
        val expenseTxns = filtered.filter { !it.isIncome }

        val income = incomeTxns.sumOf { it.amount }
        val expense = expenseTxns.sumOf { it.amount }
        val balance = income - expense

        // Highest month (income & expense separately)
        fun highestMonth(txns: List<Transaction>) =
            txns.groupBy { "${it.dateTime.year}-${it.dateTime.monthNumber.toString().padStart(2, '0')}" }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .maxByOrNull { it.value }
                ?.let { HighestMonth(month = it.key, amount = it.value) }

        val highestIncomeMonth = highestMonth(incomeTxns)
        val highestExpenseMonth = highestMonth(expenseTxns)

        // Highest category (income & expense separately)
        fun highestCategory(txns: List<Transaction>) =
            txns.groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .maxByOrNull { it.value }
                ?.let { HighestCategory(category = it.key, amount = it.value) }

        val highestIncomeCategory = highestCategory(incomeTxns)
        val highestExpenseCategory = highestCategory(expenseTxns)

        // Highest day (income & expense separately)
        fun highestDay(txns: List<Transaction>) =
            txns.groupBy { it.dateTime.date }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .maxByOrNull { it.value }
                ?.let { HighestDay(date = it.key, amount = it.value) }

        val highestIncomeDay = highestDay(incomeTxns)
        val highestExpenseDay = highestDay(expenseTxns)

        // Average per day
        val distinctDays = expenseTxns.groupBy { it.dateTime.date }.size.coerceAtLeast(1)
        val averagePerDay = if (distinctDays > 0) expense / distinctDays else 0.0

        // Weekly & monthly category breakdown
        val weeklyCategorySummary = filtered.groupBy {
            val week = it.dateTime.toJavaLocalDateTime()
                .get(WeekFields.ISO.weekOfYear())
            "${it.dateTime.year}-W${week.toString().padStart(2, '0')}"
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

        val monthlyCategorySummary = filtered.groupBy {
            "${it.dateTime.year}-${it.dateTime.monthNumber.toString().padStart(2, '0')}"
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
            highestMonth = highestExpenseMonth,
            highestCategory = highestExpenseCategory,
            highestDay = highestExpenseDay,
            highestIncomeMonth = highestIncomeMonth,
            highestIncomeCategory = highestIncomeCategory,
            highestIncomeDay = highestIncomeDay,
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
        dateTime = this[TransactionsTable.dateTime].toKotlinLocalDateTime(),
        description = this[TransactionsTable.description]
    )
}
