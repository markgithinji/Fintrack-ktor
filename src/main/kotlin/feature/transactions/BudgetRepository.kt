package feature.transactions

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.time.ZoneId
import org.jetbrains.exposed.sql.ResultRow
import kotlinx.datetime.toKotlinLocalDate
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDate as JavaLocalDate

class BudgetRepository {

    fun getAllByUser(userId: Int, accountId: Int? = null): List<Budget> = transaction {
        var query = BudgetsTable.selectAll().where { BudgetsTable.userId eq userId }

        if (accountId != null) query = query.andWhere { BudgetsTable.accountId eq accountId }

        query.map { it.toBudget() }
    }

    fun getById(userId: Int, id: Int): Budget? = transaction {
        BudgetsTable.selectAll()
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
        val inserted = BudgetsTable.batchInsert(budgets, shouldReturnGeneratedValues = true) { budget ->
            this[BudgetsTable.userId] = budget.userId
            this[BudgetsTable.accountId] = budget.accountId
            this[BudgetsTable.name] = budget.name
            this[BudgetsTable.categories] = Json.encodeToString(budget.categories)
            this[BudgetsTable.limit] = budget.limit
            this[BudgetsTable.isExpense] = budget.isExpense
            this[BudgetsTable.startDate] = budget.startDate.toJavaLocalDate()
            this[BudgetsTable.endDate] = budget.endDate.toJavaLocalDate()
        }
        inserted.map { it.toBudget() }
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
}

// --- Extension to map DB row -> domain ---
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

