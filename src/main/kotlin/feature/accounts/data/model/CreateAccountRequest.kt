package com.fintrack.feature.accounts.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateAccountRequest(
    val name: String
)