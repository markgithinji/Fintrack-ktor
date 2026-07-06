package feature.transaction.data

import com.fintrack.core.data.TableNames
import com.fintrack.feature.transaction.data.CategoriesColumns
import com.fintrack.feature.user.UsersTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object CategoriesTable : UUIDTable(TableNames.CATEGORIES) {
    val userId = reference(CategoriesColumns.USER_ID, UsersTable, onDelete = ReferenceOption.CASCADE)
    val name = varchar(CategoriesColumns.NAME, 100)
    val isExpense = bool(CategoriesColumns.IS_EXPENSE)
    val iconName = varchar(CategoriesColumns.ICON_NAME, 100).nullable()
    val isDefault = bool(CategoriesColumns.IS_DEFAULT).default(false)
    val createdAt = timestamp(CategoriesColumns.CREATED_AT)

    init {
        uniqueIndex(userId, name, isExpense)
    }
}
