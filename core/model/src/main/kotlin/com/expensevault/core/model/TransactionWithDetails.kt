package com.expensevault.core.model

/**
 * Enriched transaction for UI display.
 */
data class TransactionWithDetails(
    val transaction: Transaction,
    val account: Account?,
    val category: Category?,
    val parentCategory: Category?  // if category is a subcategory
)
