package com.rushi.sentinel.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushi.sentinel.data.repository.VaultRepository
import com.rushi.sentinel.domain.model.Entry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EntryDetailViewModel @Inject constructor(
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _entryId = MutableStateFlow<Long?>(null)

    val entry: StateFlow<Entry?> = _entryId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else vaultRepository.getEntryById(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _isPasswordMasked = MutableStateFlow(true)
    val isPasswordMasked: StateFlow<Boolean> = _isPasswordMasked.asStateFlow()

    private var maskJob: Job? = null

    fun loadEntry(id: Long) {
        _entryId.value = id
        _isPasswordMasked.value = true
        maskJob?.cancel()
        
        viewModelScope.launch {
            vaultRepository.updateEntryAccess(id)
        }
    }

    fun togglePasswordMask() {
        val wasMasked = _isPasswordMasked.value
        _isPasswordMasked.value = !wasMasked

        if (wasMasked) {
            // Start 30s auto-mask countdown
            maskJob?.cancel()
            maskJob = viewModelScope.launch {
                delay(30000L) // 30 seconds
                _isPasswordMasked.value = true
            }
        } else {
            maskJob?.cancel()
        }
    }

    fun deleteEntry(onComplete: () -> Unit) {
        viewModelScope.launch {
            entry.value?.let {
                vaultRepository.deleteEntry(it)
            }
            onComplete()
        }
    }

    override fun onCleared() {
        super.onCleared()
        maskJob?.cancel()
    }
}
