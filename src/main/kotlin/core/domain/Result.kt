package com.fintrack.core.domain

import io.ktor.http.*

sealed class Result<out T> {
    data class Success<out T>(val value: T) : Result<T>()
    data class Failure(val error: AppError) : Result<Nothing>()
}

sealed class AppError(
    val message: String,
    val errorCode: String? = null
) {
    class Validation(message: String, errorCode: String? = "VALIDATION_ERROR") : AppError(message, errorCode)
    class Authentication(message: String, errorCode: String? = "AUTHENTICATION_FAILED") : AppError(message, errorCode)
    class Unauthorized(message: String, errorCode: String? = "UNAUTHORIZED_ACCESS") : AppError(message, errorCode)
    class NotFound(message: String, errorCode: String? = "RESOURCE_NOT_FOUND") : AppError(message, errorCode)
    class Conflict(message: String, errorCode: String? = "CONFLICT") : AppError(message, errorCode)
    class Forbidden(message: String, errorCode: String? = "FORBIDDEN") : AppError(message, errorCode)
    class Internal(message: String, errorCode: String? = "INTERNAL_SERVER_ERROR") : AppError(message, errorCode)
}

fun AppError.toHttpStatusCode(): HttpStatusCode = when (this) {
    is AppError.Authentication -> HttpStatusCode.Unauthorized
    is AppError.Unauthorized -> HttpStatusCode.Unauthorized
    is AppError.Forbidden -> HttpStatusCode.Forbidden
    is AppError.Validation -> HttpStatusCode.UnprocessableEntity
    is AppError.NotFound -> HttpStatusCode.NotFound
    is AppError.Conflict -> HttpStatusCode.Conflict
    is AppError.Internal -> HttpStatusCode.InternalServerError
}

fun AppError.toApiResponse(): ApiResponse.Error = ApiResponse.Error(this.message, this.errorCode)
