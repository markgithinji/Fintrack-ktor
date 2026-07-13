package com.fintrack.feature.transaction.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class UpdateTransactionRequest(
    val accountId: String,
    val isIncome: Boolean,
    @Contextual val amount: BigDecimal,
    @Contextual val transactionCost: BigDecimal? = null,
    val category: String,
    val categoryId: String = "",
    val dateTime: Instant,
    val description: String,
    val externalId: String? = null,
    @Contextual val balance: BigDecimal? = null
)
