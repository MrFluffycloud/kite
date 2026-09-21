package com.expensevault.core.domain.usecase

data class ImportResult(
    val importedCount: Int,
    val accountsCreated: Int = 0,
    val categoriesCreated: Int = 0,
    val personsCreated: Int = 0,
    val debtsCreated: Int = 0,
    val recurringRulesCreated: Int = 0
)

interface ImportDataUseCase {
    suspend fun import(content: String): Result<ImportResult>
}
