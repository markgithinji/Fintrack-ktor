package com.fintrack.feature.accounts.domain

import com.fintrack.feature.accounts.data.model.AccountDto

interface AccountService {
    suspend fun getAllAccounts(userId: Int): List<AccountDto>
    suspend fun getAccount(userId: Int, accountId: Int): AccountDto?
    suspend fun createAccount(userId: Int, request: AccountDto): AccountDto
    suspend fun updateAccount(userId: Int, accountId: Int, request: AccountDto): AccountDto?
    suspend fun deleteAccount(userId: Int, accountId: Int): Boolean
}