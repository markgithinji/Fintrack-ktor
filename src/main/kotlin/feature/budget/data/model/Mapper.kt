package com.fintrack.feature.budget.data

import com.fintrack.feature.budget.data.model.CreateBudgetRequest
import com.fintrack.feature.budget.data.model.UpdateBudgetRequest
import feature.budget.domain.model.BudgetStatus
import feature.budget.domain.model.BudgetWithStatus
import feature.budget.domain.model.Budget
import java.util.UUID

fun Budget.toDto(): BudgetDto =
    BudgetDto(
        id = id.toString(),
        accountId = accountId.toString(),
        name = name,
        categories = categories,
        limit = limit,
        isExpense = isExpense,
        startDate = startDate,
        endDate = endDate
    )

fun BudgetStatus.toDto(): BudgetStatusDto = BudgetStatusDto(
    spent = spent,
    remaining = remaining,
    percentageUsed = percentageUsed,
    isExceeded = isExceeded
)

fun BudgetWithStatus.toDto(): BudgetWithStatusDto =
    BudgetWithStatusDto(
        budget = budget.toDto(),
        status = status.toDto()
    )

fun CreateBudgetRequest.toDomain(): Budget = Budget(
    accountId = UUID.fromString(accountId),
    name = name,
    categories = categories,
    limit = limit,
    isExpense = isExpense,
    startDate = startDate,
    endDate = endDate
)

fun UpdateBudgetRequest.toDomain(budgetId: UUID): Budget = Budget(
    id = budgetId,
    accountId = UUID.fromString(accountId),
    name = name,
    categories = categories,
    limit = limit,
    isExpense = isExpense,
    startDate = startDate,
    endDate = endDate
)
