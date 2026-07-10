package com.fintrack.feature.transaction.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BulkCreateTransactionRequest(
    val transactions: List<CreateTransactionRequest>
)