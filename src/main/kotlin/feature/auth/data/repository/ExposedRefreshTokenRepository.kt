package com.fintrack.feature.auth.data.repository

import com.fintrack.feature.auth.data.table.RefreshTokensTable
import com.fintrack.feature.auth.domain.model.RefreshToken
import com.fintrack.feature.auth.domain.repository.RefreshTokenRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class ExposedRefreshTokenRepository : RefreshTokenRepository {
    override suspend fun save(refreshToken: RefreshToken) {
        transaction {
            RefreshTokensTable.insert {
                it[id] = refreshToken.id
                it[token] = refreshToken.token
                it[userId] = refreshToken.userId
                it[expiresAt] = refreshToken.expiresAt
            }
        }
    }

    override suspend fun findByToken(token: String): RefreshToken? {
        return transaction {
            RefreshTokensTable.selectAll().where { RefreshTokensTable.token eq token }
                .map { rowToRefreshToken(it) }
                .singleOrNull()
        }
    }

    override suspend fun deleteByToken(token: String) {
        transaction {
            RefreshTokensTable.deleteWhere { RefreshTokensTable.token eq token }
        }
    }

    override suspend fun deleteByUserId(userId: UUID) {
        transaction {
            RefreshTokensTable.deleteWhere { RefreshTokensTable.userId eq userId }
        }
    }

    private fun rowToRefreshToken(row: ResultRow) = RefreshToken(
        id = row[RefreshTokensTable.id].value,
        token = row[RefreshTokensTable.token],
        userId = row[RefreshTokensTable.userId],
        expiresAt = row[RefreshTokensTable.expiresAt]
    )
}
