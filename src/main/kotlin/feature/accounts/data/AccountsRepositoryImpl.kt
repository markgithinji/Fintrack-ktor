package feature.accounts.data

import com.fintrack.feature.accounts.domain.Account
import com.fintrack.feature.accounts.domain.AccountsRepository
import com.fintrack.feature.user.UsersTable
import core.dbQuery
import feature.transaction.data.TransactionsTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
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
            val insertStatement = AccountsTable.insert { row ->
                // UUIDTable automatically handles the ID, so we don't need to set it
                row[AccountsTable.userId] = EntityID(account.userId, UsersTable)
                row[AccountsTable.name] = account.name
            }
            val id = insertStatement[AccountsTable.id].value
            account.copy(id = id)
        }

    override suspend fun updateAccount(account: Account): Account =
        dbQuery {
            requireNotNull(account.id) { "Account ID must not be null for update" }
            AccountsTable.update({ AccountsTable.id eq EntityID(account.id, AccountsTable) }) {
                it[AccountsTable.name] = account.name
            }
            account
        }

    override suspend fun deleteAccount(id: UUID): Unit =
        dbQuery {
            AccountsTable.deleteWhere { AccountsTable.id eq EntityID(id, AccountsTable) }
        }

    override suspend fun getTransactionAmounts(
        userId: UUID,
        accountId: UUID?
    ): List<Pair<Double, Boolean>> =
        dbQuery {
            val query = TransactionsTable
                .select(TransactionsTable.amount, TransactionsTable.isIncome)
                .where { TransactionsTable.userId eq EntityID(userId, UsersTable) }

            val filteredQuery = accountId?.let {
                query.andWhere { TransactionsTable.accountId eq EntityID(it, AccountsTable) }
            } ?: query

            filteredQuery.map {
                it[TransactionsTable.amount] to it[TransactionsTable.isIncome]
            }
        }

    private fun toAccount(row: ResultRow): Account = Account(
        id = row[AccountsTable.id].value,
        userId = row[AccountsTable.userId].value,
        name = row[AccountsTable.name]
    )
}