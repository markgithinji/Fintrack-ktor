package feature.auth.data

import com.fintrack.core.data.TableNames
import com.fintrack.feature.user.UsersTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime

object RefreshTokensTable : UUIDTable(TableNames.REFRESH_TOKENS) {
    val token = varchar("token", 255).uniqueIndex()
    val userId = uuid("user_id").references(UsersTable.id)
    val expiresAt = datetime("expires_at")
}
