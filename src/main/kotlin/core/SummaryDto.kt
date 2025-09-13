package core

import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDate

@Serializable
data class SummaryDto(
    val income: Double,
    val expense: Double,
    val balance: Double,

    // Expense highlights
    val highestMonth: HighlightDto? = null,
    val highestCategory: HighlightDto? = null,
    val highestDay: HighlightDto? = null,
    val averagePerDay: Double = 0.0,

    // Income highlights
    val highestIncomeMonth: HighlightDto? = null,
    val highestIncomeCategory: HighlightDto? = null,
    val highestIncomeDay: HighlightDto? = null,
    val averageIncomePerDay: Double = 0.0,

    // Expense categories
    val weeklyCategorySummary: Map<String, List<CategorySummaryDto>> = emptyMap(),
    val monthlyCategorySummary: Map<String, List<CategorySummaryDto>> = emptyMap(),

    // Income categories
    val weeklyIncomeCategorySummary: Map<String, List<CategorySummaryDto>> = emptyMap(),
    val monthlyIncomeCategorySummary: Map<String, List<CategorySummaryDto>> = emptyMap()
)

@Serializable
data class HighlightDto(
    val label: String,
    val value: String, // duplicate for convenience
    val amount: Double
)

@Serializable
data class CategorySummaryDto(
    val category: String,
    val total: Double,
    val percentage: Double
)