package feature.transaction.data.repository

import com.fintrack.core.data.dbQuery
import core.util.IdGenerator
import feature.transaction.data.table.TransactionsTable
import feature.transaction.domain.TransactionRepository
import feature.transaction.domain.model.Transaction
import feature.category.data.table.CategoriesTable
import kotlinx.datetime.Instant
import kotlinx.datetime.Clock
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import java.util.*

class TransactionRepositoryImpl : TransactionRepository {
    private val joinTable = TransactionsTable.leftJoin(
        CategoriesTable,
        { TransactionsTable.categoryId },
        { CategoriesTable.id }
    )

    override suspend fun getAllCursor(
        userId: UUID,
        accountId: UUID?,
        isIncome: Boolean?,
        categoryIds: List<UUID>?,
        start: Instant?,
        end: Instant?,
        sortBy: String,
        order: SortOrder,
        limit: Int,
        afterDateTime: Instant?,
        afterId: UUID?,
        hasTransactionCost: Boolean?
    ): List<Transaction> = dbQuery {
        var query = joinTable.selectAll()
            .where { TransactionsTable.userId eq userId }

        if (accountId != null) query =
            query.andWhere { TransactionsTable.accountId eq accountId }
        if (isIncome != null) query = query.andWhere { TransactionsTable.isIncome eq isIncome }
        if (!categoryIds.isNullOrEmpty()) query = query.andWhere { TransactionsTable.categoryId inList categoryIds }
        if (start != null) query = query.andWhere { TransactionsTable.dateTime greaterEq start }
        if (end != null) query = query.andWhere { TransactionsTable.dateTime lessEq end }
        if (hasTransactionCost == true) query = query.andWhere { TransactionsTable.transactionCost greater java.math.BigDecimal.ZERO }
        if (hasTransactionCost == false) query = query.andWhere { TransactionsTable.transactionCost eq java.math.BigDecimal.ZERO }

        if (afterDateTime != null && afterId != null) {
            query = if (order == SortOrder.DESC) {
                query.andWhere {
                    (TransactionsTable.dateTime less afterDateTime) or
                            ((TransactionsTable.dateTime eq afterDateTime) and
                                    (TransactionsTable.id less afterId))
                }
            } else {
                query.andWhere {
                    (TransactionsTable.dateTime greater afterDateTime) or
                            ((TransactionsTable.dateTime eq afterDateTime) and
                                    (TransactionsTable.id greater afterId))
                }
            }
        }

        val orderColumn = when (sortBy) {
            "amount" -> TransactionsTable.amount
            else -> TransactionsTable.dateTime
        }

        query.orderBy(orderColumn, order)
            .orderBy(TransactionsTable.id, order)
            .limit(limit)
            .map { it.toTransaction() }
    }

    override suspend fun getById(id: UUID, userId: UUID): Transaction? = dbQuery {
        getByIdInternal(id, userId)
    }

    private fun getByIdInternal(id: UUID, userId: UUID): Transaction? =
        joinTable
            .selectAll().where {
                (TransactionsTable.id eq id) and
                        (TransactionsTable.userId eq userId)
            }
            .map { it.toTransaction() }
            .singleOrNull()

    override suspend fun add(entity: Transaction): Transaction = dbQuery {
        val now = Clock.System.now()
        val transactionId = entity.id ?: IdGenerator.nextId()

        val inserted = TransactionsTable.insert { row ->
            row[id] = EntityID(transactionId, TransactionsTable)
            row[userId] = EntityID(entity.userId, com.fintrack.feature.user.UsersTable)
            row[accountId] = EntityID(entity.accountId, com.fintrack.feature.accounts.data.table.AccountsTable)
            row[isIncome] = entity.isIncome
            row[amount] = entity.amount
            row[transactionCost] = entity.transactionCost
            row[categoryId] = EntityID(entity.categoryId, CategoriesTable)
            row[dateTime] = entity.dateTime
            row[description] = entity.description
            row[externalId] = entity.externalId
            row[balance] = entity.balance
            row[createdAt] = now
            row[updatedAt] = now
        }.resultedValues?.singleOrNull()

        // Still need to re-fetch if we want category name, try to be efficient
        if (inserted != null) {
            getByIdInternal(transactionId, entity.userId)
                ?: throw IllegalStateException("Failed to retrieve inserted transaction")
        } else {
            throw IllegalStateException("Failed to insert transaction")
        }
    }

    override suspend fun update(id: UUID, userId: UUID, entity: Transaction): Transaction? = dbQuery {
        val now = Clock.System.now()
        
        val updated = TransactionsTable.update(
            where = {
                (TransactionsTable.id eq EntityID(id, TransactionsTable)) and
                        (TransactionsTable.userId eq EntityID(userId, com.fintrack.feature.user.UsersTable))
            }
        ) { row ->
            row[TransactionsTable.accountId] = EntityID(entity.accountId, com.fintrack.feature.accounts.data.table.AccountsTable)
            row[TransactionsTable.isIncome] = entity.isIncome
            row[TransactionsTable.amount] = entity.amount
            row[TransactionsTable.transactionCost] = entity.transactionCost
            row[TransactionsTable.categoryId] = EntityID(entity.categoryId, CategoriesTable)
            row[TransactionsTable.dateTime] = entity.dateTime
            row[TransactionsTable.description] = entity.description
            row[TransactionsTable.externalId] = entity.externalId
            row[TransactionsTable.balance] = entity.balance
            row[TransactionsTable.updatedAt] = now
        }

        if (updated == 0) return@dbQuery null
        
        getByIdInternal(id, userId)
    }

