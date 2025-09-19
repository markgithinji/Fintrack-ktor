package com.fintrack.feature.user

import org.jetbrains.exposed.dao.id.IntIdTable

object UsersTable : IntIdTable("users") {
    val username = varchar("username", 50).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
}
