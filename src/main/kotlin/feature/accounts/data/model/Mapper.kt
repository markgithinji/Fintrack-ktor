package com.fintrack.feature.accounts.data.model

import com.fintrack.feature.accounts.domain.model.Account
import com.fintrack.feature.accounts.domain.model.AccountType
import java.math.BigDecimal
import java.util.UUID

fun Account.toDto(id: String, income: BigDecimal, expense: BigDecimal, balance: BigDecimal): AccountDto =
    AccountDto(
        id = id,
        name = name,
        income = income,
        expense = expense,
        balance = balance,
        isDefault = isDefault,
        type = type,
        linkedSources = linkedSources,
        createdAt = createdAt,
        lastSyncedAt = lastSyncedAt
    )

fun CreateAccountRequest.toDomain(userId: UUID): Account {
    val sources = if (linkedSources.isEmpty()) {
        when (type) {
            AccountType.MPESA -> setOf("mpesa")
            AccountType.EQUITY -> setOf("equity")
            else -> emptySet()
        }
    } else linkedSources

    return Account(
        userId = userId,
        name = name,
        type = type,
        linkedSources = sources
    )
}

fun UpdateAccountRequest.toDomain(userId: UUID, accountId: UUID): Account {
    val sources = linkedSources ?: when (type) {
        AccountType.MPESA -> setOf("mpesa")
        AccountType.EQUITY -> setOf("equity")
        else -> emptySet()
    }
    return Account(
        id = accountId,
        userId = userId,
        name = name,
        type = type,
        linkedSources = sources,
        balance = balance ?: BigDecimal.ZERO,
        lastSyncedAt = lastSyncedAt
    )
}
