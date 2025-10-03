package feature.transactions.domain

import com.fintrack.feature.transactions.data.model.CreateTransactionRequest
import com.fintrack.feature.transactions.data.model.UpdateTransactionRequest
import feature.transactions.data.model.TransactionDto
import feature.transactions.data.model.toDomain
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

    override suspend fun add(userId: Int, request: CreateTransactionRequest): Transaction {
        val transaction = request.toDomain(userId)
        return repo.add(transaction)
    }

    override suspend fun update(userId: Int, id: Int, request: UpdateTransactionRequest): Transaction {
        val transaction = request.toDomain(userId, id)
        return repo.update(id, userId, transaction)
    }

    override suspend fun delete(userId: Int, id: Int): Boolean {
        return repo.delete(id, userId)
    }

    override suspend fun clearAll(userId: Int, accountId: Int?): Boolean {
        return repo.clearAll(userId, accountId)
    }

    override suspend fun addBulk(userId: Int, requests: List<CreateTransactionRequest>): List<Transaction> {
        return requests.map { request ->
            val transaction = request.toDomain(userId)
            repo.add(transaction)
        }
    }
}