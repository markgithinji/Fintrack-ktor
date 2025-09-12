package core

import kotlinx.serialization.Serializable

@Serializable
data class PaginatedTransactionDto(
    val data: List<TransactionDto>,
    val nextCursor: String? = null
)
