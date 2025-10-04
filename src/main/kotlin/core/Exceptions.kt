package core

// Thrown when validation fails
class ValidationException(message: String) : RuntimeException(message)

// Thrown when authentication fails (login/register)
class AuthenticationException(message: String) : RuntimeException(message)

// Thrown when a user is not authorized
class UnauthorizedAccessException(message: String) : RuntimeException(message)

// Global exceptions
class ResourceNotFoundException(message: String) : RuntimeException(message)