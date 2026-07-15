package feature.category.domain

import feature.category.domain.model.Category
import java.util.UUID

interface CategoryRepository {
    suspend fun getAll(userId: UUID): List<Category>
    suspend fun getById(id: UUID, userId: UUID): Category?
    suspend fun getByIds(ids: List<UUID>): List<Category>
    suspend fun add(category: Category): Category
    suspend fun addAll(categories: List<Category>): List<Category>
    suspend fun delete(id: UUID, userId: UUID): Boolean
    suspend fun exists(userId: UUID, name: String, isExpense: Boolean): Boolean
}
