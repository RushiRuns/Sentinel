package com.rushi.sentinel.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushi.sentinel.data.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _success = MutableStateFlow(false)
    val success: StateFlow<Boolean> = _success.asStateFlow()

    fun changePassword(oldPassword: CharArray, newPassword: CharArray) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null
                _success.value = false
                
                val result = vaultRepository.changePassword(oldPassword, newPassword)
                _loading.value = false
                
                if (result.isSuccess) {
                    _success.value = true
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to change master password"
                }
            } catch (e: Exception) {
                _loading.value = false
                _error.value = e.message ?: "Failed to change master password"
            } finally {
                // Memory safety: Wipe inputs immediately
                oldPassword.fill('\u0000')
                newPassword.fill('\u0000')
            }
        }
    }

    fun clearState() {
        _error.value = null
        _success.value = false
    }
}
