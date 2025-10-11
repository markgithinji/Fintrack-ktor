package com.fintrack.feature.budget.data.model

import kotlinx.datetime.LocalDate
import java.util.UUID

data class CreateBudgetRequest(
    val accountId: UUID,
    val name: String,
    val categories: List<String>,
    val limit: Double,
    val isExpense: Boolean,
    val startDate: LocalDate,
    val endDate: LocalDate
)