package feature.transaction.data.table

import com.fintrack.core.data.TableNames
import com.fintrack.feature.transaction.data.TransactionsColumns
import com.fintrack.feature.user.UsersTable
import com.fintrack.feature.accounts.data.table.AccountsTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object TransactionsTable : UUIDTable(TableNames.TRANSACTIONS) {
    val userId =
        reference(TransactionsColumns.USER_ID, UsersTable, onDelete = ReferenceOption.CASCADE).index()
    val accountId = reference(TransactionsColumns.ACCOUNT_ID, AccountsTable).index()
    val isIncome = bool(TransactionsColumns.IS_INCOME)
    val amount = double(TransactionsColumns.AMOUNT)
    val transactionCost = double(TransactionsColumns.TRANSACTION_COST).default(0.0)
    val category = varchar(TransactionsColumns.CATEGORY, 100)
    val dateTime = timestamp(TransactionsColumns.DATE_TIME).index()
    val description = varchar(TransactionsColumns.DESCRIPTION, 255).nullable()
    val externalId = varchar(TransactionsColumns.EXTERNAL_ID, 100).nullable()
    val balance = double(TransactionsColumns.BALANCE).nullable()

    init {
        uniqueIndex(externalId, userId)
        // Industry Standard: Composite index for Equality (userId) + Sort/Range (dateTime)
        index(false, userId, dateTime)
        // Optimized for account-specific filtering
        index(false, userId, accountId, dateTime)
    }
}