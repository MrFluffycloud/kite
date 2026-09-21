package com.expensevault.core.domain.usecase

/**
 * Completely wipes all database records, resets preferences, clears linked credentials,
 * and reseeds clean default accounts and categories.
 */
interface ClearAllDataUseCase {
    suspend operator fun invoke(): Result<Unit>
}
