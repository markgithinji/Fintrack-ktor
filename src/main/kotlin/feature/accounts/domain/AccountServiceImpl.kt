package com.fintrack.feature.accounts.domain

import com.fintrack.core.debug
import com.fintrack.core.info
import com.fintrack.core.logger
import com.fintrack.core.warn
import com.fintrack.core.withContext
import com.fintrack.feature.accounts.data.model.AccountDto
import com.fintrack.feature.accounts.data.model.CreateAccountRequest
import com.fintrack.feature.accounts.data.model.UpdateAccountRequest
import com.fintrack.feature.summary.data.model.AccountAggregates
import core.UnauthorizedAccessException
import feature.accounts.data.toDomain
import feature.accounts.data.toDto

class AccountServiceImpl(
    private val accountsRepository: AccountsRepository
) : AccountService {

    private val log = logger<AccountServiceImpl>()

    override suspend fun getAccountAggregates(userId: Int, accountId: Int?): AccountAggregates {
        log.withContext("userId" to userId, "accountId" to accountId)
            .debug { "Calculating account aggregates" }

        val transactions = accountsRepository.getTransactionAmounts(userId, accountId)
        val income = transactions.filter { it.second }.sumOf { it.first }
        val expense = transactions.filter { !it.second }.sumOf { it.first }
        val balance = income - expense

        log.withContext(
            "userId" to userId,
            "accountId" to accountId,
            "income" to income,
            "expense" to expense,
            "balance" to balance
        ).debug { "Account aggregates calculated" }

        return AccountAggregates(income, expense, balance)
    }

    override suspend fun getAllAccounts(userId: Int): List<AccountDto> {
        log.withContext("userId" to userId).info { "Fetching all accounts" }

        val accounts = accountsRepository.getAllAccounts(userId)
        val result = accounts.map { account ->
            val aggregates = getAccountAggregates(userId, account.id)
            account.toDto(
                income = aggregates.income,
                expense = aggregates.expense,
                balance = aggregates.balance
            )
        }

        log.withContext("userId" to userId, "accountCount" to result.size)
            .debug { "All accounts retrieved successfully" }
        return result
    }

    override suspend fun getAccount(userId: Int, accountId: Int): AccountDto? {
        log.withContext("userId" to userId, "accountId" to accountId)
            .debug { "Fetching account" }

        val account = accountsRepository.getAccountById(accountId) ?: return null
        if (account.userId != userId) return null

        val aggregates = getAccountAggregates(userId, account.id)
        val accountDto = account.toDto(
            income = aggregates.income,
            expense = aggregates.expense,
            balance = aggregates.balance
        )

        log.withContext("userId" to userId, "accountId" to accountId)
            .debug { "Account retrieved successfully" }
        return accountDto
    }

    override suspend fun createAccount(userId: Int, request: CreateAccountRequest): AccountDto {
        log.withContext("userId" to userId, "accountName" to request.name)
            .info { "Creating account" }

        val account = request.toDomain(userId)
        val createdAccount = accountsRepository.addAccount(account)

        // New account has zero aggregates
        val accountDto = createdAccount.toDto(
            income = 0.0,
            expense = 0.0,
            balance = 0.0
        )

        log.withContext("userId" to userId, "accountId" to createdAccount.id)
            .info { "Account created successfully" }
        return accountDto
    }

    override suspend fun updateAccount(userId: Int, accountId: Int, request: UpdateAccountRequest): AccountDto {
        log.withContext("userId" to userId, "accountId" to accountId)
            .info { "Updating account" }

        val existingAccount = accountsRepository.getAccountById(accountId)
            ?: throw NoSuchElementException("Account not found")

        if (existingAccount.userId != userId) {
            log.withContext(
                "attemptedUserId" to userId,
                "actualUserId" to existingAccount.userId,
                "accountId" to accountId
            ).warn { "Unauthorized account update attempt" }
            throw UnauthorizedAccessException("Account does not belong to user")
        }

        val account = request.toDomain(userId, accountId)
        val updatedAccount = accountsRepository.updateAccount(account)
        val aggregates = getAccountAggregates(userId, updatedAccount.id)
        val accountDto = updatedAccount.toDto(
            income = aggregates.income,
            expense = aggregates.expense,
            balance = aggregates.balance
        )

        log.withContext("userId" to userId, "accountId" to accountId)
            .info { "Account updated successfully" }
        return accountDto
    }

    override suspend fun deleteAccount(userId: Int, accountId: Int): Boolean {
        log.withContext("userId" to userId, "accountId" to accountId)
            .info { "Deleting account" }

        val account = accountsRepository.getAccountById(accountId) ?: return false
        if (account.userId != userId) {
            log.withContext(
                "attemptedUserId" to userId,
                "actualUserId" to account.userId,
                "accountId" to accountId
            ).warn { "Unauthorized account deletion attempt" }
            return false
        }

        accountsRepository.deleteAccount(accountId)
        log.withContext("userId" to userId, "accountId" to accountId)
            .info { "Account deleted successfully" }
        return true
    }
}