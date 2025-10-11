package feature.transaction.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TransactionDto(
    val id: String?,
    val accountId: String,
    val isIncome: Boolean,
    val amount: Double,
    val category: String,
    val dateTime: String,
    val description: String? = null
)