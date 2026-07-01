package feature.transaction.domain.model

import kotlinx.datetime.Instant

import java.util.UUID

data class Transaction(
    val id: UUID?,
    val userId: UUID,
    val isIncome: Boolean,
    val amount: Double,
    val transactionCost: Double,
    val category: String,
    val dateTime: Instant,
    val description: String?,
    val accountId: UUID
) {
    val totalAmount: Double
        get() = if (isIncome) amount - transactionCost else amount + transactionCost
}
