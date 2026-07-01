package com.fintrack.feature.summary.data.repository

import com.fintrack.feature.user.UsersTable
import core.dbQuery
import feature.accounts.data.AccountsTable
import feature.summary.domain.StatisticsRepository
import feature.transaction.data.TransactionsTable
import feature.transaction.domain.model.Transaction
import kotlinx.datetime.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll
import java.time.temporal.IsoFields
import java.util.UUID

class StatisticsRepositoryImpl : StatisticsRepository {
    override suspend fun getTransactions(
        userId: UUID,
        accountId: UUID?,
        isIncome: Boolean?,
        start: Instant?,
        end: Instant?
    ): List<Transaction> =
        dbQuery {
            var query: Query = TransactionsTable.selectAll()
                .andWhere { TransactionsTable.userId eq EntityID(userId, UsersTable) }

            if (accountId != null) query =
                query.andWhere { TransactionsTable.accountId eq EntityID(accountId, AccountsTable) }
            if (isIncome != null) query = query.andWhere { TransactionsTable.isIncome eq isIncome }
            if (start != null) query =
                query.andWhere { TransactionsTable.dateTime greaterEq start }
            if (end != null) query =
                query.andWhere { TransactionsTable.dateTime lessEq end }

            query.map { it.toTransaction() }
        }

    override suspend fun getAvailablePeriods(
        userId: UUID,
        accountId: UUID?,
        periodType: String
    ): List<String> =
        dbQuery {
            var query = TransactionsTable.selectAll()
                .andWhere { TransactionsTable.userId eq EntityID(userId, UsersTable) }

            if (accountId != null) {
                query = query.andWhere {
                    TransactionsTable.accountId eq EntityID(
                        accountId,
                        AccountsTable
                    )
                }
            }

            when (periodType) {
                "weeks" -> query
                    .map { it[TransactionsTable.dateTime].toLocalDateTime(TimeZone.UTC).toJavaLocalDateTime().toLocalDate() }
                    .map { date ->
                        val year = date.year
                        val week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                        "%04d-W%02d".format(year, week)
                    }
                    .distinct()
                    .sortedDescending()

                "months" -> query
                    .map { it[TransactionsTable.dateTime].toLocalDateTime(TimeZone.UTC).toJavaLocalDateTime().toLocalDate() }
                    .map { date ->
                        val year = date.year
                        val month = date.monthValue
                        "%04d-%02d".format(year, month)
                    }
                    .distinct()
                    .sortedDescending()

                "years" -> query
                    .map { it[TransactionsTable.dateTime].toLocalDateTime(TimeZone.UTC).year.toString() }
                    .distinct()
                    .sortedDescending()

                else -> emptyList()
            }
        }

    override suspend fun getTransactionsByDateRange(
        userId: UUID,
        start: LocalDate,
        end: LocalDate,
        accountId: UUID?
    ): List<Transaction> =
        dbQuery {
            val startInstant = start.atStartOfDayIn(TimeZone.UTC)
            val endInstant = end.atTime(23, 59, 59, 999_999_999).toInstant(TimeZone.UTC)

            var query = TransactionsTable.selectAll().where {
                (TransactionsTable.userId eq EntityID(userId, UsersTable)) and
                        (TransactionsTable.dateTime greaterEq startInstant) and
                        (TransactionsTable.dateTime lessEq endInstant)
            }

            if (accountId != null) {
                query = query.andWhere {
                    TransactionsTable.accountId eq EntityID(
                        accountId,
                        AccountsTable
                    )
                }
            }

            query.map { it.toTransaction() }
        }

    override suspend fun getCategoryTotals(
        userId: UUID,
        start: LocalDate?,
        end: LocalDate?,
        accountId: UUID?
    ): Map<String, Double> =
        dbQuery {
            var query = TransactionsTable.selectAll()
                .where { TransactionsTable.userId eq EntityID(userId, UsersTable) }

            if (accountId != null) query =
                query.andWhere { TransactionsTable.accountId eq EntityID(accountId, AccountsTable) }
            if (start != null) {
                val startInstant = start.atStartOfDayIn(TimeZone.UTC)
                query = query.andWhere { TransactionsTable.dateTime greaterEq startInstant }
            }
            if (end != null) {
                val endInstant = end.atTime(23, 59, 59).toInstant(TimeZone.UTC)
                query = query.andWhere { TransactionsTable.dateTime lessEq endInstant }
            }

            query
                .groupBy { it[TransactionsTable.category] }
                .mapValues { (_, rows) ->
                    rows.sumOf {
                        val amt = it[TransactionsTable.amount]
                        val cost = it[TransactionsTable.transactionCost]
                        val isInc = it[TransactionsTable.isIncome]
                        if (isInc) amt - cost else amt + cost
                    }
                }
        }

    override suspend fun getTransactionCounts(
        userId: UUID,
        accountId: UUID?,
        isIncome: Boolean?
    ): TransactionCounts = dbQuery {
        val baseCondition = { ->
            (TransactionsTable.userId eq EntityID(userId, UsersTable)) and
                    (accountId?.let {
                        TransactionsTable.accountId eq EntityID(it, AccountsTable)
                    } ?: Op.TRUE)
        }

        val incomeCount = when (isIncome) {
            true -> TransactionsTable
                .selectAll()
                .where { baseCondition() and (TransactionsTable.isIncome eq true) }
                .count()

            false -> 0L // If filtering for expenses only, income count is 0

            null -> TransactionsTable // No filter, count all income
                .selectAll()
                .where { baseCondition() and (TransactionsTable.isIncome eq true) }
                .count()
        }

        val expenseCount = when (isIncome) {
            false -> TransactionsTable
                .selectAll()
                .where { baseCondition() and (TransactionsTable.isIncome eq false) }
                .count()

            true -> 0L // If filtering for income only, expense count is 0

            null -> TransactionsTable // No filter, count all expenses
                .selectAll()
                .where { baseCondition() and (TransactionsTable.isIncome eq false) }
                .count()
        }

        TransactionCounts(
            incomeCount = incomeCount.toInt(),
            expenseCount = expenseCount.toInt(),
            totalCount = (incomeCount + expenseCount).toInt()
        )
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
        accountId = this[TransactionsTable.accountId].value
    )
}

data class TransactionCounts(
    val incomeCount: Int,
    val expenseCount: Int,
    val totalCount: Int
)