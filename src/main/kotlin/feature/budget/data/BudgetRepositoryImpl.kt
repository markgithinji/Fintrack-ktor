package feature.budget.data

import com.fintrack.feature.user.UsersTable
import core.dbQuery
import com.fintrack.feature.accounts.data.table.AccountsTable
import feature.budget.domain.BudgetRepository
import feature.budget.domain.model.Budget
import feature.transaction.data.table.TransactionsTable
import kotlinx.datetime.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import java.util.UUID
import kotlinx.datetime.TimeZone as KTimeZone

class BudgetRepositoryImpl : BudgetRepository {
    override suspend fun getAllByUser(userId: UUID, accountId: UUID?, limit: Int, offset: Long): List<Budget> =
        dbQuery {
            var query = BudgetsTable.selectAll()
                .where { BudgetsTable.userId eq EntityID(userId, UsersTable) }
            if (accountId != null) {
                query = query.andWhere { BudgetsTable.accountId eq EntityID(accountId, AccountsTable) }
            }
            query.orderBy(BudgetsTable.updatedAt, SortOrder.DESC)
                .limit(limit, offset)
                .map { it.toBudget() }
        }

    override suspend fun getById(userId: UUID, id: UUID): Budget? =
        dbQuery {
            BudgetsTable
                .selectAll()
                .where {
                    (BudgetsTable.id eq EntityID(id, BudgetsTable)) and 
                    (BudgetsTable.userId eq EntityID(userId, UsersTable))
                }
                .map { it.toBudget() }
                .singleOrNull()
        }

    override suspend fun add(budget: Budget): Budget =
        dbQuery {
            val account = AccountsTable
                .selectAll()
                .where { AccountsTable.id eq EntityID(budget.accountId, AccountsTable) }
                .singleOrNull()
                ?: throw IllegalArgumentException("Account not found")
            val accountUserId = account[AccountsTable.userId].value
            val now = Clock.System.now()
            val insertStatement = BudgetsTable.insert {
                it[id] = EntityID(budget.id ?: UUID.randomUUID(), BudgetsTable)
                it[userId] = EntityID(accountUserId, UsersTable)
                it[accountId] = EntityID(budget.accountId, AccountsTable)
                it[name] = budget.name
                it[categories] = Json.encodeToString(budget.categories)
                it[limit] = budget.limit
                it[isExpense] = budget.isExpense
                it[startDate] = budget.startDate.toJavaLocalDate()
                it[endDate] = budget.endDate.toJavaLocalDate()
                it[createdAt] = now
                it[updatedAt] = now
            }
            val generatedId = insertStatement[BudgetsTable.id].value
            budget.copy(id = generatedId)
        }

    override suspend fun addAll(budgets: List<Budget>): List<Budget> =
        dbQuery {
            val now = Clock.System.now()
            budgets.map { budget ->
                val account = AccountsTable
                    .selectAll()
                    .where { AccountsTable.id eq EntityID(budget.accountId, AccountsTable) }
                    .singleOrNull()
                    ?: throw IllegalArgumentException("Account ${budget.accountId} not found")
                val accountUserId = account[AccountsTable.userId].value

                val inserted = BudgetsTable.insert {
                    it[id] = EntityID(budget.id ?: UUID.randomUUID(), BudgetsTable)
                    it[userId] = EntityID(accountUserId, UsersTable)
                    it[accountId] = EntityID(budget.accountId, AccountsTable)
                    it[name] = budget.name
                    it[categories] = Json.encodeToString(budget.categories)
                    it[limit] = budget.limit
                    it[isExpense] = budget.isExpense
                    it[startDate] = budget.startDate.toJavaLocalDate()
                    it[endDate] = budget.endDate.toJavaLocalDate()
                    it[createdAt] = now
                    it[updatedAt] = now
                }

                val generatedId =
                    inserted.resultedValues?.singleOrNull()?.get(BudgetsTable.id)?.value
                        ?: throw IllegalStateException("Failed to get generated ID for budget")

                budget.copy(id = generatedId)
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
                it[updatedAt] = Clock.System.now()
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

    override suspend fun deleteAllByUser(userId: UUID, accountIds: List<UUID>?): Int =
        dbQuery {
            if (!accountIds.isNullOrEmpty()) {
                BudgetsTable.deleteWhere {
                    (BudgetsTable.userId eq EntityID(userId, UsersTable)) and
                            (BudgetsTable.accountId inList accountIds)
                }
            } else {
                BudgetsTable.deleteWhere {
                    BudgetsTable.userId eq EntityID(userId, UsersTable)
                }
            }
        }

    override suspend fun getSpentAmount(
        accountId: UUID,
        categories: List<String>,
        isExpense: Boolean,
        start: Instant,
        end: Instant
    ): Double = dbQuery {
        val amountSum = TransactionsTable.amount.sum()
        TransactionsTable
            .select(amountSum)
            .where {
                (TransactionsTable.accountId eq EntityID(accountId, AccountsTable)) and
                (TransactionsTable.dateTime.between(start, end)) and
                (TransactionsTable.category inList categories) and
                (TransactionsTable.isIncome eq !isExpense)
            }
            .map { it[amountSum] ?: 0.0 }
            .single()
    }

    override suspend fun getSpentAmounts(budgets: List<Budget>): Map<UUID, Double> = dbQuery {
        if (budgets.isEmpty()) return@dbQuery emptyMap()
        
        val accountIds = budgets.map { it.accountId }.distinct()
        val minStart = budgets.minOf { it.startDate }.atStartOfDay(KTimeZone.UTC)
        val maxEnd = budgets.maxOf { it.endDate }.atEndOfDay(KTimeZone.UTC)
        val allCategories = budgets.flatMap { it.categories }.distinct()

        val transactions = TransactionsTable
            .selectAll()
            .where {
                (TransactionsTable.accountId inList accountIds) and
                (TransactionsTable.dateTime.between(minStart, maxEnd)) and
                (TransactionsTable.category inList allCategories)
            }
            .map { 
                TransactionSummary(
                    accountId = it[TransactionsTable.accountId].value,
                    category = it[TransactionsTable.category],
                    isIncome = it[TransactionsTable.isIncome],
                    amount = it[TransactionsTable.amount],
                    dateTime = it[TransactionsTable.dateTime]
                )
            }

        budgets.associate { budget ->
            val spent = transactions.filter { txn ->
                txn.accountId == budget.accountId &&
                txn.isIncome == !budget.isExpense &&
                budget.categories.contains(txn.category) &&
                txn.dateTime >= budget.startDate.atStartOfDay(KTimeZone.UTC) &&
                txn.dateTime <= budget.endDate.atEndOfDay(KTimeZone.UTC)
            }.sumOf { it.amount }
            
            (budget.id ?: UUID.randomUUID()) to spent
        }
    }

    private data class TransactionSummary(
        val accountId: UUID,
        val category: String,
        val isIncome: Boolean,
        val amount: Double,
        val dateTime: Instant
    )
    
    private fun LocalDate.atStartOfDay(zone: KTimeZone): Instant =
        this.atTime(LocalTime(0, 0)).toInstant(zone)

    private fun LocalDate.atEndOfDay(zone: KTimeZone): Instant =
        this.atTime(LocalTime(23, 59, 59, 999_999_999)).toInstant(zone)
}

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
