package feature.transactions

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime

object TransactionsTable : Table("transactions") {
    val id = integer("id").autoIncrement()
    val isIncome = bool("is_income")
    val amount = double("amount")
    val category = varchar("category", 100)
    val dateTime = datetime("date_time")
    val description = varchar("description", 255).nullable()

    override val primaryKey = PrimaryKey(id)
}
