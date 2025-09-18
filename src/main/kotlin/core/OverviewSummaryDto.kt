package core

import kotlinx.datetime.LocalDate

@kotlinx.serialization.Serializable
data class OverviewSummaryDto(
    val weeklyOverview: List<DaySummaryDto>,
    val monthlyOverview: List<DaySummaryDto>
)

@kotlinx.serialization.Serializable
data class DaySummaryDto(
    val date: String, // keep as ISO string
    val income: Double,
    val expense: Double
)

data class OverviewSummary(
    val weeklyOverview: List<DaySummary>,
    val monthlyOverview: List<DaySummary>
)

data class DaySummary(
    val date: LocalDate,
    val income: Double,
    val expense: Double
)

fun OverviewSummary.toDto() = OverviewSummaryDto(
    weeklyOverview = weeklyOverview.map { it.toDto() },
    monthlyOverview = monthlyOverview.map { it.toDto() }
)

fun DaySummary.toDto() = DaySummaryDto(
    date = date.toString(),
    income = income,
    expense = expense
)
