package feature.transactions.domain

import feature.transactions.data.model.TransactionDto
import feature.transactions.data.model.toTransaction
import feature.transactions.domain.model.Transaction
import feature.transactions.validate
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.sql.SortOrder

class TransactionServiceImpl(
    private val repo: TransactionRepository
) : TransactionService {

    override suspend fun getAllCursor(
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
    ): List<Transaction> {
        return repo.getAllCursor(
            userId, accountId, isIncome, categories,
            start, end, sortBy, order, limit, afterDateTime, afterId
        )
    }

    override suspend fun getById(userId: Int, id: Int): Transaction {
        return repo.getById(id, userId)
    }

    override suspend fun add(userId: Int, dto: TransactionDto): Transaction {
        dto.validate()
        val transaction = dto.toTransaction(userId)
        return repo.add(transaction)
    }

    override suspend fun update(userId: Int, id: Int, dto: TransactionDto): Transaction {
        dto.validate()
        val transaction = dto.toTransaction(userId).copy(id = id)
        return repo.update(id, userId, transaction)
    }

    override suspend fun delete(userId: Int, id: Int): Boolean {
        return repo.delete(id, userId)
    }

    override suspend fun clearAll(userId: Int, accountId: Int?): Boolean {
        return repo.clearAll(userId, accountId)
    }

    override suspend fun addBulk(userId: Int, dtos: List<TransactionDto>): List<Transaction> {
        return dtos.map { dto ->
            dto.validate()
            repo.add(dto.toTransaction(userId))
        }
    }
}
