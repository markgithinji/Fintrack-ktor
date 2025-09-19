package feature.accounts.data

import org.jetbrains.exposed.sql.Table

object AccountsTable : Table("accounts") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id")
    val name = varchar("name", 100)

    override val primaryKey = PrimaryKey(id)
}