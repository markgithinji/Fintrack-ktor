package feature.accounts.domain

import kotlinx.serialization.Serializable


data class Account(
    val id: Int,
    val userId: Int,
    val name: String
)