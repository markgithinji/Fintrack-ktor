package com.fintrack.feature.transactions

import org.jetbrains.exposed.sql.Table

object TransactionsTable : Table("transactions") {
    val id = integer("id").autoIncrement()
    val type = varchar("type", 50) // "income" or "expense"
    val amount = double("amount")
    val category = varchar("category", 100)
    val date = varchar("date", 20) // ISO date string
    val description = varchar("description", 255).nullable()

    override val primaryKey = PrimaryKey(id)
}
