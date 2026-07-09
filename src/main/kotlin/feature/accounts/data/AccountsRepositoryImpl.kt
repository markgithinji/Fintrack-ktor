package feature.accounts.data

import com.fintrack.feature.accounts.domain.Account
import com.fintrack.feature.accounts.domain.AccountsRepository
import com.fintrack.feature.accounts.domain.TransactionSummary
import com.fintrack.feature.user.UsersTable
import core.dbQuery
import feature.transaction.data.TransactionsTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.update
import kotlinx.datetime.Clock
import java.util.UUID

class AccountsRepositoryImpl : AccountsRepository {

    override suspend fun getAllAccounts(userId: UUID): List<Account> =
        dbQuery {
            AccountsTable.selectAll().where { AccountsTable.userId eq EntityID(userId, UsersTable) }
                .map { toAccount(it) }
        }

    override suspend fun getAccountById(id: UUID): Account? =
        dbQuery {
            AccountsTable.selectAll().where { AccountsTable.id eq EntityID(id, AccountsTable) }
                .map { toAccount(it) }
                .singleOrNull()
        }

    override suspend fun addAccount(account: Account): Account =
        dbQuery {
            val now = Clock.System.now()
            val insertStatement = AccountsTable.insert { row ->
                row[userId] = EntityID(account.userId, UsersTable)
                fillRow(row, account, now)
            }
            val id = insertStatement[AccountsTable.id].value
            val createdAt = insertStatement[AccountsTable.createdAt]
            account.copy(id = id, createdAt = createdAt)
        }

    override suspend fun addAll(accounts: List<Account>): List<Account> =
        dbQuery {
            val now = Clock.System.now()
            AccountsTable.batchInsert(accounts) { account ->
                this[AccountsTable.userId] = EntityID(account.userId, UsersTable)
                fillRow(this, account, now)
            }.map { toAccount(it) }
        }

    override suspend fun updateAccount(account: Account): Account =
        dbQuery {
            requireNotNull(account.id) { "Account ID must not be null for update" }
            AccountsTable.update({ AccountsTable.id eq EntityID(account.id, AccountsTable) }) { row ->
                fillRow(row, account, Clock.System.now())
            }
            account
        }

    private fun fillRow(row: UpdateBuilder<*>, account: Account, now: kotlinx.datetime.Instant) {
        row[AccountsTable.name] = account.name
        row[AccountsTable.isDefault] = account.isDefault
        row[AccountsTable.type] = account.type
        row[AccountsTable.balance] = account.balance
        row[AccountsTable.createdAt] = account.createdAt ?: now
    }

    override suspend fun deleteAccount(id: UUID): Unit =
        dbQuery {
            AccountsTable.deleteWhere { AccountsTable.id eq EntityID(id, AccountsTable) }
        }

    override suspend fun getTransactionSummary(
        userId: UUID,
        accountId: UUID?
    ): TransactionSummary =
        dbQuery {
            val amountSum = TransactionsTable.amount.sum()
            val query = TransactionsTable
                .select(TransactionsTable.isIncome, amountSum)
                .where { TransactionsTable.userId eq EntityID(userId, UsersTable) }

            accountId?.let {
                query.andWhere { TransactionsTable.accountId eq EntityID(it, AccountsTable) }
            }

            val results = query.groupBy(TransactionsTable.isIncome).associate {
                it[TransactionsTable.isIncome] to (it[amountSum] ?: 0.0)
            }

            TransactionSummary(
                income = results[true] ?: 0.0,
                expense = results[false] ?: 0.0
            )
        }

    override suspend fun getTransactionSummaries(userId: UUID): Map<UUID?, TransactionSummary> =
        dbQuery {
            val amountSum = TransactionsTable.amount.sum()
            TransactionsTable
                .select(TransactionsTable.accountId, TransactionsTable.isIncome, amountSum)
                .where { TransactionsTable.userId eq EntityID(userId, UsersTable) }
                .groupBy(TransactionsTable.accountId, TransactionsTable.isIncome)
                .map {
                    val accountId = it[TransactionsTable.accountId].value
                    val isIncome = it[TransactionsTable.isIncome]
                    val sum = it[amountSum] ?: 0.0
                    Triple(accountId, isIncome, sum)
                }
                .groupBy { it.first }
                .mapValues { (_, values) ->
                    val income = values.find { it.second }?.third ?: 0.0
                    val expense = values.find { !it.second }?.third ?: 0.0
                    TransactionSummary(income, expense)
                }
        }

    override suspend fun getLatestBalance(userId: UUID, accountId: UUID?): Double? =
        dbQuery {
            val query = TransactionsTable
                .select(TransactionsTable.balance)
                .where { TransactionsTable.userId eq EntityID(userId, UsersTable) }
                .andWhere { TransactionsTable.balance.isNotNull() }

            val filteredQuery = accountId?.let {
                query.andWhere { TransactionsTable.accountId eq EntityID(it, AccountsTable) }
            } ?: query

            filteredQuery
                .orderBy(TransactionsTable.dateTime to org.jetbrains.exposed.sql.SortOrder.DESC)
                .orderBy(TransactionsTable.id to org.jetbrains.exposed.sql.SortOrder.DESC)
                .limit(1)
                .map { it[TransactionsTable.balance] }
                .singleOrNull()
        }

    override suspend fun updateBalance(accountId: UUID, balance: Double): Unit =
        dbQuery {
            AccountsTable.update({ AccountsTable.id eq EntityID(accountId, AccountsTable) }) {
                it[AccountsTable.balance] = balance
            }
        }

    private fun toAccount(row: ResultRow): Account = Account(
        id = row[AccountsTable.id].value,
        userId = row[AccountsTable.userId].value,
        name = row[AccountsTable.name],
        isDefault = row[AccountsTable.isDefault],
        type = row[AccountsTable.type],
        balance = row[AccountsTable.balance],
        createdAt = row[AccountsTable.createdAt]
    )
}
