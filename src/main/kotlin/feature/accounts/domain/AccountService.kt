package com.fintrack.feature.accounts.domain

import com.fintrack.feature.accounts.data.model.AccountDto
import com.fintrack.feature.accounts.data.model.CreateAccountRequest
import com.fintrack.feature.accounts.data.model.UpdateAccountRequest
import com.fintrack.feature.summary.data.model.AccountAggregates

import java.util.UUID

interface AccountService {
    suspend fun getAccountAggregates(userId: UUID, accountId: UUID?): AccountAggregates
    suspend fun getAllAccounts(userId: UUID): List<AccountDto>
    suspend fun getAccount(userId: UUID, accountId: UUID): AccountDto?
    suspend fun createAccount(userId: UUID, request: CreateAccountRequest): AccountDto
    suspend fun updateAccount(userId: UUID, accountId: UUID, request: UpdateAccountRequest): AccountDto
    suspend fun deleteAccount(userId: UUID, accountId: UUID): Boolean
}