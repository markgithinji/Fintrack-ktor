package feature.budget.domain

import com.fintrack.feature.budget.data.BudgetDto
import com.fintrack.feature.budget.domain.BudgetWithStatus

interface BudgetService {
    suspend fun getAllBudgets(userId: Int, accountId: Int? = null): List<BudgetWithStatus>
    suspend fun getBudgetById(userId: Int, id: Int): BudgetWithStatus?
    suspend fun createBudget(userId: Int, budgetDto: BudgetDto): BudgetWithStatus
    suspend fun createBudgets(userId: Int, budgetDtos: List<BudgetDto>): List<BudgetWithStatus>
    suspend fun updateBudget(userId: Int, id: Int, budgetDto: BudgetDto): BudgetWithStatus?
    suspend fun deleteBudget(userId: Int, id: Int): Boolean
}
