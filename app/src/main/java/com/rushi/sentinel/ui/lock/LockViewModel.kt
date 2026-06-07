package com.rushi.sentinel.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushi.sentinel.crypto.BiometricKeyManager
import com.rushi.sentinel.data.datastore.SettingsDataStore
import com.rushi.sentinel.data.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LockViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val settingsDataStore: SettingsDataStore,
    private val biometricKeyManager: BiometricKeyManager
) : ViewModel() {

    val isSetupMode: StateFlow<Boolean> = settingsDataStore.saltFlow
        .map { it == null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val biometricEnabled: StateFlow<Boolean> = settingsDataStore.biometricEnabledFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val biometricCiphertext: StateFlow<ByteArray?> = settingsDataStore.biometricCiphertextFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val biometricIv: StateFlow<ByteArray?> = settingsDataStore.biometricIvFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _failedAttempts = MutableStateFlow(0)
    val failedAttempts: StateFlow<Int> = _failedAttempts.asStateFlow()

    private val _lockoutTimeRemaining = MutableStateFlow(0L)
    val lockoutTimeRemaining: StateFlow<Long> = _lockoutTimeRemaining.asStateFlow()

    /** Fires true when a legacy incompatible DB was wiped and the vault was reset to first-launch. */
    private val _vaultResetEvent = MutableStateFlow(false)
    val vaultResetEvent: StateFlow<Boolean> = _vaultResetEvent.asStateFlow()

    fun clearVaultResetEvent() { _vaultResetEvent.value = false }

    private val failedAttemptLimit: StateFlow<Int> = settingsDataStore.failedAttemptLimitFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 5
        )

    private var countdownJob: Job? = null

    fun unlock(password: CharArray) {
        if (_lockoutTimeRemaining.value > 0) {
            password.fill('\u0000')
            return
        }

        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null

                val result = vaultRepository.unlock(password)
                _loading.value = false

                if (result.isSuccess) {
                    _failedAttempts.value = 0
                    _error.value = null
                } else {
                    val errorMsg = result.exceptionOrNull()?.message.orEmpty()
                    if (errorMsg.startsWith("VAULT_RESET:")) {
                        // Incompatible legacy DB was wiped — signal the UI to show a reset dialog.
                        _vaultResetEvent.value = true
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
            } finally {
                password.fill('\u0000')
            }
        }
    }

    fun setupVault(password: CharArray, confirmPassword: CharArray) {
        if (password.isEmpty()) {
            _error.value = "Password cannot be empty"
            password.fill('\u0000')
            confirmPassword.fill('\u0000')
            return
        }
        if (!password.contentEquals(confirmPassword)) {
            _error.value = "Passwords do not match"
            password.fill('\u0000')
            confirmPassword.fill('\u0000')
            return
        }

        viewModelScope.launch {
            try {
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
            } finally {
                password.fill('\u0000')
                confirmPassword.fill('\u0000')
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

    suspend fun getBiometricDecryptionCipher(): javax.crypto.Cipher? {
        val iv = settingsDataStore.biometricIvFlow.first() ?: return null
        return try {
            biometricKeyManager.getInitializedCipherForDecryption(iv)
        } catch (e: Exception) {
            null
        }
    }

    fun unlockWithBiometricCipher(cipher: javax.crypto.Cipher) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            
            try {
                val ciphertext = settingsDataStore.biometricCiphertextFlow.first()
                if (ciphertext == null) {
                    _error.value = "Biometric data missing"
                    _loading.value = false
                    return@launch
                }

                val decryptedPassword = biometricKeyManager.decryptMasterPassword(cipher, ciphertext)
                val result = vaultRepository.unlock(decryptedPassword)
                _loading.value = false

                if (result.isSuccess) {
                    _failedAttempts.value = 0
                    _error.value = null
                } else {
                    _error.value = "Biometric unlock failed: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _loading.value = false
                _error.value = "Biometric decryption failed: ${e.message}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
