package feature.transaction.domain

import com.fintrack.core.logger
import com.fintrack.core.withContext
import com.fintrack.feature.transaction.data.model.DeleteTransactionsResponse
import com.fintrack.feature.transactions.data.model.CreateTransactionRequest
import com.fintrack.feature.transactions.data.model.UpdateTransactionRequest
import feature.transaction.data.model.PaginatedTransactionDto
import feature.transaction.data.model.TransactionDto
import feature.transaction.data.model.toDomain
import feature.transaction.data.model.toDto
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime
import org.jetbrains.exposed.sql.SortOrder
import java.util.UUID

class TransactionServiceImpl(
    private val repo: TransactionRepository
) : TransactionService {

    private val log = logger<TransactionServiceImpl>()

    override suspend fun getAllCursor(
        userId: UUID,
        accountId: UUID?,
        typeFilter: String?,
        categories: List<String>?,
        startDate: String?,
        endDate: String?,
        sortBy: String,
        order: String?,
        limit: Int,
        afterDateTime: String?,
        afterId: UUID?
    ): PaginatedTransactionDto {
        log.withContext(
            "userId" to userId,
            "accountId" to accountId,
            "typeFilter" to typeFilter,
            "categories" to categories?.size,
            "startDate" to startDate,
            "endDate" to endDate,
            "sortBy" to sortBy,
            "order" to order,
            "limit" to limit,
            "afterDateTime" to afterDateTime,
            "afterId" to afterId
        ).debug { "Fetching transactions with cursor pagination" }

        val isIncome = when (typeFilter?.lowercase()) {
            "income" -> true
            "expense" -> false
            else -> null
        }

        val start = startDate?.let { LocalDate.parse(it).atTime(0, 0, 0) }
        val end = endDate?.let { LocalDate.parse(it).atTime(23, 59, 59) }
        val sortOrder = if (order == "DESC") SortOrder.DESC else SortOrder.ASC
        val parsedAfterDateTime = afterDateTime?.let { LocalDateTime.parse(it) }

        val transactions = repo.getAllCursor(
            userId, accountId, isIncome, categories,
            start, end, sortBy, sortOrder, limit, parsedAfterDateTime, afterId
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

    override suspend fun delete(userId: UUID, id: UUID) {
        log.withContext("userId" to userId, "transactionId" to id)
            .info { "Deleting transaction" }

        val deleted = repo.delete(id, userId)

        if (!deleted) {
            log.withContext("userId" to userId, "transactionId" to id)
                .warn { "Transaction deletion failed - not found" }
            throw NoSuchElementException("Transaction not found")
        }

        log.withContext("userId" to userId, "transactionId" to id)
            .info { "Transaction deleted successfully" }
    }

    override suspend fun clearAll(userId: UUID, accountId: UUID?): DeleteTransactionsResponse {
        log.withContext("userId" to userId, "accountId" to accountId)
            .warn { "Clearing all transactions" }

        val cleared = repo.clearAll(userId, accountId)

        val message = if (accountId != null)
            "All transactions cleared for account $accountId"
        else "All transactions cleared for user $userId"

        log.withContext("userId" to userId, "accountId" to accountId)
            .info { "All transactions cleared successfully" }

        return DeleteTransactionsResponse(message, cleared)
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