package feature.summary.data.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class ProfileMetricsDto(
    val name: String,
    val email: String,
    @Contextual val netWorth: BigDecimal,
    val savingsRate: Double?,
    val essentialSpendRatio: Double?
)
