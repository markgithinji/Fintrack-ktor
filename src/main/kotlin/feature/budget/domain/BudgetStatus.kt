package com.fintrack.feature.budget.domain

data class BudgetStatus(
    val limit: Double,
    val spent: Double,
    val remaining: Double,
    val percentageUsed: Double,
    val isExceeded: Boolean
)