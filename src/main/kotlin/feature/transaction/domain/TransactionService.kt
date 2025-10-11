package feature.transaction.domain

import com.fintrack.feature.transactions.data.model.CreateTransactionRequest
import com.fintrack.feature.transactions.data.model.UpdateTransactionRequest
import feature.transaction.data.model.PaginatedTransactionDto
import feature.transaction.data.model.TransactionDto
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
    ): PaginatedTransactionDto

    suspend fun getById(userId: UUID, id: UUID): TransactionDto

    suspend fun add(userId: UUID, request: CreateTransactionRequest): TransactionDto

    suspend fun update(
        userId: UUID,
        id: UUID,
        request: UpdateTransactionRequest
    ): TransactionDto

    suspend fun delete(userId: UUID, id: UUID): Boolean

    suspend fun clearAll(userId: UUID, accountId: UUID?): Boolean

    suspend fun addBulk(
        userId: UUID,
        requests: List<CreateTransactionRequest>
    ): List<TransactionDto>
}