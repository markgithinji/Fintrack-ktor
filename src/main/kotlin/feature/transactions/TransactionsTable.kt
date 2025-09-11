package feature.transactions

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

object TransactionsTable : Table("transactions") {
    val id = integer("id").autoIncrement()
    val isIncome = bool("is_income")
    val amount = double("amount")
    val category = varchar("category", 100)
    val date = date("date")
    val description = varchar("description", 255).nullable()

    override val primaryKey = PrimaryKey(id)
}