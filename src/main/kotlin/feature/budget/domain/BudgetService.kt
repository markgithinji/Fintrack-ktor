package feature.budget.domain


import com.fintrack.feature.budget.data.BudgetDto
import com.fintrack.feature.budget.data.BudgetRepository
import com.fintrack.feature.budget.data.toDomain
import com.fintrack.feature.budget.domain.BudgetStatus
import com.fintrack.feature.budget.domain.BudgetWithStatus
import feature.transactions.Budget
import kotlinx.datetime.*
import org.jetbrains.exposed.sql.*

interface BudgetService {
    suspend fun getAllBudgets(userId: Int, accountId: Int? = null): List<BudgetWithStatus>
    suspend fun getBudgetById(userId: Int, id: Int): BudgetWithStatus?
    suspend fun createBudget(userId: Int, budgetDto: BudgetDto): BudgetWithStatus
    suspend fun createBudgets(userId: Int, budgetDtos: List<BudgetDto>): List<BudgetWithStatus>
    suspend fun updateBudget(userId: Int, id: Int, budgetDto: BudgetDto): BudgetWithStatus?
    suspend fun deleteBudget(userId: Int, id: Int): Boolean
}

class BudgetServiceImpl(
    private val budgetRepository: BudgetRepository
) : BudgetService {
    override suspend fun getAllBudgets(userId: Int, accountId: Int?): List<BudgetWithStatus> {
        val budgets = budgetRepository.getAllByUser(userId, accountId)
        return budgets.map { budget ->
            BudgetWithStatus(budget, calculateBudgetStatus(budget))
        }
    }

    override suspend fun getBudgetById(userId: Int, id: Int): BudgetWithStatus? {
        val budget = budgetRepository.getById(userId, id) ?: return null
        return BudgetWithStatus(budget, calculateBudgetStatus(budget))
    }

    override suspend fun createBudget(userId: Int, budgetDto: BudgetDto): BudgetWithStatus {
        val budget = budgetRepository.add(budgetDto.toDomain(userId))
        return BudgetWithStatus(budget, calculateBudgetStatus(budget))
    }

    override suspend fun createBudgets(userId: Int, budgetDtos: List<BudgetDto>): List<BudgetWithStatus> {
        val budgets = budgetRepository.addAll(budgetDtos.map { it.toDomain(userId) })
        return budgets.map { budget ->
            BudgetWithStatus(budget, calculateBudgetStatus(budget))
        }
    }

    override suspend fun updateBudget(userId: Int, id: Int, budgetDto: BudgetDto): BudgetWithStatus? {
        val updatedBudget = budgetRepository.update(userId, id, budgetDto.toDomain(userId)) ?: return null
        return BudgetWithStatus(updatedBudget, calculateBudgetStatus(updatedBudget))
    }

    override suspend fun deleteBudget(userId: Int, id: Int): Boolean {
        return budgetRepository.delete(userId, id)
    }

    private fun calculateBudgetStatus(budget: Budget): BudgetStatus {
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