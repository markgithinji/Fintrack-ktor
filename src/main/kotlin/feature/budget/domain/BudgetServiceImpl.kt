package feature.budget.domain

import com.fintrack.core.domain.AppError
import com.fintrack.core.domain.Result
import com.fintrack.core.logger
import com.fintrack.core.withContext
import com.fintrack.feature.budget.data.BudgetWithStatusDto
import com.fintrack.feature.budget.data.model.CreateBudgetRequest
import com.fintrack.feature.budget.data.model.UpdateBudgetRequest
import com.fintrack.feature.budget.data.toDomain
import com.fintrack.feature.budget.data.toDto
import feature.budget.domain.model.BudgetStatus
import feature.budget.domain.model.BudgetWithStatus
import com.fintrack.feature.transaction.data.model.DeleteResponse
import feature.budget.domain.model.Budget
import kotlinx.datetime.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

class BudgetServiceImpl(
    private val budgetRepository: BudgetRepository
) : BudgetService {

    private val log = logger<BudgetServiceImpl>()

    override suspend fun getAllBudgets(
        userId: UUID, 
        accountId: UUID?,
        limit: Int,
        offset: Long
    ): Result<List<BudgetWithStatusDto>> {
        log.withContext("userId" to userId, "accountId" to accountId, "limit" to limit, "offset" to offset)
            .debug { "Fetching all budgets with pagination" }

        val budgets = budgetRepository.getAllByUser(userId, accountId, limit, offset)

        val spentAmounts = budgetRepository.getSpentAmounts(budgets)

        val result: List<BudgetWithStatusDto> = budgets.map { budget ->
            val spent = spentAmounts[budget.id] ?: BigDecimal.ZERO
            BudgetWithStatus(
                budget = budget,
                status = assembleBudgetStatus(budget, spent)
            ).toDto()
        }

        log.withContext("userId" to userId, "budgetCount" to result.size)
            .debug { "Budgets retrieved successfully" }
        return Result.Success(result)
    }

    override suspend fun getBudgetById(userId: UUID, id: UUID): Result<BudgetWithStatusDto> {
        log.withContext("userId" to userId, "budgetId" to id)
            .debug { "Fetching budget by ID" }

        val budget = budgetRepository.getById(userId, id)
            ?: return Result.Failure(AppError.NotFound("Budget $id not found"))

        val spent = budgetRepository.getSpentAmount(
            accountIds = budget.accountIds,
            categoryIds = budget.categoryIds,
            isExpense = budget.isExpense,
            start = budget.startDate.atStartOfDay(TimeZone.UTC),
            end = budget.endDate.atEndOfDay(TimeZone.UTC)
        )

        val budgetWithStatus = BudgetWithStatus(
            budget = budget,
            status = assembleBudgetStatus(budget, spent)
        ).toDto()

        log.withContext("userId" to userId, "budgetId" to id)
            .debug { "Budget retrieved successfully" }
        return Result.Success(budgetWithStatus)
    }

    override suspend fun createBudget(
        userId: UUID,
        request: CreateBudgetRequest
    ): Result<BudgetWithStatusDto> {
        log.withContext(
            "userId" to userId,
            "budgetName" to request.name,
            "limit" to request.limit,
            "accountIds" to request.accountIds
        ).info { "Creating budget" }

        val budget = budgetRepository.add(request.toDomain())
        
        // New budget starts with 0 spent
        val budgetWithStatus = BudgetWithStatus(
            budget = budget,
            status = assembleBudgetStatus(budget, BigDecimal.ZERO)
        ).toDto()

        log.withContext("userId" to userId, "budgetId" to budget.id)
            .info { "Budget created successfully" }
        return Result.Success(budgetWithStatus)
    }

    override suspend fun createBudgets(
        userId: UUID,
        requests: List<CreateBudgetRequest>
    ): Result<List<BudgetWithStatusDto>> {
        log.withContext("userId" to userId, "budgetCount" to requests.size)
            .info { "Creating multiple budgets" }

        val budgets = budgetRepository.addAll(requests.map { it.toDomain() })
        val result = budgets.map { budget ->
            BudgetWithStatus(
                budget = budget,
                status = assembleBudgetStatus(budget, BigDecimal.ZERO)
            ).toDto()
        }

        log.withContext("userId" to userId, "createdCount" to result.size)
            .info { "Multiple budgets created successfully" }
        return Result.Success(result)
    }

    override suspend fun updateBudget(
        userId: UUID,
        id: UUID,
        request: UpdateBudgetRequest
    ): Result<BudgetWithStatusDto> {
        log.withContext("userId" to userId, "budgetId" to id)
            .info { "Updating budget" }

        val updatedBudget = budgetRepository.update(userId, id, request.toDomain(id))
            ?: return Result.Failure(AppError.Internal("Failed to update budget $id"))

        val spent = budgetRepository.getSpentAmount(
            accountIds = updatedBudget.accountIds,
            categoryIds = updatedBudget.categoryIds,
            isExpense = updatedBudget.isExpense,
            start = updatedBudget.startDate.atStartOfDay(TimeZone.UTC),
            end = updatedBudget.endDate.atEndOfDay(TimeZone.UTC)
        )

        val budgetWithStatus = BudgetWithStatus(
            budget = updatedBudget,
            status = assembleBudgetStatus(updatedBudget, spent)
        ).toDto()

        log.withContext("userId" to userId, "budgetId" to id)
            .info { "Budget updated successfully" }
        return Result.Success(budgetWithStatus)
    }

    override suspend fun deleteBudget(userId: UUID, id: UUID): Result<Unit> {
        log.withContext("userId" to userId, "budgetId" to id)
            .info { "Deleting budget" }

        val deleted = budgetRepository.delete(userId, id)

        if (!deleted) {
            log.withContext("userId" to userId, "budgetId" to id)
                .warn { "Budget deletion failed - not found" }
            return Result.Failure(AppError.NotFound("Budget $id not found"))
        }

        log.withContext("userId" to userId, "budgetId" to id)
            .info { "Budget deleted successfully" }
        return Result.Success(Unit)
    }

    override suspend fun deleteAllBudgets(userId: UUID, accountIds: List<UUID>?): Result<DeleteResponse> {
        log.withContext("userId" to userId, "accountIds" to accountIds)
            .info { "Deleting budgets" }

        val deletedCount = budgetRepository.deleteAllByUser(userId, accountIds)

        val result = DeleteResponse(
            message = if (!accountIds.isNullOrEmpty())
                "Successfully deleted $deletedCount budgets for selected accounts"
            else "Successfully deleted $deletedCount budgets",
            cleared = deletedCount > 0
        )

        log.withContext(
            "userId" to userId,
            "accountIds" to accountIds,
            "deletedCount" to deletedCount,
            "cleared" to result.cleared
        ).info { "Budgets deletion completed" }

        return Result.Success(result)
    }

    private fun assembleBudgetStatus(budget: Budget, spent: BigDecimal): BudgetStatus {
        val remaining = budget.limit - spent
        val percentageUsed = if (budget.limit.compareTo(BigDecimal.ZERO) > 0) {
            spent.divide(budget.limit, 4, RoundingMode.HALF_UP).toDouble() * 100
        } else 0.0
        val isExceeded = spent.compareTo(budget.limit) > 0

        return BudgetStatus(
            limit = budget.limit,
            spent = spent,
            remaining = remaining,
            percentageUsed = percentageUsed,
            isExceeded = isExceeded
        )
    }

    private fun LocalDate.atStartOfDay(zone: TimeZone = TimeZone.UTC): Instant =
        this.atTime(LocalTime(0, 0)).toInstant(zone)

    private fun LocalDate.atEndOfDay(zone: TimeZone = TimeZone.UTC): Instant =
        this.atTime(LocalTime(23, 59, 59, 999_999_999)).toInstant(zone)
}
