package feature.transaction.domain

import com.fintrack.core.domain.Result
import feature.transaction.data.model.CategoryDto
import feature.transaction.data.model.CreateCategoryRequest
import java.util.UUID

interface CategoryService {
    suspend fun getAll(userId: UUID): Result<List<CategoryDto>>
    suspend fun add(userId: UUID, request: CreateCategoryRequest): Result<CategoryDto>
    suspend fun delete(userId: UUID, id: UUID): Result<Unit>
}
