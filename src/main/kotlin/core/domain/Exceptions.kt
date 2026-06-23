package core

sealed class AppException(
    override val message: String,
    val errorCode: String? = null
) : RuntimeException(message)

// Thrown when validation fails
class ValidationException(message: String, errorCode: String? = "VALIDATION_ERROR") : AppException(message, errorCode)

// Thrown when authentication fails (login/register)
class AuthenticationException(message: String, errorCode: String? = "AUTHENTICATION_FAILED") : AppException(message, errorCode)

// Thrown when a user is not authorized
class UnauthorizedAccessException(message: String, errorCode: String? = "UNAUTHORIZED_ACCESS") : AppException(message, errorCode)

// Global exceptions
class ResourceNotFoundException(message: String, errorCode: String? = "RESOURCE_NOT_FOUND") : AppException(message, errorCode)
