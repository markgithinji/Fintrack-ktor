package feature.user.domain


import feature.user.data.model.CreateUserRequest
import feature.user.data.model.UpdateUserRequest
import io.ktor.server.plugins.requestvalidation.*

fun RequestValidationConfig.configureUserValidation() {
    validate<CreateUserRequest> { request ->
        val violations = mutableListOf<String>()

        // Username validation
        when {
            request.username.isBlank() -> violations.add("Username cannot be blank")
            request.username.length > 100 -> violations.add("Username cannot exceed 100 characters")
            !request.username.matches(Regex("^[a-zA-Z0-9@.+_-]+$")) ->
                violations.add("Username can only contain letters, numbers, and @ . + - _ characters")
        }

        // Password validation
        when {
            request.password.isBlank() -> violations.add("Password cannot be blank")
            request.password.length < 6 -> violations.add("Password must be at least 6 characters long")
            request.password.length > 100 -> violations.add("Password cannot exceed 100 characters")
        }

        if (violations.isNotEmpty()) {
            ValidationResult.Invalid(violations.joinToString(", "))
        } else {
            ValidationResult.Valid
        }
    }

    validate<UpdateUserRequest> { request ->
        val violations = mutableListOf<String>()

        // Username validation (if provided)
        if (request.username != null) {
            when {
                request.username.isBlank() -> violations.add("Username cannot be blank")
                request.username.length > 100 -> violations.add("Username cannot exceed 100 characters")
                !request.username.matches(Regex("^[a-zA-Z0-9@.+_-]+$")) ->
                    violations.add("Username can only contain letters, numbers, and @ . + - _ characters")
            }
        }

        // Password validation (if provided)
        if (request.password != null) {
            when {
                request.password.isBlank() -> violations.add("Password cannot be blank")
                request.password.length < 6 -> violations.add("Password must be at least 6 characters long")
                request.password.length > 100 -> violations.add("Password cannot exceed 100 characters")
            }
        }

        // At least one field should be provided
        if (request.username == null && request.password == null) {
            violations.add("At least one field (username or password) must be provided")
        }

        if (violations.isNotEmpty()) {
            ValidationResult.Invalid(violations.joinToString(", "))
        } else {
            ValidationResult.Valid
        }
    }
}