package com.fintrack.core.domain

import kotlinx.serialization.Serializable

@Serializable
sealed class ApiResponse<out T> {
    @Serializable
    data class Success<T>(val result: T) : ApiResponse<T>()

    @Serializable
    data class Error(val error: String, val errorCode: String? = null) : ApiResponse<Nothing>()
}

@Serializable
data class ErrorResponse(
    val message: String,
    val errorCode: String? = null
)
