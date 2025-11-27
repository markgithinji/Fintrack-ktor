package feature.summary.domain

import com.fintrack.feature.summary.data.repository.TransactionCounts
import feature.transaction.domain.model.Transaction
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

import java.util.UUID

interface StatisticsRepository {
    suspend fun getTransactions(
        userId: UUID,
        accountId: UUID?,
        isIncome: Boolean?,
        start: LocalDateTime?,
        end: LocalDateTime?
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
        accountId: UUID?
    ): Map<String, Double>

    suspend fun getTransactionCounts(
        userId: UUID,
        accountId: UUID?,
        isIncome: Boolean? = null
    ): TransactionCounts
}