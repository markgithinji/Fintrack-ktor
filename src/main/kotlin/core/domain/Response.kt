package com.fintrack.core.domain

import kotlinx.serialization.Serializable

@Serializable
sealed class ApiResponse<out T> {
    @Serializable
    data class Success<T>(val result: T) : ApiResponse<T>()

    @Serializable
    data class Error(val error: String) : ApiResponse<Nothing>()
}
