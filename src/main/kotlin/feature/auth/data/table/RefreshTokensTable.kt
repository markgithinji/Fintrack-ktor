package com.fintrack.feature.auth.data.table

import com.fintrack.core.data.TableNames
import com.fintrack.feature.user.UsersTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object RefreshTokensTable : UUIDTable(TableNames.REFRESH_TOKENS) {
    val token = varchar("token", 255).uniqueIndex()
    val userId = uuid("user_id").references(UsersTable.id)
    val expiresAt = timestamp("expires_at")
    val isUsed = bool("is_used").default(false)
    val rotatedAt = timestamp("rotated_at").nullable()
}
