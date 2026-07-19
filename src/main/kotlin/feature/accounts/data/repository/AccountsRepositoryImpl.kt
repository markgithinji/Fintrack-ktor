package com.fintrack.feature.accounts.data.repository

import com.fintrack.core.data.dbQuery
import com.fintrack.feature.accounts.data.model.toDomain
import com.fintrack.feature.accounts.data.model.toDto
import com.fintrack.feature.accounts.data.table.AccountsTable
import com.fintrack.feature.accounts.domain.model.Account
import com.fintrack.feature.accounts.domain.model.TransactionSummary
import com.fintrack.feature.accounts.domain.repository.AccountsRepository
import com.fintrack.feature.user.UsersTable
import feature.transaction.data.table.TransactionsTable
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Case
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.minus
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.update
import java.util.UUID

class AccountsRepositoryImpl : AccountsRepository {

    override suspend fun getAllAccounts(userId: UUID): List<Account> =
        dbQuery {
            AccountsTable.selectAll().where { AccountsTable.userId eq EntityID(userId, UsersTable) }
                .orderBy(AccountsTable.isDefault to SortOrder.DESC)
                .orderBy(AccountsTable.createdAt to SortOrder.ASC)
                .map { toAccount(it) }
        }

    override suspend fun getAccountById(id: UUID): Account? =
        dbQuery {
            AccountsTable.selectAll().where { AccountsTable.id eq EntityID(id, AccountsTable) }
                .map { toAccount(it) }
                .singleOrNull()
        }

    override suspend fun addAccount(account: Account): Account =
        dbQuery {
            val now = Clock.System.now()
            val insertStatement = AccountsTable.insert { row ->
                row[userId] = EntityID(account.userId, UsersTable)
                fillRow(row, account, now)
            }
            val id = insertStatement[AccountsTable.id].value
            val createdAt = insertStatement[AccountsTable.createdAt]
            account.copy(id = id, createdAt = createdAt)
        }

    override suspend fun addAll(accounts: List<Account>): List<Account> =
        dbQuery {
            val now = Clock.System.now()
            AccountsTable.batchInsert(accounts) { account ->
                this[AccountsTable.userId] = EntityID(account.userId, UsersTable)
                fillRow(this, account, now)
            }.map { toAccount(it) }
        }

    override suspend fun updateAccount(account: Account): Account =
        dbQuery {
            requireNotNull(account.id) { "Account ID must not be null for update" }
            AccountsTable.update({
                AccountsTable.id eq EntityID(
                    account.id,
                    AccountsTable
                )
            }) { row ->
                fillRow(row, account, Clock.System.now())
            }
            account
        }

    private fun fillRow(row: UpdateBuilder<*>, account: Account, now: Instant) {
        row[AccountsTable.name] = account.name
        row[AccountsTable.isDefault] = account.isDefault
        row[AccountsTable.type] = account.type.toDto()
        row[AccountsTable.linkedSources] = Json.encodeToString(account.linkedSources)
        row[AccountsTable.balance] = account.balance
        row[AccountsTable.createdAt] = account.createdAt ?: now
        row[AccountsTable.lastSyncedAt] = account.lastSyncedAt
    }

    override suspend fun deleteAccount(id: UUID): Unit =
        dbQuery {
            AccountsTable.deleteWhere { AccountsTable.id eq EntityID(id, AccountsTable) }
        }

    override suspend fun getTransactionSummary(
        userId: UUID,
        accountId: UUID?
    ): TransactionSummary =
        dbQuery {
            val netAmount = Case()
                .When(
                    TransactionsTable.isIncome eq true,
                    TransactionsTable.amount - TransactionsTable.transactionCost
                )
                .Else(TransactionsTable.amount + TransactionsTable.transactionCost)

            val totalSum = netAmount.sum()
            val query = TransactionsTable
                .select(TransactionsTable.isIncome, totalSum)
                .where { TransactionsTable.userId eq EntityID(userId, UsersTable) }

            accountId?.let {
                query.andWhere { TransactionsTable.accountId eq EntityID(it, AccountsTable) }
            }

            val results = query.groupBy(TransactionsTable.isIncome).associate {
                it[TransactionsTable.isIncome] to (it[totalSum] ?: java.math.BigDecimal.ZERO)
            }

            TransactionSummary(
                income = results[true] ?: java.math.BigDecimal.ZERO,
                expense = results[false] ?: java.math.BigDecimal.ZERO
            )
        }

