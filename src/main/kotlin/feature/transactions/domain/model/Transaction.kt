package feature.transactions.domain.model

import kotlinx.datetime.LocalDateTime

data class Transaction(
    val id: Int? = null,
    val userId: Int,
    val accountId: Int,
    val isIncome: Boolean,
    val amount: Double,
    val category: String,
    val dateTime: LocalDateTime,
    val description: String? = null
)