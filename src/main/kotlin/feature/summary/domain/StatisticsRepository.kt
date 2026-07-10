package feature.summary.domain

import feature.transaction.domain.model.Transaction
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Instant

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
    ): Map<String, Double>

    suspend fun getTransactionCounts(
        userId: UUID,
        accountId: UUID?,
        isIncome: Boolean? = null,
        category: String? = null,
        hasCost: Boolean? = null,
        start: Instant? = null,
        end: Instant? = null
    ): TransactionCounts
}

data class TransactionCounts(
    val incomeCount: Int,
    val expenseCount: Int,
    val totalCount: Int,
    val totalTransactionCost: Double = 0.0
)
