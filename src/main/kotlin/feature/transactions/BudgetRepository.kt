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
    fun getAll(): List<Budget> = transaction {
        BudgetsTable.selectAll().map { it.toBudget() }
    }

    fun getById(id: Int): Budget? = transaction {
        BudgetsTable
            .selectAll().where { BudgetsTable.id eq id }
            .map { it.toBudget() }
            .singleOrNull()
    }

    fun add(budget: Budget): Budget = transaction {
        val insertStatement = BudgetsTable.insert {
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
        val inserted = BudgetsTable.batchInsert(
            budgets,
            shouldReturnGeneratedValues = true
        ) { budget ->
            this[BudgetsTable.name] = budget.name
            this[BudgetsTable.categories] = Json.encodeToString(budget.categories)
            this[BudgetsTable.limit] = budget.limit
            this[BudgetsTable.isExpense] = budget.isExpense
            this[BudgetsTable.startDate] = budget.startDate.toJavaLocalDate()
            this[BudgetsTable.endDate] = budget.endDate.toJavaLocalDate()
        }

        inserted.map { it.toBudget() }
    }

    fun update(id: Int, budget: Budget): Boolean = transaction {
        BudgetsTable.update({ BudgetsTable.id eq id }) {
            it[name] = budget.name
            it[categories] = Json.encodeToString(budget.categories)
            it[limit] = budget.limit
            it[isExpense] = budget.isExpense
            it[startDate] = budget.startDate.toJavaLocalDate()
            it[endDate] = budget.endDate.toJavaLocalDate()
        } > 0
    }

    fun delete(id: Int): Boolean = transaction {
        BudgetsTable.deleteWhere { BudgetsTable.id eq id } > 0
    }
}

// --- Extension to map DB row -> domain ---
fun ResultRow.toBudget(): Budget {
    val categoriesJson = this[BudgetsTable.categories]
    val categories: List<String> = Json.decodeFromString(categoriesJson)
    return Budget(
        id = this[BudgetsTable.id],
        name = this[BudgetsTable.name],
        categories = categories,
        limit = this[BudgetsTable.limit],
        isExpense = this[BudgetsTable.isExpense],
        startDate = this[BudgetsTable.startDate].toKotlinLocalDate(),
        endDate = this[BudgetsTable.endDate].toKotlinLocalDate()
    )
}

