package com.fintrack.feature.budget.data

import com.fintrack.feature.user.UsersTable
import core.dbQuery
import feature.accounts.data.AccountsTable
import feature.budget.domain.BudgetRepository
import feature.transaction.Budget
import feature.transaction.BudgetsTable
import feature.transaction.data.TransactionsTable
import kotlinx.datetime.*
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class BudgetRepositoryImpl : BudgetRepository {
    override suspend fun getAllByUser(userId: UUID, accountId: UUID?): List<Budget> =
        dbQuery {
            var query = BudgetsTable.selectAll().where { BudgetsTable.userId eq EntityID(userId, UsersTable) }
            if (accountId != null) query =
                query.andWhere { BudgetsTable.accountId eq EntityID(accountId, AccountsTable) }
            query.map { it.toBudget() }
        }

    override suspend fun getById(userId: UUID, id: UUID): Budget? =
        dbQuery {
            BudgetsTable
                .selectAll()
                .where {
                    (BudgetsTable.id eq EntityID(id, BudgetsTable)) and (BudgetsTable.userId eq EntityID(
                        userId,
                        UsersTable
                    ))
                }
                .map { it.toBudget() }
                .singleOrNull()
        }

    override suspend fun add(budget: Budget): Budget =
        dbQuery {
            // We need to get the userId from the account to ensure security
            val account = AccountsTable
                .selectAll()
                .where { AccountsTable.id eq EntityID(budget.accountId, AccountsTable) }
                .singleOrNull()
                ?: throw IllegalArgumentException("Account not found")
            val accountUserId = account[AccountsTable.userId].value
            val insertStatement = BudgetsTable.insert {
                it[id] = EntityID(budget.id ?: UUID.randomUUID(), BudgetsTable)
                it[userId] = EntityID(accountUserId, UsersTable) // Use the account's userId
                it[accountId] = EntityID(budget.accountId, AccountsTable)
                it[name] = budget.name
                it[categories] = Json.encodeToString(budget.categories)
                it[limit] = budget.limit
                it[isExpense] = budget.isExpense
                it[startDate] = budget.startDate.toJavaLocalDate()
                it[endDate] = budget.endDate.toJavaLocalDate()
            }
            val generatedId = insertStatement[BudgetsTable.id].value
            budget.copy(id = generatedId)
        }

    override suspend fun addAll(budgets: List<Budget>): List<Budget> =
        dbQuery {
            budgets.map { budget ->
                // For each budget, get the account's userId
                val account = AccountsTable
                    .selectAll()
                    .where { AccountsTable.id eq EntityID(budget.accountId, AccountsTable) }
                    .singleOrNull()
                    ?: throw IllegalArgumentException("Account ${budget.accountId} not found")
                val accountUserId = account[AccountsTable.userId].value

                BudgetsTable.insert {
                    it[id] = EntityID(budget.id ?: UUID.randomUUID(), BudgetsTable)
                    it[userId] = EntityID(accountUserId, UsersTable)
                    it[accountId] = EntityID(budget.accountId, AccountsTable)
                    it[name] = budget.name
                    it[categories] = Json.encodeToString(budget.categories)
                    it[limit] = budget.limit
                    it[isExpense] = budget.isExpense
                    it[startDate] = budget.startDate.toJavaLocalDate()
                    it[endDate] = budget.endDate.toJavaLocalDate()
                }
            }
            // Return the budgets with their generated IDs
            budgets.mapIndexed { index, budget ->
                budget.copy(id = UUID.randomUUID()) // TODO: We might want to fetch the actual generated IDs
            }
        }

    override suspend fun update(userId: UUID, id: UUID, budget: Budget): Budget? =
        dbQuery {
            val rows = BudgetsTable.update({
                (BudgetsTable.id eq EntityID(id, BudgetsTable)) and
                        (BudgetsTable.userId eq EntityID(userId, UsersTable))
            }) {
                it[accountId] = EntityID(budget.accountId, AccountsTable)
                it[name] = budget.name
                it[categories] = Json.encodeToString(budget.categories)
                it[limit] = budget.limit
                it[isExpense] = budget.isExpense
                it[startDate] = budget.startDate.toJavaLocalDate()
                it[endDate] = budget.endDate.toJavaLocalDate()
            }

            if (rows > 0) {
                BudgetsTable
                    .selectAll()
                    .where {
                        (BudgetsTable.id eq EntityID(id, BudgetsTable)) and
                                (BudgetsTable.userId eq EntityID(userId, UsersTable))
                    }
                    .map { it.toBudget() }
                    .singleOrNull()
            } else {
                null
            }
        }

    override suspend fun delete(userId: UUID, id: UUID): Boolean =
        dbQuery {
            BudgetsTable.deleteWhere {
                (BudgetsTable.id eq EntityID(id, BudgetsTable)) and
                        (BudgetsTable.userId eq EntityID(userId, UsersTable))
            } > 0
        }

    override suspend fun getTransactionsInDateRange(
        accountId: UUID,
        categories: List<String>,
        isExpense: Boolean,
        start: Instant,
        end: Instant
    ): List<feature.transaction.domain.model.Transaction> =
        dbQuery {
            TransactionsTable
                .selectAll()
                .where {
                    (TransactionsTable.accountId eq EntityID(accountId, AccountsTable)) and
                            (TransactionsTable.dateTime.between(
                                start.toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime(),
                                end.toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime()
                            )) and
                            (TransactionsTable.category inList categories) and
                            (TransactionsTable.isIncome eq !isExpense)
                }
                .map { it.toTransaction() }
        }
}

// Extension functions
private fun ResultRow.toBudget(): Budget {
    val categoriesJson = this[BudgetsTable.categories]
    val categories: List<String> = Json.decodeFromString(categoriesJson)
    return Budget(
        id = this[BudgetsTable.id].value,
        accountId = this[BudgetsTable.accountId].value,
        name = this[BudgetsTable.name],
        categories = categories,
        limit = this[BudgetsTable.limit],
        isExpense = this[BudgetsTable.isExpense],
        startDate = this[BudgetsTable.startDate].toKotlinLocalDate(),
        endDate = this[BudgetsTable.endDate].toKotlinLocalDate()
    )
}

private fun ResultRow.toTransaction(): feature.transaction.domain.model.Transaction {
    return feature.transaction.domain.model.Transaction(
        id = this[TransactionsTable.id].value,
        userId = this[TransactionsTable.userId].value,
        isIncome = this[TransactionsTable.isIncome],
        amount = this[TransactionsTable.amount],
        category = this[TransactionsTable.category],
        dateTime = this[TransactionsTable.dateTime].toKotlinLocalDateTime(),
        description = this[TransactionsTable.description],
        accountId = this[TransactionsTable.accountId].value
    )
}