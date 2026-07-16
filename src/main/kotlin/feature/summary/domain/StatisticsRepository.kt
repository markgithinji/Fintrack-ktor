package feature.summary.domain

import feature.transaction.domain.model.Transaction
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Instant
import java.math.BigDecimal

import java.util.UUID

interface StatisticsRepository {
    suspend fun getTransactions(
        userId: UUID,
        accountId: UUID?,
        isIncome: Boolean?,
        start: Instant?,
        end: Instant?
    ): List<Transaction>

    suspend fun getAvailablePeriods(
        userId: UUID,
        accountId: UUID?,
        periodType: String
    ): List<String>

    suspend fun getTransactionsByDateRange(
        userId: UUID,
        start: LocalDate,
        end: LocalDate,
        accountId: UUID?
    ): List<Transaction>

    suspend fun getCategoryTotals(
        userId: UUID,
        start: LocalDate?,
        end: LocalDate?,
        accountId: UUID?,
        isIncome: Boolean? = null
    ): Map<String, BigDecimal>

    suspend fun getDailyTotals(
        userId: UUID,
        start: LocalDate,
        end: LocalDate,
        accountId: UUID?
    ): Map<LocalDate, DailyTotal>

    suspend fun getMonthlyCategoryStats(
        userId: UUID,
        start: LocalDate,
        end: LocalDate,
        accountId: UUID?
    ): Map<String, Map<String, CategoryStats>>

    suspend fun getTopDescriptions(
        userId: UUID,
        start: LocalDate,
        end: LocalDate,
        accountId: UUID?,
        isIncome: Boolean?,
        limit: Int = 5
    ): Map<String, List<DescriptionTotal>>

    suspend fun getTransactionCounts(
        userId: UUID,
        accountId: UUID?,
        isIncome: Boolean? = null,
        categoryIds: List<UUID>? = null,
        hasCost: Boolean? = null,
        start: Instant? = null,
        end: Instant? = null
    ): TransactionCounts
}

data class TransactionCounts(
    val incomeCount: Int,
    val expenseCount: Int,
    val totalCount: Int,
    val totalTransactionCost: BigDecimal = BigDecimal.ZERO,
    val totalAmount: BigDecimal = BigDecimal.ZERO
)

data class DailyTotal(
    val income: BigDecimal,
    val expense: BigDecimal
)

data class CategoryStats(
    val categoryId: UUID? = null,
    val name: String = "",
    val totalAmount: BigDecimal,
    val count: Int,
    val isIncome: Boolean = false
)

data class DescriptionTotal(
    val description: String,
    val totalAmount: BigDecimal
)
