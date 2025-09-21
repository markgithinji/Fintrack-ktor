package feature.accounts.data

import kotlinx.serialization.Serializable

@Serializable
data class AccountDto(
    val id: Int? = null,
    val name: String,
    val balance: Double? = null,
    val income: Double? = null,
    val expense: Double? = null,
)
