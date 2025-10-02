package feature.summary.domain

import com.fintrack.feature.summary.data.repository.TransactionCounts
import feature.transactions.domain.model.Transaction
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

interface StatisticsRepository {
    suspend fun getTransactions(
        userId: Int,
        accountId: Int? = null,
        isIncome: Boolean? = null,
        start: LocalDateTime? = null,
        end: LocalDateTime? = null
    ): List<Transaction>

    suspend fun getAvailablePeriods(
        userId: Int,
        accountId: Int? = null,
        periodType: String
    ): List<String>

    suspend fun getTransactionsByDateRange(
        userId: Int,
        start: LocalDate,
        end: LocalDate,
        accountId: Int? = null
    ): List<Transaction>

    suspend fun getCategoryTotals(
        userId: Int,
        start: LocalDate? = null,
        end: LocalDate? = null,
        accountId: Int? = null
    ): Map<String, Double>

    suspend fun getTransactionCounts(
        userId: Int,
        accountId: Int? = null
    ): TransactionCounts
}