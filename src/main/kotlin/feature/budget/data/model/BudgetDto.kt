package com.fintrack.feature.budget.data

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class BudgetDto(
    val id: Int? = null,
    val accountId: Int,
    val name: String,
    val categories: List<String>,
    val limit: Double,
    val isExpense: Boolean,
    val startDate: LocalDate,
    val endDate: LocalDate
)