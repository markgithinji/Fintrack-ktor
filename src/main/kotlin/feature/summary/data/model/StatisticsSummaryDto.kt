package feature.summary.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class StatisticsSummaryDto(
    val period: String = "",
    val isCurrent: Boolean = true,
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val balance: Double = 0.0,
    val totalTransactionCost: Double = 0.0,
    val incomeHighlights: HighlightsDto = HighlightsDto(),
    val expenseHighlights: HighlightsDto = HighlightsDto()
)

@Serializable
data class HighlightsDto(
    val highestMonth: HighlightDto? = null,
    val highestCategory: HighlightDto? = null,
    val highestDay: HighlightDto? = null,
    val averagePerDay: Double = 0.0,
    @SerialName("ytd_change_percentage") val ytdChangePercentage: Double? = null,
    @SerialName("projected_total") val projectedTotal: Double? = null
)

@Serializable
data class HighlightDto(
    val label: String = "",
    val value: String = "",
    val amount: Double = 0.0,
    @SerialName("volatility_percentage") val volatilityPercentage: Double? = null
)
