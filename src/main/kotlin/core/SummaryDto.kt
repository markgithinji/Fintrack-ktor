package core

import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDate

@Serializable
data class SummaryDto(
    val income: Double,
    val expense: Double,
    val balance: Double,

    val highestMonth: HighlightDto?,
    val highestCategory: HighlightDto?,
    val highestDay: HighlightDto?,

    val averagePerDay: Double,

    val weeklyCategorySummary: Map<String, List<CategorySummaryDto>>,
    val monthlyCategorySummary: Map<String, List<CategorySummaryDto>>
)

@Serializable
data class HighlightDto(
    val label: String,   // month/category/date as string
    val value: String,   // duplicate for convenience
    val amount: Double
)

@Serializable
data class CategorySummaryDto(
    val category: String,
    val total: Double,
    val percentage: Double
)