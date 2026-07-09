package com.fintrack.feature.auth.data.table

import com.fintrack.core.data.TableNames
import com.fintrack.feature.user.UsersTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object EmailVerificationTokensTable : UUIDTable(TableNames.EMAIL_VERIFICATION_TOKENS) {
    val userId = uuid("user_id").references(UsersTable.id)
    val newEmail = varchar("new_email", 100)
    val token = varchar("token", 255).index()
    val expiresAt = timestamp("expires_at")
}
