package com.fintrack.feature.summary.data.repository

import com.fintrack.feature.summary.data.model.AccountAggregates
import com.fintrack.feature.summary.data.model.TransactionCountSummaryDto
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
import feature.transactions.data.TransactionsTable
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
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.temporal.IsoFields
import java.time.temporal.WeekFields
import kotlin.math.absoluteValue

class StatisticsRepository {
    fun getTransactionAmounts(userId: Int, accountId: Int? = null): List<Pair<Double, Boolean>> = transaction {
        val query = TransactionsTable
            .select(TransactionsTable.amount, TransactionsTable.isIncome)
            .where { TransactionsTable.userId eq userId }

        val filteredQuery = accountId?.let { query.andWhere { TransactionsTable.accountId eq it } } ?: query
        filteredQuery.map { it[TransactionsTable.amount] to it[TransactionsTable.isIncome] }
    }

    fun getTransactions(
        userId: Int,
        accountId: Int? = null,
        isIncome: Boolean? = null,
        start: LocalDateTime? = null,
        end: LocalDateTime? = null
    ): List<Transaction> = transaction {
        var query: Query = TransactionsTable.selectAll()
            .andWhere { TransactionsTable.userId eq userId }

        if (accountId != null) query = query.andWhere { TransactionsTable.accountId eq accountId }
        if (isIncome != null) query = query.andWhere { TransactionsTable.isIncome eq isIncome }
        if (start != null) query = query.andWhere { TransactionsTable.dateTime greaterEq start.toJavaLocalDateTime() }
        if (end != null) query = query.andWhere { TransactionsTable.dateTime lessEq end.toJavaLocalDateTime() }

        query.map { it.toTransaction() }
    }

    fun getAvailablePeriods(
        userId: Int,
        accountId: Int? = null,
        periodType: String
    ): List<String> = transaction {
        var query = TransactionsTable.selectAll().andWhere { TransactionsTable.userId eq userId }

        if (accountId != null) {
            query = query.andWhere { TransactionsTable.accountId eq accountId }
        }

        when (periodType) {
            "weeks" -> query
                .map { it[TransactionsTable.dateTime].toLocalDate() }
                .map { date ->
                    val year = date.year
                    val week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                    "%04d-W%02d".format(year, week)
                }
                .distinct()
                .sortedDescending()

            "months" -> query
                .map { it[TransactionsTable.dateTime].toLocalDate() }
                .map { date ->
                    val year = date.year
                    val month = date.monthValue
                    "%04d-%02d".format(year, month)
                }
                .distinct()
                .sortedDescending()

            "years" -> query
                .map { it[TransactionsTable.dateTime].toLocalDate().year.toString() }
                .distinct()
                .sortedDescending()

            else -> emptyList()
        }
    }

    fun getTransactionsByDateRange(
        userId: Int,
        start: LocalDate,
        end: LocalDate,
        accountId: Int? = null
    ): List<Transaction> = transaction {
        val startJ = java.time.LocalDateTime.of(start.year, start.monthNumber, start.dayOfMonth, 0, 0, 0, 0)
        val endJ = java.time.LocalDateTime.of(end.year, end.monthNumber, end.dayOfMonth, 23, 59, 59, 999_999_999)

        var query = TransactionsTable.selectAll().where {
            (TransactionsTable.userId eq userId) and
                    (TransactionsTable.dateTime greaterEq startJ) and
                    (TransactionsTable.dateTime lessEq endJ)
        }

        if (accountId != null) {
            query = query.andWhere { TransactionsTable.accountId eq accountId }
        }

        query.map { it.toTransaction() }
    }

    fun getCategoryTotals(
        userId: Int,
        start: LocalDate? = null,
        end: LocalDate? = null,
        accountId: Int? = null
    ): Map<String, Double> = transaction {
        var query = TransactionsTable.selectAll().where { TransactionsTable.userId eq userId }

        if (accountId != null) query = query.andWhere { TransactionsTable.accountId eq accountId }
        if (start != null) {
            val startJ = java.time.LocalDateTime.of(start.year, start.monthNumber, start.dayOfMonth, 0, 0)
            query = query.andWhere { TransactionsTable.dateTime greaterEq startJ }
        }
        if (end != null) {
            val endJ = java.time.LocalDateTime.of(end.year, end.monthNumber, end.dayOfMonth, 23, 59, 59)
            query = query.andWhere { TransactionsTable.dateTime lessEq endJ }
        }

        query
            .groupBy { it[TransactionsTable.category] }
            .mapValues { (_, rows) -> rows.sumOf { it[TransactionsTable.amount] } }
    }

    fun getTransactionCounts(
        userId: Int,
        accountId: Int? = null
    ): TransactionCounts = transaction {
        val incomeCount = TransactionsTable
            .selectAll()
            .where {
                (TransactionsTable.userId eq userId) and
                        (accountId?.let { TransactionsTable.accountId eq it } ?: Op.TRUE) and
                        (TransactionsTable.isIncome eq true)
            }
            .count()

        val expenseCount = TransactionsTable
            .selectAll()
            .where {
                (TransactionsTable.userId eq userId) and
                        (accountId?.let { TransactionsTable.accountId eq it } ?: Op.TRUE) and
                        (TransactionsTable.isIncome eq false)
            }
            .count()

        TransactionCounts(
            incomeCount = incomeCount.toInt(),
            expenseCount = expenseCount.toInt(),
            totalCount = (incomeCount + expenseCount).toInt()
        )
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

data class TransactionCounts(
    val incomeCount: Int,
    val expenseCount: Int,
    val totalCount: Int
)
