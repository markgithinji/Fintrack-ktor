package feature.transaction.domain

import com.fintrack.core.domain.AppError
import com.fintrack.core.domain.Result
import com.fintrack.core.logger
import com.fintrack.core.withContext
import com.fintrack.feature.transaction.data.model.DeleteResponse
import com.fintrack.feature.transaction.data.model.CreateTransactionRequest
import com.fintrack.feature.transaction.data.model.UpdateTransactionRequest
import com.fintrack.feature.accounts.domain.repository.AccountsRepository
import feature.transaction.data.model.PaginatedTransactionDto
import feature.transaction.data.model.RecurringBillDto
import feature.transaction.data.model.TransactionDto
import core.util.IdGenerator
import com.fintrack.core.util.*
import feature.transaction.data.model.toDomain
import feature.transaction.data.model.toDto
import kotlinx.datetime.*
import org.jetbrains.exposed.sql.SortOrder
import java.math.BigDecimal
import java.util.UUID
import kotlin.math.abs

class TransactionServiceImpl(
    private val transactionRepository: TransactionRepository,
    private val accountsRepository: AccountsRepository
) : TransactionService {

    private val log = logger<TransactionServiceImpl>()

    override suspend fun getAllCursor(
        userId: UUID,
        accountId: UUID?,
        typeFilter: String?,
        isIncome: Boolean?,
        categoryIds: List<UUID>?,
        startDate: String?,
        endDate: String?,
        sortBy: String,
        order: String?,
        limit: Int,
        afterDateTime: String?,
        afterId: UUID?,
        hasTransactionCost: Boolean?
    ): Result<PaginatedTransactionDto> {
        log.withContext(
            "userId" to userId,
            "accountId" to accountId,
            "limit" to limit,
        ).debug { "Fetching transactions with cursor pagination" }

        // PREFER isIncome parameter over typeFilter if both are provided
        val finalIsIncome = isIncome ?: when (typeFilter?.lowercase()) {
            "income" -> true
            "expense" -> false
            else -> null
        }

        val start = try {
            startDate?.let { LocalDate.parse(it).atTime(0, 0, 0).toInstant(TimeZone.UTC) }
        } catch (e: IllegalArgumentException) {
            return Result.Failure(AppError.Validation("Invalid start date format: $startDate"))
        }

        val end = try {
            endDate?.let { LocalDate.parse(it).atTime(23, 59, 59, 999_999_999).toInstant(TimeZone.UTC) }
        } catch (e: IllegalArgumentException) {
            return Result.Failure(AppError.Validation("Invalid end date format: $endDate"))
        }

        val sortOrder = if (order == "DESC") SortOrder.DESC else SortOrder.ASC
        val parsedAfterDateTime = try {
            afterDateTime?.let { Instant.parse(it) }
        } catch (e: IllegalArgumentException) {
            return Result.Failure(AppError.Validation("Invalid cursor datetime: $afterDateTime"))
        }

        val transactions = transactionRepository.getAllCursor(
            userId, accountId, finalIsIncome, categoryIds,
            start, end, sortBy, sortOrder, limit, parsedAfterDateTime, afterId, hasTransactionCost
        )

        val transactionDtos = transactions.map { it.toDto() }
        val last = transactionDtos.lastOrNull()
        val nextCursor = last?.let { "${it.dateTime}|${it.id}" }

        return Result.Success(PaginatedTransactionDto(transactionDtos, nextCursor))
    }

    override suspend fun getById(userId: UUID, id: UUID): Result<TransactionDto> {
        val transaction = transactionRepository.getById(id, userId)
            ?: return Result.Failure(AppError.NotFound("Transaction $id not found"))

        return Result.Success(transaction.toDto())
    }

    override suspend fun add(userId: UUID, request: CreateTransactionRequest): Result<TransactionDto> {
        log.withContext(
            "userId" to userId,
            "accountId" to request.accountId,
            "amount" to request.amount,
            "category" to request.category,
            "categoryId" to request.categoryId
        ).info { "Creating transaction" }

        val transaction = request.toDomain(userId)
        val result = transactionRepository.add(transaction)

        return Result.Success(result.toDto())
    }

    override suspend fun update(
        userId: UUID,
        id: UUID,
        request: UpdateTransactionRequest
    ): Result<TransactionDto> {
        log.withContext("userId" to userId, "transactionId" to id)
            .info { "Updating transaction" }

        val transaction = request.toDomain(userId, id)
        val result = transactionRepository.update(id, userId, transaction)
            ?: return Result.Failure(AppError.NotFound("Transaction $id not found"))

        return Result.Success(result.toDto())
    }

    override suspend fun delete(userId: UUID, id: UUID): Result<Unit> {
        val deleted = transactionRepository.delete(id, userId)

        if (!deleted) {
            return Result.Failure(AppError.NotFound("Transaction $id not found"))
        }

        return Result.Success(Unit)
    }

    override suspend fun clearAll(userId: UUID, accountIds: List<UUID>?): Result<DeleteResponse> {
        val cleared = transactionRepository.clearAll(userId, accountIds)

        val message = if (!accountIds.isNullOrEmpty())
            "All transactions cleared for accounts ${accountIds.joinToString()}"
        else "All transactions cleared for user $userId"

        return Result.Success(DeleteResponse(message, cleared))
    }

    override suspend fun addBulk(
        userId: UUID,
        requests: List<CreateTransactionRequest>
    ): Result<List<TransactionDto>> {
        val transactions = requests.map { it.toDomain(userId) }
        val result = transactionRepository.addBulk(transactions)

        // Account Balance Update (match logic in syncEquityTransactions)
        val mostRecentWithBalance = requests
            .filter { it.balance != null }
            .maxByOrNull { it.dateTime }

        if (mostRecentWithBalance != null) {
            val accountId = UUID.fromString(mostRecentWithBalance.accountId)
            accountsRepository.updateBalance(accountId, mostRecentWithBalance.balance!!)
        }

        return Result.Success(result.map { it.toDto() })
    }

    override suspend fun syncEquityTransactions(
        userId: UUID,
        requests: List<CreateTransactionRequest>
    ): Result<List<TransactionDto>> {
        if (requests.isEmpty()) return Result.Success(emptyList())

        // 1. Convert to domain objects
        val transactions = requests.map { it.toDomain(userId) }

        // 2. Deduplication is handled by repo.addBulk (using ignore = true and uniqueIndex(externalId, userId))
        val result = transactionRepository.addBulk(transactions)

        // 3. Account Balance Update
        val mostRecentWithBalance = requests
            .filter { it.balance != null }
            .maxByOrNull { it.dateTime }

        if (mostRecentWithBalance != null) {
            val accountId = UUID.fromString(mostRecentWithBalance.accountId)
            accountsRepository.updateBalance(accountId, mostRecentWithBalance.balance!!)
        }

        return Result.Success(result.map { it.toDto() })
    }

    override suspend fun detectRecurringBills(userId: UUID): Result<List<RecurringBillDto>> {
        // Fetch last 90 days of transactions for analysis
        val now = Clock.System.now()
        val analysisStart = now.minus(90, DateTimeUnit.DAY, TimeZone.UTC)
        
        val transactions = transactionRepository.getAllCursor(
            userId = userId,
            accountId = null,
            isIncome = false,
            categoryIds = null,
            start = analysisStart,
            end = now,
            sortBy = "date",
            order = SortOrder.DESC,
            limit = 1000,
            afterDateTime = null,
            afterId = null
        )

        // Group by normalized description and category ID
        val groups = transactions.groupBy { 
            val normalizedDesc = it.description?.split("(Ref:")?.get(0)?.trim()?.lowercase() ?: ""
            "${it.categoryId}|$normalizedDesc"
        }

        val recurringBills = mutableListOf<RecurringBillDto>()

        groups.forEach { (_, txns) ->
            if (txns.size >= 3) {
                val sortedTxns = txns.sortedBy { it.dateTime }
                
                // Check for regular intervals (approx 30 days)
                val intervals = mutableListOf<Long>()
                for (i in 0 until sortedTxns.size - 1) {
                    val days = sortedTxns[i].dateTime.until(sortedTxns[i + 1].dateTime, DateTimeUnit.DAY, TimeZone.UTC)
                    intervals.add(days)
                }

                val avgInterval = if (intervals.isEmpty()) 0.0 else intervals.average()
                val isRegular = if (intervals.isEmpty()) false else intervals.all { abs(it - avgInterval) <= 3 } && avgInterval in 25.0..35.0

                if (isRegular) {
                    val lastTxn = sortedTxns.last()
                    val totalAmount = sortedTxns.fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }
                    val avgAmount = totalAmount.calculateAverage(sortedTxns.size)
                    
                    val name = lastTxn.description?.split("(Ref:")?.get(0)?.trim() ?: lastTxn.category
                    val nextDueDate = lastTxn.dateTime.toLocalDateTime(TimeZone.UTC).date.plus(30, DateTimeUnit.DAY)

                    recurringBills.add(
                        RecurringBillDto(
                            id = IdGenerator.nextId().toString(),
                            name = name,
                            amount = avgAmount,
                            category = lastTxn.category,
                            categoryId = lastTxn.categoryId.toString(),
                            frequency = "Monthly",
                            nextDueDate = nextDueDate,
                            isActive = true
                        )
                    )
                }
            }
        }

        return Result.Success(recurringBills)
    }
}
