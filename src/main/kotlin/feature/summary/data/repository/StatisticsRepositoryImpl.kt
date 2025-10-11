package com.fintrack.feature.summary.data.repository

import com.fintrack.feature.user.UsersTable
import core.dbQuery
import feature.accounts.data.AccountsTable
import feature.summary.domain.StatisticsRepository
import feature.transaction.data.TransactionsTable
import feature.transaction.domain.model.Transaction
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
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
        start: LocalDateTime?,
        end: LocalDateTime?
    ): List<Transaction> =
        dbQuery {
            var query: Query = TransactionsTable.selectAll()
                .andWhere { TransactionsTable.userId eq EntityID(userId, UsersTable) }

            if (accountId != null) query =
                query.andWhere { TransactionsTable.accountId eq EntityID(accountId, AccountsTable) }
            if (isIncome != null) query = query.andWhere { TransactionsTable.isIncome eq isIncome }
            if (start != null) query =
                query.andWhere { TransactionsTable.dateTime greaterEq start.toJavaLocalDateTime() }
            if (end != null) query =
                query.andWhere { TransactionsTable.dateTime lessEq end.toJavaLocalDateTime() }

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

    override suspend fun getTransactionsByDateRange(
        userId: UUID,
        start: LocalDate,
        end: LocalDate,
        accountId: UUID?
    ): List<Transaction> =
        dbQuery {
            val startJ = java.time.LocalDateTime.of(
                start.year,
                start.monthNumber,
                start.dayOfMonth,
                0,
                0,
                0,
                0
            )
            val endJ = java.time.LocalDateTime.of(
                end.year,
                end.monthNumber,
                end.dayOfMonth,
                23,
                59,
                59,
                999_999_999
            )

            var query = TransactionsTable.selectAll().where {
                (TransactionsTable.userId eq EntityID(userId, UsersTable)) and
                        (TransactionsTable.dateTime greaterEq startJ) and
                        (TransactionsTable.dateTime lessEq endJ)
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
                val startJ = java.time.LocalDateTime.of(
                    start.year,
                    start.monthNumber,
                    start.dayOfMonth,
                    0,
                    0
                )
                query = query.andWhere { TransactionsTable.dateTime greaterEq startJ }
            }
            if (end != null) {
                val endJ = java.time.LocalDateTime.of(
                    end.year,
                    end.monthNumber,
                    end.dayOfMonth,
                    23,
                    59,
                    59
                )
                query = query.andWhere { TransactionsTable.dateTime lessEq endJ }
            }

            query
                .groupBy { it[TransactionsTable.category] }
                .mapValues { (_, rows) -> rows.sumOf { it[TransactionsTable.amount] } }
        }

    override suspend fun getTransactionCounts(
        userId: UUID,
        accountId: UUID?
    ): TransactionCounts =
        dbQuery {
            val incomeCount = TransactionsTable
                .selectAll()
                .where {
                    (TransactionsTable.userId eq EntityID(userId, UsersTable)) and
                            (accountId?.let {
                                TransactionsTable.accountId eq EntityID(
                                    it,
                                    AccountsTable
                                )
                            } ?: Op.TRUE) and
                            (TransactionsTable.isIncome eq true)
                }
                .count()

            val expenseCount = TransactionsTable
                .selectAll()
                .where {
                    (TransactionsTable.userId eq EntityID(userId, UsersTable)) and
                            (accountId?.let {
                                TransactionsTable.accountId eq EntityID(
                                    it,
                                    AccountsTable
                                )
                            } ?: Op.TRUE) and
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

data class TransactionCounts(
    val incomeCount: Int,
    val expenseCount: Int,
    val totalCount: Int
)