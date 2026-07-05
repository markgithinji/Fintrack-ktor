package feature.summary.domain.model

import kotlinx.datetime.LocalDate

data class OverviewSummary(
    val period: String,
    val isCurrent: Boolean,
    val weeklyOverview: List<DaySummary>,
    val monthlyOverview: List<DaySummary>
)

data class DaySummary(
    val date: LocalDate,
    val income: Double,
    val expense: Double
)