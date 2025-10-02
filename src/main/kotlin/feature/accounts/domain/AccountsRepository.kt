package com.fintrack.feature.accounts.domain

import feature.accounts.domain.Account

interface AccountsRepository {
    suspend fun getTransactionAmounts(userId: Int, accountId: Int?): List<Pair<Double, Boolean>>
    suspend fun getAllAccounts(userId: Int): List<Account>
    suspend fun getAccountById(id: Int): Account?
    suspend fun addAccount(account: Account): Account
    suspend fun updateAccount(account: Account): Account
    suspend fun deleteAccount(id: Int)
}
