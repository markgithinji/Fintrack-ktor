package feature.transactions

import core.TransactionsTable
import com.fintrack.feature.transactions.Transaction
import core.ValidationException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import org.jetbrains.exposed.sql.selectAll

class TransactionRepository {

    fun getAll(
        isIncome: Boolean? = null,
        categories: List<String>? = null,
        start: LocalDate? = null,
        end: LocalDate? = null,
        sortBy: String = "date",
        order: SortOrder = SortOrder.ASC,
        page: Int = 1,
        size: Int = 20
    ): List<Transaction> = transaction {
        var query: Query = TransactionsTable.selectAll()

        // Apply filters
        if (isIncome != null) query = query.andWhere { TransactionsTable.isIncome eq isIncome }
        if (!categories.isNullOrEmpty()) query = query.andWhere { TransactionsTable.category inList categories }
        if (start != null) query = query.andWhere { TransactionsTable.date greaterEq start.toJavaLocalDate() }
        if (end != null) query = query.andWhere { TransactionsTable.date lessEq end.toJavaLocalDate() }

        // Sorting
        val orderColumn = when (sortBy) {
            "amount" -> TransactionsTable.amount
            else -> TransactionsTable.date
        }
        query.orderBy(orderColumn, order)

        // Pagination
        query.limit(n = size, offset = ((page - 1) * size).toLong())
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
        end: LocalDate? = null,
        byCategory: Boolean = false,
        monthly: Boolean = false
    ): Map<String, Any> = transaction {

        var filteredQuery: Query = TransactionsTable.selectAll()
        if (isIncome != null) filteredQuery = filteredQuery.andWhere { TransactionsTable.isIncome eq isIncome }
        if (start != null) filteredQuery =
            filteredQuery.andWhere { TransactionsTable.date greaterEq start.toJavaLocalDate() }
        if (end != null) filteredQuery = filteredQuery.andWhere { TransactionsTable.date lessEq end.toJavaLocalDate() }

        val filtered = filteredQuery.map { it.toTransaction() }

        val income = filtered.filter { it.isIncome }.sumOf { it.amount }
        val expense = filtered.filter { !it.isIncome }.sumOf { it.amount }

        val result = mutableMapOf<String, Any>(
            "income" to income,
            "expense" to expense,
            "balance" to (income - expense)
        )

        if (byCategory) {
            val categoryBreakdown = filtered.groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
            result["categoryBreakdown"] = categoryBreakdown
        }

        if (monthly) {
            val monthlyBreakdown =
                filtered.groupBy { "${it.date.year}-${it.date.monthNumber.toString().padStart(2, '0')}" }
                    .mapValues { entry ->
                        val incomeMonth = entry.value.filter { it.isIncome }.sumOf { it.amount }
                        val expenseMonth = entry.value.filter { !it.isIncome }.sumOf { it.amount }
                        mapOf(
                            "income" to incomeMonth,
                            "expense" to expenseMonth,
                            "balance" to incomeMonth - expenseMonth
                        )
                    }
            result["monthly"] = monthlyBreakdown
        }

        result
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
}
