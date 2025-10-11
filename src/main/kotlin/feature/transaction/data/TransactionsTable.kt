package feature.transaction.data

import com.fintrack.core.data.TableNames
import com.fintrack.feature.transaction.data.TransactionsColumns
import com.fintrack.feature.user.UsersTable
import feature.accounts.data.AccountsTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.datetime

object TransactionsTable : UUIDTable(TableNames.TRANSACTIONS) {
    val userId =
        reference(TransactionsColumns.USER_ID, UsersTable, onDelete = ReferenceOption.CASCADE)
    val accountId = reference(TransactionsColumns.ACCOUNT_ID, AccountsTable)
    val isIncome = bool(TransactionsColumns.IS_INCOME)
    val amount = double(TransactionsColumns.AMOUNT)
    val category = varchar(TransactionsColumns.CATEGORY, 100)
    val dateTime = datetime(TransactionsColumns.DATE_TIME)
    val description = varchar(TransactionsColumns.DESCRIPTION, 255).nullable()
}