package com.fintrack.feature.user.data

import com.fintrack.feature.user.UsersTable
import com.fintrack.feature.user.domain.User
import core.dbQuery
import feature.user.domain.UserRepository
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.mindrot.jbcrypt.BCrypt
import java.util.UUID

class UserRepositoryImpl : UserRepository {
    override suspend fun createUser(email: String, password: String): UUID =
        dbQuery {
            val hashed = BCrypt.hashpw(password, BCrypt.gensalt())

            val inserted = UsersTable.insert {
                it[UsersTable.email] = email
                it[UsersTable.passwordHash] = hashed
            }

            inserted.resultedValues?.singleOrNull()?.get(UsersTable.id)?.value
                ?: throw IllegalStateException("Failed to create user")
        }

    override suspend fun findByEmail(email: String): User? =
        dbQuery {
            UsersTable.selectAll().where { UsersTable.email eq email }
                .singleOrNull()
                ?.let {
                    User(
                        id = it[UsersTable.id].value,
                        email = it[UsersTable.email],
                        passwordHash = it[UsersTable.passwordHash]
                    )
                }
        }

    override suspend fun findById(userId: UUID): User? =
        dbQuery {
            UsersTable.selectAll().where { UsersTable.id eq EntityID(userId, UsersTable) }
                .singleOrNull()
                ?.let {
                    User(
                        id = it[UsersTable.id].value,
                        email = it[UsersTable.email],
                        passwordHash = it[UsersTable.passwordHash]
                    )
                }
        }

    override suspend fun updateUser(userId: UUID, email: String?, password: String?): Boolean =
        dbQuery {
            val updateStatement =
                UsersTable.update({ UsersTable.id eq EntityID(userId, UsersTable) }) {
                    if (email != null) it[UsersTable.email] = email
                    if (password != null) it[UsersTable.passwordHash] =
                        BCrypt.hashpw(password, BCrypt.gensalt())
                }
            updateStatement > 0
        }

    override suspend fun deleteUser(userId: UUID): Boolean =
        dbQuery {
            UsersTable.deleteWhere { UsersTable.id eq EntityID(userId, UsersTable) } > 0
        }

    override suspend fun userExists(email: String): Boolean =
        dbQuery {
            UsersTable.select(UsersTable.id).where { UsersTable.email eq email }.count() > 0
        }
}