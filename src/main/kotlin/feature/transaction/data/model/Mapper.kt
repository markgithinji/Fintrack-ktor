package feature.transaction.data.model

import com.fintrack.feature.transactions.data.model.CreateTransactionRequest
import com.fintrack.feature.transactions.data.model.UpdateTransactionRequest
import feature.transaction.domain.model.Transaction
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

fun CreateTransactionRequest.toDomain(userId: Int) = Transaction(
    id = 0,
    userId = userId,
    accountId = accountId,
    isIncome = isIncome,
    amount = amount,
    category = category,
    dateTime = dateTime,
    description = description
)

fun UpdateTransactionRequest.toDomain(userId: Int, transactionId: Int) = Transaction(
    id = transactionId,
    userId = userId,
    accountId = accountId,
    isIncome = isIncome,
    amount = amount,
    category = category,
    dateTime = dateTime,
    description = description
)