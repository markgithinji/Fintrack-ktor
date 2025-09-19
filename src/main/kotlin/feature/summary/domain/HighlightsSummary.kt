package feature.transactions

import kotlinx.serialization.Serializable

// --------- Domain ---------
@Serializable
data class HighlightsSummary(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val balance: Double = 0.0,
    val incomeHighlights: Highlights = Highlights(),
    val expenseHighlights: Highlights = Highlights()
)

@Serializable
data class Highlights(
    val highestMonth: Highlight? = null,
    val highestCategory: Highlight? = null,
    val highestDay: Highlight? = null,
    val averagePerDay: Double = 0.0
)

@Serializable
data class Highlight(
    val label: String = "",
    val value: String = "",
    val amount: Double = 0.0
)

