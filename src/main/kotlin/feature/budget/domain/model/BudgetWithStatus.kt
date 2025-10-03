package com.fintrack.feature.budget.domain

import feature.transaction.Budget

data class BudgetWithStatus(
    val budget: Budget,
    val status: BudgetStatus
)