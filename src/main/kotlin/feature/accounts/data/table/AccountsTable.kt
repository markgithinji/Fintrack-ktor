package com.fintrack.feature.accounts.data.table

import com.fintrack.core.data.TableNames
import com.fintrack.feature.user.UsersTable
import com.fintrack.feature.accounts.data.model.AccountTypeDto
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object AccountsTable : UUIDTable(TableNames.ACCOUNTS) {
    val userId = reference(AccountsColumns.USER_ID, UsersTable, onDelete = ReferenceOption.CASCADE)
    val name = varchar(AccountsColumns.NAME, 100)
    val isDefault = bool(AccountsColumns.IS_DEFAULT).default(false)
    val type = enumerationByName(AccountsColumns.TYPE, 20, AccountTypeDto::class).default(AccountTypeDto.OTHER)
    val linkedSources = text(AccountsColumns.LINKED_SOURCES).default("[]")
    val balance = decimal(AccountsColumns.BALANCE, precision = 19, scale = 4).default(java.math.BigDecimal.ZERO)
    val createdAt = timestamp(AccountsColumns.CREATED_AT)
    val lastSyncedAt = timestamp(AccountsColumns.LAST_SYNCED_AT).nullable()
}
