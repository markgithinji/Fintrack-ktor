package feature.user.domain


import feature.user.data.model.CreateUserRequest
import feature.user.data.model.UpdateUserRequest
import io.ktor.server.plugins.requestvalidation.RequestValidationConfig
import io.ktor.server.plugins.requestvalidation.ValidationResult

fun RequestValidationConfig.configureUserValidation() {
    validate<CreateUserRequest> { request ->
        val violations = mutableListOf<String>()

        // Email validation
        when {
            request.email.isBlank() -> violations.add("Email cannot be blank")
            request.email.length > 100 -> violations.add("Email cannot exceed 100 characters")
            !request.email.matches(Regex("^[a-zA-Z0-9@.+_-]+$")) ->
                violations.add("Email can only contain letters, numbers, and @ . + - _ characters")

            !request.email.contains("@") -> violations.add("Email must contain @ symbol")
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

        // Email validation (if provided)
        if (request.email != null) {
            when {
                request.email.isBlank() -> violations.add("Email cannot be blank")
                request.email.length > 100 -> violations.add("Email cannot exceed 100 characters")
                !request.email.matches(Regex("^[a-zA-Z0-9@.+_-]+$")) ->
                    violations.add("Email can only contain letters, numbers, and @ . + - _ characters")

                !request.email.contains("@") -> violations.add("Email must contain @ symbol")
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
        if (request.email == null && request.password == null) {
            violations.add("At least one field (email or password) must be provided")
        }

        if (violations.isNotEmpty()) {
            ValidationResult.Invalid(violations.joinToString(", "))
        } else {
            ValidationResult.Valid
        }
    }
}