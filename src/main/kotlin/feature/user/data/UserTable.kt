package com.fintrack.feature.user

import com.fintrack.core.data.TableNames
import com.fintrack.feature.user.data.UsersColumns
import feature.category.data.table.CategoriesTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object UsersTable : UUIDTable(TableNames.USERS) {
    val email = varchar(UsersColumns.EMAIL, 100).uniqueIndex()
    val name = varchar(UsersColumns.NAME, 100)
    val passwordHash = varchar(UsersColumns.PASSWORD_HASH, 255)
    val isEmailVerified = bool("is_email_verified").default(false)
}

object UserTrackedCategoriesTable : Table(TableNames.USER_TRACKED_CATEGORIES) {
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val categoryId = reference("category_id", CategoriesTable, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(userId, categoryId)
}
