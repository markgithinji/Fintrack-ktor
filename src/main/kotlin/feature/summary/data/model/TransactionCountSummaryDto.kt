package feature.summary.data.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class TransactionCountSummaryDto(
    val totalIncomeTransactions: Int,
    val totalExpenseTransactions: Int,
    val totalTransactions: Int,
    @Contextual val totalTransactionCost: BigDecimal = BigDecimal.ZERO,
    @Contextual val totalAmount: BigDecimal = BigDecimal.ZERO
)
