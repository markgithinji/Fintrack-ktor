package feature.summary.data.repository

import com.fintrack.feature.user.UsersTable
import com.fintrack.core.data.dbQuery
import com.fintrack.feature.accounts.data.table.AccountsTable
import feature.summary.domain.*
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

    override suspend fun getDailyTotals(
        userId: UUID,
        start: LocalDate,
        end: LocalDate,
        accountId: UUID?
    ): Map<LocalDate, DailyTotal> = dbQuery {
        val startInstant = start.atStartOfDayIn(TimeZone.UTC)
        val endInstant = end.atTime(23, 59, 59, 999_999_999).toInstant(TimeZone.UTC)

        val netAmount = Case()
            .When(TransactionsTable.isIncome eq true, TransactionsTable.amount - TransactionsTable.transactionCost)
            .Else(TransactionsTable.amount + TransactionsTable.transactionCost)

        var query = TransactionsTable
            .select(TransactionsTable.dateTime, TransactionsTable.isIncome, netAmount)
            .where {
                (TransactionsTable.userId eq EntityID(userId, UsersTable)) and
                        (TransactionsTable.dateTime greaterEq startInstant) and
                        (TransactionsTable.dateTime lessEq endInstant)
            }

        if (accountId != null) {
            query = query.andWhere {
                TransactionsTable.accountId eq EntityID(accountId, AccountsTable)
            }
        }

        query.map {
            val date = it[TransactionsTable.dateTime].toLocalDateTime(TimeZone.UTC).date
            val isIncome = it[TransactionsTable.isIncome]
            val amount = it[netAmount] ?: java.math.BigDecimal.ZERO
            Triple(date, isIncome, amount)
        }
            .groupBy { it.first }
            .mapValues { (_, values) ->
                val income = values.filter { it.second }.fold(java.math.BigDecimal.ZERO) { acc, t -> acc + t.third }
                val expense = values.filter { !it.second }.fold(java.math.BigDecimal.ZERO) { acc, t -> acc + t.third }
                DailyTotal(income, expense)
            }
    }

    override suspend fun getMonthlyCategoryStats(
        userId: UUID,
        start: LocalDate,
        end: LocalDate,
        accountId: UUID?
    ): Map<String, Map<String, CategoryStats>> = dbQuery {
        val startInstant = start.atStartOfDayIn(TimeZone.UTC)
        val endInstant = end.atTime(23, 59, 59, 999_999_999).toInstant(TimeZone.UTC)

        val netAmount = Case()
            .When(TransactionsTable.isIncome eq true, TransactionsTable.amount - TransactionsTable.transactionCost)
            .Else(TransactionsTable.amount + TransactionsTable.transactionCost)

        var query = TransactionsTable
            .select(TransactionsTable.dateTime, TransactionsTable.category, TransactionsTable.isIncome, netAmount)
            .where {
                (TransactionsTable.userId eq EntityID(userId, UsersTable)) and
                        (TransactionsTable.dateTime greaterEq startInstant) and
                        (TransactionsTable.dateTime lessEq endInstant)
            }

        if (accountId != null) {
            query = query.andWhere {
                TransactionsTable.accountId eq EntityID(accountId, AccountsTable)
            }
        }

        query.map {
            val date = it[TransactionsTable.dateTime].toLocalDateTime(TimeZone.UTC)
            val period = "%04d-%02d".format(date.year, date.monthNumber)
            val isIncome = it[TransactionsTable.isIncome]
            val category = if (isIncome) "__INCOME__" else it[TransactionsTable.category].trim().lowercase()
            val amount = it[netAmount] ?: java.math.BigDecimal.ZERO
            Triple(period, category, amount)
        }
            .groupBy { it.first }
            .mapValues { (_, items) ->
                items.groupBy { it.second }
                    .mapValues { (_, groupItems) ->
                        CategoryStats(
                            totalAmount = groupItems.fold(java.math.BigDecimal.ZERO) { acc, t -> acc + t.third },
                            count = groupItems.size
                        )
                    }
            }
    }

    override suspend fun getTopDescriptions(
        userId: UUID,
        start: LocalDate,
        end: LocalDate,
        accountId: UUID?,
        isIncome: Boolean?,
        limit: Int
    ): Map<String, List<DescriptionTotal>> = dbQuery {
        val startInstant = start.atStartOfDayIn(TimeZone.UTC)
        val endInstant = end.atTime(23, 59, 59, 999_999_999).toInstant(TimeZone.UTC)

        val netAmount = Case()
            .When(TransactionsTable.isIncome eq true, TransactionsTable.amount - TransactionsTable.transactionCost)
            .Else(TransactionsTable.amount + TransactionsTable.transactionCost)

        val totalSum = netAmount.sum()

        var query = TransactionsTable
            .select(TransactionsTable.category, TransactionsTable.description, totalSum)
            .where {
                (TransactionsTable.userId eq EntityID(userId, UsersTable)) and
                        (TransactionsTable.dateTime greaterEq startInstant) and
                        (TransactionsTable.dateTime lessEq endInstant) and
                        (TransactionsTable.description.isNotNull())
            }

        if (accountId != null) query =
            query.andWhere { TransactionsTable.accountId eq EntityID(accountId, AccountsTable) }
        if (isIncome != null) query = query.andWhere { TransactionsTable.isIncome eq isIncome }

        query.groupBy(TransactionsTable.category, TransactionsTable.description)
            .map {
                val cat = it[TransactionsTable.category]
                val desc = it[TransactionsTable.description]!!
                val sum = it[totalSum] ?: java.math.BigDecimal.ZERO
                cat to DescriptionTotal(desc, sum)
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, totals) ->
                totals.sortedByDescending { it.totalAmount }.take(limit * 2)
            }
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
    ): Map<String, java.math.BigDecimal> =
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
                .associateBy({ it[TransactionsTable.category] }, { it[totalSum] ?: java.math.BigDecimal.ZERO })
        }

    override suspend fun getTransactionCounts(
        userId: UUID,
        accountId: UUID?,
        isIncome: Boolean?,
        categoryIds: List<UUID>?,
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
            if (!categoryIds.isNullOrEmpty()) {
                cond = cond and (TransactionsTable.categoryId inList categoryIds)
            }
            if (hasCost == true) cond = cond and (TransactionsTable.transactionCost greater java.math.BigDecimal.ZERO)
            if (hasCost == false) cond = cond and (TransactionsTable.transactionCost eq java.math.BigDecimal.ZERO)
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
            .singleOrNull()?.get(costSum) ?: java.math.BigDecimal.ZERO

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
        categoryId = this[TransactionsTable.categoryId].value,
        dateTime = this[TransactionsTable.dateTime],
        description = this[TransactionsTable.description],
        accountId = this[TransactionsTable.accountId].value,
        externalId = this[TransactionsTable.externalId],
        balance = this[TransactionsTable.balance]
    )
}
