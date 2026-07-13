package feature.summary.data.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class CategoryComparisonDto(
    val category: String,
    @Contextual val currentTotal: BigDecimal,
    @Contextual val previousTotal: BigDecimal,
    val changePercentage: Double,
    val isIncome: Boolean = false,
    val period: String = "monthly",
    val weeklyChangePercentage: Double? = null,
    @Contextual val weeklyCurrentTotal: BigDecimal? = null
)
