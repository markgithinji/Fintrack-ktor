package plugins

import com.fintrack.feature.accounts.data.model.CreateAccountRequest
import com.fintrack.feature.accounts.data.model.UpdateAccountRequest
import com.fintrack.feature.auth.AuthRequest
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*

fun Application.configureValidation() {
    install(RequestValidation) {
        validate<CreateAccountRequest> { request ->
            when {
                request.name.isBlank() -> ValidationResult.Invalid("Account name cannot be blank")
                request.name.length > 50 -> ValidationResult.Invalid("Account name cannot exceed 50 characters")
                !request.name.matches(Regex("^[a-zA-Z0-9\\s]+$")) ->
                    ValidationResult.Invalid("Account name can only contain letters, numbers, and spaces")
                else -> ValidationResult.Valid
            }
        }

        validate<UpdateAccountRequest> { request ->
            when {
                request.name.isBlank() -> ValidationResult.Invalid("Account name cannot be blank")
                request.name.length > 50 -> ValidationResult.Invalid("Account name cannot exceed 50 characters")
                !request.name.matches(Regex("^[a-zA-Z0-9\\s]+$")) ->
                    ValidationResult.Invalid("Account name can only contain letters, numbers, and spaces")
                else -> ValidationResult.Valid
            }
        }

        validate<AuthRequest> { request ->
            val violations = mutableListOf<String>()

            // Email validation
            when {
                request.email.isBlank() -> violations.add("Email cannot be blank")
//                !request.email.contains("@") -> violations.add("Email must be valid")
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
}