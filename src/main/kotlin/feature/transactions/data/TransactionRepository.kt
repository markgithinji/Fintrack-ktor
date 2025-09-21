package feature.transactions.data

import com.fintrack.feature.summary.data.model.AccountAggregates
import com.fintrack.feature.summary.domain.CategoryComparison
import com.fintrack.feature.summary.domain.CategorySummary
import com.fintrack.feature.summary.domain.DaySummary
import com.fintrack.feature.summary.domain.DistributionSummary
import com.fintrack.feature.summary.domain.OverviewSummary
import core.AvailableMonths
import core.AvailableWeeks
import core.AvailableYears
import core.ValidationException
import feature.transactions.Highlight
import feature.transactions.Highlights
import feature.transactions.StatisticsSummary
import feature.transactions.domain.model.Transaction
import feature.transactions.validate
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.temporal.IsoFields
import java.time.temporal.WeekFields

class TransactionRepository {
    fun getAllCursor(
        userId: Int,
        accountId: Int? = null,
        isIncome: Boolean? = null,
        categories: List<String>? = null,
        start: LocalDateTime? = null,
        end: LocalDateTime? = null,
        sortBy: String = "dateTime",
        order: SortOrder = SortOrder.ASC,
        limit: Int = 20,
        afterDateTime: LocalDateTime? = null,
        afterId: Int? = null
    ): List<Transaction> = transaction {
        var query = TransactionsTable.selectAll()
            .andWhere { TransactionsTable.userId eq userId }
        // Filter by account
        if (accountId != null) query = query.andWhere { TransactionsTable.accountId eq accountId }
        // Apply filters
        if (isIncome != null) query = query.andWhere { TransactionsTable.isIncome eq isIncome }
        if (!categories.isNullOrEmpty()) query = query.andWhere { TransactionsTable.category inList categories }
        if (start != null) query = query.andWhere { TransactionsTable.dateTime greaterEq start.toJavaLocalDateTime() }
        if (end != null) query = query.andWhere { TransactionsTable.dateTime lessEq end.toJavaLocalDateTime() }
        // Cursor filter
        if (afterDateTime != null && afterId != null) {
            query = query.andWhere {
                (TransactionsTable.dateTime greater afterDateTime.toJavaLocalDateTime()) or
                        ((TransactionsTable.dateTime eq afterDateTime.toJavaLocalDateTime()) and
                                (TransactionsTable.id greater afterId))
            }
        }
        // Sorting
        val orderColumn = when (sortBy) {
            "amount" -> TransactionsTable.amount
            else -> TransactionsTable.dateTime
        }

        query.orderBy(orderColumn, order)
            .orderBy(TransactionsTable.id, order) // tie-breaker
            .limit(limit)
            .map { it.toTransaction() }
    }



    fun getById(id: Int, userId: Int): Transaction = transaction {
        TransactionsTable
            .selectAll().where { (TransactionsTable.id eq id) and (TransactionsTable.userId eq userId) }
            .map { it.toTransaction() }
            .singleOrNull()
            ?: throw NoSuchElementException("Transaction with id $id not found for user $userId")
    }


    fun add(entity: Transaction): Transaction = transaction {
        try {
            entity.validate()
        } catch (e: IllegalArgumentException) {
            throw ValidationException(e.message ?: "Invalid transaction")
        }
        val inserted = TransactionsTable.insert { row ->
            row[userId] = entity.userId
            row[accountId] = entity.accountId
            row[isIncome] = entity.isIncome
            row[amount] = entity.amount
            row[category] = entity.category
            row[dateTime] = entity.dateTime.toJavaLocalDateTime()
            row[description] = entity.description
        }.resultedValues?.singleOrNull()
            ?: throw IllegalStateException("Failed to insert transaction")

        inserted.toTransaction()
    }




    fun update(id: Int, userId: Int, entity: Transaction): Transaction = transaction {
        try {
            entity.validate()
        } catch (e: IllegalArgumentException) {
            throw ValidationException(e.message ?: "Invalid transaction")
        }
        val updated = TransactionsTable.update(
            where = { (TransactionsTable.id eq id) and (TransactionsTable.userId eq userId) }
        ) { row ->
            row[accountId] = entity.accountId
            row[isIncome] = entity.isIncome
            row[amount] = entity.amount
            row[category] = entity.category
            row[dateTime] = entity.dateTime.toJavaLocalDateTime()
            row[description] = entity.description
        }

        if (updated == 0) throw NoSuchElementException("Transaction with id $id not found for user $userId")

        getById(id, userId)
    }


    fun delete(id: Int, userId: Int): Boolean = transaction {
        val deleted = TransactionsTable.deleteWhere {
            (TransactionsTable.id eq id) and (TransactionsTable.userId eq userId)
        }
        if (deleted == 0) throw NoSuchElementException("Transaction with id $id not found for user $userId")
        true
    }









    fun clearAll(userId: Int, accountId: Int? = null): Boolean = transaction {
        val deleted = if (accountId != null) {
            TransactionsTable.deleteWhere {
                (TransactionsTable.userId eq userId) and (TransactionsTable.accountId eq accountId)
            }
        } else {
            TransactionsTable.deleteWhere { TransactionsTable.userId eq userId }
        }
        deleted > 0
    }







    private fun ResultRow.toTransaction() = Transaction(
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