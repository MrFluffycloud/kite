package com.expensevault.core.data.impl

import com.expensevault.core.database.dao.CategoryDao
import com.expensevault.core.database.entity.CategoryEntity
import com.expensevault.core.database.mapper.toDomain
import com.expensevault.core.database.mapper.toEntity
import com.expensevault.core.domain.repository.CategoryRepository
import com.expensevault.core.model.Category
import com.expensevault.core.model.CategoryWithSubcategories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao
) : CategoryRepository {
    override fun getCategories(): Flow<List<Category>> =
        categoryDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getTopLevelCategories(): Flow<List<Category>> =
        categoryDao.getTopLevel().map { list -> list.map { it.toDomain() } }

    override fun getSubcategories(parentId: Long): Flow<List<Category>> =
        categoryDao.getSubcategories(parentId).map { list -> list.map { it.toDomain() } }

    override fun getCategoriesWithSubcategories(): Flow<List<CategoryWithSubcategories>> =
        categoryDao.getWithSubcategories().map { list -> 
            list.map { relation -> 
                CategoryWithSubcategories(
                    category = relation.category.toDomain(),
                    subcategories = relation.subcategories.map { it.toDomain() }
                )
            }
        }

    override suspend fun getCategoryById(id: Long): Category? =
        categoryDao.getById(id)?.toDomain()

    override suspend fun addCategory(category: Category): Long =
        categoryDao.insert(category.toEntity())

    override suspend fun updateCategory(category: Category) {
        categoryDao.update(category.toEntity())
    }

    override suspend fun deleteCategory(id: Long) {
        categoryDao.deleteById(id)
    }

    override suspend fun seedDefaultCategories() {
        val now = System.currentTimeMillis()
        val defaults = listOf(
            CategoryEntity(name = "Food", iconName = "ic_food", colorHex = "#FF5722", parentId = null, createdAt = now),
            CategoryEntity(name = "Transport", iconName = "ic_transport", colorHex = "#2196F3", parentId = null, createdAt = now),
            CategoryEntity(name = "Housing", iconName = "ic_housing", colorHex = "#4CAF50", parentId = null, createdAt = now)
        )
        defaults.forEach { categoryDao.insert(it) }
    }
}
