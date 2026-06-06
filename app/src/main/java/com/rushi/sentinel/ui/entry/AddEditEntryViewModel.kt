package com.rushi.sentinel.ui.entry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushi.sentinel.data.repository.VaultRepository
import com.rushi.sentinel.domain.model.Entry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditEntryViewModel @Inject constructor(
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isSaveSuccess = MutableSharedFlow<Boolean>()
    val isSaveSuccess: SharedFlow<Boolean> = _isSaveSuccess.asSharedFlow()

    // Form inputs state
    var name by mutableStateOf("")
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var url by mutableStateOf("")
    var notes by mutableStateOf("")
    var isFavorite by mutableStateOf(false)

    private var entryId: Long? = null
    private var createdAt: Long = 0L

    fun loadEntry(id: Long?) {
        entryId = id
        if (id == null) {
            name = ""
            username = ""
            password = ""
            url = ""
            notes = ""
            isFavorite = false
            createdAt = 0L
            _error.value = null
        } else {
            viewModelScope.launch {
                _loading.value = true
                val entry = vaultRepository.getEntryById(id).first()
                _loading.value = false
                if (entry != null) {
                    name = entry.name
                    username = entry.username
                    password = String(entry.password)
                    url = entry.url ?: ""
                    notes = entry.notes ?: ""
                    isFavorite = entry.isFavorite
                    createdAt = entry.createdAt
                } else {
                    _error.value = "Failed to load entry for editing"
                }
            }
        }
    }

    fun saveEntry() {
        if (name.isBlank()) {
            _error.value = "Name is required"
            return
        }
        if (password.isBlank()) {
            _error.value = "Password is required"
            return
        }

        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val now = System.currentTimeMillis()
                val pwdBytes = password.toByteArray()
                val entry = Entry(
                    id = entryId ?: 0,
                    name = name.trim(),
                    username = username.trim(),
                    password = pwdBytes,
                    url = url.trim(),
                    notes = notes.trim(),
                    categoryId = null,
                    isFavorite = isFavorite,
                    createdAt = if (createdAt == 0L) now else createdAt,
                    updatedAt = now,
                    lastAccessedAt = now
                )

                if (entryId == null) {
                    vaultRepository.insertEntry(entry)
                } else {
                    vaultRepository.updateEntry(entry)
                }

                pwdBytes.fill(0) // Zero out derived byte array
                _isSaveSuccess.emit(true)
            } catch (e: Exception) {
                _error.value = "Failed to save credential: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
}
