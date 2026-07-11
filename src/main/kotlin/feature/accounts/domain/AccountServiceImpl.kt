package com.fintrack.feature.accounts.domain

import com.fintrack.core.domain.AppError
import com.fintrack.core.domain.Result
import com.fintrack.core.logger
import com.fintrack.core.withContext
import com.fintrack.feature.accounts.data.model.AccountDto
import com.fintrack.feature.accounts.data.model.CreateAccountRequest
import com.fintrack.feature.accounts.data.model.UpdateAccountRequest
import com.fintrack.feature.accounts.data.model.toDomain
import com.fintrack.feature.accounts.data.model.toDto
import com.fintrack.feature.accounts.domain.model.TransactionSummary
import com.fintrack.feature.accounts.domain.repository.AccountsRepository
import com.fintrack.feature.summary.data.model.AccountAggregates
import kotlinx.datetime.Clock
import java.util.UUID

class AccountServiceImpl(
    private val accountsRepository: AccountsRepository,
) : AccountService {

    private val log = logger<AccountServiceImpl>()

    override suspend fun getAccountAggregates(userId: UUID, accountId: UUID?): Result<AccountAggregates> {
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

        return Result.Success(AccountAggregates(incomeSum, expenseSum, balance))
    }

    override suspend fun getAllAccounts(userId: UUID): Result<List<AccountDto>> {
        log.withContext("userId" to userId).info { "Fetching all accounts" }

        val accounts = accountsRepository.getAllAccounts(userId)
        val summaries = accountsRepository.getTransactionSummaries(userId)

        val now = Clock.System.now()

        val result = accounts.map { account ->
            val summary = summaries[account.id] ?: TransactionSummary(0.0, 0.0)

            // If it's a linked account (M-Pesa/Equity), we might have a latest balance from sync.
            // If not, or if it's a general account, we use income - expense as the derived balance.
            // We prioritize the balance stored in the account table if it's non-zero,
            // as that might have been explicitly set during a sync.
            val derivedBalance = if (account.balance != 0.0) {
                account.balance
            } else {
                summary.income - summary.expense
            }

            account.toDto(
                id = account.id.toString(),
                income = summary.income,
                expense = summary.expense,
                balance = derivedBalance
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
        return Result.Success(sortedResult)
    }

    override suspend fun getAccount(userId: UUID, accountId: UUID): Result<AccountDto> {
        log.withContext("userId" to userId, "accountId" to accountId)
            .debug { "Fetching account" }

        val account = accountsRepository.getAccountById(accountId)
            ?: return Result.Failure(AppError.NotFound("Account not found"))

        if (account.userId != userId) {
            return Result.Failure(AppError.Unauthorized("Account does not belong to user"))
        }

        // Internal call to getAccountAggregates - we know it returns Success here
        val aggregatesResult = getAccountAggregates(userId, account.id)
        val aggregates = (aggregatesResult as Result.Success).value
        
        val accountDto = account.toDto(
            id = account.id.toString(),
            income = aggregates.income,
            expense = aggregates.expense,
            balance = aggregates.balance
        )

        log.withContext("userId" to userId, "accountId" to accountId)
            .debug { "Account retrieved successfully" }
        return Result.Success(accountDto)
    }

    override suspend fun createAccount(userId: UUID, request: CreateAccountRequest): Result<AccountDto> {
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
        return Result.Success(accountDto)
    }

    override suspend fun updateAccount(
        userId: UUID,
        accountId: UUID,
        request: UpdateAccountRequest
    ): Result<AccountDto> {
        log.withContext("userId" to userId, "accountId" to accountId)
            .info { "Updating account" }

        val existingAccount = accountsRepository.getAccountById(accountId)
            ?: return Result.Failure(AppError.NotFound("Account not found"))

        if (existingAccount.userId != userId) {
            log.withContext(
                "attemptedUserId" to userId,
                "actualUserId" to existingAccount.userId,
                "accountId" to accountId
            ).warn { "Unauthorized account update attempt" }
            return Result.Failure(AppError.Unauthorized("Account does not belong to user"))
        }

        val account = existingAccount.copy(
            name = request.name,
            type = request.type
        )
        val updatedAccount = accountsRepository.updateAccount(account)
        
        // Internal call
        val aggregatesResult = getAccountAggregates(userId, updatedAccount.id)
        val aggregates = (aggregatesResult as Result.Success).value
        
        val accountDto = updatedAccount.toDto(
            id = updatedAccount.id.toString(),
            income = aggregates.income,
            expense = aggregates.expense,
            balance = aggregates.balance
        )

        log.withContext("userId" to userId, "accountId" to accountId)
            .info { "Account updated successfully" }
        return Result.Success(accountDto)
    }

    override suspend fun deleteAccount(userId: UUID, accountId: UUID): Result<Unit> {
        log.withContext("userId" to userId, "accountId" to accountId)
            .info { "Deleting account" }

        val account = accountsRepository.getAccountById(accountId)
            ?: return Result.Failure(AppError.NotFound("Account not found"))

        if (account.userId != userId) {
            log.withContext(
                "attemptedUserId" to userId,
                "actualUserId" to account.userId,
                "accountId" to accountId
            ).warn { "Unauthorized account deletion attempt" }
            return Result.Failure(AppError.Unauthorized("Account does not belong to user"))
        }

        if (account.isDefault) {
            return Result.Failure(AppError.Validation("Cannot delete a system default account"))
        }

        accountsRepository.deleteAccount(accountId)
        log.withContext("userId" to userId, "accountId" to accountId)
            .info { "Account deleted successfully" }
        return Result.Success(Unit)
    }
}
