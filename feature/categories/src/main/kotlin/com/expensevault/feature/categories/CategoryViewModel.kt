package com.expensevault.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensevault.core.model.Category
import com.expensevault.core.model.CategoryWithSubcategories
import com.expensevault.core.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class CategoryUiState(
    val categories: List<CategoryWithSubcategories> = emptyList(),
    val showAddSheet: Boolean = false,
    val editingCategory: Category? = null,
    val expandedCategoryIds: Set<Long> = emptySet(),
    val formName: String = "",
    val formParentId: Long? = null,
    val formColorHex: String = "#2196F3",
    val formIconName: String = ""
)

class CategoryViewModel(
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getCategoriesWithSubcategories().collect { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
    }

    fun toggleExpand(categoryId: Long) {
        _uiState.update { state ->
            val expanded = state.expandedCategoryIds.toMutableSet()
            if (expanded.contains(categoryId)) {
                expanded.remove(categoryId)
            } else {
                expanded.add(categoryId)
            }
            state.copy(expandedCategoryIds = expanded)
        }
    }

    fun showAddCategory(parentId: Long? = null, category: Category? = null) {
        _uiState.update {
            if (category != null) {
                it.copy(
                    showAddSheet = true,
                    editingCategory = category,
                    formName = category.name,
                    formParentId = category.parentId,
                    formColorHex = category.colorHex ?: "#2196F3",
                    formIconName = category.iconName ?: ""
                )
            } else {
                it.copy(
                    showAddSheet = true,
                    editingCategory = null,
                    formName = "",
                    formParentId = parentId,
                    formColorHex = "#2196F3",
                    formIconName = ""
                )
            }
        }
    }

    fun dismissAddCategory() {
        _uiState.update { it.copy(showAddSheet = false, editingCategory = null) }
    }

    fun updateFormName(name: String) { _uiState.update { it.copy(formName = name) } }
    fun updateFormParentId(id: Long?) { _uiState.update { it.copy(formParentId = id) } }
    fun updateFormColorHex(color: String) { _uiState.update { it.copy(formColorHex = color) } }
    fun updateFormIconName(icon: String) { _uiState.update { it.copy(formIconName = icon) } }

    fun saveCategory() {
        val state = _uiState.value
        viewModelScope.launch {
            if (state.editingCategory != null) {
                val updated = state.editingCategory.copy(
                    name = state.formName,
                    parentId = state.formParentId,
                    colorHex = state.formColorHex,
                    iconName = state.formIconName.takeIf { it.isNotBlank() }
                )
                categoryRepository.updateCategory(updated)
            } else {
                val newCategory = Category(
                    name = state.formName,
                    parentId = state.formParentId,
                    colorHex = state.formColorHex,
                    iconName = state.formIconName.takeIf { it.isNotBlank() },
                    createdAt = Clock.System.now()
                )
                categoryRepository.addCategory(newCategory)
            }
            dismissAddCategory()
        }
    }

    fun archiveCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.updateCategory(category.copy(isArchived = !category.isArchived))
        }
    }
}
