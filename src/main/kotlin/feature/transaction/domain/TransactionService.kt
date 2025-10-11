package feature.transaction.domain

import com.fintrack.feature.transactions.data.model.CreateTransactionRequest
import com.fintrack.feature.transactions.data.model.UpdateTransactionRequest
import feature.transaction.domain.model.Transaction
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.sql.SortOrder

import java.util.UUID

interface TransactionService {
    suspend fun getAllCursor(
        userId: UUID,
        accountId: UUID?,
        isIncome: Boolean?,
        categories: List<String>?,
        start: LocalDateTime?,
        end: LocalDateTime?,
        sortBy: String,
        order: SortOrder,
        limit: Int,
        afterDateTime: LocalDateTime?,
        afterId: UUID?
    ): List<Transaction>

    suspend fun getById(userId: UUID, id: UUID): Transaction
    suspend fun add(userId: UUID, request: CreateTransactionRequest): Transaction
    suspend fun update(userId: UUID, id: UUID, request: UpdateTransactionRequest): Transaction
    suspend fun delete(userId: UUID, id: UUID): Boolean
    suspend fun clearAll(userId: UUID, accountId: UUID?): Boolean
    suspend fun addBulk(userId: UUID, requests: List<CreateTransactionRequest>): List<Transaction>
}