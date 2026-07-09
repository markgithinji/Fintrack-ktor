package feature.auth.data.repository

import feature.auth.data.table.EmailVerificationTokensTable
import feature.auth.domain.model.EmailVerificationToken
import feature.auth.domain.repository.EmailVerificationRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class ExposedEmailVerificationRepository : EmailVerificationRepository {
    override suspend fun saveToken(token: EmailVerificationToken) {
        transaction {
            EmailVerificationTokensTable.insert {
                it[id] = token.id
                it[userId] = token.userId
                it[newEmail] = token.newEmail
                it[EmailVerificationTokensTable.token] = token.token
                it[expiresAt] = token.expiresAt
            }
        }
    }

    override suspend fun findByToken(token: String): EmailVerificationToken? {
        return transaction {
            EmailVerificationTokensTable.selectAll().where { EmailVerificationTokensTable.token eq token }
                .map { rowToToken(it) }
                .singleOrNull()
        }
    }

    override suspend fun deleteByToken(token: String) {
        transaction {
            EmailVerificationTokensTable.deleteWhere { EmailVerificationTokensTable.token eq token }
        }
    }

    override suspend fun deleteByUserId(userId: UUID) {
        transaction {
            EmailVerificationTokensTable.deleteWhere { EmailVerificationTokensTable.userId eq userId }
        }
    }

    private fun rowToToken(row: ResultRow) = EmailVerificationToken(
        id = row[EmailVerificationTokensTable.id].value,
        userId = row[EmailVerificationTokensTable.userId],
        newEmail = row[EmailVerificationTokensTable.newEmail],
        token = row[EmailVerificationTokensTable.token],
        expiresAt = row[EmailVerificationTokensTable.expiresAt]
    )
}
