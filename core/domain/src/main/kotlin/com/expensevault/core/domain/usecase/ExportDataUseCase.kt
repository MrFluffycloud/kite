package com.expensevault.core.domain.usecase

enum class ExportFormat {
    CSV, JSON
}

data class ExportResult(
    val fileName: String,
    val mimeType: String,
    val content: String
)

interface ExportDataUseCase {
    suspend fun export(format: ExportFormat): ExportResult
}
