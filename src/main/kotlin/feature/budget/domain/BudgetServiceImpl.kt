package feature.budget.domain


import com.fintrack.feature.budget.data.model.CreateBudgetRequest
import com.fintrack.feature.budget.data.model.UpdateBudgetRequest
import com.fintrack.feature.budget.data.toDomain
import com.fintrack.feature.budget.domain.BudgetStatus
import com.fintrack.feature.budget.domain.BudgetWithStatus
import core.ResourceNotFoundException
import feature.transaction.Budget
import kotlinx.datetime.*

class BudgetServiceImpl(
    private val budgetRepository: BudgetRepository
) : BudgetService {
    override suspend fun getAllBudgets(userId: Int, accountId: Int?): List<BudgetWithStatus> {
        val budgets = budgetRepository.getAllByUser(userId, accountId)
        return budgets.map { budget ->
            BudgetWithStatus(budget, calculateBudgetStatus(budget))
        }
    }

    override suspend fun getBudgetById(userId: Int, id: Int): BudgetWithStatus {
        val budget = budgetRepository.getById(userId, id) ?: throw ResourceNotFoundException("Budget $id not found")
        return BudgetWithStatus(budget, calculateBudgetStatus(budget))
    }

    override suspend fun createBudget(userId: Int, request: CreateBudgetRequest): BudgetWithStatus {
        val budget = budgetRepository.add(request.toDomain(userId))
        return BudgetWithStatus(budget, calculateBudgetStatus(budget))
    }

    override suspend fun createBudgets(userId: Int, requests: List<CreateBudgetRequest>): List<BudgetWithStatus> {
        val budgets = budgetRepository.addAll(requests.map { it.toDomain(userId) })
        return budgets.map { budget ->
            BudgetWithStatus(budget, calculateBudgetStatus(budget))
        }
    }

    override suspend fun updateBudget(userId: Int, id: Int, request: UpdateBudgetRequest): BudgetWithStatus {
        val existingBudget = budgetRepository.getById(userId, id)
            ?: throw ResourceNotFoundException("Budget $id not found")

        val updatedBudget = budgetRepository.update(userId, id, request.toDomain(userId, id))
            ?: throw IllegalStateException("Failed to update budget $id")

        return BudgetWithStatus(updatedBudget, calculateBudgetStatus(updatedBudget))
    }

    override suspend fun deleteBudget(userId: Int, id: Int): Boolean {
        return budgetRepository.delete(userId, id)
    }

    private suspend fun calculateBudgetStatus(budget: Budget): BudgetStatus {
        val tz = TimeZone.currentSystemDefault()
        val start = budget.startDate.atStartOfDay(tz)
        val end = budget.endDate.atEndOfDay(tz)
        val transactions = budgetRepository.getTransactionsInDateRange(
            accountId = budget.accountId,
            categories = budget.categories,
            isExpense = budget.isExpense,
            start = start,
            end = end
        )
        val spent = transactions.sumOf { it.amount }
        val remaining = budget.limit - spent
        val percentageUsed = if (budget.limit > 0) (spent / budget.limit) * 100 else 0.0
        val isExceeded = spent > budget.limit

        return BudgetStatus(
            limit = budget.limit,
            spent = spent,
            remaining = remaining,
            percentageUsed = percentageUsed,
            isExceeded = isExceeded
        )
    }

    private fun LocalDate.atStartOfDay(zone: TimeZone = TimeZone.currentSystemDefault()): Instant =
        this.atTime(LocalTime(0, 0)).toInstant(zone)

    private fun LocalDate.atEndOfDay(zone: TimeZone = TimeZone.currentSystemDefault()): Instant =
        this.plus(1, DateTimeUnit.DAY).atStartOfDay(zone)
}