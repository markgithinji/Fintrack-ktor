package feature.transaction.data.repository

import com.fintrack.core.data.dbQuery
import core.util.IdGenerator
import feature.transaction.data.table.TransactionsTable
import feature.transaction.domain.TransactionRepository
import feature.transaction.domain.model.Transaction
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import java.util.*

class TransactionRepositoryImpl : TransactionRepository {
    override suspend fun getAllCursor(
        userId: UUID,
        accountId: UUID?,
        isIncome: Boolean?,
        categories: List<String>?,
        start: Instant?,
        end: Instant?,
        sortBy: String,
        order: SortOrder,
        limit: Int,
        afterDateTime: Instant?,
        afterId: UUID?,
        hasTransactionCost: Boolean?
    ): List<Transaction> = dbQuery {
        var query = TransactionsTable.selectAll()
            .andWhere { TransactionsTable.userId eq userId }

        if (accountId != null) query =
            query.andWhere { TransactionsTable.accountId eq accountId }
        if (isIncome != null) query = query.andWhere { TransactionsTable.isIncome eq isIncome }
        if (!categories.isNullOrEmpty()) query = query.andWhere { TransactionsTable.category inList categories }
        if (start != null) query = query.andWhere { TransactionsTable.dateTime greaterEq start }
        if (end != null) query = query.andWhere { TransactionsTable.dateTime lessEq end }
        if (hasTransactionCost == true) query = query.andWhere { TransactionsTable.transactionCost greater 0.0 }
        if (hasTransactionCost == false) query = query.andWhere { TransactionsTable.transactionCost eq 0.0 }

        if (afterDateTime != null && afterId != null) {
            query = if (order == SortOrder.DESC) {
                // For DESC order: get records that are "less than" (older than) the cursor
                query.andWhere {
                    (TransactionsTable.dateTime less afterDateTime) or
                            ((TransactionsTable.dateTime eq afterDateTime) and
                                    (TransactionsTable.id less afterId))
                }
            } else {
                // For ASC order: get records that are "greater than" (newer than) the cursor
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
        TransactionsTable
            .selectAll().where {
                (TransactionsTable.id eq id) and
                        (TransactionsTable.userId eq userId)
            }
            .map { it.toTransaction() }
            .singleOrNull()
    }

    override suspend fun add(entity: Transaction): Transaction = dbQuery {
        val inserted = TransactionsTable.insert { row ->
            row[TransactionsTable.id] = entity.id ?: IdGenerator.nextId()
            row[TransactionsTable.userId] = entity.userId
            row[TransactionsTable.accountId] = entity.accountId
            row[TransactionsTable.isIncome] = entity.isIncome
            row[TransactionsTable.amount] = entity.amount
            row[TransactionsTable.transactionCost] = entity.transactionCost
            row[TransactionsTable.category] = entity.category
            row[TransactionsTable.dateTime] = entity.dateTime
            row[TransactionsTable.description] = entity.description
            row[TransactionsTable.externalId] = entity.externalId
            row[TransactionsTable.balance] = entity.balance
        }.resultedValues?.singleOrNull()
            ?: throw IllegalStateException("Failed to insert transaction")

        inserted.toTransaction()
    }

    override suspend fun update(id: UUID, userId: UUID, entity: Transaction): Transaction? = dbQuery {
        val updated = TransactionsTable.update(
            where = {
                (TransactionsTable.id eq id) and
                        (TransactionsTable.userId eq userId)
            }
        ) { row ->
            row[TransactionsTable.accountId] = entity.accountId
            row[TransactionsTable.isIncome] = entity.isIncome
            row[TransactionsTable.amount] = entity.amount
            row[TransactionsTable.transactionCost] = entity.transactionCost
            row[TransactionsTable.category] = entity.category
            row[TransactionsTable.dateTime] = entity.dateTime
            row[TransactionsTable.description] = entity.description
            row[TransactionsTable.externalId] = entity.externalId
            row[TransactionsTable.balance] = entity.balance
        }

        if (updated == 0) return@dbQuery null
        
        // Re-fetch the updated transaction
        TransactionsTable
            .selectAll().where {
                (TransactionsTable.id eq id) and
                        (TransactionsTable.userId eq userId)
            }
            .map { it.toTransaction() }
            .singleOrNull()
    }

    override suspend fun delete(id: UUID, userId: UUID): Boolean = dbQuery {
        val deleted = TransactionsTable.deleteWhere {
            (TransactionsTable.id eq id) and
                    (TransactionsTable.userId eq userId)
        }
        deleted > 0
    }

    override suspend fun clearAll(userId: UUID, accountIds: List<UUID>?): Boolean = dbQuery {
        val deleted = if (!accountIds.isNullOrEmpty()) {
            TransactionsTable.deleteWhere {
                (TransactionsTable.userId eq userId) and
                        (TransactionsTable.accountId inList accountIds)
            }
        } else {
            TransactionsTable.deleteWhere { TransactionsTable.userId eq userId }
        }
        deleted > 0
    }

    override suspend fun addBulk(entities: List<Transaction>): List<Transaction> = dbQuery {
        TransactionsTable.batchInsert(entities, ignore = true) { entity ->
            this[TransactionsTable.id] = entity.id ?: IdGenerator.nextId()
            this[TransactionsTable.userId] = entity.userId
            this[TransactionsTable.accountId] = entity.accountId
            this[TransactionsTable.isIncome] = entity.isIncome
            this[TransactionsTable.amount] = entity.amount
            this[TransactionsTable.transactionCost] = entity.transactionCost
            this[TransactionsTable.category] = entity.category
            this[TransactionsTable.dateTime] = entity.dateTime
            this[TransactionsTable.description] = entity.description
            this[TransactionsTable.externalId] = entity.externalId
            this[TransactionsTable.balance] = entity.balance
        }.map { it.toTransaction() }
    }

    override suspend fun getLatestBalance(userId: UUID, accountId: UUID?): Double? = dbQuery {
        val query = TransactionsTable.selectAll()
            .where { TransactionsTable.userId eq userId }
            .andWhere { TransactionsTable.balance.isNotNull() }

        if (accountId != null) {
            query.andWhere { TransactionsTable.accountId eq accountId }
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
        category = this[TransactionsTable.category],
        dateTime = this[TransactionsTable.dateTime],
        description = this[TransactionsTable.description],
        accountId = this[TransactionsTable.accountId].value,
        externalId = this[TransactionsTable.externalId],
        balance = this[TransactionsTable.balance]
    )
}
