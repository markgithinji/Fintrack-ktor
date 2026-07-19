package com.fintrack.feature.accounts.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateAccountRequest(
    val name: String,
    val type: AccountTypeDto = AccountTypeDto.OTHER,
    val linkedSources: Set<String> = emptySet()
)
