package feature.transaction.domain


import com.fintrack.feature.transactions.data.model.CreateTransactionRequest
import com.fintrack.feature.transactions.data.model.UpdateTransactionRequest
import io.ktor.server.plugins.requestvalidation.*
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone

fun RequestValidationConfig.configureTransactionValidation() {
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

        // Account ID validation
        if (request.accountId.isBlank()) {
            violations.add("Account ID cannot be blank")
        }

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