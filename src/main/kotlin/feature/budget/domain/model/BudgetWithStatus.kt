package feature.budget.domain.model

import feature.budget.domain.model.Budget

data class BudgetWithStatus(
    val budget: Budget,
    val status: BudgetStatus
)