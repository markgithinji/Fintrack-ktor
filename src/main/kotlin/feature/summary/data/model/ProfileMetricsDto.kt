package feature.summary.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ProfileMetricsDto(
    val name: String,
    val email: String,
    val netWorth: Double,
    val savingsRate: Double?,
    val essentialSpendRatio: Double?
)
