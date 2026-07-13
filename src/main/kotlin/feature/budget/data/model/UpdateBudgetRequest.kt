package com.fintrack.feature.budget.data.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class UpdateBudgetRequest(
    val accountIds: List<String>,
    val name: String,
    val categoryIds: List<String>,
    @Contextual val limit: BigDecimal,
    val isExpense: Boolean,
    val startDate: LocalDate,
    val endDate: LocalDate
)
