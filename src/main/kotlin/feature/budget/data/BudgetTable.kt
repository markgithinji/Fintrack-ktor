package feature.transaction

import com.fintrack.core.TableNames
import com.fintrack.feature.budget.data.BudgetsColumns
import com.fintrack.feature.user.UsersTable
import feature.accounts.data.AccountsTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.date

object BudgetsTable : UUIDTable(TableNames.BUDGETS) {
    val userId = reference(BudgetsColumns.USER_ID, UsersTable, onDelete = ReferenceOption.CASCADE)
    val accountId = reference(BudgetsColumns.ACCOUNT_ID, AccountsTable)
    val name = varchar(BudgetsColumns.NAME, 100)
    val categories = text(BudgetsColumns.CATEGORIES)
    val limit = double(BudgetsColumns.LIMIT)
    val isExpense = bool(BudgetsColumns.IS_EXPENSE)
    val startDate = date(BudgetsColumns.START_DATE)
    val endDate = date(BudgetsColumns.END_DATE)
}