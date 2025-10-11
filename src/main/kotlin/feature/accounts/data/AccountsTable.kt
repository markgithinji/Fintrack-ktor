package feature.accounts.data

import com.fintrack.core.data.TableNames
import com.fintrack.feature.accounts.data.AccountsColumns
import com.fintrack.feature.user.UsersTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption

object AccountsTable : UUIDTable(TableNames.ACCOUNTS) {
    val userId = reference(AccountsColumns.USER_ID, UsersTable, onDelete = ReferenceOption.CASCADE)
    val name = varchar(AccountsColumns.NAME, 100)
}