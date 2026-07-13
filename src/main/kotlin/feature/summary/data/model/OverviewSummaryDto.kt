package feature.summary.data.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class OverviewSummaryDto(
    val period: String,
    val isCurrent: Boolean,
    val weeklyOverview: List<DaySummaryDto>,
    val monthlyOverview: List<DaySummaryDto>
)

@Serializable
data class DaySummaryDto(
    val date: LocalDate,
    @Contextual val income: BigDecimal,
    @Contextual val expense: BigDecimal
)
