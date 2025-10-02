package com.fintrack.feature.budget.domain

import feature.transactions.Budget

data class BudgetWithStatus(
    val budget: Budget,
    val status: BudgetStatus
)