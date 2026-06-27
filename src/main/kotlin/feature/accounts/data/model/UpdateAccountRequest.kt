package com.fintrack.feature.accounts.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateAccountRequest(
    val name: String
)