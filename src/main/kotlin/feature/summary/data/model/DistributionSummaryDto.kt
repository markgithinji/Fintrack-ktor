package feature.summary.data.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class DistributionSummaryDto(
    val period: String = "", // e.g. "2025-W37" or "2025-09"
    @Contextual val totalTransactionCost: BigDecimal = BigDecimal.ZERO,
    val incomeCategories: List<CategorySummaryDto> = emptyList(),
    val expenseCategories: List<CategorySummaryDto> = emptyList(),
    val othersInsightSummary: String? = null
)

@Serializable
data class CategorySummaryDto(
    val category: String = "",
    @Contextual val total: BigDecimal = BigDecimal.ZERO,
    val percentage: Double = 0.0,
    val transactionCount: Int = 0,
    val averageTransactionCount: Double? = null,
    val momentumTrend: String? = null,
    val topDescriptionInsights: List<String>? = null
)
