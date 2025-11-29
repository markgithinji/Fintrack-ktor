package feature.budget.domain

import feature.transaction.Budget
import kotlinx.datetime.Instant
import java.util.*

interface BudgetRepository {
    suspend fun getAllByUser(userId: UUID, accountId: UUID?): List<Budget>
    suspend fun getById(userId: UUID, id: UUID): Budget?
    suspend fun add(budget: Budget): Budget
    suspend fun addAll(budgets: List<Budget>): List<Budget>
    suspend fun update(userId: UUID, id: UUID, budget: Budget): Budget?
    suspend fun delete(userId: UUID, id: UUID): Boolean
    suspend fun deleteAllByUser(userId: UUID): Int
    suspend fun getTransactionsInDateRange(
        accountId: UUID,
        categories: List<String>,
        isExpense: Boolean,
        start: Instant,
        end: Instant
    ): List<feature.transaction.domain.model.Transaction>
}