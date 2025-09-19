package feature.transactions.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TransactionDto(
    val id: Int? = null,
    val accountId: Int,
    val isIncome: Boolean,
    val amount: Double,
    val category: String,
    val dateTime: String,
    val description: String? = null
)