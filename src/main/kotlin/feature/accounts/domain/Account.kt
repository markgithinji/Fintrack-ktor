package com.fintrack.feature.accounts.domain

import java.util.UUID

data class Account(
    val id: UUID? = null,
    val userId: UUID,
    val name: String
)