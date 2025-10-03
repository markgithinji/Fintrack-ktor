package feature.accounts.domain

import com.fintrack.feature.accounts.data.model.CreateAccountRequest
import com.fintrack.feature.accounts.data.model.UpdateAccountRequest
import io.ktor.server.plugins.requestvalidation.*

fun RequestValidationConfig.configureAccountValidation() {
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
}