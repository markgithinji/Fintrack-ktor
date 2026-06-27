package feature.transaction.domain

import feature.transaction.domain.model.Category
import java.util.UUID

interface CategoryRepository {
    suspend fun getAll(userId: UUID): List<Category>
    suspend fun getById(id: UUID, userId: UUID): Category?
    suspend fun add(category: Category): Category
    suspend fun delete(id: UUID, userId: UUID): Boolean
}
