package com.fintrack.feature.transactions

fun Transaction.validate() {
    require(type == "income" || type == "expense") { "type must be 'income' or 'expense'" }
    require(amount > 0) { "amount must be greater than 0" }
}