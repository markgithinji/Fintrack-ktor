package core

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable


@Serializable
data class OverviewSummaryDto(
    val weeklyOverview: List<DaySummaryDto>,
    val monthlyOverview: List<DaySummaryDto>
)

@Serializable
data class DaySummaryDto(
    val date: LocalDate,
    val income: Double,
    val expense: Double
)