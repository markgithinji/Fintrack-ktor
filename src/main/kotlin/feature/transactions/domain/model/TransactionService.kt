package feature.transactions.domain.model

import feature.transactions.data.TransactionRepository
import feature.transactions.data.model.TransactionDto
import feature.transactions.data.model.toTransaction
import feature.transactions.validate
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.update

class TransactionService(
    private val repo: TransactionRepository
) {

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
    ): List<Transaction> {
        return repo.getAllCursor(
            userId, accountId, isIncome, categories,
            start, end, sortBy, order, limit, afterDateTime, afterId
        )
    }

    suspend fun getById(userId: Int, id: Int): Transaction {
        return repo.getById(id, userId)
    }

    suspend fun add(userId: Int, dto: TransactionDto): Transaction {
        dto.validate()
        val transaction = dto.toTransaction(userId) // dto exposes kotlinx.datetime.LocalDateTime
        return repo.add(transaction)
    }

    suspend fun update(userId: Int, id: Int, dto: TransactionDto): Transaction {
        dto.validate()
        val transaction = dto.toTransaction(userId).copy(id = id)
        return repo.update(id, userId, transaction)
    }

    suspend fun delete(userId: Int, id: Int): Boolean {
        return repo.delete(id, userId)
    }

    suspend fun clearAll(userId: Int, accountId: Int? = null): Boolean {
        return repo.clearAll(userId, accountId)
    }

    suspend fun addBulk(userId: Int, dtos: List<TransactionDto>): List<Transaction> {
        return dtos.map { dto ->
            dto.validate()
            repo.add(dto.toTransaction(userId))
        }
    }
}
