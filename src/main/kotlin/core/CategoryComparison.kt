package core

import kotlinx.serialization.Serializable

data class CategoryComparison(
    val period: String,        // "weekly" or "monthly"
    val category: String,      // category name
    val currentTotal: Double,  // total amount for this period
    val previousTotal: Double, // total amount for last period
    val changePercentage: Double // ((current - previous)/previous)*100
)

@Serializable
data class CategoryComparisonDto(
    val period: String,
    val category: String,
    val currentTotal: Double,
    val previousTotal: Double,
    val changePercentage: Double
)

fun CategoryComparison.toDto() = CategoryComparisonDto(
    period = period,
    category = category,
    currentTotal = currentTotal,
    previousTotal = previousTotal,
    changePercentage = changePercentage
)
