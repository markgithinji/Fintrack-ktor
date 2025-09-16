package com.fintrack.feature.user

import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
class UserRepository {

    fun createUser(username: String, password: String): Int = transaction {
        val hashed = BCrypt.hashpw(password, BCrypt.gensalt())
        UsersTable.insertAndGetId {
            it[UsersTable.username] = username
            it[UsersTable.passwordHash] = hashed
        }.value
    }

    fun findByUsername(username: String): User? = transaction {
        UsersTable.selectAll().where { UsersTable.username eq username }
            .singleOrNull()
            ?.let {
                User(
                    id = it[UsersTable.id].value,
                    username = it[UsersTable.username],
                    passwordHash = it[UsersTable.passwordHash]
                )
            }
    }

    fun findById(userId: Int): User? = transaction {
        UsersTable.selectAll().where { UsersTable.id eq userId }
            .singleOrNull()
            ?.let {
                User(
                    id = it[UsersTable.id].value,
                    username = it[UsersTable.username],
                    passwordHash = it[UsersTable.passwordHash]
                )
            }
    }
}
