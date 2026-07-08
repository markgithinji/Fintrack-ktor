package feature.transaction.domain

import com.fintrack.core.logger
import com.fintrack.core.withContext
import com.fintrack.feature.transaction.data.model.DeleteResponse
import com.fintrack.feature.transactions.data.model.CreateTransactionRequest
import com.fintrack.feature.transactions.data.model.UpdateTransactionRequest
import com.fintrack.feature.accounts.domain.AccountsRepository
import feature.transaction.data.model.PaginatedTransactionDto
import feature.transaction.data.model.RecurringBillDto
import feature.transaction.data.model.TransactionDto
import feature.transaction.data.model.toDomain
import feature.transaction.data.model.toDto
import kotlinx.datetime.*
import org.jetbrains.exposed.sql.SortOrder
import java.util.UUID
import kotlin.math.abs

class TransactionServiceImpl(
    private val repo: TransactionRepository,
    private val accountsRepository: AccountsRepository
) : TransactionService {

    private val log = logger<TransactionServiceImpl>()

    override suspend fun getAllCursor(
        userId: UUID,
        accountId: UUID?,
        typeFilter: String?,
        isIncome: Boolean?,
        categories: List<String>?,
        startDate: String?,
        endDate: String?,
        sortBy: String,
        order: String?,
        limit: Int,
        afterDateTime: String?,
        afterId: UUID?,
        hasTransactionCost: Boolean?
    ): PaginatedTransactionDto {
        log.withContext(
            "userId" to userId,
            "accountId" to accountId,
            "typeFilter" to typeFilter,
            "isIncome" to isIncome,
            "categories" to categories?.size,
            "startDate" to startDate,
            "endDate" to endDate,
            "sortBy" to sortBy,
            "order" to order,
            "limit" to limit,
            "afterDateTime" to afterDateTime,
            "afterId" to afterId,
            "hasTransactionCost" to hasTransactionCost
        ).debug { "Fetching transactions with cursor pagination" }

        // PREFER isIncome parameter over typeFilter if both are provided
        val finalIsIncome = isIncome ?: when (typeFilter?.lowercase()) {
            "income" -> true
            "expense" -> false
            else -> null
        }

        val start = startDate?.let { LocalDate.parse(it).atTime(0, 0, 0).toInstant(TimeZone.UTC) }
        val end = endDate?.let { LocalDate.parse(it).atTime(23, 59, 59).toInstant(TimeZone.UTC) }
        val sortOrder = if (order == "DESC") SortOrder.DESC else SortOrder.ASC
        val parsedAfterDateTime = afterDateTime?.let { Instant.parse(it) }

        val transactions = repo.getAllCursor(
            userId, accountId, finalIsIncome, categories,
            start, end, sortBy, sortOrder, limit, parsedAfterDateTime, afterId, hasTransactionCost
        )

        val transactionDtos = transactions.map { it.toDto() }
        val last = transactionDtos.lastOrNull()
        val nextCursor = last?.let { "${it.dateTime}|${it.id}" }

        log.withContext(
            "userId" to userId,
            "transactionCount" to transactionDtos.size,
            "hasNextCursor" to (nextCursor != null),
            "finalIsIncome" to finalIsIncome
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

    override suspend fun clearAll(userId: UUID, accountIds: List<UUID>?): DeleteResponse {
        log.withContext("userId" to userId, "accountIds" to accountIds)
            .warn { "Clearing all transactions" }

        val cleared = repo.clearAll(userId, accountIds)

        val message = if (!accountIds.isNullOrEmpty())
            "All transactions cleared for accounts ${accountIds.joinToString()}"
        else "All transactions cleared for user $userId"

        log.withContext("userId" to userId, "accountIds" to accountIds)
            .info { "All transactions cleared successfully" }

        return DeleteResponse(message, cleared)
    }

    override suspend fun addBulk(
        userId: UUID,
        requests: List<CreateTransactionRequest>
    ): List<TransactionDto> {
        log.withContext("userId" to userId, "bulkCount" to requests.size)
            .info { "Creating bulk transactions" }

        val transactions = requests.map { it.toDomain(userId) }
        val result = repo.addBulk(transactions)

        log.withContext(
            "userId" to userId,
            "requestedCount" to requests.size,
            "createdCount" to result.size
        ).info { "Bulk transactions created successfully" }

        return result.map { it.toDto() }
    }

    override suspend fun syncEquityTransactions(
        userId: UUID,
        requests: List<CreateTransactionRequest>
    ): List<TransactionDto> {
        log.withContext("userId" to userId, "bulkCount" to requests.size)
            .info { "Syncing Equity Bank transactions" }

        if (requests.isEmpty()) return emptyList()

        // 1. Convert to domain objects
        val transactions = requests.map { it.toDomain(userId) }

        // 2. Deduplication is handled by repo.addBulk (using ignore = true and uniqueIndex(externalId, userId))
        val result = repo.addBulk(transactions)

        // 3. Account Balance Update
        // Identify the most recent transaction (by dateTime) that has a balance field
        val mostRecentWithBalance = requests
            .filter { it.balance != null }
            .maxByOrNull { it.dateTime }

        if (mostRecentWithBalance != null) {
            val accountId = UUID.fromString(mostRecentWithBalance.accountId)
            log.withContext(
                "userId" to userId,
                "accountId" to accountId,
                "newBalance" to mostRecentWithBalance.balance
            ).info { "Updating account balance from Equity transaction" }
            
            accountsRepository.updateBalance(accountId, mostRecentWithBalance.balance!!)
        }

        log.withContext(
            "userId" to userId,
            "requestedCount" to requests.size,
            "createdCount" to result.size
        ).info { "Equity transactions synced successfully" }

        return result.map { it.toDto() }
    }

    override suspend fun detectRecurringBills(userId: UUID): List<RecurringBillDto> {
        log.withContext("userId" to userId).info { "Detecting recurring bills" }

        // Fetch last 90 days of transactions for analysis
        val now = Clock.System.now()
        val analysisStart = now.minus(90, DateTimeUnit.DAY, TimeZone.UTC)
        
        val transactions = repo.getAllCursor(
            userId = userId,
            accountId = null,
            isIncome = false,
            categories = null,
            start = analysisStart,
            end = now,
            sortBy = "date",
            order = SortOrder.DESC,
            limit = 1000,
            afterDateTime = null,
            afterId = null
        )

        // Group by normalized description and category
        val groups = transactions.groupBy { 
            val normalizedDesc = it.description?.split("(Ref:")?.get(0)?.trim()?.lowercase() ?: ""
            "${it.category.lowercase()}|$normalizedDesc"
        }

        val recurringBills = mutableListOf<RecurringBillDto>()

        groups.forEach { (key, txns) ->
            if (txns.size >= 3) {
                val sortedTxns = txns.sortedBy { it.dateTime }
                
                // Check for regular intervals (approx 30 days)
                val intervals = mutableListOf<Long>()
                for (i in 0 until sortedTxns.size - 1) {
                    val days = sortedTxns[i].dateTime.until(sortedTxns[i + 1].dateTime, DateTimeUnit.DAY, TimeZone.UTC)
                    intervals.add(days)
                }

                val avgInterval = intervals.average()
                val isRegular = if (intervals.isEmpty()) false else intervals.all { abs(it - avgInterval) <= 3 } && avgInterval in 25.0..35.0

                if (isRegular) {
                    val lastTxn = sortedTxns.last()
                    val avgAmount = sortedTxns.map { it.amount }.average()
                    
                    val name = lastTxn.description?.split("(Ref:")?.get(0)?.trim() ?: lastTxn.category
                    val nextDueDate = lastTxn.dateTime.toLocalDateTime(TimeZone.UTC).date.plus(30, DateTimeUnit.DAY)

                    recurringBills.add(
                        RecurringBillDto(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            amount = avgAmount,
                            category = lastTxn.category,
                            frequency = "Monthly",
                            nextDueDate = nextDueDate,
                            isActive = true
                        )
                    )
                }
            }
        }

        log.withContext("userId" to userId, "detectedCount" to recurringBills.size).info { "Recurring bills detected" }
        return recurringBills
    }
}
