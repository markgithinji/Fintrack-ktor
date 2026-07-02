package com.fintrack.feature.summary.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CategoryComparisonDto(
    val category: String,
    val currentTotal: Double,
    val previousTotal: Double,
    val changePercentage: Double,
    val period: String = "monthly",
    val weeklyChangePercentage: Double? = null,
    val weeklyCurrentTotal: Double? = null
)