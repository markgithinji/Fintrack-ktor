package feature.summary.domain.model

import java.math.BigDecimal

data class CategoryComparison(
    val category: String,
    val currentTotal: BigDecimal,
    val previousTotal: BigDecimal,
    val changePercentage: Double,
    val isIncome: Boolean = false,
    val period: String = "monthly",
    val weeklyChangePercentage: Double? = null,
    val weeklyCurrentTotal: BigDecimal? = null
)
