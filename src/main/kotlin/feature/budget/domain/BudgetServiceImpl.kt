package feature.budget.domain


import com.fintrack.core.logger
import com.fintrack.core.withContext
import com.fintrack.feature.budget.data.BudgetWithStatusDto
import com.fintrack.feature.budget.data.model.CreateBudgetRequest
import com.fintrack.feature.budget.data.model.UpdateBudgetRequest
import com.fintrack.feature.budget.data.toDomain
import com.fintrack.feature.budget.data.toDto
import com.fintrack.feature.budget.domain.BudgetStatus
import com.fintrack.feature.budget.domain.BudgetWithStatus
import core.ResourceNotFoundException
import feature.transaction.Budget
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import java.util.UUID

class BudgetServiceImpl(
    private val budgetRepository: BudgetRepository
) : BudgetService {

    private val log = logger<BudgetServiceImpl>()

    override suspend fun getAllBudgets(userId: UUID, accountId: UUID?): List<BudgetWithStatusDto> {
        log.withContext("userId" to userId, "accountId" to accountId)
            .debug { "Fetching all budgets" }

        val budgets = budgetRepository.getAllByUser(userId, accountId)
        val result = budgets.map { budget ->
            BudgetWithStatus(
                budget = budget,
                status = calculateBudgetStatus(budget)
            ).toDto()
        }

        log.withContext("userId" to userId, "budgetCount" to result.size)
            .debug { "Budgets retrieved successfully" }
        return result
    }

    override suspend fun getBudgetById(userId: UUID, id: UUID): BudgetWithStatusDto {
        log.withContext("userId" to userId, "budgetId" to id)
            .debug { "Fetching budget by ID" }

        val budget = budgetRepository.getById(userId, id)
            ?: throw ResourceNotFoundException("Budget $id not found")

        val budgetWithStatus = BudgetWithStatus(
            budget = budget,
            status = calculateBudgetStatus(budget)
        ).toDto()

        log.withContext("userId" to userId, "budgetId" to id)
            .debug { "Budget retrieved successfully" }
        return budgetWithStatus
    }

    override suspend fun createBudget(
        userId: UUID,
        request: CreateBudgetRequest
    ): BudgetWithStatusDto {
        log.withContext(
            "userId" to userId,
            "budgetName" to request.name,
            "limit" to request.limit,
            "accountId" to request.accountId
        ).info { "Creating budget" }

        val budget = budgetRepository.add(request.toDomain())
        val budgetWithStatus = BudgetWithStatus(
            budget = budget,
            status = calculateBudgetStatus(budget)
        ).toDto()

        log.withContext("userId" to userId, "budgetId" to budget.id)
            .info { "Budget created successfully" }
        return budgetWithStatus
    }

    override suspend fun createBudgets(
        userId: UUID,
        requests: List<CreateBudgetRequest>
    ): List<BudgetWithStatusDto> {
        log.withContext("userId" to userId, "budgetCount" to requests.size)
            .info { "Creating multiple budgets" }

        val budgets = budgetRepository.addAll(requests.map { it.toDomain() })
        val result = budgets.map { budget ->
            BudgetWithStatus(
                budget = budget,
                status = calculateBudgetStatus(budget)
            ).toDto()
        }

        log.withContext("userId" to userId, "createdCount" to result.size)
            .info { "Multiple budgets created successfully" }
        return result
    }

    override suspend fun updateBudget(
        userId: UUID,
        id: UUID,
        request: UpdateBudgetRequest
    ): BudgetWithStatusDto {
        log.withContext("userId" to userId, "budgetId" to id)
            .info { "Updating budget" }

        val existingBudget = budgetRepository.getById(userId, id)
            ?: throw ResourceNotFoundException("Budget $id not found")

        val updatedBudget = budgetRepository.update(userId, id, request.toDomain(id))
            ?: throw IllegalStateException("Failed to update budget $id")

        val budgetWithStatus = BudgetWithStatus(
            budget = updatedBudget,
            status = calculateBudgetStatus(updatedBudget)
        ).toDto()

        log.withContext("userId" to userId, "budgetId" to id)
            .info { "Budget updated successfully" }
        return budgetWithStatus
    }

    override suspend fun deleteBudget(userId: UUID, id: UUID): Boolean {
        log.withContext("userId" to userId, "budgetId" to id)
            .info { "Deleting budget" }

        val deleted = budgetRepository.delete(userId, id)

        if (deleted) {
            log.withContext("userId" to userId, "budgetId" to id)
                .info { "Budget deleted successfully" }
        } else {
            log.withContext("userId" to userId, "budgetId" to id)
                .warn { "Budget deletion failed - not found" }
        }

        return deleted
    }

    private suspend fun calculateBudgetStatus(budget: Budget): BudgetStatus {
        log.withContext("budgetId" to budget.id).debug { "Calculating budget status" }

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

        log.withContext(
            "budgetId" to budget.id,
            "limit" to budget.limit,
            "spent" to spent,
            "remaining" to remaining,
            "percentageUsed" to percentageUsed,
            "isExceeded" to isExceeded,
            "transactionCount" to transactions.size
        ).debug { "Budget status calculated" }

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