    override suspend fun delete(id: UUID, userId: UUID): Boolean = dbQuery {
        val deleted = TransactionsTable.deleteWhere {
            (TransactionsTable.id eq EntityID(id, TransactionsTable)) and
                    (TransactionsTable.userId eq EntityID(userId, com.fintrack.feature.user.UsersTable))
        }
        deleted > 0
    }

    override suspend fun clearAll(userId: UUID, accountIds: List<UUID>?): Boolean = dbQuery {
        val deleted = if (!accountIds.isNullOrEmpty()) {
            TransactionsTable.deleteWhere {
                (TransactionsTable.userId eq EntityID(userId, com.fintrack.feature.user.UsersTable)) and
                        (TransactionsTable.accountId inList accountIds)
            }
        } else {
            TransactionsTable.deleteWhere { TransactionsTable.userId eq EntityID(userId, com.fintrack.feature.user.UsersTable) }
        }
        deleted > 0
    }

    override suspend fun reassignCategory(userId: UUID, oldCategoryId: UUID, newCategoryId: UUID): Int = dbQuery {
        TransactionsTable.update({
            (TransactionsTable.userId eq EntityID(userId, com.fintrack.feature.user.UsersTable)) and
                    (TransactionsTable.categoryId eq EntityID(oldCategoryId, CategoriesTable))
        }) {
            it[categoryId] = EntityID(newCategoryId, CategoriesTable)
            it[updatedAt] = Clock.System.now()
        }
    }

    override suspend fun addBulk(entities: List<Transaction>): List<Transaction> = dbQuery {
        val now = Clock.System.now()
        val entitiesWithIds = entities.map { it.copy(id = it.id ?: IdGenerator.nextId()) }

        TransactionsTable.batchInsert(entitiesWithIds, ignore = true) { entity ->
            this[TransactionsTable.id] = EntityID(entity.id!!, TransactionsTable)
            this[TransactionsTable.userId] = EntityID(entity.userId, com.fintrack.feature.user.UsersTable)
            this[TransactionsTable.accountId] = EntityID(entity.accountId, com.fintrack.feature.accounts.data.table.AccountsTable)
            this[TransactionsTable.isIncome] = entity.isIncome
            this[TransactionsTable.amount] = entity.amount
            this[TransactionsTable.transactionCost] = entity.transactionCost
            this[TransactionsTable.categoryId] = EntityID(entity.categoryId, CategoriesTable)
            this[TransactionsTable.dateTime] = entity.dateTime
            this[TransactionsTable.description] = entity.description
            this[TransactionsTable.externalId] = entity.externalId
            this[TransactionsTable.balance] = entity.balance
            this[TransactionsTable.createdAt] = now
            this[TransactionsTable.updatedAt] = now
        }
        
        // Return re-fetched to get names. 
        // Optimization: Use the IDs we just generated to fetch in one batch.
        val ids = entitiesWithIds.mapNotNull { it.id }
        if (ids.isEmpty()) return@dbQuery emptyList()

        joinTable.selectAll()
            .where { TransactionsTable.id inList ids }
            .map { it.toTransaction() }
    }

    override suspend fun getLatestBalance(userId: UUID, accountId: UUID?): java.math.BigDecimal? = dbQuery {
        val query = TransactionsTable.selectAll()
            .where { TransactionsTable.userId eq EntityID(userId, com.fintrack.feature.user.UsersTable) }
            .andWhere { TransactionsTable.balance.isNotNull() }

        if (accountId != null) {
            query.andWhere { TransactionsTable.accountId eq EntityID(accountId, com.fintrack.feature.accounts.data.table.AccountsTable) }
        }

        query.orderBy(TransactionsTable.dateTime to SortOrder.DESC)
            .orderBy(TransactionsTable.id to SortOrder.DESC)
            .limit(1)
            .map { it[TransactionsTable.balance] }
            .singleOrNull()
    }

    private fun ResultRow.toTransaction() = Transaction(
        id = this[TransactionsTable.id].value,
        userId = this[TransactionsTable.userId].value,
        isIncome = this[TransactionsTable.isIncome],
        amount = this[TransactionsTable.amount],
        transactionCost = this[TransactionsTable.transactionCost],
        category = this.getOrNull(CategoriesTable.name) ?: "Uncategorized",
        categoryId = this[TransactionsTable.categoryId].value,
        dateTime = this[TransactionsTable.dateTime],
        description = this[TransactionsTable.description],
        accountId = this[TransactionsTable.accountId].value,
        externalId = this[TransactionsTable.externalId],
        balance = this[TransactionsTable.balance],
        createdAt = this[TransactionsTable.createdAt],
        updatedAt = this[TransactionsTable.updatedAt]
    )
}
