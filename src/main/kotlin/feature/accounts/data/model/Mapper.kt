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
        type = type.toDto(),
        linkedSources = linkedSources,
        createdAt = createdAt,
        lastSyncedAt = lastSyncedAt
    )

fun CreateAccountRequest.toDomain(userId: UUID): Account {
    return Account(
        userId = userId,
        name = name,
        type = type.toDomain(),
        linkedSources = linkedSources
    )
}

fun UpdateAccountRequest.toDomain(userId: UUID, accountId: UUID): Account {
    return Account(
        id = accountId,
        userId = userId,
        name = name,
        type = type.toDomain(),
        linkedSources = linkedSources ?: emptySet(),
        balance = balance ?: BigDecimal.ZERO,
        lastSyncedAt = lastSyncedAt
    )
}

fun AccountType.toDto(): AccountTypeDto = when (this) {
    AccountType.OTHER -> AccountTypeDto.OTHER
    AccountType.MPESA -> AccountTypeDto.MPESA
    AccountType.BANK -> AccountTypeDto.BANK
    AccountType.CASH -> AccountTypeDto.CASH
    AccountType.WALLET -> AccountTypeDto.WALLET
    AccountType.SAVINGS -> AccountTypeDto.SAVINGS
}

fun AccountTypeDto.toDomain(): AccountType = when (this) {
    AccountTypeDto.OTHER -> AccountType.OTHER
    AccountTypeDto.MPESA -> AccountType.MPESA
    AccountTypeDto.BANK -> AccountType.BANK
    AccountTypeDto.CASH -> AccountType.CASH
    AccountTypeDto.WALLET -> AccountType.WALLET
    AccountTypeDto.SAVINGS -> AccountType.SAVINGS
}
