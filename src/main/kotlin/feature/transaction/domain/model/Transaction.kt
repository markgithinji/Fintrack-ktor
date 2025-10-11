package feature.transaction.domain.model

import kotlinx.datetime.LocalDateTime

import java.util.UUID

data class Transaction(
    val id: UUID?,
    val userId: UUID,
    val isIncome: Boolean,
    val amount: Double,
    val category: String,
    val dateTime: LocalDateTime,
    val description: String?,
    val accountId: UUID
)