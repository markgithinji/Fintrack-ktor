package feature.transaction.domain

import com.fintrack.core.logger
import com.fintrack.core.withContext
import com.fintrack.feature.transactions.data.model.CreateTransactionRequest
import com.fintrack.feature.transactions.data.model.UpdateTransactionRequest
import feature.transaction.data.model.PaginatedTransactionDto
import feature.transaction.data.model.TransactionDto
import feature.transaction.data.model.toDomain
import feature.transaction.data.model.toDto
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.sql.SortOrder
import java.util.UUID

class TransactionServiceImpl(
    private val repo: TransactionRepository
) : TransactionService {

    private val log = logger<TransactionServiceImpl>()

    override suspend fun getAllCursor(
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
    ): PaginatedTransactionDto {
        log.withContext(
            "userId" to userId,
            "accountId" to accountId,
            "isIncome" to isIncome,
            "categories" to categories?.size,
            "start" to start?.toString(),
            "end" to end?.toString(),
            "sortBy" to sortBy,
            "order" to order,
            "limit" to limit,
            "afterDateTime" to afterDateTime?.toString(),
            "afterId" to afterId
        ).debug { "Fetching transactions with cursor pagination" }

        val transactions = repo.getAllCursor(
            userId, accountId, isIncome, categories,
            start, end, sortBy, order, limit, afterDateTime, afterId
        )

        val transactionDtos = transactions.map { it.toDto() }
        val last = transactionDtos.lastOrNull()
        val nextCursor = last?.let { "${it.dateTime}|${it.id}" }

        log.withContext(
            "userId" to userId,
            "transactionCount" to transactionDtos.size,
            "hasNextCursor" to (nextCursor != null)
        ).debug { "Transactions retrieved with cursor pagination" }

        return PaginatedTransactionDto(transactionDtos, nextCursor)
    }

    override suspend fun getById(userId: UUID, id: UUID): TransactionDto {
        log.withContext("userId" to userId, "transactionId" to id)
            .debug { "Fetching transaction by ID" }

        val transaction = repo.getById(id, userId)

        log.withContext("userId" to userId, "transactionId" to id)
            .debug { "Transaction retrieved successfully" }
        return transaction.toDto()
    }

    override suspend fun add(userId: UUID, request: CreateTransactionRequest): TransactionDto {
        log.withContext(
            "userId" to userId,
            "accountId" to request.accountId,
            "amount" to request.amount,
            "isIncome" to request.isIncome,
            "category" to request.category
        ).info { "Creating transaction" }

        val transaction = request.toDomain(userId)
        val result = repo.add(transaction)

        log.withContext(
            "userId" to userId,
            "transactionId" to result.id,
            "accountId" to result.accountId
        ).info { "Transaction created successfully" }

        return result.toDto()
    }

    override suspend fun update(
        userId: UUID,
        id: UUID,
        request: UpdateTransactionRequest
    ): TransactionDto {
        log.withContext("userId" to userId, "transactionId" to id)
            .info { "Updating transaction" }

        val transaction = request.toDomain(userId, id)
        val result = repo.update(id, userId, transaction)

        log.withContext("userId" to userId, "transactionId" to id)
            .info { "Transaction updated successfully" }
        return result.toDto()
    }

    override suspend fun delete(userId: UUID, id: UUID): Boolean {
        log.withContext("userId" to userId, "transactionId" to id)
            .info { "Deleting transaction" }

        val deleted = repo.delete(id, userId)

        if (deleted) {
            log.withContext("userId" to userId, "transactionId" to id)
                .info { "Transaction deleted successfully" }
        } else {
            log.withContext("userId" to userId, "transactionId" to id)
                .warn { "Transaction deletion failed - not found" }
        }

        return deleted
    }

    override suspend fun clearAll(userId: UUID, accountId: UUID?): Boolean {
        log.withContext("userId" to userId, "accountId" to accountId)
            .warn { "Clearing all transactions" }

        val cleared = repo.clearAll(userId, accountId)

        log.withContext("userId" to userId, "accountId" to accountId)
            .info { "All transactions cleared successfully" }
        return cleared
    }

    override suspend fun addBulk(
        userId: UUID,
        requests: List<CreateTransactionRequest>
    ): List<TransactionDto> {
        log.withContext("userId" to userId, "bulkCount" to requests.size)
            .info { "Creating bulk transactions" }

        val transactions = requests.map { request ->
            val transaction = request.toDomain(userId)
            repo.add(transaction)
        }

        log.withContext(
            "userId" to userId,
            "requestedCount" to requests.size,
            "createdCount" to transactions.size
        ).info { "Bulk transactions created successfully" }

        return transactions.map { it.toDto() }
    }
}