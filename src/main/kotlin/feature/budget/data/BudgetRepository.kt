package com.fintrack.feature.budget.data

import com.fintrack.feature.budget.domain.BudgetStatus
import com.fintrack.feature.budget.domain.BudgetWithStatus
import feature.transactions.Budget
import feature.transactions.BudgetsTable
import feature.transactions.data.TransactionsTable
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.datetime.toJavaLocalDate
import org.jetbrains.exposed.sql.ResultRow
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.selectAll
import kotlinx.datetime.*
import kotlinx.datetime.toJavaLocalDateTime
import org.jetbrains.exposed.sql.SqlExpressionBuilder.between
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus

class BudgetRepository {
    fun getAllByUser(userId: Int, accountId: Int? = null): List<Budget> = transaction {
        var query = BudgetsTable.selectAll().where { BudgetsTable.userId eq userId }
        if (accountId != null) query = query.andWhere { BudgetsTable.accountId eq accountId }
        query.map { it.toBudget() }
    }

    fun getById(userId: Int, id: Int): Budget? = transaction {
        BudgetsTable
            .selectAll()
            .where { (BudgetsTable.id eq id) and (BudgetsTable.userId eq userId) }
            .map { it.toBudget() }
            .singleOrNull()
    }

    fun add(budget: Budget): Budget = transaction {
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

    fun addAll(budgets: List<Budget>): List<Budget> = transaction {
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

    fun update(userId: Int, id: Int, budget: Budget): Budget? = transaction {
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
            getById(userId, id)
        } else {
            null
        }
    }

    fun delete(userId: Int, id: Int): Boolean = transaction {
        BudgetsTable.deleteWhere { (BudgetsTable.id eq id) and (BudgetsTable.userId eq userId) } > 0
    }

    fun getTransactionsInDateRange(
        accountId: Int,
        categories: List<String>,
        isExpense: Boolean,
        start: Instant,
        end: Instant
    ): List<feature.transactions.domain.model.Transaction> = transaction {
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

