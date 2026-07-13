package feature.transaction.data.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class TransactionDto(
    val id: String?,
    val accountId: String,
    val isIncome: Boolean,
    @Contextual val amount: BigDecimal,
    @Contextual val transactionCost: BigDecimal,
    val category: String,
    val categoryId: String,
    val dateTime: String,
    val description: String? = null,
    val externalId: String? = null,
    @Contextual val balance: BigDecimal? = null
)
