package core

import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDate

@Serializable
data class SummaryDto(
    val income: Double,
    val expense: Double,
    val balance: Double,

    // Expense highlights
    val highestMonth: HighlightDto?,
    val highestCategory: HighlightDto?,
    val highestDay: HighlightDto?,

    // Income highlights
    val highestIncomeMonth: HighlightDto? = null,
    val highestIncomeCategory: HighlightDto? = null,
    val highestIncomeDay: HighlightDto? = null,

    val averagePerDay: Double,

    val weeklyCategorySummary: Map<String, List<CategorySummaryDto>>,
    val monthlyCategorySummary: Map<String, List<CategorySummaryDto>>
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