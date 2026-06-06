package com.rushi.sentinel.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushi.sentinel.data.datastore.SettingsDataStore
import com.rushi.sentinel.data.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LockViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val isSetupMode: StateFlow<Boolean> = settingsDataStore.saltFlow
        .map { it == null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _failedAttempts = MutableStateFlow(0)
    val failedAttempts: StateFlow<Int> = _failedAttempts.asStateFlow()

    private val _lockoutTimeRemaining = MutableStateFlow(0L)
    val lockoutTimeRemaining: StateFlow<Long> = _lockoutTimeRemaining.asStateFlow()

    private val failedAttemptLimit: StateFlow<Int> = settingsDataStore.failedAttemptLimitFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 5
        )

    private var countdownJob: Job? = null

    fun unlock(password: CharArray) {
        if (_lockoutTimeRemaining.value > 0) return

        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            val result = vaultRepository.unlock(password)
            _loading.value = false

            if (result.isSuccess) {
                _failedAttempts.value = 0
                _error.value = null
            } else {
                val attempts = _failedAttempts.value + 1
                _failedAttempts.value = attempts
                _error.value = "Incorrect master password"

                val limit = failedAttemptLimit.value
                if (attempts >= limit) {
                    val delaySec = 1L shl (attempts - limit) // 1s, 2s, 4s, 8s, 16s...
                    startLockoutCountdown(delaySec)
                }
            }
        }
    }

    fun setupVault(password: CharArray, confirmPassword: CharArray) {
        if (password.isEmpty()) {
            _error.value = "Password cannot be empty"
            return
        }
        if (!password.contentEquals(confirmPassword)) {
            _error.value = "Passwords do not match"
            return
        }

        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            val result = vaultRepository.unlock(password)
            _loading.value = false

            if (result.isSuccess) {
                _failedAttempts.value = 0
                _error.value = null
            } else {
                _error.value = "Failed to initialize vault: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    private fun startLockoutCountdown(seconds: Long) {
        countdownJob?.cancel()
        _lockoutTimeRemaining.value = seconds
        countdownJob = viewModelScope.launch {
            while (_lockoutTimeRemaining.value > 0) {
                delay(1000)
                _lockoutTimeRemaining.value -= 1
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
