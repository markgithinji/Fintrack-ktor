package feature.budget.domain.model

import kotlinx.datetime.LocalDate
import java.math.BigDecimal
import java.util.*

data class Budget(
    val id: UUID? = null,
    val accountIds: List<UUID>,
    val name: String,
    val categoryIds: List<UUID>,
    val limit: BigDecimal,
    val isExpense: Boolean,
    val startDate: LocalDate,
    val endDate: LocalDate
)
