package feature.accounts.data

import com.fintrack.feature.accounts.domain.AccountsRepository
import feature.accounts.domain.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class AccountsRepositoryImpl : AccountsRepository {

    override suspend fun getAllAccounts(userId: Int): List<Account> =
        withContext(Dispatchers.IO) {
            transaction {
                AccountsTable.selectAll().where { AccountsTable.userId eq userId }
                    .map { toAccount(it) }
            }
        }

    override suspend fun getAccountById(id: Int): Account? =
        withContext(Dispatchers.IO) {
            transaction {
                AccountsTable.selectAll().where { AccountsTable.id eq id }
                    .map { toAccount(it) }
                    .singleOrNull()
            }
        }

    override suspend fun addAccount(account: Account): Account =
        withContext(Dispatchers.IO) {
            transaction {
                val insertStatement = AccountsTable.insert { row ->
                    row[userId] = account.userId
                    row[name] = account.name
                }
                val id = insertStatement[AccountsTable.id]
                account.copy(id = id)
            }
        }

    override suspend fun updateAccount(account: Account): Account =
        withContext(Dispatchers.IO) {
            requireNotNull(account.id) { "Account ID must not be null for update" }
            transaction {
                AccountsTable.update({ AccountsTable.id eq account.id }) {
                    it[name] = account.name
                }
            }
            account
        }

    override suspend fun deleteAccount(id: Int): Unit =
        withContext(Dispatchers.IO) {
            transaction {
                AccountsTable.deleteWhere { AccountsTable.id eq id }
            }
        }

    private fun toAccount(row: ResultRow): Account = Account(
        id = row[AccountsTable.id],
        userId = row[AccountsTable.userId],
        name = row[AccountsTable.name]
    )
}
