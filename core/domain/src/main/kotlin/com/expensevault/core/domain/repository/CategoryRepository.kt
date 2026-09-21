package com.expensevault.core.domain.repository

import com.expensevault.core.model.Category
import com.expensevault.core.model.CategoryWithSubcategories
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getCategories(): Flow<List<Category>>
    fun getTopLevelCategories(): Flow<List<Category>>
    fun getSubcategories(parentId: Long): Flow<List<Category>>
    fun getCategoriesWithSubcategories(): Flow<List<CategoryWithSubcategories>>
    suspend fun getCategoryById(id: Long): Category?
    suspend fun addCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(id: Long)
    suspend fun seedDefaultCategories()
}
