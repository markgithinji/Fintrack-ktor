package com.fintrack.feature.budget.data

import kotlinx.serialization.Serializable

@Serializable
data class BudgetWithStatusDto(
    val budget: BudgetDto,
    val status: BudgetStatusDto
)
