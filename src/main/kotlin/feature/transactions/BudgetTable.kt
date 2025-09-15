package feature.transactions

import feature.transactions.TransactionsTable.integer
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

object BudgetsTable : Table("budgets") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100)
    val categories = text("categories")
    val limit = double("limit")
    val isExpense = bool("is_expense")
    val startDate = date("start_date")
    val endDate = date("end_date")
    override val primaryKey = PrimaryKey(id)
}
