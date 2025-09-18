package core

import com.fintrack.feature.user.UsersTable.integer
import org.jetbrains.exposed.sql.Table

object AccountsTable : Table("accounts") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id")
    val name = varchar("name", 100)

    override val primaryKey = PrimaryKey(id)
}
