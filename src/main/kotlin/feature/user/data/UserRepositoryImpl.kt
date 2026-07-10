package com.fintrack.feature.user.data

import com.fintrack.feature.user.UsersTable
import com.fintrack.feature.user.domain.User
import core.PasswordHasher
import com.fintrack.core.data.dbQuery
import com.fintrack.feature.user.domain.UserRepository
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.UUID

class UserRepositoryImpl : UserRepository {
    override suspend fun createUser(email: String, password: String, name: String): UUID =
        dbQuery {
            val hashed = PasswordHasher.hash(password)

            val inserted = UsersTable.insert {
                it[UsersTable.email] = email
                it[UsersTable.name] = name
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
                        name = it[UsersTable.name],
                        passwordHash = it[UsersTable.passwordHash],
                        trackedCategories = it[UsersTable.trackedCategories],
                        isEmailVerified = it[UsersTable.isEmailVerified]
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
                        name = it[UsersTable.name],
                        passwordHash = it[UsersTable.passwordHash],
                        trackedCategories = it[UsersTable.trackedCategories],
                        isEmailVerified = it[UsersTable.isEmailVerified]
                    )
                }
        }

    override suspend fun updateUser(userId: UUID, name: String?, email: String?, password: String?): Boolean =
        dbQuery {
            val updateStatement =
                UsersTable.update({ UsersTable.id eq EntityID(userId, UsersTable) }) {
                    if (name != null) it[UsersTable.name] = name
                    if (email != null) it[UsersTable.email] = email
                    if (password != null) it[UsersTable.passwordHash] =
                        PasswordHasher.hash(password)
                }
            updateStatement > 0
        }

    override suspend fun updateTrackedCategories(userId: UUID, categories: List<String>): Boolean =
        dbQuery {
            UsersTable.update({ UsersTable.id eq EntityID(userId, UsersTable) }) {
                it[UsersTable.trackedCategories] = categories.joinToString(",")
            } > 0
        }

    override suspend fun updatePassword(userId: UUID, newPassword: String): Boolean =
        dbQuery {
            val hashed = PasswordHasher.hash(newPassword)
            UsersTable.update({ UsersTable.id eq EntityID(userId, UsersTable) }) {
                it[UsersTable.passwordHash] = hashed
            } > 0
        }

    override suspend fun updateEmail(userId: UUID, newEmail: String): Boolean =
        dbQuery {
            UsersTable.update({ UsersTable.id eq EntityID(userId, UsersTable) }) {
                it[UsersTable.email] = newEmail
                it[UsersTable.isEmailVerified] = true
            } > 0
        }

    override suspend fun updateEmailVerificationStatus(userId: UUID, isVerified: Boolean): Boolean =
        dbQuery {
            UsersTable.update({ UsersTable.id eq EntityID(userId, UsersTable) }) {
                it[UsersTable.isEmailVerified] = isVerified
            } > 0
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
