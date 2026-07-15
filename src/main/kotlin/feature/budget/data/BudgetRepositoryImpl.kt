package feature.budget.data

import com.fintrack.feature.user.UsersTable
import com.fintrack.core.data.dbQuery
import com.fintrack.feature.accounts.data.table.AccountsTable
import core.util.IdGenerator
import feature.budget.domain.BudgetRepository
import feature.budget.domain.model.Budget
import feature.transaction.data.table.TransactionsTable
import kotlinx.datetime.*
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
                val budgetIdsWithAccount = BudgetAccountsTable
                    .select(BudgetAccountsTable.budgetId)
                    .where { BudgetAccountsTable.accountId eq accountId }
                    .map { it[BudgetAccountsTable.budgetId].value }
                
                query = query.andWhere { BudgetsTable.id inList budgetIdsWithAccount }
            }
            
            val budgets = query.orderBy(BudgetsTable.updatedAt, SortOrder.DESC)
                .limit(limit)
                .offset(offset)
                .map { row ->
                    val id = row[BudgetsTable.id].value
                    Budget(
                        id = id,
                        accountIds = fetchAccountIds(id),
                        name = row[BudgetsTable.name],
                        categoryIds = fetchCategoryIds(id),
                        limit = row[BudgetsTable.limit],
                        isExpense = row[BudgetsTable.isExpense],
                        startDate = row[BudgetsTable.startDate].toKotlinLocalDate(),
                        endDate = row[BudgetsTable.endDate].toKotlinLocalDate()
                    )
                }
            budgets
        }

    override suspend fun getById(userId: UUID, id: UUID): Budget? =
        dbQuery {
            BudgetsTable
                .selectAll()
                .where {
                    (BudgetsTable.id eq EntityID(id, BudgetsTable)) and 
                    (BudgetsTable.userId eq EntityID(userId, UsersTable))
                }
                .singleOrNull()
                ?.let { row ->
                    Budget(
                        id = id,
                        accountIds = fetchAccountIds(id),
                        name = row[BudgetsTable.name],
                        categoryIds = fetchCategoryIds(id),
                        limit = row[BudgetsTable.limit],
                        isExpense = row[BudgetsTable.isExpense],
                        startDate = row[BudgetsTable.startDate].toKotlinLocalDate(),
                        endDate = row[BudgetsTable.endDate].toKotlinLocalDate()
                    )
                }
        }

    override suspend fun add(budget: Budget): Budget =
        dbQuery {
            val firstAccountId = budget.accountIds.firstOrNull() 
                ?: throw IllegalArgumentException("Budget must have at least one account")
                
            val account = AccountsTable
                .selectAll()
                .where { AccountsTable.id eq EntityID(firstAccountId, AccountsTable) }
                .singleOrNull()
                ?: throw IllegalArgumentException("Account not found")
                
            val accountUserId = account[AccountsTable.userId].value
            val now = Clock.System.now()
            val budgetIdValue = budget.id ?: IdGenerator.nextId()
            
            BudgetsTable.insert {
                it[id] = EntityID(budgetIdValue, BudgetsTable)
                it[userId] = EntityID(accountUserId, UsersTable)
                it[accountId] = EntityID(firstAccountId, AccountsTable)
                it[name] = budget.name
                it[limit] = budget.limit
                it[isExpense] = budget.isExpense
                it[startDate] = budget.startDate.toJavaLocalDate()
                it[endDate] = budget.endDate.toJavaLocalDate()
                it[createdAt] = now
                it[updatedAt] = now
            }

            // Insert relations
            BudgetAccountsTable.batchInsert(budget.accountIds) { accId ->
                this[BudgetAccountsTable.budgetId] = budgetIdValue
                this[BudgetAccountsTable.accountId] = accId
            }
            BudgetCategoriesTable.batchInsert(budget.categoryIds) { catId ->
                this[BudgetCategoriesTable.budgetId] = budgetIdValue
                this[BudgetCategoriesTable.categoryId] = catId
            }

            budget.copy(id = budgetIdValue)
        }

    override suspend fun addAll(budgets: List<Budget>): List<Budget> = budgets.map { add(it) }

    override suspend fun update(userId: UUID, id: UUID, budget: Budget): Budget? =
        dbQuery {
            val now = Clock.System.now()
            val rows = BudgetsTable.update({
                (BudgetsTable.id eq EntityID(id, BudgetsTable)) and
                        (BudgetsTable.userId eq EntityID(userId, UsersTable))
            }) {
                if (budget.accountIds.isNotEmpty()) {
                    it[accountId] = EntityID(budget.accountIds.first(), AccountsTable)
                }
                it[name] = budget.name
                it[limit] = budget.limit
                it[isExpense] = budget.isExpense
                it[startDate] = budget.startDate.toJavaLocalDate()
                it[endDate] = budget.endDate.toJavaLocalDate()
                it[updatedAt] = now
            }

            if (rows > 0) {
                // Update relations
                BudgetAccountsTable.deleteWhere { budgetId eq id }
                BudgetAccountsTable.batchInsert(budget.accountIds) { accId ->
                    this[BudgetAccountsTable.budgetId] = id
                    this[BudgetAccountsTable.accountId] = accId
                }
                
                BudgetCategoriesTable.deleteWhere { budgetId eq id }
                BudgetCategoriesTable.batchInsert(budget.categoryIds) { catId ->
                    this[BudgetCategoriesTable.budgetId] = id
                    this[BudgetCategoriesTable.categoryId] = catId
                }

                getById(userId, id)
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
                val budgetIdsWithAccounts = BudgetAccountsTable
                    .select(BudgetAccountsTable.budgetId)
                    .where { BudgetAccountsTable.accountId inList accountIds }
                    .map { it[BudgetAccountsTable.budgetId].value }

                BudgetsTable.deleteWhere {
                    (BudgetsTable.userId eq EntityID(userId, UsersTable)) and
                    (BudgetsTable.id inList budgetIdsWithAccounts)
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
    ): java.math.BigDecimal = dbQuery {
        if (categoryIds.isEmpty()) return@dbQuery java.math.BigDecimal.ZERO
        
        val amountSum = TransactionsTable.amount.sum()
        TransactionsTable
            .select(amountSum)
            .where {
                (TransactionsTable.accountId inList accountIds) and
                (TransactionsTable.dateTime.between(start, end)) and
                (TransactionsTable.categoryId inList categoryIds) and
                (TransactionsTable.isIncome eq !isExpense)
            }
            .map { it[amountSum] ?: java.math.BigDecimal.ZERO }
            .single()
    }

    override suspend fun getSpentAmounts(budgets: List<Budget>): Map<UUID, java.math.BigDecimal> = dbQuery {
        if (budgets.isEmpty()) return@dbQuery emptyMap()
        
        val allAccountIds = budgets.flatMap { it.accountIds }.distinct()
        val minStart = budgets.minOf { it.startDate }.atStartOfDay(KTimeZone.UTC)
        val maxEnd = budgets.maxOf { it.endDate }.atEndOfDay(KTimeZone.UTC)
        val allCategoryIds = budgets.flatMap { it.categoryIds }.distinct()

        if (allCategoryIds.isEmpty()) return@dbQuery budgets.associate { (it.id ?: IdGenerator.nextId()) to java.math.BigDecimal.ZERO }

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
            }.fold(java.math.BigDecimal.ZERO) { acc, txn -> acc + txn.amount }
            
            (budget.id ?: IdGenerator.nextId()) to spent
        }
    }

    private fun fetchAccountIds(budgetId: UUID): List<UUID> =
        BudgetAccountsTable.select(BudgetAccountsTable.accountId)
            .where { BudgetAccountsTable.budgetId eq budgetId }
            .map { it[BudgetAccountsTable.accountId].value }

    private fun fetchCategoryIds(budgetId: UUID): List<UUID> =
        BudgetCategoriesTable.select(BudgetCategoriesTable.categoryId)
            .where { BudgetCategoriesTable.budgetId eq budgetId }
            .map { it[BudgetCategoriesTable.categoryId].value }

    private data class TransactionSummary(
        val accountId: UUID,
        val categoryId: UUID,
        val isIncome: Boolean,
        val amount: java.math.BigDecimal,
        val dateTime: Instant
    )
    
    private fun LocalDate.atStartOfDay(zone: KTimeZone): Instant =
        this.atTime(LocalTime(0, 0)).toInstant(zone)

    private fun LocalDate.atEndOfDay(zone: KTimeZone): Instant =
        this.atTime(LocalTime(23, 59, 59, 999_999_999)).toInstant(zone)
}
