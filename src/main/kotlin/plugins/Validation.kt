package plugins

import com.fintrack.feature.accounts.data.model.CreateAccountRequest
import com.fintrack.feature.accounts.data.model.UpdateAccountRequest
import com.fintrack.feature.auth.AuthRequest
import com.fintrack.feature.budget.data.model.CreateBudgetRequest
import com.fintrack.feature.budget.data.model.UpdateBudgetRequest
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import kotlinx.datetime.LocalDateTime

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

        validate<CreateBudgetRequest> { request ->
            val violations = mutableListOf<String>()

            when {
                request.name.isBlank() -> violations.add("Budget name cannot be blank")
                request.name.length > 100 -> violations.add("Budget name cannot exceed 100 characters")
            }

            if (request.categories.isEmpty()) {
                violations.add("Budget must have at least one category")
            }

            if (request.limit <= 0) {
                violations.add("Budget limit must be greater than 0")
            }

            if (request.startDate > request.endDate) {
                violations.add("Start date cannot be after end date")
            }

            if (violations.isNotEmpty()) {
                ValidationResult.Invalid(violations.joinToString(", "))
            } else {
                ValidationResult.Valid
            }
        }

        validate<UpdateBudgetRequest> { request ->
            val violations = mutableListOf<String>()

            when {
                request.name.isBlank() -> violations.add("Budget name cannot be blank")
                request.name.length > 100 -> violations.add("Budget name cannot exceed 100 characters")
            }

            if (request.categories.isEmpty()) {
                violations.add("Budget must have at least one category")
            }

            if (request.limit <= 0) {
                violations.add("Budget limit must be greater than 0")
            }

            if (request.startDate > request.endDate) {
                violations.add("Start date cannot be after end date")
            }

            if (violations.isNotEmpty()) {
                ValidationResult.Invalid(violations.joinToString(", "))
            } else {
                ValidationResult.Valid
            }
        }

        validate<List<CreateBudgetRequest>> { requests ->
            val allViolations = requests.flatMapIndexed { index, request ->
                val violations = mutableListOf<String>()

                when {
                    request.name.isBlank() -> violations.add("Budget #${index + 1}: name cannot be blank")
                    request.name.length > 100 -> violations.add("Budget #${index + 1}: name cannot exceed 100 characters")
                }

                if (request.categories.isEmpty()) {
                    violations.add("Budget #${index + 1}: must have at least one category")
                }

                if (request.limit <= 0) {
                    violations.add("Budget #${index + 1}: limit must be greater than 0")
                }

                if (request.startDate > request.endDate) {
                    violations.add("Budget #${index + 1}: start date cannot be after end date")
                }

                violations
            }

            if (allViolations.isNotEmpty()) {
                ValidationResult.Invalid(allViolations.joinToString(", "))
            } else {
                ValidationResult.Valid
            }
        }

        validate<Pair<LocalDateTime?, LocalDateTime?>> { (start, end) ->
            if (start != null && end != null && start > end) {
                ValidationResult.Invalid("Start date cannot be after end date")
            } else {
                ValidationResult.Valid
            }
        }
    }
}