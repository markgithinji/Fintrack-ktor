package core

import kotlinx.serialization.Serializable

@Serializable
data class HighlightsSummaryDto(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val balance: Double = 0.0,
    val incomeHighlights: HighlightsDto = HighlightsDto(),
    val expenseHighlights: HighlightsDto = HighlightsDto()
)

@Serializable
data class HighlightsDto(
    val highestMonth: HighlightDto? = null,
    val highestCategory: HighlightDto? = null,
    val highestDay: HighlightDto? = null,
    val averagePerDay: Double = 0.0
)

@Serializable
data class HighlightDto(
    val label: String = "",
    val value: String = "",
    val amount: Double = 0.0
)