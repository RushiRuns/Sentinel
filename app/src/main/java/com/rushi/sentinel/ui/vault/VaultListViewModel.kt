package com.rushi.sentinel.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushi.sentinel.data.repository.VaultRepository
import com.rushi.sentinel.domain.model.Entry
import dagger.hilt.android.lifecycle.HiltViewModel
import com.rushi.sentinel.domain.model.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VaultListViewModel @Inject constructor(
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    val categories: StateFlow<List<Category>> = vaultRepository.getCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val entries: StateFlow<List<Entry>> = combine(
        _searchQuery,
        _selectedCategoryId,
        vaultRepository.getEntries()
    ) { query, categoryId, allEntries ->
        allEntries.filter { entry ->
            val matchesQuery = query.isBlank() ||
                    entry.name.contains(query, ignoreCase = true) ||
                    entry.username.contains(query, ignoreCase = true) ||
                    (entry.url != null && entry.url.contains(query, ignoreCase = true))
            val matchesCategory = categoryId == null || entry.categoryId == categoryId
            matchesQuery && matchesCategory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun lockVault() {
        vaultRepository.lock()
    }

    fun deleteEntry(entry: Entry) {
        viewModelScope.launch {
            vaultRepository.deleteEntry(entry)
        }
    }
}
