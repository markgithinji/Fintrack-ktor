package com.fintrack.feature.summary.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DistributionSummaryDto(
    val period: String = "", // e.g. "2025-W37" or "2025-09"
    val incomeCategories: List<CategorySummaryDto> = emptyList(),
    val expenseCategories: List<CategorySummaryDto> = emptyList()
)

@Serializable
data class CategorySummaryDto(
    val category: String = "",
    val total: Double = 0.0,
    val percentage: Double = 0.0
)
