package com.fintrack.feature.transaction.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class UpdateTransactionRequest(
    val accountId: String,
    val isIncome: Boolean,
    val amount: Double,
    val transactionCost: Double? = null,
    val category: String,
    val dateTime: Instant,
    val description: String,
    val externalId: String? = null,
    val balance: Double? = null
)
