package com.fintrack.feature.accounts.data.table

import com.fintrack.core.data.TableNames
import com.fintrack.feature.user.UsersTable
import com.fintrack.feature.accounts.domain.model.AccountType
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object AccountsTable : UUIDTable(TableNames.ACCOUNTS) {
    val userId = reference(AccountsColumns.USER_ID, UsersTable, onDelete = ReferenceOption.CASCADE)
    val name = varchar(AccountsColumns.NAME, 100)
    val isDefault = bool(AccountsColumns.IS_DEFAULT).default(false)
    val type = enumerationByName(AccountsColumns.TYPE, 20, AccountType::class).default(AccountType.GENERAL)
    val balance = double(AccountsColumns.BALANCE).default(0.0)
    val createdAt = timestamp(AccountsColumns.CREATED_AT)
}