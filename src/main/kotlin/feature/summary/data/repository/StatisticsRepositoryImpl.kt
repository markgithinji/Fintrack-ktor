package com.fintrack.feature.summary.data.repository

import com.fintrack.feature.user.UsersTable
import core.dbQuery
import feature.accounts.data.AccountsTable
import feature.summary.domain.StatisticsRepository
import feature.transaction.data.TransactionsTable
import feature.transaction.domain.model.Transaction
import kotlinx.datetime.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import java.time.temporal.IsoFields
import java.util.UUID

class StatisticsRepositoryImpl : StatisticsRepository {
    override suspend fun getTransactions(
        userId: UUID,
        accountId: UUID?,
        isIncome: Boolean?,
        start: Instant?,
        end: Instant?,
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
                "weeks" -> query.asSequence()
                    .map { it[TransactionsTable.dateTime].toLocalDateTime(TimeZone.UTC).toJavaLocalDateTime().toLocalDate() }
                    .map { date ->
                        val year = date.year
                        val week = date[IsoFields.WEEK_OF_WEEK_BASED_YEAR]
                        "%04d-W%02d".format(year, week)
                    }
                    .distinct()
                    .toList()
                    .sortedDescending()

                "months" -> query.asSequence()
                    .map { it[TransactionsTable.dateTime].toLocalDateTime(TimeZone.UTC).toJavaLocalDateTime().toLocalDate() }
                    .map { date ->
                        val year = date.year
                        val month = date.monthValue
                        "%04d-%02d".format(year, month)
                    }
                    .distinct()
                    .toList()
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
        accountId: UUID?,
        isIncome: Boolean?
    ): Map<String, Double> =
        dbQuery {
            var query = TransactionsTable.selectAll()
                .where { TransactionsTable.userId eq EntityID(userId, UsersTable) }

            if (accountId != null) query =
                query.andWhere { TransactionsTable.accountId eq EntityID(accountId, AccountsTable) }
            if (isIncome != null) query =
                query.andWhere { TransactionsTable.isIncome eq isIncome }
            
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
                        // If it's an income, amount - cost is the net gain.
                        // If it's an expense, amount + cost is the total loss.
                        // However, for trends we usually want absolute impact.
                        if (isInc) amt - cost else amt + cost
                    }
                }
        }

    override suspend fun getTransactionCounts(
        userId: UUID,
        accountId: UUID?,
        isIncome: Boolean?,
        category: String?,
        hasCost: Boolean?,
        start: Instant?,
        end: Instant?
    ): TransactionCounts = dbQuery {
        val baseCondition = {
            var cond = (TransactionsTable.userId eq EntityID(userId, UsersTable)) and
                    (accountId?.let {
                        TransactionsTable.accountId eq EntityID(it, AccountsTable)
                    } ?: Op.TRUE)
            
            if (start != null) cond = cond and (TransactionsTable.dateTime greaterEq start)
            if (end != null) cond = cond and (TransactionsTable.dateTime lessEq end)
            if (category != null) {
                val categoryCond = if (category.contains(",")) {
                    TransactionsTable.category inList category.split(",").map { it.trim() }
                } else {
                    TransactionsTable.category eq category
                }
                cond = cond and categoryCond
            }
            if (hasCost == true) cond = cond and (TransactionsTable.transactionCost greater 0.0)
            if (hasCost == false) cond = cond and (TransactionsTable.transactionCost eq 0.0)
            cond
        }

        val incomeCount = when {
            isIncome == false -> 0L
            hasCost == true -> 0L // Income usually doesn't have transaction cost in this app's logic
            else -> TransactionsTable
                .selectAll()
                .where { baseCondition() and (TransactionsTable.isIncome eq true) }
                .count()
        }

        val expenseCount = when {
            isIncome == true -> 0L
            else -> TransactionsTable
                .selectAll()
                .where { baseCondition() and (TransactionsTable.isIncome eq false) }
                .count()
        }

        val costSum = TransactionsTable.transactionCost.sum()
        val totalCost = TransactionsTable
            .select(costSum)
            .where {
                val cond = baseCondition()
                // If hasCost is true, we already filtered for cost > 0 in baseCondition.
                // We just need to make sure we don't double filter isIncome if it was already handled.
                if (isIncome != null) cond and (TransactionsTable.isIncome eq isIncome) else cond
            }
            .singleOrNull()?.get(costSum) ?: 0.0

        TransactionCounts(
            incomeCount = incomeCount.toInt(),
            expenseCount = expenseCount.toInt(),
            totalCount = (incomeCount + expenseCount).toInt(),
            totalTransactionCost = totalCost
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
        accountId = this[TransactionsTable.accountId].value,
        externalId = this[TransactionsTable.externalId],
        balance = this[TransactionsTable.balance]
    )
}

data class TransactionCounts(
    val incomeCount: Int,
    val expenseCount: Int,
    val totalCount: Int,
    val totalTransactionCost: Double = 0.0
)