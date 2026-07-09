package com.fintrack.feature.accounts.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

data class Account(
    val id: UUID? = null,
    val userId: UUID,
    val name: String,
    val isDefault: Boolean = false,
    val type: AccountType = AccountType.GENERAL,
    val balance: Double = 0.0,
    val createdAt: Instant? = null
)

@Serializable
enum class AccountType {
    @SerialName("general")
    GENERAL,

    @SerialName("mpesa")
    MPESA,

    @SerialName("equity")
    EQUITY
}