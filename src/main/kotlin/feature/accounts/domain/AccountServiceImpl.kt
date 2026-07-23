package com.fintrack.feature.accounts.domain

import com.fintrack.core.domain.AppError
import com.fintrack.core.domain.Result
import com.fintrack.core.domain.getOrNull
import com.fintrack.core.logger
import com.fintrack.core.withContext
import com.fintrack.feature.accounts.data.model.AccountDto
import com.fintrack.feature.accounts.data.model.CreateAccountRequest
import com.fintrack.feature.accounts.data.model.UpdateAccountRequest
import com.fintrack.feature.accounts.data.model.toDomain
import com.fintrack.feature.accounts.data.model.toDto
import com.fintrack.feature.accounts.domain.model.TransactionSummary
import com.fintrack.feature.accounts.domain.model.AccountType
import com.fintrack.feature.accounts.domain.repository.AccountsRepository
import feature.transaction.domain.TransactionRepository
import feature.budget.domain.BudgetRepository
import com.fintrack.feature.summary.data.model.AccountAggregates
import kotlinx.datetime.Clock
import java.math.BigDecimal
import java.util.UUID

class AccountServiceImpl(
    private val accountsRepository: AccountsRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository
) : AccountService {

    private val log = logger<AccountServiceImpl>()

    override suspend fun getAccountAggregates(userId: UUID, accountId: UUID?): Result<AccountAggregates> {
        log.withContext("userId" to userId, "accountId" to accountId)
            .debug { "Calculating account aggregates" }

        val (incomeSum, expenseSum, balance) = if (accountId != null) {
            val account = accountsRepository.getAccountById(accountId)
                ?: return Result.Failure(AppError.NotFound("Account not found"))

            val summary = accountsRepository.getTransactionSummary(userId, accountId)
            val latestBalance = accountsRepository.getLatestBalance(userId, accountId)

            Triple(summary.income, summary.expense, deriveBalance(account, summary, latestBalance))
        } else {
            // Aggregate across all accounts
            val accounts = when (val res = getAllAccounts(userId)) {
                is Result.Success -> res.value
                is Result.Failure -> return Result.Failure(res.error)
            }

            val totalIncome = accounts.fold(BigDecimal.ZERO) { acc, a -> acc + (a.income ?: BigDecimal.ZERO) }
            val totalExpense = accounts.fold(BigDecimal.ZERO) { acc, a -> acc + (a.expense ?: BigDecimal.ZERO) }
            val totalBalance = accounts.fold(BigDecimal.ZERO) { acc, a -> acc + (a.balance ?: BigDecimal.ZERO) }
            Triple(totalIncome, totalExpense, totalBalance)
        }

        log.withContext(
            "userId" to userId,
            "accountId" to accountId,
            "income" to incomeSum,
            "expense" to expenseSum,
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
            val summary = summaries[account.id] ?: TransactionSummary(BigDecimal.ZERO, BigDecimal.ZERO)
            val latestTransactionBalance = accountsRepository.getLatestBalance(userId, account.id)

            account.toDto(
                id = account.id.toString(),
                income = summary.income,
                expense = summary.expense,
                balance = deriveBalance(account, summary, latestTransactionBalance)
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

        // Internal call to getAccountAggregates; we know it returns Success here
        val aggregates = getAccountAggregates(userId, account.id).getOrNull() 
            ?: return Result.Failure(AppError.Internal("Failed to calculate aggregates"))
        
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
            income = BigDecimal.ZERO,
            expense = BigDecimal.ZERO,
            balance = BigDecimal.ZERO
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

        if (existingAccount.isDefault && existingAccount.name != request.name) {
            return Result.Failure(AppError.Validation("Cannot rename a system default account"))
        }

        val account = existingAccount.copy(
            name = request.name,
            type = request.type.toDomain(),
            linkedSources = request.linkedSources ?: existingAccount.linkedSources,
            balance = request.balance ?: existingAccount.balance,
            lastSyncedAt = request.lastSyncedAt ?: existingAccount.lastSyncedAt
        )
        val updatedAccount = accountsRepository.updateAccount(account)
        
        // Internal call
        val aggregates = getAccountAggregates(userId, updatedAccount.id).getOrNull()
            ?: return Result.Failure(AppError.Internal("Failed to calculate aggregates"))
        
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
            .info { "Deleting account and cleaning up associated data" }

        val accountToDelete = accountsRepository.getAccountById(accountId)
            ?: return Result.Failure(AppError.NotFound("Account not found"))

        if (accountToDelete.userId != userId) {
            log.withContext(
                "attemptedUserId" to userId,
                "actualUserId" to accountToDelete.userId,
                "accountId" to accountId
            ).warn { "Unauthorized account deletion attempt" }
            return Result.Failure(AppError.Unauthorized("Account does not belong to user"))
        }

        if (accountToDelete.isDefault) {
            return Result.Failure(AppError.Validation("Cannot delete a system default account"))
        }

        // 1. Delete all transactions associated with this account
        transactionRepository.clearAll(userId, listOf(accountId))
        log.withContext("userId" to userId, "accountId" to accountId).debug { "Deleted transactions for account" }

        // 2. Handle Budgets
        val userAccounts = accountsRepository.getAllAccounts(userId)
        val fallbackAccount = userAccounts.find { it.type == AccountType.MPESA && it.id != accountId }
            ?: userAccounts.find { it.id != accountId && it.isDefault }
            ?: userAccounts.find { it.id != accountId }
        
        val userBudgets = budgetRepository.getAllByUser(userId, null, 1000, 0)
        userBudgets.forEach { budget ->
            if (budget.accountIds.contains(accountId)) {
                val updatedAccountIds = budget.accountIds.filter { it != accountId }.toMutableList()
                
                if (updatedAccountIds.isEmpty()) {
                    // If it was the only account, reassign to fallback account if available
                    fallbackAccount?.id?.let { updatedAccountIds.add(it) }
                }

                if (updatedAccountIds.isNotEmpty()) {
                    budgetRepository.update(userId, budget.id!!, budget.copy(accountIds = updatedAccountIds))
                } else {
                    budgetRepository.delete(userId, budget.id!!)
                }
            }
        }

        // 3. Delete the account itself
        accountsRepository.deleteAccount(accountId)
        
        log.withContext("userId" to userId, "accountId" to accountId)
            .info { "Account deleted successfully along with associated data" }
        return Result.Success(Unit)
    }

    private fun deriveBalance(
        account: com.fintrack.feature.accounts.domain.model.Account,
        summary: TransactionSummary,
        latestBalance: BigDecimal?
    ): BigDecimal {
        // Prioritize: 1. Latest transaction balance, 2. Stored account balance, 3. Calculated balance
        return latestBalance ?: if (account.balance != BigDecimal.ZERO || account.lastSyncedAt != null) {
            account.balance
        } else if (summary.income != BigDecimal.ZERO || summary.expense != BigDecimal.ZERO) {
            summary.income - summary.expense
        } else {
            account.balance
        }
    }
}
