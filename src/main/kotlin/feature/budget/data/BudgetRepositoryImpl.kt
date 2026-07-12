package feature.budget.data

import com.fintrack.feature.user.UsersTable
import com.fintrack.core.data.dbQuery
import com.fintrack.feature.accounts.data.table.AccountsTable
import core.util.IdGenerator
import feature.budget.domain.BudgetRepository
import feature.budget.domain.model.Budget
import feature.transaction.data.table.TransactionsTable
import kotlinx.datetime.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import java.util.UUID
import kotlinx.datetime.TimeZone as KTimeZone

class BudgetRepositoryImpl : BudgetRepository {
    override suspend fun getAllByUser(userId: UUID, accountId: UUID?, limit: Int, offset: Long): List<Budget> =
        dbQuery {
            var query = BudgetsTable.selectAll()
                .where { BudgetsTable.userId eq EntityID(userId, UsersTable) }
            
            if (accountId != null) {
                // Search for the account ID within the JSON array string
                query = query.andWhere { BudgetsTable.accountIds like "%$accountId%" }
            }
            
            query.orderBy(BudgetsTable.updatedAt, SortOrder.DESC)
                .limit(limit)
                .offset(offset)
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
            // We use the first account ID to verify existence and get the user ID
            val firstAccountId = budget.accountIds.firstOrNull() 
                ?: throw IllegalArgumentException("Budget must have at least one account")
                
            val account = AccountsTable
                .selectAll()
                .where { AccountsTable.id eq EntityID(firstAccountId, AccountsTable) }
                .singleOrNull()
                ?: throw IllegalArgumentException("Account not found")
                
            val accountUserId = account[AccountsTable.userId].value
            val now = Clock.System.now()
            
            val insertStatement = BudgetsTable.insert {
                it[id] = EntityID(budget.id ?: IdGenerator.nextId(), BudgetsTable)
                it[userId] = EntityID(accountUserId, UsersTable)
                // We keep accountId (nullable) for backward compatibility in the short term if needed, 
                // but primary storage is now accountIds
                it[accountId] = EntityID(firstAccountId, AccountsTable)
                it[accountIds] = Json.encodeToString(budget.accountIds.map { accountId -> accountId.toString() })
                it[name] = budget.name
                it[categoryIds] = Json.encodeToString(budget.categoryIds.map { it.toString() })
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
                val firstAccountId = budget.accountIds.firstOrNull()
                    ?: throw IllegalArgumentException("Budget ${budget.name} must have at least one account")
                    
                val account = AccountsTable
                    .selectAll()
                    .where { AccountsTable.id eq EntityID(firstAccountId, AccountsTable) }
                    .singleOrNull()
                    ?: throw IllegalArgumentException("Account $firstAccountId not found")
                    
                val accountUserId = account[AccountsTable.userId].value

                val inserted = BudgetsTable.insert {
                    it[id] = EntityID(budget.id ?: IdGenerator.nextId(), BudgetsTable)
                    it[userId] = EntityID(accountUserId, UsersTable)
                    it[accountId] = EntityID(firstAccountId, AccountsTable)
                    it[accountIds] = Json.encodeToString(budget.accountIds.map { accountId -> accountId.toString() })
                    it[name] = budget.name
                    it[categoryIds] = Json.encodeToString(budget.categoryIds.map { it.toString() })
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
                it[accountIds] = Json.encodeToString(budget.accountIds.map { accountId -> accountId.toString() })
                // Also update the legacy accountId column if multiple accounts are present, just use the first one
                if (budget.accountIds.isNotEmpty()) {
                    it[accountId] = EntityID(budget.accountIds.first(), AccountsTable)
                }
                it[name] = budget.name
                it[categoryIds] = Json.encodeToString(budget.categoryIds.map { it.toString() })
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
                // Delete budgets where the legacy accountId is in the list OR any of the new accountIds match
                // This is a bit aggressive but safer for a "clear data" operation
                BudgetsTable.deleteWhere {
                    val userMatch = BudgetsTable.userId eq EntityID(userId, UsersTable)
                    val accountMatch = accountIds.map { id -> BudgetsTable.accountIds.like("%$id%") }
                        .fold<Op<Boolean>, Op<Boolean>>(BudgetsTable.accountId inList accountIds) { acc, op -> acc or op }
                    userMatch and accountMatch
                }
            } else {
                BudgetsTable.deleteWhere {
                    BudgetsTable.userId eq EntityID(userId, UsersTable)
                }
            }
        }

    override suspend fun getSpentAmount(
        accountIds: List<UUID>,
        categoryIds: List<UUID>,
        isExpense: Boolean,
        start: Instant,
        end: Instant
    ): Double = dbQuery {
        val amountSum = TransactionsTable.amount.sum()
        TransactionsTable
            .select(amountSum)
            .where {
                (TransactionsTable.accountId inList accountIds) and
                (TransactionsTable.dateTime.between(start, end)) and
                (TransactionsTable.categoryId inList categoryIds) and
                (TransactionsTable.isIncome eq !isExpense)
            }
            .map { it[amountSum] ?: 0.0 }
            .single()
    }

    override suspend fun getSpentAmounts(budgets: List<Budget>): Map<UUID, Double> = dbQuery {
        if (budgets.isEmpty()) return@dbQuery emptyMap()
        
        val allAccountIds = budgets.flatMap { it.accountIds }.distinct()
        val minStart = budgets.minOf { it.startDate }.atStartOfDay(KTimeZone.UTC)
        val maxEnd = budgets.maxOf { it.endDate }.atEndOfDay(KTimeZone.UTC)
        val allCategoryIds = budgets.flatMap { it.categoryIds }.distinct()

        val transactions = TransactionsTable
            .selectAll()
            .where {
                (TransactionsTable.accountId inList allAccountIds) and
                (TransactionsTable.dateTime.between(minStart, maxEnd)) and
                (TransactionsTable.categoryId inList allCategoryIds)
            }
            .map { 
                TransactionSummary(
                    accountId = it[TransactionsTable.accountId].value,
                    categoryId = it[TransactionsTable.categoryId].value,
                    isIncome = it[TransactionsTable.isIncome],
                    amount = it[TransactionsTable.amount],
                    dateTime = it[TransactionsTable.dateTime]
                )
            }

        budgets.associate { budget ->
            val spent = transactions.filter { txn ->
                (budget.accountIds.contains(txn.accountId)) &&
                (txn.isIncome == !budget.isExpense) &&
                (budget.categoryIds.contains(txn.categoryId)) &&
                (txn.dateTime >= budget.startDate.atStartOfDay(KTimeZone.UTC)) &&
                (txn.dateTime <= budget.endDate.atEndOfDay(KTimeZone.UTC))
            }.sumOf { it.amount }
            
            (budget.id ?: IdGenerator.nextId()) to spent
        }
    }

    private data class TransactionSummary(
        val accountId: UUID,
        val categoryId: UUID,
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
    val categoryIdsJson = this[BudgetsTable.categoryIds]
    val categoryIds: List<UUID> = Json.decodeFromString<List<String>>(categoryIdsJson).map { UUID.fromString(it) }
    val accountIdsJson = this[BudgetsTable.accountIds]
    val accountIds: List<UUID> = Json.decodeFromString<List<String>>(accountIdsJson).map { UUID.fromString(it) }
    return Budget(
        id = this[BudgetsTable.id].value,
        accountIds = accountIds,
        name = this[BudgetsTable.name],
        categoryIds = categoryIds,
        limit = this[BudgetsTable.limit],
        isExpense = this[BudgetsTable.isExpense],
        startDate = this[BudgetsTable.startDate].toKotlinLocalDate(),
        endDate = this[BudgetsTable.endDate].toKotlinLocalDate()
    )
}
