package com.fintrack.feature.transactions.data.model

import com.fintrack.core.UUIDSerializer
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import java.util.UUID

data class UpdateTransactionRequest(
    val accountId: String,
    val isIncome: Boolean,
    val amount: Double,
    val category: String,
    val dateTime: LocalDateTime,
    val description: String
)