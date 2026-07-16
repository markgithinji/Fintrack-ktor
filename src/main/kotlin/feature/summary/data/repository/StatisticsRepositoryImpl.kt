package feature.summary.data.repository

import com.fintrack.core.data.dbQuery
import com.fintrack.feature.accounts.data.table.AccountsTable
import com.fintrack.feature.user.UsersTable
import feature.category.data.table.CategoriesTable
import feature.summary.domain.CategoryStats
import feature.summary.domain.DailyTotal
import feature.summary.domain.DescriptionTotal
import feature.summary.domain.StatisticsRepository
import feature.summary.domain.TransactionCounts
import feature.transaction.data.table.TransactionsTable
import feature.transaction.domain.model.Transaction
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Case
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.minus
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.intLiteral
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.sum
import java.time.temporal.IsoFields
import java.util.UUID

class StatisticsRepositoryImpl : StatisticsRepository {
    private val joinTable = TransactionsTable.leftJoin(CategoriesTable)

    override suspend fun getTransactions(
        userId: UUID,
        accountId: UUID?,
        isIncome: Boolean?,
        start: Instant?,
        end: Instant?,
    ): List<Transaction> =
        dbQuery {
            var query = joinTable.selectAll()
                .where { TransactionsTable.userId eq EntityID(userId, UsersTable) }

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
            .When(
                TransactionsTable.isIncome eq true,
                TransactionsTable.amount - TransactionsTable.transactionCost
            )
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
            val amount = it[netAmount]
            Triple(date, isIncome, amount)
        }
            .groupBy { it.first }
            .mapValues { (_, values) ->
                val income = values.filter { it.second }
                    .fold(java.math.BigDecimal.ZERO) { acc, t -> acc + t.third }
                val expense = values.filter { !it.second }
                    .fold(java.math.BigDecimal.ZERO) { acc, t -> acc + t.third }
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
            .When(
                TransactionsTable.isIncome eq true,
                TransactionsTable.amount - TransactionsTable.transactionCost
            )
            .Else(TransactionsTable.amount + TransactionsTable.transactionCost)

        var query = joinTable
            .select(
                TransactionsTable.dateTime,
                CategoriesTable.id,
                CategoriesTable.name,
                TransactionsTable.isIncome,
                netAmount
            )
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
            val categoryId = it[CategoriesTable.id].value
            val categoryName = it[CategoriesTable.name].trim().lowercase()
            val amount = it[netAmount]

            val stats = CategoryStats(
                categoryId = categoryId,
                name = it[CategoriesTable.name],
                totalAmount = amount,
                count = 1,
                isIncome = isIncome
            )

            period to (categoryName to stats)
        }
            .groupBy { it.first }
            .mapValues { (_, items) ->
                items.map { it.second }
                    .groupBy { it.first }
                    .mapValues { (catName, groupItems) ->
                        val first = groupItems.first().second
                        CategoryStats(
                            categoryId = first.categoryId,
                            name = first.name,
                            totalAmount = groupItems.fold(java.math.BigDecimal.ZERO) { acc, t -> acc + t.second.totalAmount },
                            count = groupItems.size,
                            isIncome = first.isIncome
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
            .When(
                TransactionsTable.isIncome eq true,
                TransactionsTable.amount - TransactionsTable.transactionCost
            )
            .Else(TransactionsTable.amount + TransactionsTable.transactionCost)

        val totalSum = netAmount.sum()

        var query = joinTable
            .select(CategoriesTable.name, TransactionsTable.description, totalSum)
            .where {
                (TransactionsTable.userId eq EntityID(userId, UsersTable)) and
                        (TransactionsTable.dateTime greaterEq startInstant) and
                        (TransactionsTable.dateTime lessEq endInstant) and
                        (TransactionsTable.description.isNotNull())
            }

        if (accountId != null) query =
            query.andWhere { TransactionsTable.accountId eq EntityID(accountId, AccountsTable) }
        if (isIncome != null) query = query.andWhere { TransactionsTable.isIncome eq isIncome }

        query.groupBy(CategoriesTable.name, TransactionsTable.description)
            .map {
                val cat = it[CategoriesTable.name]
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

            var query = joinTable.selectAll().where {
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
                .When(
                    TransactionsTable.isIncome eq true,
                    TransactionsTable.amount - TransactionsTable.transactionCost
                )
                .Else(TransactionsTable.amount + TransactionsTable.transactionCost)

            val totalSum = netAmount.sum()

            var query = joinTable
                .select(CategoriesTable.name, totalSum)
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
                .groupBy(CategoriesTable.name)
                .associateBy(
                    { it[CategoriesTable.name] },
                    { it[totalSum] ?: java.math.BigDecimal.ZERO })
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
        // FIX: Replaced 3 queries with 1 single efficient query using conditional aggregation
        val incomeCase =
            Case().When(TransactionsTable.isIncome eq true, intLiteral(1)).Else(intLiteral(0))
        val expenseCase =
            Case().When(TransactionsTable.isIncome eq false, intLiteral(1)).Else(intLiteral(0))

        val incomeCountSum = incomeCase.sum()
        val expenseCountSum = expenseCase.sum()
        val totalCostSum = TransactionsTable.transactionCost.sum()
        val totalAmountSum = TransactionsTable.amount.sum()

        var query = TransactionsTable
            .select(incomeCountSum, expenseCountSum, totalCostSum, totalAmountSum)
            .where { TransactionsTable.userId eq EntityID(userId, UsersTable) }

        if (accountId != null) query =
            query.andWhere { TransactionsTable.accountId eq EntityID(accountId, AccountsTable) }
        if (start != null) query = query.andWhere { TransactionsTable.dateTime greaterEq start }
        if (end != null) query = query.andWhere { TransactionsTable.dateTime lessEq end }
        if (isIncome != null) query = query.andWhere { TransactionsTable.isIncome eq isIncome }
        if (!categoryIds.isNullOrEmpty()) query =
            query.andWhere { TransactionsTable.categoryId inList categoryIds }
        if (hasCost == true) query =
            query.andWhere { TransactionsTable.transactionCost greater java.math.BigDecimal.ZERO }
        if (hasCost == false) query =
            query.andWhere { TransactionsTable.transactionCost eq java.math.BigDecimal.ZERO }

        val row = query.singleOrNull()

        TransactionCounts(
            incomeCount = row?.get(incomeCountSum) ?: 0,
            expenseCount = row?.get(expenseCountSum) ?: 0,
            totalCount = (row?.get(incomeCountSum) ?: 0) + (row?.get(expenseCountSum) ?: 0),
            totalTransactionCost = row?.get(totalCostSum) ?: java.math.BigDecimal.ZERO,
            totalAmount = row?.get(totalAmountSum) ?: java.math.BigDecimal.ZERO
        )
    }

    private fun ResultRow.toTransaction() = Transaction(
        id = this[TransactionsTable.id].value,
        userId = this[TransactionsTable.userId].value,
        isIncome = this[TransactionsTable.isIncome],
        amount = this[TransactionsTable.amount],
        transactionCost = this[TransactionsTable.transactionCost],
        category = this[CategoriesTable.name],
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
