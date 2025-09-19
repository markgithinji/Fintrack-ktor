package com.fintrack.feature.budget.data

import feature.transactions.Budget

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
