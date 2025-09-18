package feature.transactions

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual


data class Transaction(
    val id: Int? = null,
    val userId: Int,
    val accountId: Int,
    val isIncome: Boolean,
    val amount: Double,
    val category: String,
    val dateTime: LocalDateTime,
    val description: String? = null
)
