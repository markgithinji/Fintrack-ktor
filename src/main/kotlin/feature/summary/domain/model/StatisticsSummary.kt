package feature.summary.domain.model

import java.math.BigDecimal

data class StatisticsSummary(
    val period: String = "",
    val isCurrent: Boolean = true,
    val income: BigDecimal = BigDecimal.ZERO,
    val expense: BigDecimal = BigDecimal.ZERO,
    val balance: BigDecimal = BigDecimal.ZERO,
    val totalTransactionCost: BigDecimal = BigDecimal.ZERO,
    val incomeHighlights: Highlights = Highlights(),
    val expenseHighlights: Highlights = Highlights()
)

data class Highlights(
    val highestMonth: Highlight? = null,
    val highestCategory: Highlight? = null,
    val highestDay: Highlight? = null,
    val averagePerDay: Double = 0.0,
    val ytdChangePercentage: Double? = null,
    val projectedTotal: BigDecimal? = null,
    val savingsRate: Double? = null,
    val essentialSpendRatio: Double? = null,
    val projectedExceedMonth: String? = null,
    val correlations: List<Correlation>? = null
)

data class Correlation(
    val source: String,
    val target: String,
    val insight: String
)

data class Highlight(
    val label: String = "",
    val value: String = "",
    val amount: BigDecimal = BigDecimal.ZERO,
    val volatilityPercentage: Double? = null
)
