package feature.transaction.domain.model

import kotlinx.datetime.Instant

import java.math.BigDecimal
import java.util.UUID

data class Transaction(
    val id: UUID?,
    val userId: UUID,
    val isIncome: Boolean,
    val amount: BigDecimal,
    val transactionCost: BigDecimal,
    val category: String,
    val categoryId: UUID?,
    val dateTime: Instant,
    val description: String?,
    val accountId: UUID,
    val externalId: String? = null,
    val balance: BigDecimal? = null
) {
    val totalAmount: BigDecimal
        get() = if (isIncome) amount - transactionCost else amount + transactionCost
}
