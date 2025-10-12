package feature.budget.domain

import com.fintrack.feature.budget.data.model.CreateBudgetRequest
import com.fintrack.feature.budget.data.model.UpdateBudgetRequest
import io.ktor.server.plugins.requestvalidation.*

fun RequestValidationConfig.configureBudgetValidation() {
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
}