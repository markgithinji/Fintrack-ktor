package feature.category.data.repository

import com.fintrack.feature.user.UsersTable
import core.dbQuery
import feature.category.data.table.CategoriesTable
import feature.category.domain.CategoryRepository
import feature.category.domain.model.Category
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import kotlinx.datetime.Clock
import java.util.UUID

class CategoryRepositoryImpl : CategoryRepository {
    override suspend fun getAll(userId: UUID): List<Category> = dbQuery {
        CategoriesTable.selectAll()
            .where { CategoriesTable.userId eq EntityID(userId, UsersTable) }
            .orderBy(CategoriesTable.createdAt to SortOrder.ASC)
            .map { it.toCategory() }
    }

    override suspend fun getById(id: UUID, userId: UUID): Category? = dbQuery {
        CategoriesTable.selectAll()
            .where { (CategoriesTable.id eq EntityID(id, CategoriesTable)) and (CategoriesTable.userId eq EntityID(userId, UsersTable)) }
            .map { it.toCategory() }
            .singleOrNull()
    }

    override suspend fun add(category: Category): Category = dbQuery {
        val now = Clock.System.now()
        val inserted = CategoriesTable.insert { row ->
            row[id] = EntityID(category.id, CategoriesTable)
            row[userId] = EntityID(category.userId, UsersTable)
            row[name] = category.name
            row[isExpense] = category.isExpense
            row[iconName] = category.iconName
            row[isDefault] = category.isDefault
            row[createdAt] = category.createdAt ?: now
        }.resultedValues?.singleOrNull() ?: throw IllegalStateException("Failed to insert category")

        inserted.toCategory()
    }

    override suspend fun addAll(categories: List<Category>): List<Category> = dbQuery {
        val now = Clock.System.now()
        CategoriesTable.batchInsert(categories) { category ->
            this[CategoriesTable.id] = EntityID(category.id, CategoriesTable)
            this[CategoriesTable.userId] = EntityID(category.userId, UsersTable)
            this[CategoriesTable.name] = category.name
            this[CategoriesTable.isExpense] = category.isExpense
            this[CategoriesTable.iconName] = category.iconName
            this[CategoriesTable.isDefault] = category.isDefault
            this[CategoriesTable.createdAt] = category.createdAt ?: now
        }.map { it.toCategory() }
    }

    override suspend fun delete(id: UUID, userId: UUID): Boolean = dbQuery {
        val deleted = CategoriesTable.deleteWhere {
            (CategoriesTable.id eq EntityID(id, CategoriesTable)) and (CategoriesTable.userId eq EntityID(userId, UsersTable))
        }
        deleted > 0
    }

    override suspend fun exists(userId: UUID, name: String, isExpense: Boolean): Boolean = dbQuery {
        CategoriesTable.selectAll()
            .where {
                (CategoriesTable.userId eq EntityID(userId, UsersTable)) and
                        (CategoriesTable.name.lowerCase() eq name.lowercase()) and
                        (CategoriesTable.isExpense eq isExpense)
            }
            .count() > 0
    }

    private fun ResultRow.toCategory() = Category(
        id = this[CategoriesTable.id].value,
        userId = this[CategoriesTable.userId].value,
        name = this[CategoriesTable.name],
        isExpense = this[CategoriesTable.isExpense],
        iconName = this[CategoriesTable.iconName],
        isDefault = this[CategoriesTable.isDefault],
        createdAt = this[CategoriesTable.createdAt]
    )
}
