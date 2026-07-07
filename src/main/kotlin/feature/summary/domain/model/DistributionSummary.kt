package feature.summary.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DistributionSummary(
    val period: String = "",
    val incomeCategories: List<CategorySummary> = emptyList(),
    val expenseCategories: List<CategorySummary> = emptyList(),
    val othersInsightSummary: String? = null
)

@Serializable
data class CategorySummary(
    val category: String = "",
    val total: Double = 0.0,
    val percentage: Double = 0.0,
    val transactionCount: Int = 0,
    val averageTransactionCount: Double? = null,
    val momentumTrend: String? = null,
    val topDescriptionInsights: List<String>? = null
)