package com.fintrack.feature.auth.data.model

import com.fintrack.feature.auth.domain.model.AuthValidationResponse

fun AuthValidationResponse.toDto(): AuthValidationResponseDto =
    AuthValidationResponseDto(
        isValid = isValid,
        userId = userId,
        message = message
    )
