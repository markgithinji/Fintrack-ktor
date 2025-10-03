package plugins
import com.fintrack.feature.accounts.data.model.CreateAccountRequest
import com.fintrack.feature.accounts.data.model.UpdateAccountRequest
import com.fintrack.feature.auth.AuthRequest
import com.fintrack.feature.budget.data.model.CreateBudgetRequest
import com.fintrack.feature.budget.data.model.UpdateBudgetRequest
import com.fintrack.feature.transactions.data.model.CreateTransactionRequest
import com.fintrack.feature.transactions.data.model.UpdateTransactionRequest
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone

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

        validate<CreateTransactionRequest> { request ->
            val violations = mutableListOf<String>()

            // Amount validation
            when {
                request.amount <= 0 -> violations.add("Amount must be greater than 0")
                request.amount > 1_000_000 -> violations.add("Amount cannot exceed 1,000,000")
            }

            // Category validation
            when {
                request.category.isBlank() -> violations.add("Category cannot be blank")
                request.category.length > 50 -> violations.add("Category cannot exceed 50 characters")
            }

            // Description validation
            if (request.description.length > 255) {
                violations.add("Description cannot exceed 255 characters")
            }

            // Date validation - prevent future dates (simplified)
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            if (request.dateTime > now) {
                violations.add("Transaction date cannot be in the future")
            }

            if (violations.isNotEmpty()) {
                ValidationResult.Invalid(violations.joinToString(", "))
            } else {
                ValidationResult.Valid
            }
        }

        validate<UpdateTransactionRequest> { request ->
            val violations = mutableListOf<String>()

            // Amount validation
            when {
                request.amount <= 0 -> violations.add("Amount must be greater than 0")
                request.amount > 1_000_000 -> violations.add("Amount cannot exceed 1,000,000")
            }

            // Category validation
            when {
                request.category.isBlank() -> violations.add("Category cannot be blank")
                request.category.length > 50 -> violations.add("Category cannot exceed 50 characters")
            }

            // Description validation
            if (request.description.length > 255) {
                violations.add("Description cannot exceed 255 characters")
            }

            // Date validation - prevent future dates (simplified)
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            if (request.dateTime > now) {
                violations.add("Transaction date cannot be in the future")
            }

            if (violations.isNotEmpty()) {
                ValidationResult.Invalid(violations.joinToString(", "))
            } else {
                ValidationResult.Valid
            }
        }

        validate<List<CreateTransactionRequest>> { requests ->
            val allViolations = requests.flatMapIndexed { index, request ->
                val violations = mutableListOf<String>()

                // Amount validation
                when {
                    request.amount <= 0 -> violations.add("Transaction #${index + 1}: amount must be greater than 0")
                    request.amount > 1_000_000 -> violations.add("Transaction #${index + 1}: amount cannot exceed 1,000,000")
                }

                // Category validation
                when {
                    request.category.isBlank() -> violations.add("Transaction #${index + 1}: category cannot be blank")
                    request.category.length > 50 -> violations.add("Transaction #${index + 1}: category cannot exceed 50 characters")
                }

                // Description validation
                if (request.description.length > 255) {
                    violations.add("Transaction #${index + 1}: description cannot exceed 255 characters")
                }

                violations
            }

            if (allViolations.isNotEmpty()) {
                ValidationResult.Invalid(allViolations.joinToString(", "))
            } else {
                ValidationResult.Valid
            }
        }
    }
}