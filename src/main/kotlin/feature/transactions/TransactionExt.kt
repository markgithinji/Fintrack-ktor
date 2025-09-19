package feature.transactions

import feature.transactions.data.model.TransactionDto
import feature.transactions.domain.model.Transaction
import kotlin.text.isNotBlank

fun Transaction.validate() {
    require(amount > 0) { "amount must be greater than 0" }
    require(category.isNotBlank()) { "category must not be empty" }
}

fun TransactionDto.validate() {
    require(amount > 0) { "amount must be greater than 0" }
    require(category.isNotBlank()) { "category must not be empty" }
}
