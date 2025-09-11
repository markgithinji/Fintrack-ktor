package feature.transactions

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual


@Serializable
data class Transaction(
    val id: Int? = null,
    val isIncome: Boolean,
    val amount: Double,
    val category: String,
    @Contextual val date: LocalDate,
    val description: String? = null
)