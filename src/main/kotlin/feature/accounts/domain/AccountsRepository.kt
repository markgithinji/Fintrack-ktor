package com.fintrack.feature.accounts.domain

import com.fintrack.feature.accounts.domain.Account

import java.util.UUID

interface AccountsRepository {
    suspend fun getAllAccounts(userId: UUID): List<Account>
    suspend fun getAccountById(id: UUID): Account?
    suspend fun addAccount(account: Account): Account
    suspend fun updateAccount(account: Account): Account
    suspend fun deleteAccount(id: UUID)
    suspend fun getTransactionAmounts(userId: UUID, accountId: UUID?): List<Pair<Double, Boolean>>
}