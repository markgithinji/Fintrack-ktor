package com.fintrack.feature.user.domain

import com.fintrack.feature.user.domain.User

import java.util.UUID

interface UserRepository {
    suspend fun createUser(email: String, password: String, name: String): UUID
    suspend fun findByEmail(email: String): User?
    suspend fun findById(userId: UUID): User?
    suspend fun updateUser(userId: UUID, name: String?, email: String?, password: String?): Boolean
    suspend fun updateTrackedCategories(userId: UUID, categories: List<String>): Boolean
    suspend fun updatePassword(userId: UUID, newPassword: String): Boolean
    suspend fun updateEmail(userId: UUID, newEmail: String): Boolean
    suspend fun updateEmailVerificationStatus(userId: UUID, isVerified: Boolean): Boolean
    suspend fun deleteUser(userId: UUID): Boolean
    suspend fun userExists(email: String): Boolean
}