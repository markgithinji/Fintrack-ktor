package com.fintrack.feature.budget.data

import feature.budget.domain.BudgetRepository
import feature.transactions.Budget
import feature.transactions.BudgetsTable
import feature.transactions.data.TransactionsTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.datetime.toJavaLocalDate
import org.jetbrains.exposed.sql.ResultRow
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import kotlinx.datetime.*
import kotlinx.datetime.toJavaLocalDateTime

class BudgetRepositoryImpl : BudgetRepository {
    override suspend fun getAllByUser(userId: Int, accountId: Int?): List<Budget> =
        withContext(Dispatchers.IO) {
            transaction {
                var query = BudgetsTable.selectAll().where { BudgetsTable.userId eq userId }
                if (accountId != null) query = query.andWhere { BudgetsTable.accountId eq accountId }
                query.map { it.toBudget() }
            }
        }

    override suspend fun getById(userId: Int, id: Int): Budget? =
        withContext(Dispatchers.IO) {
            transaction {
                BudgetsTable
                    .selectAll()
                    .where { (BudgetsTable.id eq id) and (BudgetsTable.userId eq userId) }
                    .map { it.toBudget() }
                    .singleOrNull()
            }
        }

    override suspend fun add(budget: Budget): Budget =
        withContext(Dispatchers.IO) {
            transaction {
                val insertStatement = BudgetsTable.insert {
                    it[userId] = budget.userId
                    it[accountId] = budget.accountId
                    it[name] = budget.name
                    it[categories] = Json.encodeToString(budget.categories)
                    it[limit] = budget.limit
                    it[isExpense] = budget.isExpense
                    it[startDate] = budget.startDate.toJavaLocalDate()
                    it[endDate] = budget.endDate.toJavaLocalDate()
                }
                val generatedId = insertStatement[BudgetsTable.id]
                budget.copy(id = generatedId)
            }
        }

    override suspend fun addAll(budgets: List<Budget>): List<Budget> =
        withContext(Dispatchers.IO) {
            transaction {
                BudgetsTable.batchInsert(budgets, shouldReturnGeneratedValues = true) { budget ->
                    this[BudgetsTable.userId] = budget.userId
                    this[BudgetsTable.accountId] = budget.accountId
                    this[BudgetsTable.name] = budget.name
                    this[BudgetsTable.categories] = Json.encodeToString(budget.categories)
                    this[BudgetsTable.limit] = budget.limit
                    this[BudgetsTable.isExpense] = budget.isExpense
                    this[BudgetsTable.startDate] = budget.startDate.toJavaLocalDate()
                    this[BudgetsTable.endDate] = budget.endDate.toJavaLocalDate()
                }.map { it.toBudget() }
            }
        }

    override suspend fun update(userId: Int, id: Int, budget: Budget): Budget? =
        withContext(Dispatchers.IO) {
            transaction {
                val rows = BudgetsTable.update({ (BudgetsTable.id eq id) and (BudgetsTable.userId eq userId) }) {
                    it[accountId] = budget.accountId
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
                        .where { (BudgetsTable.id eq id) and (BudgetsTable.userId eq userId) }
                        .map { it.toBudget() }
                        .singleOrNull()
                } else {
                    null
                }
            }
        }
    override suspend fun delete(userId: Int, id: Int): Boolean =
        withContext(Dispatchers.IO) {
            transaction {
                BudgetsTable.deleteWhere { (BudgetsTable.id eq id) and (BudgetsTable.userId eq userId) } > 0
            }
        }

    override suspend fun getTransactionsInDateRange(
        accountId: Int,
        categories: List<String>,
        isExpense: Boolean,
        start: Instant,
        end: Instant
    ): List<feature.transactions.domain.model.Transaction> =
        withContext(Dispatchers.IO) {
            transaction {
                TransactionsTable
                    .selectAll()
                    .where {
                        (TransactionsTable.accountId eq accountId) and
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
}

// Extension functions
private fun ResultRow.toBudget(): Budget {
    val categoriesJson = this[BudgetsTable.categories]
    val categories: List<String> = Json.decodeFromString(categoriesJson)
    return Budget(
        id = this[BudgetsTable.id],
        userId = this[BudgetsTable.userId],
        accountId = this[BudgetsTable.accountId],
        name = this[BudgetsTable.name],
        categories = categories,
        limit = this[BudgetsTable.limit],
        isExpense = this[BudgetsTable.isExpense],
        startDate = this[BudgetsTable.startDate].toKotlinLocalDate(),
        endDate = this[BudgetsTable.endDate].toKotlinLocalDate()
    )
}

private fun ResultRow.toTransaction(): feature.transactions.domain.model.Transaction {
    return feature.transactions.domain.model.Transaction(
        id = this[TransactionsTable.id],
        userId = this[TransactionsTable.userId],
        isIncome = this[TransactionsTable.isIncome],
        amount = this[TransactionsTable.amount],
        category = this[TransactionsTable.category],
        dateTime = this[TransactionsTable.dateTime].toKotlinLocalDateTime(),
        description = this[TransactionsTable.description],
        accountId = this[TransactionsTable.accountId]
    )
}

