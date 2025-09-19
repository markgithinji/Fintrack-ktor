package feature.transactions.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PaginatedTransactionDto(
    val data: List<TransactionDto>,
    val nextCursor: String? = null
)