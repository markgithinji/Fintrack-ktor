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

    fun update(userId: Int, id: Int, budget: Budget): Boolean = transaction {
        BudgetsTable.update({ (BudgetsTable.id eq id) and (BudgetsTable.userId eq userId) }) {
            it[accountId] = budget.accountId
            it[name] = budget.name
            it[categories] = Json.encodeToString(budget.categories)
            it[limit] = budget.limit
            it[isExpense] = budget.isExpense
            it[startDate] = budget.startDate.toJavaLocalDate()
            it[endDate] = budget.endDate.toJavaLocalDate()
        } > 0
    }

    fun delete(userId: Int, id: Int): Boolean = transaction {
        BudgetsTable.deleteWhere { (BudgetsTable.id eq id) and (BudgetsTable.userId eq userId) } > 0
    }

    private fun LocalDate.atStartOfDay(zone: TimeZone = TimeZone.currentSystemDefault()): Instant =
        this.atTime(LocalTime(0, 0)).toInstant(zone)

    private fun LocalDate.atEndOfDay(zone: TimeZone = TimeZone.currentSystemDefault()): Instant =
        this.plus(1, DateTimeUnit.DAY).atStartOfDay(zone)

    // --- Status calculation ---
    private fun calculateStatus(budget: Budget): BudgetStatus {
        val tz = TimeZone.currentSystemDefault()
        val start = budget.startDate.atStartOfDay(tz)
        val end = budget.endDate.atEndOfDay(tz)

        val spent = transaction {
            TransactionsTable
                .select(TransactionsTable.amount.sum())
                .where {
                    (TransactionsTable.accountId eq budget.accountId) and
                            (TransactionsTable.dateTime.between(
                                start.toLocalDateTime(tz).toJavaLocalDateTime(),
                                end.toLocalDateTime(tz).toJavaLocalDateTime()
                            )) and
                            (TransactionsTable.category inList budget.categories) and
                            (TransactionsTable.isIncome eq !budget.isExpense)
                }
                .firstOrNull()
                ?.getOrNull(TransactionsTable.amount.sum()) ?: 0.0
        }

        val remaining = budget.limit - spent
        val percentageUsed = if (budget.limit > 0) (spent / budget.limit) * 100 else 0.0
        val isExceeded = spent > budget.limit

        return BudgetStatus(
            limit = budget.limit,
            spent = spent,
            remaining = remaining,
            percentageUsed = percentageUsed,
            isExceeded = isExceeded
        )
    }

    fun getAllWithStatus(userId: Int, accountId: Int? = null): List<BudgetWithStatus> {
        val budgets = getAllByUser(userId, accountId)
        return budgets.map { budget -> BudgetWithStatus(budget, calculateStatus(budget)) }
    }

    fun getByIdWithStatus(userId: Int, id: Int): BudgetWithStatus? {
        val budget = getById(userId, id) ?: return null
        return BudgetWithStatus(budget, calculateStatus(budget))
    }

}

// --- DB Row -> Domain ---
fun ResultRow.toBudget(): Budget {
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


