package feature.transactions.domain

import com.fintrack.feature.transactions.data.model.CreateTransactionRequest
import com.fintrack.feature.transactions.data.model.UpdateTransactionRequest
import feature.transactions.data.model.TransactionDto
import feature.transactions.domain.model.Transaction
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.sql.SortOrder

interface TransactionService {
    suspend fun getAllCursor(
        userId: Int,
        accountId: Int?,
        isIncome: Boolean?,
        categories: List<String>?,
        start: LocalDateTime?,
        end: LocalDateTime?,
        sortBy: String,
        order: SortOrder,
        limit: Int,
        afterDateTime: LocalDateTime?,
        afterId: Int?
    ): List<Transaction>

    suspend fun getById(userId: Int, id: Int): Transaction
    suspend fun add(userId: Int, request: CreateTransactionRequest): Transaction
    suspend fun update(userId: Int, id: Int, request: UpdateTransactionRequest): Transaction
    suspend fun delete(userId: Int, id: Int): Boolean
    suspend fun clearAll(userId: Int, accountId: Int?): Boolean
    suspend fun addBulk(userId: Int, requests: List<CreateTransactionRequest>): List<Transaction>
}