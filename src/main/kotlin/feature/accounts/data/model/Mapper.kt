package com.fintrack.feature.accounts.data.model

import com.fintrack.feature.accounts.domain.model.Account
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
        createdAt = createdAt,
        lastSyncedAt = lastSyncedAt
    )

fun CreateAccountRequest.toDomain(userId: UUID): Account = Account(
    userId = userId,
    name = name,
    type = type
)

fun UpdateAccountRequest.toDomain(userId: UUID, accountId: UUID): Account = Account(
    id = accountId,
    userId = userId,
    name = name,
    type = type
)
