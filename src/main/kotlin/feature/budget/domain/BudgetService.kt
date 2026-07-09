package feature.budget.domain

import com.fintrack.core.domain.Result
import com.fintrack.feature.budget.data.BudgetWithStatusDto
import com.fintrack.feature.budget.data.model.CreateBudgetRequest
import com.fintrack.feature.budget.data.model.UpdateBudgetRequest
import com.fintrack.feature.transaction.data.model.DeleteResponse
import java.util.UUID


interface BudgetService {
    suspend fun getAllBudgets(userId: UUID, accountId: UUID?): Result<List<BudgetWithStatusDto>>
    suspend fun getBudgetById(userId: UUID, id: UUID): Result<BudgetWithStatusDto>
    suspend fun createBudget(userId: UUID, request: CreateBudgetRequest): Result<BudgetWithStatusDto>
    suspend fun createBudgets(
        userId: UUID,
        requests: List<CreateBudgetRequest>
    ): Result<List<BudgetWithStatusDto>>

    suspend fun updateBudget(
        userId: UUID,
        id: UUID,
        request: UpdateBudgetRequest
    ): Result<BudgetWithStatusDto>

    suspend fun deleteBudget(userId: UUID, id: UUID): Result<Unit>
    suspend fun deleteAllBudgets(userId: UUID, accountIds: List<UUID>?): Result<DeleteResponse>
}
