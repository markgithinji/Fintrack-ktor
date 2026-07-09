package com.fintrack.feature.accounts.domain

import com.fintrack.core.domain.Result
import com.fintrack.feature.accounts.data.model.AccountDto
import com.fintrack.feature.accounts.data.model.CreateAccountRequest
import com.fintrack.feature.accounts.data.model.UpdateAccountRequest
import com.fintrack.feature.summary.data.model.AccountAggregates
import java.util.UUID

interface AccountService {
    suspend fun getAccountAggregates(userId: UUID, accountId: UUID?): Result<AccountAggregates>
    suspend fun getAllAccounts(userId: UUID): Result<List<AccountDto>>
    suspend fun getAccount(userId: UUID, accountId: UUID): Result<AccountDto>
    suspend fun createAccount(userId: UUID, request: CreateAccountRequest): Result<AccountDto>
    suspend fun updateAccount(userId: UUID, accountId: UUID, request: UpdateAccountRequest): Result<AccountDto>
    suspend fun deleteAccount(userId: UUID, accountId: UUID): Result<Unit>
}
