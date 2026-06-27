package feature.transaction.domain

import feature.transaction.data.model.CategoryDto
import feature.transaction.data.model.CreateCategoryRequest
import java.util.UUID

interface CategoryService {
    suspend fun getAll(userId: UUID): List<CategoryDto>
    suspend fun add(userId: UUID, request: CreateCategoryRequest): CategoryDto
    suspend fun delete(userId: UUID, id: UUID)
}
