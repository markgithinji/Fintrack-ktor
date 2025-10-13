package feature.transaction.domain


import com.fintrack.feature.transaction.data.model.BulkCreateTransactionRequest
import com.fintrack.feature.transactions.data.model.CreateTransactionRequest
import com.fintrack.feature.transactions.data.model.UpdateTransactionRequest
import io.ktor.server.plugins.requestvalidation.RequestValidationConfig
import io.ktor.server.plugins.requestvalidation.ValidationResult
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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

        // Date validation - prevent future dates
//        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
//        if (request.dateTime > now) {
//            violations.add("Transaction date cannot be in the future")
//        }

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

        // Date validation - prevent future dates
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

    validate<BulkCreateTransactionRequest> { bulkRequest ->
        val allViolations = bulkRequest.transactions.flatMapIndexed { index, request ->
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

            // Date validation - prevent future dates
//            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
//            if (request.dateTime > now) {
//                violations.add("Transaction #${index + 1}: date cannot be in the future")
//            }

            violations
        }

        if (allViolations.isNotEmpty()) {
            ValidationResult.Invalid(allViolations.joinToString(", "))
        } else {
            ValidationResult.Valid
        }
    }
}