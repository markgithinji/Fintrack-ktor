package com.fintrack.feature.accounts.domain

import com.fintrack.feature.accounts.data.model.AccountDto
import com.fintrack.feature.summary.data.model.AccountAggregates
import feature.accounts.data.toDomain
import feature.accounts.data.toDto
import kotlinx.coroutines.coroutineScope

class AccountServiceImpl(
    private val accountsRepository: AccountsRepository
) : AccountService {

    override suspend fun getAccountAggregates(userId: Int, accountId: Int?): AccountAggregates {
        val transactions = accountsRepository.getTransactionAmounts(userId, accountId)
        val income = transactions.filter { it.second }.sumOf { it.first }
        val expense = transactions.filter { !it.second }.sumOf { it.first }
        val balance = income - expense

        return AccountAggregates(income, expense, balance)
    }

    override suspend fun getAllAccounts(userId: Int): List<AccountDto> =
        accountsRepository.getAllAccounts(userId).map { account ->
            val aggregates = getAccountAggregates(userId, account.id)
            account.toDto(
                income = aggregates.income,
                expense = aggregates.expense,
                balance = aggregates.balance
            )
        }

    override suspend fun getAccount(userId: Int, accountId: Int): AccountDto? {
        val account = accountsRepository.getAccountById(accountId) ?: return null
        if (account.userId != userId) return null
        val aggregates = getAccountAggregates(userId, account.id)
        return account.toDto(
            income = aggregates.income,
            expense = aggregates.expense,
            balance = aggregates.balance
        )
    }

    override suspend fun createAccount(userId: Int, request: AccountDto): AccountDto {
        val account = accountsRepository.addAccount(request.toDomain(userId))
        return account.toDto()
    }

    override suspend fun updateAccount(userId: Int, accountId: Int, request: AccountDto): AccountDto {
        val account = request.toDomain(userId).copy(id = accountId)
        val updatedAccount = accountsRepository.updateAccount(account)
        val aggregates = getAccountAggregates(userId, updatedAccount.id)
        return updatedAccount.toDto(
            income = aggregates.income,
            expense = aggregates.expense,
            balance = aggregates.balance
        )
    }

    override suspend fun deleteAccount(userId: Int, accountId: Int): Boolean {
        val account = accountsRepository.getAccountById(accountId) ?: return false
        if (account.userId != userId) return false
        accountsRepository.deleteAccount(accountId)
        return true
    }
}