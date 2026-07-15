package feature.transaction.data.table

import com.fintrack.core.data.TableNames
import com.fintrack.feature.transaction.data.TransactionsColumns
import com.fintrack.feature.user.UsersTable
import com.fintrack.feature.accounts.data.table.AccountsTable
import feature.category.data.table.CategoriesTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object TransactionsTable : UUIDTable(TableNames.TRANSACTIONS) {
    val userId =
        reference(TransactionsColumns.USER_ID, UsersTable, onDelete = ReferenceOption.CASCADE).index()
    val accountId = reference(TransactionsColumns.ACCOUNT_ID, AccountsTable, onDelete = ReferenceOption.CASCADE).index()
    val isIncome = bool(TransactionsColumns.IS_INCOME)
    val amount = decimal(TransactionsColumns.AMOUNT, precision = 19, scale = 4)
    val transactionCost = decimal(TransactionsColumns.TRANSACTION_COST, precision = 19, scale = 4).default(java.math.BigDecimal.ZERO)
    val categoryId = reference(TransactionsColumns.CATEGORY_ID, CategoriesTable, onDelete = ReferenceOption.CASCADE).index()
    val dateTime = timestamp(TransactionsColumns.DATE_TIME).index()
    val description = varchar(TransactionsColumns.DESCRIPTION, 255).nullable()
    val externalId = varchar(TransactionsColumns.EXTERNAL_ID, 100).nullable()
    val balance = decimal(TransactionsColumns.BALANCE, precision = 19, scale = 4).nullable()
    val createdAt = timestamp(TransactionsColumns.CREATED_AT).index()
    val updatedAt = timestamp(TransactionsColumns.UPDATED_AT).index()

    init {
        uniqueIndex(externalId, userId)
        // Industry Standard: Composite index for Equality (userId) + Sort/Range (dateTime)
        index(false, userId, dateTime)
        // Optimized for account-specific filtering
        index(false, userId, accountId, dateTime)
    }
}
