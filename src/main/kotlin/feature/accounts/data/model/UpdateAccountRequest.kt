package com.fintrack.feature.accounts.data.model

import com.fintrack.feature.accounts.domain.AccountType
import kotlinx.serialization.Serializable

@Serializable
data class UpdateAccountRequest(
    val name: String,
    val type: AccountType = AccountType.GENERAL
)