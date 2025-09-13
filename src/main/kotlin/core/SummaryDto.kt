package core

import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDate

@Serializable
data class SummaryDto(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val balance: Double = 0.0,
    val incomeHighlights: HighlightsDto = HighlightsDto(),
    val expenseHighlights: HighlightsDto = HighlightsDto(),
    val incomeCategorySummary: CategorySummariesDto = CategorySummariesDto(),
    val expenseCategorySummary: CategorySummariesDto = CategorySummariesDto()
)

@Serializable
data class HighlightsDto(
    val highestMonth: HighlightDto? = null,
    val highestCategory: HighlightDto? = null,
    val highestDay: HighlightDto? = null,
    val averagePerDay: Double = 0.0
)

@Serializable
data class CategorySummariesDto(
    val weekly: Map<String, List<CategorySummaryDto>> = emptyMap(),
    val monthly: Map<String, List<CategorySummaryDto>> = emptyMap()
)

@Serializable
data class HighlightDto(
    val label: String = "",
    val value: String = "",
    val amount: Double = 0.0
)

@Serializable
data class CategorySummaryDto(
    val category: String = "",
    val total: Double = 0.0,
    val percentage: Double = 0.0
)