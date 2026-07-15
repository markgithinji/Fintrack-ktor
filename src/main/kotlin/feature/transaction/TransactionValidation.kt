package com.fintrack.feature.transaction

import com.fintrack.feature.transaction.data.model.BulkCreateTransactionRequest
import com.fintrack.feature.transaction.data.model.CreateTransactionRequest
import com.fintrack.feature.transaction.data.model.UpdateTransactionRequest
import io.ktor.server.plugins.requestvalidation.RequestValidationConfig
import io.ktor.server.plugins.requestvalidation.ValidationResult
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.math.BigDecimal
import kotlin.time.Duration.Companion.seconds

fun RequestValidationConfig.configureTransactionValidation() {
    validate<CreateTransactionRequest> { request ->
        validateTransactionFields(
            amount = request.amount,
            categoryId = request.categoryId,
            description = request.description,
            dateTime = request.dateTime,
            transactionCost = request.transactionCost,
        )
    }

    validate<UpdateTransactionRequest> { request ->
        val violations = mutableListOf<String>()
        
        if (request.accountId.isBlank()) {
            violations.add("Account ID cannot be blank")
        }
        
        val fieldResult = validateTransactionFields(
            amount = request.amount,
            categoryId = request.categoryId,
            description = request.description,
            dateTime = request.dateTime,
            transactionCost = request.transactionCost
        )
        
        if (fieldResult is ValidationResult.Invalid) {
            violations.addAll(fieldResult.reasons)
        }
        
        if (violations.isNotEmpty()) {
            ValidationResult.Invalid(violations.joinToString(", "))
        } else {
            ValidationResult.Valid
        }
    }

    validate<BulkCreateTransactionRequest> { bulkRequest ->
        validateList(bulkRequest.transactions)
    }

    validate<List<CreateTransactionRequest>> { requests ->
        validateList(requests)
    }
}

private fun validateList(requests: List<CreateTransactionRequest>): ValidationResult {
    val allViolations = requests.flatMapIndexed { index, req ->
        val result = validateTransactionFields(
            amount = req.amount,
            categoryId = req.categoryId,
            description = req.description,
            dateTime = req.dateTime,
            transactionCost = req.transactionCost,
            prefix = "Transaction #${index + 1}"
        )
        (result as? ValidationResult.Invalid)?.reasons ?: emptyList()
    }

    return if (allViolations.isNotEmpty()) {
        ValidationResult.Invalid(allViolations.joinToString(", "))
    } else {
        ValidationResult.Valid
    }
}

private fun validateTransactionFields(
    amount: BigDecimal,
    categoryId: String,
    description: String,
    dateTime: Instant,
    transactionCost: BigDecimal?,
    prefix: String? = null
): ValidationResult {
    val violations = mutableListOf<String>()
    val p = if (prefix != null) "$prefix: " else ""

    // Amount validation
    when {
        amount <= BigDecimal.ZERO -> violations.add("${p}Amount must be greater than 0")
        amount > BigDecimal.valueOf(1_000_000) -> violations.add("${p}Amount cannot exceed 1,000,000")
    }

    // Transaction cost validation
    if (transactionCost != null && transactionCost < BigDecimal.ZERO) {
        violations.add("${p}Transaction cost cannot be negative")
    }

    // Category ID validation
    if (categoryId.isBlank()) {
        violations.add("${p}Category ID cannot be blank")
    }

    // Description validation
    if (description.length > 255) {
        violations.add("${p}Description cannot exceed 255 characters")
    }

    // Date validation - allow 5s buffer for clock skew
    if (dateTime > Clock.System.now() + 5.seconds) {
        violations.add("${p}Transaction date cannot be in the future")
    }

    return if (violations.isNotEmpty()) {
        ValidationResult.Invalid(violations)
    } else {
        ValidationResult.Valid
    }
}
