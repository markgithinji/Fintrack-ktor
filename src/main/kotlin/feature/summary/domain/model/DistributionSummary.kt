package feature.summary.domain.model

import java.math.BigDecimal

data class DistributionSummary(
    val period: String = "",
    val totalTransactionCost: BigDecimal = BigDecimal.ZERO,
    val incomeCategories: List<CategorySummary> = emptyList(),
    val expenseCategories: List<CategorySummary> = emptyList(),
    val othersInsightSummary: String? = null
)

data class CategorySummary(
    val category: String = "",
    val total: BigDecimal = BigDecimal.ZERO,
    val percentage: Double = 0.0,
    val transactionCount: Int = 0,
    val averageTransactionCount: Double? = null,
    val momentumTrend: String? = null,
    val topDescriptionInsights: List<String>? = null
)
