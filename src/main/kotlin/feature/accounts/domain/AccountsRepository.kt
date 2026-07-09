package com.fintrack.feature.accounts.domain

import com.fintrack.feature.accounts.domain.Account

import java.util.UUID

interface AccountsRepository {
    suspend fun getAllAccounts(userId: UUID): List<Account>
    suspend fun getAccountById(id: UUID): Account?
    suspend fun addAccount(account: Account): Account
    suspend fun addAll(accounts: List<Account>): List<Account>
    suspend fun updateAccount(account: Account): Account
    suspend fun deleteAccount(id: UUID)
    suspend fun getTransactionSummary(userId: UUID, accountId: UUID?): TransactionSummary
    suspend fun getTransactionSummaries(userId: UUID): Map<UUID?, TransactionSummary>
    suspend fun getLatestBalance(userId: UUID, accountId: UUID?): Double?
    suspend fun updateBalance(accountId: UUID, balance: Double)
}

data class TransactionSummary(val income: Double, val expense: Double)