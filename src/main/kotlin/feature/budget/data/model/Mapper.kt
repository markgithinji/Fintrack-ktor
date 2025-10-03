package com.fintrack.feature.budget.data

import com.fintrack.feature.budget.data.model.CreateBudgetRequest
import com.fintrack.feature.budget.data.model.UpdateBudgetRequest
import com.fintrack.feature.budget.domain.BudgetStatus
import com.fintrack.feature.budget.domain.BudgetWithStatus
import feature.transaction.Budget

fun BudgetDto.toDomain(userId: Int): Budget =
    Budget(
        id = id,
        userId = userId,
        accountId = accountId,
        name = name,
        categories = categories,
        limit = limit,
        isExpense = isExpense,
        startDate = startDate,
        endDate = endDate
    )

fun Budget.toDto(): BudgetDto =
    BudgetDto(
        id = id,
        accountId = accountId,
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

fun CreateBudgetRequest.toDomain(userId: Int) = Budget(
    id = 0,
    userId = userId,
    accountId = accountId,
    name = name,
    categories = categories,
    limit = limit,
    isExpense = isExpense,
    startDate = startDate,
    endDate = endDate
)

fun UpdateBudgetRequest.toDomain(userId: Int, budgetId: Int) = Budget(
    id = budgetId,
    userId = userId,
    accountId = accountId,
    name = name,
    categories = categories,
    limit = limit,
    isExpense = isExpense,
    startDate = startDate,
    endDate = endDate
)