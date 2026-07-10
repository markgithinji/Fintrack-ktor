package com.fintrack.feature.auth.domain.model

import java.util.UUID

data class AuthValidationResponse(
    val isValid: Boolean,
    val userId: UUID?,
    val message: String
)
