package feature.summary.data.repository

import com.fintrack.feature.user.UsersTable
import core.dbQuery
import com.fintrack.feature.accounts.data.table.AccountsTable
import feature.summary.domain.StatisticsRepository
import feature.summary.domain.TransactionCounts
import feature.transaction.data.table.TransactionsTable
import feature.transaction.domain.model.Transaction
import kotlinx.datetime.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.SqlExpressionBuilder.minus
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
        periodType: String,
    ): List<String> =
        dbQuery {
            val query = TransactionsTable.select(TransactionsTable.dateTime)
                .where { TransactionsTable.userId eq EntityID(userId, UsersTable) }

            if (accountId != null) {
                query.andWhere {
                    TransactionsTable.accountId eq EntityID(
                        accountId,
                        AccountsTable
                    )
                }
            }

            query.map { it[TransactionsTable.dateTime] }
                .asSequence()
                .map { it.toLocalDateTime(TimeZone.UTC).toJavaLocalDateTime().toLocalDate() }
                .map { date ->
                    when (periodType) {
                        "weeks" -> {
                            val year = date.year
                            val week = date[IsoFields.WEEK_OF_WEEK_BASED_YEAR]
                            "%04d-W%02d".format(year, week)
                        }
                        "months" -> "%04d-%02d".format(date.year, date.monthValue)
                        "years" -> date.year.toString()
                        else -> ""
                    }
                }
                .filter { it.isNotEmpty() }
                .distinct()
                .toList()
                .sortedDescending()
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
            val netAmount = Case()
                .When(TransactionsTable.isIncome eq true, TransactionsTable.amount - TransactionsTable.transactionCost)
                .Else(TransactionsTable.amount + TransactionsTable.transactionCost)
            
            val totalSum = netAmount.sum()

            var query = TransactionsTable
                .select(TransactionsTable.category, totalSum)
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
                val endInstant = end.atTime(23, 59, 59, 999_999_999).toInstant(TimeZone.UTC)
                query = query.andWhere { TransactionsTable.dateTime lessEq endInstant }
            }

            query
                .groupBy(TransactionsTable.category)
                .associateBy({ it[TransactionsTable.category] }, { it[totalSum] ?: 0.0 })
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
            hasCost == true -> 0L // Income usually doesn't have transaction cost
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
