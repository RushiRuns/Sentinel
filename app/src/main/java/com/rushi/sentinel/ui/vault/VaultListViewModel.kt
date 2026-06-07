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

enum class VaultFilter {
    ALL, FAVORITES, RECENT
}

@HiltViewModel
class VaultListViewModel @Inject constructor(
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    private val _selectedFilter = MutableStateFlow(VaultFilter.ALL)
    val selectedFilter: StateFlow<VaultFilter> = _selectedFilter.asStateFlow()

    val categories: StateFlow<List<Category>> = vaultRepository.getCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val entries: StateFlow<List<Entry>> = combine(
        _searchQuery,
        _selectedCategoryId,
        _selectedFilter,
        vaultRepository.getEntries()
    ) { query, categoryId, filter, allEntries ->
        val filtered = allEntries.filter { entry ->
            val matchesQuery = query.isBlank() ||
                    entry.name.contains(query, ignoreCase = true) ||
                    entry.username.contains(query, ignoreCase = true) ||
                    (entry.url != null && entry.url.contains(query, ignoreCase = true))
            
            val matchesCategory = categoryId == null || entry.categoryId == categoryId
            
            val matchesFilter = when (filter) {
                VaultFilter.ALL -> true
                VaultFilter.FAVORITES -> entry.isFavorite
                VaultFilter.RECENT -> entry.lastAccessedAt > 0
            }
            
            matchesQuery && matchesCategory && matchesFilter
        }

        when (filter) {
            VaultFilter.RECENT -> filtered.sortedByDescending { it.lastAccessedAt }
            else -> filtered
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
        if (categoryId != null) {
            _selectedFilter.value = VaultFilter.ALL
        }
    }

    fun selectFilter(filter: VaultFilter) {
        _selectedFilter.value = filter
        if (filter != VaultFilter.ALL) {
            _selectedCategoryId.value = null
        }
    }

    fun lockVault() {
        vaultRepository.lock()
    }

    fun deleteEntry(entry: Entry) {
        viewModelScope.launch {
            vaultRepository.deleteEntry(entry)
        }
    }

    fun recordEntryAccess(entryId: Long) {
        viewModelScope.launch {
            vaultRepository.updateEntryAccess(entryId)
        }
    }
}
