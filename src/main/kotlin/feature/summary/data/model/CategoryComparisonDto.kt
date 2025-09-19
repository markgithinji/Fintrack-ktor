package com.fintrack.feature.summary.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CategoryComparisonDto(
    val period: String,
    val category: String,
    val currentTotal: Double,
    val previousTotal: Double,
    val changePercentage: Double
)