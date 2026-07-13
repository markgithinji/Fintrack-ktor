package feature.summary.domain.model

import kotlinx.datetime.LocalDate
import java.math.BigDecimal

data class OverviewSummary(
    val period: String,
    val isCurrent: Boolean,
    val weeklyOverview: List<DaySummary>,
    val monthlyOverview: List<DaySummary>
)

data class DaySummary(
    val date: LocalDate,
    val income: BigDecimal,
    val expense: BigDecimal
)
