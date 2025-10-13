package com.fintrack.feature.budget.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BulkCreateBudgetRequest(
    val budgets: List<CreateBudgetRequest>
)