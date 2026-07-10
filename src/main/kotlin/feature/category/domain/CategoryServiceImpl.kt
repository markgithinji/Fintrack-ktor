package feature.category.domain

import com.fintrack.core.domain.AppError
import com.fintrack.core.domain.Result
import core.util.IdGenerator
import feature.category.data.model.CategoryDto
import feature.category.data.model.CreateCategoryRequest
import feature.category.domain.model.Category
import java.util.UUID

class CategoryServiceImpl(
    private val categoryRepository: CategoryRepository
) : CategoryService {
    override suspend fun getAll(userId: UUID): Result<List<CategoryDto>> {
        val categories = categoryRepository.getAll(userId)
            .map { it.toDto() }
        return Result.Success(categories)
    }

    override suspend fun add(userId: UUID, request: CreateCategoryRequest): Result<CategoryDto> {
        val trimmedName = request.name.trim()

        // Efficient existence check in DB
        if (categoryRepository.exists(userId, trimmedName, request.isExpense)) {
            // Re-fetch the existing one to return it (or just return success if the goal is idempotency)
            val existing = categoryRepository.getAll(userId).find {
                it.name.equals(trimmedName, ignoreCase = true) && it.isExpense == request.isExpense
            }
            if (existing != null) return Result.Success(existing.toDto())
        }

        val category = Category(
            id = IdGenerator.nextId(),
            userId = userId,
            name = trimmedName,
            isExpense = request.isExpense,
            iconName = request.iconName
        )
        val added = categoryRepository.add(category)
        return Result.Success(added.toDto())
    }

    override suspend fun delete(userId: UUID, id: UUID): Result<Unit> {
        val category = categoryRepository.getById(id, userId)
            ?: return Result.Failure(AppError.NotFound("Category not found"))
            
        if (category.isDefault) {
            return Result.Failure(AppError.Validation("Default categories cannot be deleted"))
        }
        
        val success = categoryRepository.delete(id, userId)
        if (!success) {
            return Result.Failure(AppError.NotFound("Category with id $id not found for user $userId"))
        }
        return Result.Success(Unit)
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
