package core

@kotlinx.serialization.Serializable
data class OverviewSummaryDto(
    val weeklyOverview: List<DaySummaryDto>,
    val monthlyOverview: List<DaySummaryDto>
)

@kotlinx.serialization.Serializable
data class DaySummaryDto(
    val date: String,
    val income: Double,
    val expense: Double
)