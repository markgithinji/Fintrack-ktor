package com.fintrack.feature.transactions

fun Transaction.validate() {
    require(amount > 0) { "amount must be greater than 0" }
    require(category.isNotBlank()) { "category must not be empty" }
}
