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
        log.debug { "Calculating aggregates for user $userId, account: $accountId" }

        val transactions = accountsRepository.getTransactionAmounts(userId, accountId)
        val income = transactions.filter { it.second }.sumOf { it.first }
        val expense = transactions.filter { !it.second }.sumOf { it.first }
        val balance = income - expense

        log.debug { "Aggregates calculated - Income: $income, Expense: $expense, Balance: $balance" }
        return AccountAggregates(income, expense, balance)
    }

    override suspend fun getAllAccounts(userId: Int): List<AccountDto> {
        log.withContext("userId" to userId).info{ "Fetching all accounts" }

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
            .debug{ "Accounts retrieved successfully" }
        return result
    }

    override suspend fun getAccount(userId: Int, accountId: Int): AccountDto? {
        log.withContext("userId" to userId, "accountId" to accountId)
            .debug{ "Fetching account" }

        val account = accountsRepository.getAccountById(accountId) ?: return null
        if (account.userId != userId) return null

        val aggregates = getAccountAggregates(userId, account.id)
        log.debug { "Account $accountId retrieved for user $userId" }
        return account.toDto(
            income = aggregates.income,
            expense = aggregates.expense,
            balance = aggregates.balance
        )
    }

    override suspend fun createAccount(userId: Int, request: CreateAccountRequest): AccountDto {
        log.withContext("userId" to userId, "accountName" to request.name)
            .info{ "Creating account" }

        val account = accountsRepository.addAccount(request.toDomain(userId))

        log.withContext("userId" to userId, "accountId" to account.id)
            .info{ "Account created successfully" }
        return account.toDto()
    }

    override suspend fun updateAccount(userId: Int, accountId: Int, request: UpdateAccountRequest): AccountDto {
        log.withContext("userId" to userId, "accountId" to accountId, "accountName" to request.name)
            .info{ "Updating account" }

        val existingAccount = accountsRepository.getAccountById(accountId)
            ?: throw NoSuchElementException("Account not found")

        if (existingAccount.userId != userId) {
            log.withContext(
                "attemptedUserId" to userId,
                "actualUserId" to existingAccount.userId,
                "accountId" to accountId
            ).warn{ "Unauthorized account update attempt" }
            throw UnauthorizedAccessException("Account does not belong to user")
        }

        val account = request.toDomain(userId, accountId)
        val updatedAccount = accountsRepository.updateAccount(account)
        val aggregates = getAccountAggregates(userId, updatedAccount.id)

        log.info { "Account $accountId updated successfully" }
        return updatedAccount.toDto(
            income = aggregates.income,
            expense = aggregates.expense,
            balance = aggregates.balance
        )
    }

    override suspend fun deleteAccount(userId: Int, accountId: Int): Boolean {
        log.withContext("userId" to userId, "accountId" to accountId)
            .info{ "Deleting account" }

        val account = accountsRepository.getAccountById(accountId) ?: return false
        if (account.userId != userId) {
            log.withContext(
                "attemptedUserId" to userId,
                "actualUserId" to account.userId,
                "accountId" to accountId
            ).warn{ "Unauthorized account deletion attempt" }
            return false
        }

        accountsRepository.deleteAccount(accountId)
        log.info { "Account $accountId deleted successfully" }
        return true
    }
}