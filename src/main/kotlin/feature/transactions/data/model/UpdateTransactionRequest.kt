package com.fintrack.feature.transactions.data.model

import kotlinx.datetime.LocalDateTime

data class UpdateTransactionRequest(
    val accountId: Int,
    val isIncome: Boolean,
    val amount: Double,
    val category: String,
    val dateTime: LocalDateTime,
    val description: String
)