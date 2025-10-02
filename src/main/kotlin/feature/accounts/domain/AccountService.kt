package com.fintrack.feature.accounts.domain

import com.fintrack.feature.accounts.data.model.AccountDto
import com.fintrack.feature.accounts.data.model.CreateAccountRequest
import com.fintrack.feature.accounts.data.model.UpdateAccountRequest
import com.fintrack.feature.summary.data.model.AccountAggregates

interface AccountService {
    suspend fun getAccountAggregates(userId: Int, accountId: Int?): AccountAggregates
    suspend fun getAllAccounts(userId: Int): List<AccountDto>
    suspend fun getAccount(userId: Int, accountId: Int): AccountDto?
    suspend fun createAccount(userId: Int, request: CreateAccountRequest): AccountDto
    suspend fun updateAccount(userId: Int, accountId: Int, request: UpdateAccountRequest): AccountDto
    suspend fun deleteAccount(userId: Int, accountId: Int): Boolean
}