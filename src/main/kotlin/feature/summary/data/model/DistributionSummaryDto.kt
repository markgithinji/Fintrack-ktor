package feature.summary.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DistributionSummaryDto(
    val period: String = "", // e.g. "2025-W37" or "2025-09"
    val totalTransactionCost: Double = 0.0,
    val incomeCategories: List<CategorySummaryDto> = emptyList(),
    val expenseCategories: List<CategorySummaryDto> = emptyList(),
    val othersInsightSummary: String? = null
)

@Serializable
data class CategorySummaryDto(
    val category: String = "",
    val total: Double = 0.0,
    val percentage: Double = 0.0,
    val transactionCount: Int = 0,
    val averageTransactionCount: Double? = null,
    val momentumTrend: String? = null,
    val topDescriptionInsights: List<String>? = null
)
