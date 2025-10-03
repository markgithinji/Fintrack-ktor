package com.fintrack.feature.transactions.data.model

import feature.transactions.domain.model.Transaction
import kotlinx.datetime.LocalDateTime

data class CreateTransactionRequest(
    val accountId: Int,
    val isIncome: Boolean,
    val amount: Double,
    val category: String,
    val dateTime: LocalDateTime,
    val description: String
)