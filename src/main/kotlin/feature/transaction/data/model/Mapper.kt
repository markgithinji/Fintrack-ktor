package feature.transaction.data.model

import com.fintrack.feature.transaction.data.model.CreateTransactionRequest
import com.fintrack.feature.transaction.data.model.UpdateTransactionRequest
import feature.transaction.domain.model.Transaction
import java.math.BigDecimal
import java.util.UUID

fun Transaction.toDto() = TransactionDto(
    id = this.id?.toString(),
    accountId = this.accountId.toString(),
    isIncome = this.isIncome,
    amount = this.amount,
    transactionCost = this.transactionCost,
    category = this.category,
    categoryId = this.categoryId.toString(),
    dateTime = this.dateTime.toString(),
    description = this.description,
    externalId = this.externalId,
    balance = this.balance,
    createdAt = this.createdAt?.toString(),
    updatedAt = this.updatedAt?.toString()
)

fun CreateTransactionRequest.toDomain(userId: UUID): Transaction = Transaction(
    id = null,
    userId = userId,
    accountId = UUID.fromString(this.accountId),
    isIncome = isIncome,
    amount = amount,
    transactionCost = transactionCost,
    category = "", // Resolved by repository on re-fetch
    categoryId = UUID.fromString(categoryId),
    dateTime = dateTime,
    description = description,
    externalId = externalId,
    balance = balance
)

fun UpdateTransactionRequest.toDomain(userId: UUID, transactionId: UUID): Transaction = Transaction(
    id = transactionId,
    userId = userId,
    accountId = UUID.fromString(this.accountId),
    isIncome = isIncome,
    amount = amount,
    transactionCost = transactionCost ?: BigDecimal.ZERO,
    category = "", // Resolved by repository on re-fetch
    categoryId = UUID.fromString(categoryId),
    dateTime = dateTime,
    description = description,
    externalId = externalId,
    balance = balance
)
