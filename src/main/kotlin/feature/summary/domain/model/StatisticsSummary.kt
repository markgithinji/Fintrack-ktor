package feature.summary.domain.model

data class StatisticsSummary(
    val period: String = "",
    val isCurrent: Boolean = true,
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val balance: Double = 0.0,
    val totalTransactionCost: Double = 0.0,
    val incomeHighlights: Highlights = Highlights(),
    val expenseHighlights: Highlights = Highlights()
)

data class Highlights(
    val highestMonth: Highlight? = null,
    val highestCategory: Highlight? = null,
    val highestDay: Highlight? = null,
    val averagePerDay: Double = 0.0,
    val ytdChangePercentage: Double? = null,
    val projectedTotal: Double? = null
)

data class Highlight(
    val label: String = "",
    val value: String = "",
    val amount: Double = 0.0,
    val volatilityPercentage: Double? = null
)