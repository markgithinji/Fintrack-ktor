package com.fintrack.feature.accounts.domain

import com.fintrack.core.logger
import com.fintrack.core.withContext
import com.fintrack.feature.accounts.data.model.AccountDto
import com.fintrack.feature.accounts.data.model.CreateAccountRequest
import com.fintrack.feature.accounts.data.model.UpdateAccountRequest
import com.fintrack.feature.summary.data.model.AccountAggregates
import core.UnauthorizedAccessException
import core.ValidationException
import feature.accounts.data.model.toDomain
import feature.accounts.data.model.toDto
import kotlinx.datetime.Clock
import java.util.UUID

class AccountServiceImpl(
    private val accountsRepository: AccountsRepository
) : AccountService {

    private val log = logger<AccountServiceImpl>()

    override suspend fun getAccountAggregates(userId: UUID, accountId: UUID?): AccountAggregates {
        log.withContext("userId" to userId, "accountId" to accountId)
            .debug { "Calculating account aggregates" }

        val summary = accountsRepository.getTransactionSummary(userId, accountId)
        val incomeSum = summary.income
        val expenseSum = summary.expense

        // Use the latest balance from transactions if available (e.g., for M-Pesa or Equity),
        // otherwise fall back to the calculated balance.
        val latestBalance = accountsRepository.getLatestBalance(userId, accountId)
        val balance = latestBalance ?: (incomeSum - expenseSum)

        log.withContext(
            "userId" to userId,
            "accountId" to accountId,
            "income" to incomeSum,
            "expense" to expenseSum,
            "latestBalance" to latestBalance,
            "finalBalance" to balance
        ).debug { "Account aggregates calculated" }

        return AccountAggregates(incomeSum, expenseSum, balance)
    }

    override suspend fun getAllAccounts(userId: UUID): List<AccountDto> {
        log.withContext("userId" to userId).info { "Fetching all accounts" }

        val accounts = accountsRepository.getAllAccounts(userId)
        val summaries = accountsRepository.getTransactionSummaries(userId)
        
        val now = Clock.System.now()

        val result = accounts.map { account ->
            val summary = summaries[account.id] ?: TransactionSummary(0.0, 0.0)
            account.toDto(
                id = account.id.toString(),
                income = summary.income,
                expense = summary.expense,
                balance = account.balance
            )
        }

        // Sort by creation date (ASC) so default accounts (with early timestamps) appear first
        val sortedResult = result.sortedWith { a, b ->
            val timeA = a.createdAt ?: now
            val timeB = b.createdAt ?: now
            
            if (timeA != timeB) {
                timeA.compareTo(timeB)
            } else {
                a.name.compareTo(b.name, ignoreCase = true)
            }
        }

        log.withContext("userId" to userId, "accountCount" to sortedResult.size)
            .debug { "All accounts retrieved and sorted successfully" }
        return sortedResult
    }

    override suspend fun getAccount(userId: UUID, accountId: UUID): AccountDto {
        log.withContext("userId" to userId, "accountId" to accountId)
            .debug { "Fetching account" }

        val account = accountsRepository.getAccountById(accountId)
            ?: throw NoSuchElementException("Account not found")

        if (account.userId != userId) {
            throw UnauthorizedAccessException("Account does not belong to user")
        }

        val aggregates = getAccountAggregates(userId, account.id)
        val accountDto = account.toDto(
            id = account.id.toString(),
            income = aggregates.income,
            expense = aggregates.expense,
            balance = aggregates.balance
        )

        log.withContext("userId" to userId, "accountId" to accountId)
            .debug { "Account retrieved successfully" }
        return accountDto
    }

    override suspend fun createAccount(userId: UUID, request: CreateAccountRequest): AccountDto {
        log.withContext("userId" to userId, "accountName" to request.name)
            .info { "Creating account" }

        val account = request.toDomain(userId)
        val createdAccount = accountsRepository.addAccount(account)

        // New account has zero aggregates
        val accountDto = createdAccount.toDto(
            id = createdAccount.id.toString(),
            income = 0.0,
            expense = 0.0,
            balance = 0.0
        )

        log.withContext("userId" to userId, "accountId" to createdAccount.id)
            .info { "Account created successfully" }
        return accountDto
    }

    override suspend fun updateAccount(
        userId: UUID,
        accountId: UUID,
        request: UpdateAccountRequest
    ): AccountDto {
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

        val account = existingAccount.copy(
            name = request.name,
            type = request.type
        )
        val updatedAccount = accountsRepository.updateAccount(account)
        val aggregates = getAccountAggregates(userId, updatedAccount.id)
        val accountDto = updatedAccount.toDto(
            id = updatedAccount.id.toString(),
            income = aggregates.income,
            expense = aggregates.expense,
            balance = aggregates.balance
        )

        log.withContext("userId" to userId, "accountId" to accountId)
            .info { "Account updated successfully" }
        return accountDto
    }

    override suspend fun deleteAccount(userId: UUID, accountId: UUID): Boolean {
        log.withContext("userId" to userId, "accountId" to accountId)
            .info { "Deleting account" }

        val account = accountsRepository.getAccountById(accountId)
            ?: throw NoSuchElementException("Account not found")

        if (account.userId != userId) {
            log.withContext(
                "attemptedUserId" to userId,
                "actualUserId" to account.userId,
                "accountId" to accountId
            ).warn { "Unauthorized account deletion attempt" }
            throw UnauthorizedAccessException("Account does not belong to user")
        }

        if (account.isDefault) {
            throw ValidationException("Cannot delete a system default account")
        }

        accountsRepository.deleteAccount(accountId)
        log.withContext("userId" to userId, "accountId" to accountId)
            .info { "Account deleted successfully" }
        return true
    }
}
