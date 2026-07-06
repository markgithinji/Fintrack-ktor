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
    val isDefault = bool("is_default").default(false)
    val isMpesa = bool("is_mpesa").default(false)
    val createdAt = timestamp(AccountsColumns.CREATED_AT)
}