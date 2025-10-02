package feature.transactions
// --------- Domain ---------

data class StatisticsSummary(
    val incomeHighlights: Highlights = Highlights(),
    val expenseHighlights: Highlights = Highlights()
)

data class Highlights(
    val highestMonth: Highlight? = null,
    val highestCategory: Highlight? = null,
    val highestDay: Highlight? = null,
    val averagePerDay: Double = 0.0
)

data class Highlight(
    val label: String = "",
    val value: String = "",
    val amount: Double = 0.0
)

