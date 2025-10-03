package feature.auth.domain

import com.fintrack.feature.auth.AuthRequest
import io.ktor.server.plugins.requestvalidation.*

fun RequestValidationConfig.configureAuthValidation() {
    validate<AuthRequest> { request ->
        val violations = mutableListOf<String>()

        // Email validation
        when {
            request.email.isBlank() -> violations.add("Email cannot be blank")
            request.email.length > 100 -> violations.add("Email cannot exceed 100 characters")
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
}