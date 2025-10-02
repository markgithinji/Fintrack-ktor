package feature.transactions.data.model

import feature.transactions.domain.model.Transaction
import kotlinx.datetime.LocalDateTime

// --- Domain -> DTO ---
fun Transaction.toDto() = TransactionDto(
    id = this.id,
    accountId = this.accountId,
    isIncome = this.isIncome,
    amount = this.amount,
    category = this.category,
    dateTime = this.dateTime.toString(),
    description = this.description
)

// --- DTO -> Domain ---
fun TransactionDto.toTransaction(userId: Int) = Transaction(
    id = this.id,
    userId = userId,
    accountId = this.accountId,
    isIncome = this.isIncome,
    amount = this.amount,
    category = this.category,
    dateTime = LocalDateTime.parse(this.dateTime),
    description = this.description
)