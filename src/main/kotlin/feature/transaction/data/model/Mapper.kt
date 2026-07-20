package feature.transaction.data.model

import com.fintrack.feature.transaction.data.model.CreateTransactionRequest
import com.fintrack.feature.transaction.data.model.UpdateTransactionRequest
import feature.category.domain.model.CategoryConstants
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

fun CreateTransactionRequest.toDomain(userId: UUID): Transaction {
    val resolvedCategoryId = try {
        UUID.fromString(categoryId)
    } catch (e: Exception) {
        // Placeholder UUID, should be resolved by Service layer if "pending"
        CategoryConstants.PENDING_ID
    }
    
    val resolvedAccountId = try {
        UUID.fromString(this.accountId)
    } catch (e: Exception) {
        // Use user's default account if possible, or a fallback.
        // This prevents 500 crashes when client sends "mpesa" or "equity" strings.
        CategoryConstants.PENDING_ID
    }

    return Transaction(
        id = null,
        userId = userId,
        accountId = resolvedAccountId,
        isIncome = isIncome,
        amount = amount,
        transactionCost = transactionCost,
        category = category ?: "",
        categoryId = resolvedCategoryId,
        dateTime = dateTime,
        description = description,
        externalId = externalId,
        balance = balance
    )
}

fun UpdateTransactionRequest.toDomain(userId: UUID, transactionId: UUID): Transaction {
    val resolvedCategoryId = try {
        UUID.fromString(categoryId)
    } catch (e: Exception) {
        // Placeholder UUID, should be resolved by Service layer if "pending"
        CategoryConstants.PENDING_ID
    }

    val resolvedAccountId = try {
        UUID.fromString(this.accountId)
    } catch (e: Exception) {
        CategoryConstants.PENDING_ID
    }

    return Transaction(
        id = transactionId,
        userId = userId,
        accountId = resolvedAccountId,
        isIncome = isIncome,
        amount = amount,
        transactionCost = transactionCost ?: BigDecimal.ZERO,
        category = category ?: "",
        categoryId = resolvedCategoryId,
        dateTime = dateTime,
        description = description,
        externalId = externalId,
        balance = balance
    )
}
