package com.fintrack.feature.transactions

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.sql.selectAll

class TransactionRepository {

    fun getAll(
        type: String? = null,
        categories: List<String>? = null,
        start: LocalDate? = null,
        end: LocalDate? = null,
        sortBy: String = "date",
        order: SortOrder = SortOrder.ASC,
        page: Int = 1,
        size: Int = 20
    ): List<Transaction> = transaction {
        TransactionsTable.selectAll()
            .map { it.toTransaction() }
            .filter { t ->
                (type == null || t.type == type) &&
                        (categories == null || categories.contains(t.category)) &&
                        (start == null || t.date >= start) &&
                        (end == null || t.date <= end)
            }
            .sortedWith(
                when (sortBy) {
                    "amount" -> compareBy<Transaction> { it.amount }
                    else -> compareBy<Transaction> { it.date }
                }.let { if (order == SortOrder.DESC) it.reversed() else it }
            )
            .drop((page - 1) * size)
            .take(size)
    }


    fun getById(id: Int): Transaction? = transaction {
        TransactionsTable
            .selectAll().where { TransactionsTable.id eq id }
            .map { it.toTransaction() }
            .singleOrNull()
    }

    fun add(entity: Transaction): Transaction = transaction {
        entity.validate()
        val inserted = TransactionsTable.insert { row ->
            row[type] = entity.type
            row[amount] = entity.amount
            row[category] = entity.category
            row[date] = entity.date.toString()
        }.resultedValues?.singleOrNull()

        inserted?.toTransaction() ?: throw IllegalStateException("Failed to insert transaction")
    }

    fun update(id: Int, entity: Transaction): Boolean = transaction {
        entity.validate()
        TransactionsTable.update({ TransactionsTable.id eq id }) { row ->
            row[type] = entity.type
            row[amount] = entity.amount
            row[category] = entity.category
            row[date] = entity.date.toString()
        } > 0
    }

    fun delete(id: Int): Boolean = transaction {
        TransactionsTable.deleteWhere { TransactionsTable.id eq id } > 0
    }

    fun getSummary(
        type: String? = null,
        start: LocalDate? = null,
        end: LocalDate? = null,
        byCategory: Boolean = false,
        monthly: Boolean = false
    ): Map<String, Any> = transaction {

        val filtered = TransactionsTable.selectAll()
            .map { it.toTransaction() }
            .filter { t ->
                (type == null || t.type == type) &&
                        (start == null || t.date >= start) &&
                        (end == null || t.date <= end)
            }

        val income = filtered.filter { it.type == "income" }.sumOf { it.amount }
        val expense = filtered.filter { it.type == "expense" }.sumOf { it.amount }

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
            val monthlyBreakdown = filtered.groupBy { "${it.date.year}-${it.date.monthNumber}" }
                .mapValues { entry ->
                    val incomeMonth = entry.value.filter { it.type == "income" }.sumOf { it.amount }
                    val expenseMonth = entry.value.filter { it.type == "expense" }.sumOf { it.amount }
                    mapOf("income" to incomeMonth, "expense" to expenseMonth, "balance" to incomeMonth - expenseMonth)
                }
            result["monthly"] = monthlyBreakdown
        }

        result
    }

    private fun ResultRow.toTransaction() = Transaction(
        id = this[TransactionsTable.id],
        type = this[TransactionsTable.type],
        amount = this[TransactionsTable.amount],
        category = this[TransactionsTable.category],
        date = LocalDate.parse(this[TransactionsTable.date])
    )

}
