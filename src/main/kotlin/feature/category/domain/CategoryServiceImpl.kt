package feature.category.domain

import com.fintrack.core.domain.AppError
import com.fintrack.core.domain.Result
import core.util.IdGenerator
import feature.budget.domain.BudgetRepository
import feature.category.data.model.CategoryDto
import feature.category.data.model.CreateCategoryRequest
import feature.category.domain.model.Category
import feature.category.domain.model.CategoryConstants
import feature.transaction.domain.TransactionRepository
import java.util.UUID

class CategoryServiceImpl(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository
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
        val categoryToDelete = categoryRepository.getById(id, userId)
            ?: return Result.Failure(AppError.NotFound("Category not found"))
            
        if (categoryToDelete.isDefault) {
            return Result.Failure(AppError.Validation("Default categories cannot be deleted"))
        }

        // 1. Reassign transactions to a default category instead of letting them be deleted by cascade
        val fallbackCategoryId = if (categoryToDelete.isExpense) {
            CategoryConstants.MISC_EXPENSE_ID
        } else {
            CategoryConstants.OTHER_INCOME_ID
        }
        
        transactionRepository.reassignCategory(userId, id, fallbackCategoryId)
        
        // 2. Handle Budgets referencing this category
        val userBudgets = budgetRepository.getAllByUser(userId, null, 1000, 0)
        userBudgets.forEach { budget ->
            if (budget.categoryIds.contains(id)) {
                val updatedCategoryIds = budget.categoryIds.filter { it != id }.toMutableList()
                
                // If the budget is now empty, we delete it or reassign it. 
                // For categories, it makes more sense to delete the budget if its only tracked category is gone,
                // or just let it be empty if the user wants to add more later.
                // However, to be safe and consistent with AccountServiceImpl, we'll delete it if empty.
                if (updatedCategoryIds.isEmpty()) {
                    budgetRepository.delete(userId, budget.id!!)
                } else {
                    budgetRepository.update(userId, budget.id!!, budget.copy(categoryIds = updatedCategoryIds))
                }
            }
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
