package com.fintrack.feature.summary.domain

import kotlinx.serialization.Serializable

@Serializable
data class DistributionSummary(
    val period: String = "",
    val incomeCategories: List<CategorySummary> = emptyList(),
    val expenseCategories: List<CategorySummary> = emptyList()
)

@Serializable
data class CategorySummary(
    val category: String = "",
    val total: Double = 0.0,
    val percentage: Double = 0.0
)
