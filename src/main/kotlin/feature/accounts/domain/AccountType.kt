package com.fintrack.feature.accounts.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AccountType {
    @SerialName("general")
    GENERAL,
    @SerialName("mpesa")
    MPESA,
    @SerialName("equity")
    EQUITY
}
