package feature.budget.domain.model

import java.math.BigDecimal

data class BudgetStatus(
    val limit: BigDecimal,
    val spent: BigDecimal,
    val remaining: BigDecimal,
    val percentageUsed: Double,
    val isExceeded: Boolean
)
