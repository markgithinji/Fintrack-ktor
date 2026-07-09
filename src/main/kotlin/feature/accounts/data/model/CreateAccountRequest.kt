package com.fintrack.feature.accounts.data.model

import com.fintrack.feature.accounts.domain.model.AccountType
import kotlinx.serialization.Serializable

@Serializable
data class CreateAccountRequest(
    val name: String,
    val type: AccountType = AccountType.GENERAL
)