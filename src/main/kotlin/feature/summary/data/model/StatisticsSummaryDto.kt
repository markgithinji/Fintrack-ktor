package feature.summary.data.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class StatisticsSummaryDto(
    val period: String = "",
    val isCurrent: Boolean = true,
    @Contextual val income: BigDecimal = BigDecimal.ZERO,
    @Contextual val expense: BigDecimal = BigDecimal.ZERO,
    @Contextual val balance: BigDecimal = BigDecimal.ZERO,
    @Contextual val totalTransactionCost: BigDecimal = BigDecimal.ZERO,
    val incomeHighlights: HighlightsDto = HighlightsDto(),
    val expenseHighlights: HighlightsDto = HighlightsDto()
)

@Serializable
data class HighlightsDto(
    val highestMonth: HighlightDto? = null,
    val highestCategory: HighlightDto? = null,
    val highestDay: HighlightDto? = null,
    val averagePerDay: Double = 0.0,
    val ytdChangePercentage: Double? = null,
    @Contextual val projectedTotal: BigDecimal? = null,
    val savingsRate: Double? = null,
    val essentialSpendRatio: Double? = null,
    val projectedExceedMonth: String? = null,
    val correlations: List<CorrelationDto>? = null
)

@Serializable
data class CorrelationDto(
    val source: String,
    val target: String,
    val insight: String
)

@Serializable
data class HighlightDto(
    val label: String = "",
    val value: String = "",
    @Contextual val amount: BigDecimal = BigDecimal.ZERO,
    val volatilityPercentage: Double? = null
)
