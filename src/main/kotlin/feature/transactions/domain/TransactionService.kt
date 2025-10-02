package feature.transactions.domain

import feature.transactions.data.model.TransactionDto
import feature.transactions.domain.model.Transaction
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.sql.SortOrder

interface TransactionService {
    suspend fun getAllCursor(
        userId: Int,
        accountId: Int? = null,
        isIncome: Boolean? = null,
        categories: List<String>? = null,
        start: LocalDateTime? = null,
        end: LocalDateTime? = null,
        sortBy: String = "dateTime",
        order: SortOrder = SortOrder.ASC,
        limit: Int = 20,
        afterDateTime: LocalDateTime? = null,
        afterId: Int? = null
    ): List<Transaction>

    suspend fun getById(userId: Int, id: Int): Transaction
    suspend fun add(userId: Int, dto: TransactionDto): Transaction
    suspend fun update(userId: Int, id: Int, dto: TransactionDto): Transaction
    suspend fun delete(userId: Int, id: Int): Boolean
    suspend fun clearAll(userId: Int, accountId: Int? = null): Boolean
    suspend fun addBulk(userId: Int, dtos: List<TransactionDto>): List<Transaction>
}