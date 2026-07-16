package feature.category.domain

import feature.category.domain.model.Category
import java.util.UUID

class CategoryMatcher {

    /**
     * Resolves a category ID based on provided inputs and existing categories.
     * Implements fuzzy matching logic for common transaction descriptions (M-Pesa, etc.)
     */
    fun resolveCategory(
        inputCategoryId: String?,
        inputCategoryName: String?,
        isIncome: Boolean,
        allCategories: List<Category>,
        defaultId: UUID
    ): UUID {
        // 1. Valid UUID Check
        if (!inputCategoryId.isNullOrBlank() &&
            inputCategoryId != "pending" &&
            inputCategoryId != "00000000-0000-0000-0000-000000000000"
        ) {
            return try { UUID.fromString(inputCategoryId) } catch (_: Exception) { defaultId }
        }

        val name = inputCategoryName ?: return defaultId
        if (name.isBlank()) return defaultId

        val isExpense = !isIncome
        val normalizedInput = name.replace("-", "").trim().lowercase()

        // 2. Direct match with type
        allCategories.find {
            it.name.equals(name, ignoreCase = true) && it.isExpense == isExpense
        }?.let { return it.id }

        // 3. Direct match without type (fallback if type mismatch but name matches)
        allCategories.find {
            it.name.equals(name, ignoreCase = true)
        }?.let { return it.id }

        // 4. Fuzzy matching for common variations (prefixes/suffixes)
        allCategories.find {
            val norm = it.name.replace("-", "").trim().lowercase()
            norm == normalizedInput || norm.startsWith(normalizedInput) || normalizedInput.startsWith(norm)
        }?.let { return it.id }

        // 5. Specific common keyword matching (Safaricom/M-Pesa aliases)
        if (normalizedInput.contains("shwari") || normalizedInput.contains("saving")) {
            allCategories.find { it.name.contains("Savings", ignoreCase = true) }?.let { return it.id }
        }
        if (normalizedInput.contains("loan")) {
            allCategories.find { it.name.contains("Loans", ignoreCase = true) }?.let { return it.id }
        }

        // 6. Final Fallback
        val fallbackName = if (isExpense) "Misc" else "Other Income"
        return allCategories.find { it.name.equals(fallbackName, ignoreCase = true) }?.id
            ?: allCategories.find { it.isExpense == isExpense }?.id
            ?: allCategories.firstOrNull { it.isDefault }?.id
            ?: allCategories.firstOrNull()?.id
            ?: defaultId
    }
}
