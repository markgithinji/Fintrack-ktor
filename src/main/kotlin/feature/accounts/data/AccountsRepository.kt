package feature.accounts.data

import feature.accounts.domain.Account
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class AccountsRepository {

    fun getAllAccounts(userId: Int): List<Account> = transaction {
        AccountsTable.selectAll().where { AccountsTable.userId eq userId }
            .map { toAccount(it) }
    }

    fun getAccountById(id: Int): Account? = transaction {
        AccountsTable.selectAll().where { AccountsTable.id eq id }
            .map { toAccount(it) }
            .singleOrNull()
    }

    fun addAccount(account: Account): Account = transaction {
        val insertStatement = AccountsTable.insert { row ->
            row[userId] = account.userId
            row[name] = account.name
        }
        // Get the generated ID from the insert
        val id = insertStatement[AccountsTable.id]
        account.copy(id = id)
    }


    fun updateAccount(account: Account): Account = transaction {
        AccountsTable.update({ AccountsTable.id eq (account.id ?: 0) }) {
            it[name] = account.name
        }
        account
    }

    fun deleteAccount(id: Int) = transaction {
        AccountsTable.deleteWhere { AccountsTable.id eq id }
    }

    private fun toAccount(row: ResultRow): Account = Account(
        id = row[AccountsTable.id],
        userId = row[AccountsTable.userId],
        name = row[AccountsTable.name]
    )
}