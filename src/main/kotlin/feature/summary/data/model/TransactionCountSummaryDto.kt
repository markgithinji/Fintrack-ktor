package com.fintrack.feature.summary.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TransactionCountSummaryDto(
    val totalIncomeTransactions: Int,
    val totalExpenseTransactions: Int,
    val totalTransactions: Int
)
