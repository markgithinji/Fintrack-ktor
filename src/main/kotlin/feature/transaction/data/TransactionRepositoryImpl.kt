package feature.transaction.data

import com.fintrack.feature.user.UsersTable
import core.dbQuery
import feature.accounts.data.AccountsTable
import feature.transaction.domain.TransactionRepository
import feature.transaction.domain.model.Transaction
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class TransactionRepositoryImpl : TransactionRepository {
    override suspend fun getAllCursor(
        userId: UUID,
        accountId: UUID?,
        isIncome: Boolean?,
        categories: List<String>?,
        start: LocalDateTime?,
        end: LocalDateTime?,
        sortBy: String,
        order: SortOrder,
        limit: Int,
        afterDateTime: LocalDateTime?,
        afterId: UUID?
    ): List<Transaction> = dbQuery {
        var query = TransactionsTable.selectAll()
            .andWhere { TransactionsTable.userId eq EntityID(userId, UsersTable) }

        if (accountId != null) query =
            query.andWhere { TransactionsTable.accountId eq EntityID(accountId, AccountsTable) }
        if (isIncome != null) query = query.andWhere { TransactionsTable.isIncome eq isIncome }
        if (!categories.isNullOrEmpty()) query = query.andWhere { TransactionsTable.category inList categories }
        if (start != null) query = query.andWhere { TransactionsTable.dateTime greaterEq start.toJavaLocalDateTime() }
        if (end != null) query = query.andWhere { TransactionsTable.dateTime lessEq end.toJavaLocalDateTime() }

        if (afterDateTime != null && afterId != null) {
            val afterDateTimeJava = afterDateTime.toJavaLocalDateTime()
            query = query.andWhere {
                (TransactionsTable.dateTime greater afterDateTimeJava) or
                        ((TransactionsTable.dateTime eq afterDateTimeJava) and
                                (TransactionsTable.id greater afterId))
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

    override suspend fun getById(id: UUID, userId: UUID): Transaction = dbQuery {
        TransactionsTable
            .selectAll().where {
                (TransactionsTable.id eq EntityID(id, TransactionsTable)) and
                        (TransactionsTable.userId eq EntityID(userId, UsersTable))
            }
            .map { it.toTransaction() }
            .singleOrNull()
            ?: throw NoSuchElementException("Transaction with id $id not found for user $userId")
    }

    override suspend fun add(entity: Transaction): Transaction = dbQuery {
        val inserted = TransactionsTable.insert { row ->
            row[id] = EntityID(entity.id ?: UUID.randomUUID(), TransactionsTable)
            row[userId] = EntityID(entity.userId, UsersTable)
            row[accountId] = EntityID(entity.accountId, AccountsTable)
            row[isIncome] = entity.isIncome
            row[amount] = entity.amount
            row[category] = entity.category
            row[dateTime] = entity.dateTime.toJavaLocalDateTime()
            row[description] = entity.description
        }.resultedValues?.singleOrNull()
            ?: throw IllegalStateException("Failed to insert transaction")

        inserted.toTransaction()
    }

    override suspend fun update(id: UUID, userId: UUID, entity: Transaction): Transaction = dbQuery {
        val updated = TransactionsTable.update(
            where = {
                (TransactionsTable.id eq EntityID(id, TransactionsTable)) and
                        (TransactionsTable.userId eq EntityID(userId, UsersTable))
            }
        ) { row ->
            row[accountId] = EntityID(entity.accountId, AccountsTable)
            row[isIncome] = entity.isIncome
            row[amount] = entity.amount
            row[category] = entity.category
            row[dateTime] = entity.dateTime.toJavaLocalDateTime()
            row[description] = entity.description
        }

        if (updated == 0) throw NoSuchElementException("Transaction with id $id not found for user $userId")
        // Re-fetch the updated transaction
        TransactionsTable
            .selectAll().where {
                (TransactionsTable.id eq EntityID(id, TransactionsTable)) and
                        (TransactionsTable.userId eq EntityID(userId, UsersTable))
            }
            .map { it.toTransaction() }
            .singleOrNull()
            ?: throw NoSuchElementException("Transaction with id $id not found after update")
    }

    override suspend fun delete(id: UUID, userId: UUID): Boolean = dbQuery {
        val deleted = TransactionsTable.deleteWhere {
            (TransactionsTable.id eq EntityID(id, TransactionsTable)) and
                    (TransactionsTable.userId eq EntityID(userId, UsersTable))
        }
        if (deleted == 0) throw NoSuchElementException("Transaction with id $id not found for user $userId")
        true
    }

    override suspend fun clearAll(userId: UUID, accountId: UUID?): Boolean = dbQuery {
        val deleted = if (accountId != null) {
            TransactionsTable.deleteWhere {
                (TransactionsTable.userId eq EntityID(userId, UsersTable)) and
                        (TransactionsTable.accountId eq EntityID(accountId, AccountsTable))
            }
        } else {
            TransactionsTable.deleteWhere { TransactionsTable.userId eq EntityID(userId, UsersTable) }
        }
        deleted > 0
    }

    private fun ResultRow.toTransaction() = Transaction(
        id = this[TransactionsTable.id].value,
        userId = this[TransactionsTable.userId].value,
        isIncome = this[TransactionsTable.isIncome],
        amount = this[TransactionsTable.amount],
        category = this[TransactionsTable.category],
        dateTime = this[TransactionsTable.dateTime].toKotlinLocalDateTime(),
        description = this[TransactionsTable.description],
        accountId = this[TransactionsTable.accountId].value
    )
}