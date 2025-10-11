package feature.transaction.data.model

import com.fintrack.feature.transactions.data.model.CreateTransactionRequest
import com.fintrack.feature.transactions.data.model.UpdateTransactionRequest
import feature.transaction.domain.model.Transaction
import java.util.UUID

fun Transaction.toDto() = TransactionDto(
    id = this.id?.toString(),
    accountId = this.accountId.toString(),
    isIncome = this.isIncome,
    amount = this.amount,
    category = this.category,
    dateTime = this.dateTime.toString(),
    description = this.description
)

fun CreateTransactionRequest.toDomain(userId: UUID): Transaction = Transaction(
    id = null,
    userId = userId,
    accountId = UUID.fromString(this.accountId),
    isIncome = isIncome,
    amount = amount,
    category = category,
    dateTime = dateTime,
    description = description
)

fun UpdateTransactionRequest.toDomain(userId: UUID, transactionId: UUID): Transaction = Transaction(
    id = transactionId,
    userId = userId,
    accountId = UUID.fromString(this.accountId),
    isIncome = isIncome,
    amount = amount,
    category = category,
    dateTime = dateTime,
    description = description
)