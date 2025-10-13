package com.fintrack.feature.transaction.data.model

import com.fintrack.feature.transactions.data.model.CreateTransactionRequest
import kotlinx.serialization.Serializable

@Serializable
data class BulkCreateTransactionRequest(
    val transactions: List<CreateTransactionRequest>
)