package core

import feature.transactions.Transaction
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class TransactionDto(
    val id: Int? = null,
    val accountId: Int,
    val isIncome: Boolean,
    val amount: Double,
    val category: String,
    val dateTime: String,
    val description: String? = null
)

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


// --- Validation ---

fun TransactionDto.validate() {
    require(amount > 0) { "amount must be greater than 0" }
    require(category.isNotBlank()) { "category must not be empty" }
}
