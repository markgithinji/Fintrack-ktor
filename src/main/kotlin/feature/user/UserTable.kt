package com.fintrack.feature.user

import feature.transactions.BudgetsTable.integer
import org.jetbrains.exposed.sql.Table

import org.jetbrains.exposed.dao.id.IntIdTable

object UsersTable : IntIdTable("users") {
    val username = varchar("username", 50).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
}
