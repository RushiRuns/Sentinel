package com.rushi.sentinel.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushi.sentinel.data.repository.VaultRepository
import com.rushi.sentinel.domain.model.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val categories: StateFlow<List<Category>> = vaultRepository.getCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addCategory(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _error.value = "Category name cannot be empty"
            return
        }

        viewModelScope.launch {
            try {
                // Check if category already exists in the local list to avoid duplicate insertions
                if (categories.value.any { it.name.equals(trimmedName, ignoreCase = true) }) {
                    _error.value = "Category '$trimmedName' already exists"
                    return@launch
                }

                val category = Category(
                    name = trimmedName,
                    createdAt = System.currentTimeMillis()
                )
                vaultRepository.insertCategory(category)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to add category: ${e.message}"
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            try {
                vaultRepository.deleteCategory(category)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to delete category: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
