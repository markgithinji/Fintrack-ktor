package feature.budget.domain

import feature.transactions.Budget
import kotlinx.datetime.Instant

interface BudgetRepository {
    suspend fun getAllByUser(userId: Int, accountId: Int? = null): List<Budget>
    suspend fun getById(userId: Int, id: Int): Budget?
    suspend fun add(budget: Budget): Budget
    suspend fun addAll(budgets: List<Budget>): List<Budget>
    suspend fun update(userId: Int, id: Int, budget: Budget): Budget?
    suspend fun delete(userId: Int, id: Int): Boolean
    suspend fun getTransactionsInDateRange(
        accountId: Int,
        categories: List<String>,
        isExpense: Boolean,
        start: Instant,
        end: Instant
    ): List<feature.transactions.domain.model.Transaction>
}