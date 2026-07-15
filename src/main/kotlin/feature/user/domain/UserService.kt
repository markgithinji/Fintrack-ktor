package com.fintrack.feature.user.domain

import com.fintrack.core.domain.Result
import com.fintrack.feature.user.data.model.UserDto
import com.fintrack.feature.user.domain.User
import com.fintrack.feature.user.data.model.UpdateUserRequest
import java.util.UUID

interface UserService {
    suspend fun getUserProfile(userId: UUID): Result<UserDto>
    suspend fun updateUser(userId: UUID, request: UpdateUserRequest): Result<UserDto>
    suspend fun updateTrackedCategories(userId: UUID, categoryIds: List<UUID>): Result<UserDto>
    suspend fun verifyEmailChange(token: String): Result<UserDto>
    suspend fun deleteUser(userId: UUID): Result<Unit>
    suspend fun validateUserCredentials(email: String, password: String): User?
    suspend fun userExists(email: String): Boolean
}
