package com.fintrack.feature.transactions

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class TransactionRepository {

    fun getAll(): List<Transaction> = transaction {
        TransactionsTable.selectAll().map {
            it.toTransaction()
        }
    }

    fun getById(id: Int): Transaction? = transaction {
        TransactionsTable
            .selectAll().where { TransactionsTable.id eq id }
            .map { it.toTransaction() }
            .singleOrNull()
    }

    fun add(entity: Transaction): Transaction = transaction {
        val inserted = TransactionsTable.insert { row ->
            row[type] = entity.type
            row[amount] = entity.amount
            row[category] = entity.category
            row[date] = entity.date
        }.resultedValues?.singleOrNull()

        inserted?.toTransaction()
            ?: throw IllegalStateException("Failed to insert transaction")
    }

    fun update(id: Int, entity: Transaction): Boolean = transaction {
        TransactionsTable.update({ TransactionsTable.id eq id }) { row ->
            row[type] = entity.type
            row[amount] = entity.amount
            row[category] = entity.category
            row[date] = entity.date
        } > 0
    }

    fun delete(id: Int): Boolean = transaction {
        TransactionsTable.deleteWhere { TransactionsTable.id eq id } > 0
    }

    // ✅ Extension to avoid repeating the mapping
    private fun ResultRow.toTransaction() = Transaction(
        id = this[TransactionsTable.id],
        type = this[TransactionsTable.type],
        amount = this[TransactionsTable.amount],
        category = this[TransactionsTable.category],
        date = this[TransactionsTable.date]
    )
}
