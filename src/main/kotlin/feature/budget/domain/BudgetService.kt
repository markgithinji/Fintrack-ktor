package feature.budget.domain

import com.fintrack.feature.budget.data.BudgetWithStatusDto
import com.fintrack.feature.budget.data.model.CreateBudgetRequest
import com.fintrack.feature.budget.data.model.UpdateBudgetRequest
import java.util.UUID


interface BudgetService {
    suspend fun getAllBudgets(userId: UUID, accountId: UUID?): List<BudgetWithStatusDto>
    suspend fun getBudgetById(userId: UUID, id: UUID): BudgetWithStatusDto
    suspend fun createBudget(userId: UUID, request: CreateBudgetRequest): BudgetWithStatusDto
    suspend fun createBudgets(
        userId: UUID,
        requests: List<CreateBudgetRequest>
    ): List<BudgetWithStatusDto>

    suspend fun updateBudget(
        userId: UUID,
        id: UUID,
        request: UpdateBudgetRequest
    ): BudgetWithStatusDto

    suspend fun deleteBudget(userId: UUID, id: UUID): Boolean
}