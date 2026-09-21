package com.expensevault.core.model

import kotlinx.datetime.Instant

/**
 * Domain model representing a category.
 */
data class Category(
    val id: Long = 0,
    val name: String,
    val parentId: Long? = null,
    val iconName: String? = null,
    val colorHex: String? = null,
    val isDefault: Boolean = false,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Instant
) {
    val isSubcategory: Boolean get() = parentId != null

    val isIncomeCategory: Boolean get() {
        val lower = name.lowercase()
        return lower.contains("salary") ||
               lower.contains("income") ||
               lower.contains("freelance") ||
               lower.contains("investment") ||
               lower.contains("refund") ||
               lower.contains("cashback") ||
               lower.contains("dividend") ||
               lower.contains("bonus") ||
               lower.contains("wage") ||
               lower.contains("interest") ||
               lower.contains("rental")
    }
}
