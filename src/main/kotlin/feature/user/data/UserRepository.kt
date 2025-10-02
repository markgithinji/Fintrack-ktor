package com.fintrack.feature.user.data

import com.fintrack.feature.user.UsersTable
import com.fintrack.feature.user.domain.User
import feature.accounts.data.AccountsTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.mindrot.jbcrypt.BCrypt

class UserRepository {
    suspend fun createUser(username: String, password: String): Int =
        withContext(Dispatchers.IO) {
            transaction {
                val hashed = BCrypt.hashpw(password, BCrypt.gensalt())

                UsersTable.insertAndGetId {
                    it[UsersTable.username] = username
                    it[UsersTable.passwordHash] = hashed
                }.value

                //        // Create default accounts for the new user
                //        val defaultAccounts = listOf("Bank", "Wallet", "Cash", "Savings")
                //        defaultAccounts.forEach { accountName ->
                //            AccountsTable.insert {
                //                it[AccountsTable.userId] = userId
                //                it[AccountsTable.name] = accountName
                //            }
                //        }
            }
        }

    suspend fun findByUsername(username: String): User? =
        withContext(Dispatchers.IO) {
            transaction {
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
        }

    suspend fun findById(userId: Int): User? =
        withContext(Dispatchers.IO) {
            transaction {
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

    suspend fun updateUser(userId: Int, username: String? = null, password: String? = null): Boolean =
        withContext(Dispatchers.IO) {
            transaction {
                val updateStatement = UsersTable.update({ UsersTable.id eq userId }) {
                    if (username != null) it[UsersTable.username] = username
                    if (password != null) it[UsersTable.passwordHash] = BCrypt.hashpw(password, BCrypt.gensalt())
                }
                updateStatement > 0
            }
        }

    suspend fun deleteUser(userId: Int): Boolean =
        withContext(Dispatchers.IO) {
            transaction {
                UsersTable.deleteWhere { UsersTable.id eq userId } > 0
            }
        }

    suspend fun userExists(username: String): Boolean =
        withContext(Dispatchers.IO) {
            transaction {
                UsersTable.select(UsersTable.id).where { UsersTable.username eq username }.count() > 0
            }
        }
}