package feature.transactions.domain

import feature.transactions.domain.model.Transaction
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.sql.SortOrder

interface TransactionRepository {
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

    suspend fun getById(id: Int, userId: Int): Transaction
    suspend fun add(entity: Transaction): Transaction
    suspend fun update(id: Int, userId: Int, entity: Transaction): Transaction
    suspend fun delete(id: Int, userId: Int): Boolean
    suspend fun clearAll(userId: Int, accountId: Int? = null): Boolean
}