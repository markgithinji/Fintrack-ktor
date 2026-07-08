package feature.accounts.data

import com.fintrack.core.data.TableNames
import com.fintrack.feature.accounts.data.AccountsColumns
import com.fintrack.feature.user.UsersTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object AccountsTable : UUIDTable(TableNames.ACCOUNTS) {
    val userId = reference(AccountsColumns.USER_ID, UsersTable, onDelete = ReferenceOption.CASCADE)
    val name = varchar(AccountsColumns.NAME, 100)
    val isDefault = bool(AccountsColumns.IS_DEFAULT).default(false)
    val isMpesa = bool(AccountsColumns.IS_MPESA).default(false)
    val isEquity = bool(AccountsColumns.IS_EQUITY).default(false)
    val balance = double(AccountsColumns.BALANCE).default(0.0)
    val createdAt = timestamp(AccountsColumns.CREATED_AT)
}