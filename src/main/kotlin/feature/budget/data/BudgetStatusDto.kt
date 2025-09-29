package com.fintrack.feature.budget.data

import kotlinx.serialization.Serializable

@Serializable
data class BudgetStatusDto(
    val spent: Double,
    val remaining: Double,
    val percentageUsed: Double,
    val isExceeded: Boolean
)