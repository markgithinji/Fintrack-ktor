package feature.budget.data

import com.fintrack.core.data.TableNames
import com.fintrack.feature.user.UsersTable
import com.fintrack.feature.accounts.data.table.AccountsTable
import feature.category.data.table.CategoriesTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object BudgetsTable : UUIDTable(TableNames.BUDGETS) {
    val userId = reference(BudgetsColumns.USER_ID, UsersTable, onDelete = ReferenceOption.CASCADE)
    val accountId = reference(BudgetsColumns.ACCOUNT_ID, AccountsTable, onDelete = ReferenceOption.CASCADE).nullable()
    val name = varchar(BudgetsColumns.NAME, 100)
    val limit = decimal(BudgetsColumns.LIMIT, precision = 19, scale = 4)
    val isExpense = bool(BudgetsColumns.IS_EXPENSE)
    val startDate = date(BudgetsColumns.START_DATE)
    val endDate = date(BudgetsColumns.END_DATE)
    val createdAt = timestamp(BudgetsColumns.CREATED_AT)
    val updatedAt = timestamp(BudgetsColumns.UPDATED_AT)
}

object BudgetAccountsTable : Table(TableNames.BUDGET_ACCOUNTS) {
    val budgetId = reference("budget_id", BudgetsTable, onDelete = ReferenceOption.CASCADE)
    val accountId = reference("account_id", AccountsTable, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(budgetId, accountId)
}

object BudgetCategoriesTable : Table(TableNames.BUDGET_CATEGORIES) {
    val budgetId = reference("budget_id", BudgetsTable, onDelete = ReferenceOption.CASCADE)
    val categoryId = reference("category_id", CategoriesTable, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(budgetId, categoryId)
}
