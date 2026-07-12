package feature.transaction.domain

import com.fintrack.core.domain.Result
import com.fintrack.feature.transaction.data.model.DeleteResponse
import com.fintrack.feature.transaction.data.model.CreateTransactionRequest
import com.fintrack.feature.transaction.data.model.UpdateTransactionRequest
import feature.transaction.data.model.PaginatedTransactionDto
import feature.transaction.data.model.RecurringBillDto
import feature.transaction.data.model.TransactionDto
import java.util.UUID

interface TransactionService {
    suspend fun getAllCursor(
        userId: UUID,
        accountId: UUID?,
        typeFilter: String?,
        isIncome: Boolean? = null,
        categoryIds: List<UUID>?,
        startDate: String?,
        endDate: String?,
        sortBy: String,
        order: String?,
        limit: Int,
        afterDateTime: String?,
        afterId: UUID?,
        hasTransactionCost: Boolean? = null
    ): Result<PaginatedTransactionDto>

    suspend fun getById(userId: UUID, id: UUID): Result<TransactionDto>

    suspend fun add(userId: UUID, request: CreateTransactionRequest): Result<TransactionDto>

    suspend fun update(
        userId: UUID,
        id: UUID,
        request: UpdateTransactionRequest
    ): Result<TransactionDto>

    suspend fun delete(userId: UUID, id: UUID): Result<Unit>

    suspend fun clearAll(userId: UUID, accountIds: List<UUID>?): Result<DeleteResponse>

    suspend fun addBulk(
        userId: UUID,
        requests: List<CreateTransactionRequest>
    ): Result<List<TransactionDto>>

    suspend fun syncEquityTransactions(
        userId: UUID,
        requests: List<CreateTransactionRequest>
    ): Result<List<TransactionDto>>

    suspend fun detectRecurringBills(userId: UUID): Result<List<RecurringBillDto>>
}
