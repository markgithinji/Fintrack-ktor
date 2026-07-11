package feature.budget.domain

import feature.budget.domain.model.Budget
import kotlinx.datetime.Instant
import java.util.*

interface BudgetRepository {
    suspend fun getAllByUser(userId: UUID, accountId: UUID?, limit: Int, offset: Long): List<Budget>
    suspend fun getById(userId: UUID, id: UUID): Budget?
    suspend fun add(budget: Budget): Budget
    suspend fun addAll(budgets: List<Budget>): List<Budget>
    suspend fun update(userId: UUID, id: UUID, budget: Budget): Budget?
    suspend fun delete(userId: UUID, id: UUID): Boolean
    suspend fun deleteAllByUser(userId: UUID, accountIds: List<UUID>?): Int
    
    /**
     * Returns the total amount spent for a budget within its date range.
     * Decoupled from full Transaction model.
     */
    suspend fun getSpentAmount(
        accountIds: List<UUID>,
        categories: List<String>,
        isExpense: Boolean,
        start: Instant,
        end: Instant
    ): Double

    /**
     * Batch calculates spent amounts for multiple budgets to fix N+1 query issue.
     */
    suspend fun getSpentAmounts(budgets: List<Budget>): Map<UUID, Double>
}
