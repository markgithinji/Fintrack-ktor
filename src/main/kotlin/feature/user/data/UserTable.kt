package com.fintrack.feature.user

import com.fintrack.core.TableNames
import com.fintrack.feature.user.data.UsersColumns
import org.jetbrains.exposed.dao.id.UUIDTable

object UsersTable : UUIDTable(TableNames.USERS) {
    val email = varchar(UsersColumns.EMAIL, 100).uniqueIndex()
    val passwordHash = varchar(UsersColumns.PASSWORD_HASH, 255)
}