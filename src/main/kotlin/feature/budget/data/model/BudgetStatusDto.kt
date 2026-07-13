package com.fintrack.feature.budget.data

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class BudgetStatusDto(
    @Contextual val spent: BigDecimal,
    @Contextual val remaining: BigDecimal,
    val percentageUsed: Double,
    val isExceeded: Boolean
)
