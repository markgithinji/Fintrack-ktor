package feature.transaction.domain

import feature.transaction.domain.model.Transaction
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.SortOrder

import java.util.UUID

interface TransactionRepository {
    suspend fun getAllCursor(
        userId: UUID,
        accountId: UUID?,
        isIncome: Boolean?,
        categories: List<String>?,
        start: Instant?,
        end: Instant?,
        sortBy: String,
        order: SortOrder,
        limit: Int,
        afterDateTime: Instant?,
        afterId: UUID?
    ): List<Transaction>

    suspend fun getById(id: UUID, userId: UUID): Transaction
    suspend fun add(entity: Transaction): Transaction
    suspend fun update(id: UUID, userId: UUID, entity: Transaction): Transaction
    suspend fun delete(id: UUID, userId: UUID): Boolean
    suspend fun clearAll(userId: UUID, accountId: UUID?): Boolean
}