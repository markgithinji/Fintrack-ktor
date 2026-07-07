package feature.transaction.domain

import feature.transaction.data.model.CategoryDto
import feature.transaction.data.model.CreateCategoryRequest
import feature.transaction.domain.model.Category
import kotlinx.datetime.Clock
import java.util.UUID

class CategoryServiceImpl(
    private val repository: CategoryRepository
) : CategoryService {
    override suspend fun getAll(userId: UUID): List<CategoryDto> {
        return repository.getAll(userId)
            .sortedBy { it.createdAt ?: Clock.System.now() }
            .map { it.toDto() }
    }

    override suspend fun add(userId: UUID, request: CreateCategoryRequest): CategoryDto {
        val trimmedName = request.name.trim()

        // Prevent duplicate names (case-insensitive) for the same user and type
        val existing = repository.getAll(userId).find {
            it.name.equals(trimmedName, ignoreCase = true) && it.isExpense == request.isExpense
        }

        if (existing != null) {
            return existing.toDto()
        }

        val category = Category(
            id = UUID.randomUUID(),
            userId = userId,
            name = trimmedName,
            isExpense = request.isExpense,
            iconName = request.iconName
        )
        return repository.add(category).toDto()
    }

    override suspend fun delete(userId: UUID, id: UUID) {
        val category = repository.getById(id, userId) ?: throw NoSuchElementException("Category not found")
        if (category.isDefault) {
            throw IllegalArgumentException("Default categories cannot be deleted")
        }
        val success = repository.delete(id, userId)
        if (!success) {
            throw NoSuchElementException("Category with id $id not found for user $userId")
        }
    }

    private fun Category.toDto() = CategoryDto(
        id = id.toString(),
        name = name,
        isExpense = isExpense,
        iconName = iconName,
        isDefault = isDefault,
        createdAt = createdAt
    )
}
