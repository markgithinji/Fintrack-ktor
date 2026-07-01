package com.fintrack.feature.transactions.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CreateTransactionRequest(
    val accountId: String,
    val isIncome: Boolean,
    val amount: Double,
    val transactionCost: Double = 0.0,
    val category: String,
    val dateTime: Instant,
    val description: String,
    val externalId: String? = null,
    val balance: Double? = null
)