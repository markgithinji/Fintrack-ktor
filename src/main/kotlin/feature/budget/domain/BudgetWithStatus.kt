package com.fintrack.feature.budget.domain

import com.fintrack.feature.budget.data.BudgetDto
import com.fintrack.feature.budget.data.BudgetStatusDto
import feature.transactions.Budget

data class BudgetWithStatus(
    val budget: Budget,
    val status: BudgetStatus
)