    override suspend fun getTransactionSummaries(userId: UUID): Map<UUID?, TransactionSummary> =
        dbQuery {
            val netAmount = Case()
                .When(
                    TransactionsTable.isIncome eq true,
                    TransactionsTable.amount - TransactionsTable.transactionCost
                )
                .Else(TransactionsTable.amount + TransactionsTable.transactionCost)

            val totalSum = netAmount.sum()
            TransactionsTable
                .select(TransactionsTable.accountId, TransactionsTable.isIncome, totalSum)
                .where { TransactionsTable.userId eq EntityID(userId, UsersTable) }
                .groupBy(TransactionsTable.accountId, TransactionsTable.isIncome)
                .map {
                    val accountId = it[TransactionsTable.accountId].value
                    val isIncome = it[TransactionsTable.isIncome]
                    val sum = it[totalSum] ?: java.math.BigDecimal.ZERO
                    Triple(accountId, isIncome, sum)
                }
                .groupBy { it.first }
                .mapValues { (_, values) ->
                    val income = values.find { it.second }?.third ?: java.math.BigDecimal.ZERO
                    val expense = values.find { !it.second }?.third ?: java.math.BigDecimal.ZERO
                    TransactionSummary(income, expense)
                }
        }

    override suspend fun getLatestBalance(userId: UUID, accountId: UUID?): java.math.BigDecimal? =
        dbQuery {
            val query = TransactionsTable
                .select(TransactionsTable.balance)
                .where { TransactionsTable.userId eq EntityID(userId, UsersTable) }
                .andWhere { TransactionsTable.balance.isNotNull() }

            val filteredQuery = accountId?.let {
                query.andWhere { TransactionsTable.accountId eq EntityID(it, AccountsTable) }
            } ?: query

            filteredQuery
                .orderBy(TransactionsTable.dateTime to SortOrder.DESC)
                .orderBy(TransactionsTable.id to SortOrder.DESC)
                .limit(1)
                .map { it[TransactionsTable.balance] }
                .singleOrNull()
        }

    override suspend fun updateBalance(accountId: UUID, balance: java.math.BigDecimal): Unit =
        dbQuery {
            AccountsTable.update({ AccountsTable.id eq EntityID(accountId, AccountsTable) }) {
                it[AccountsTable.balance] = balance
            }
        }

    override suspend fun resetBalances(userId: UUID, accountIds: List<UUID>?): Unit =
        dbQuery {
            AccountsTable.update({
                val condition = AccountsTable.userId eq EntityID(userId, UsersTable)
                if (!accountIds.isNullOrEmpty()) {
                    condition and (AccountsTable.id inList accountIds)
                } else {
                    condition
                }
            }) {
                it[AccountsTable.balance] = java.math.BigDecimal.ZERO
                it[AccountsTable.lastSyncedAt] = null
            }
        }

    private fun toAccount(row: ResultRow): Account {
        val linkedSourcesJson = row[AccountsTable.linkedSources]
        val linkedSources = try {
            Json.decodeFromString<Set<String>>(linkedSourcesJson)
        } catch (e: Exception) {
            emptySet()
        }

        return Account(
            id = row[AccountsTable.id].value,
            userId = row[AccountsTable.userId].value,
            name = row[AccountsTable.name],
            isDefault = row[AccountsTable.isDefault],
            type = row[AccountsTable.type].toDomain(),
            linkedSources = linkedSources,
            balance = row[AccountsTable.balance],
            createdAt = row[AccountsTable.createdAt],
            lastSyncedAt = row[AccountsTable.lastSyncedAt]
        )
    }
}