package feature.transaction.domain

import com.fintrack.feature.transaction.data.model.DeleteTransactionsResponse
import com.fintrack.feature.transactions.data.model.CreateTransactionRequest
import com.fintrack.feature.transactions.data.model.UpdateTransactionRequest
import feature.transaction.data.model.PaginatedTransactionDto
import feature.transaction.data.model.TransactionDto
import java.util.UUID

interface TransactionService {
    suspend fun getAllCursor(
        userId: UUID,
        accountId: UUID?,
        typeFilter: String?,
        isIncome: Boolean? = null,
        categories: List<String>?,
        startDate: String?,
        endDate: String?,
        sortBy: String,
        order: String?,
        limit: Int,
        afterDateTime: String?,
        afterId: UUID?
    ): PaginatedTransactionDto

    suspend fun getById(userId: UUID, id: UUID): TransactionDto

    suspend fun add(userId: UUID, request: CreateTransactionRequest): TransactionDto

    suspend fun update(
        userId: UUID,
        id: UUID,
        request: UpdateTransactionRequest
    ): TransactionDto

    suspend fun delete(userId: UUID, id: UUID)

    suspend fun clearAll(userId: UUID, accountId: UUID?): DeleteTransactionsResponse

    suspend fun addBulk(
        userId: UUID,
        requests: List<CreateTransactionRequest>
    ): List<TransactionDto>
}