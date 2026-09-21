package com.expensevault.core.model

/**
 * Domain model grouping a category with its subcategories.
 */
data class CategoryWithSubcategories(
    val category: Category,
    val subcategories: List<Category>
) {
    val isIncomeCategory: Boolean get() = category.isIncomeCategory
}
