package com.rushi.sentinel.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushi.sentinel.crypto.BiometricKeyManager
import com.rushi.sentinel.data.datastore.SettingsDataStore
import com.rushi.sentinel.data.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val settingsDataStore: SettingsDataStore,
    private val biometricKeyManager: BiometricKeyManager
) : ViewModel() {

    val biometricEnabled: StateFlow<Boolean> = settingsDataStore.biometricEnabledFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _verificationError = MutableStateFlow<String?>(null)
    val verificationError: StateFlow<String?> = _verificationError.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun clearError() {
        _verificationError.value = null
    }

    /**
     * Verifies the master password and prepares a cipher for biometric encryption.
     * Returns the initialized Cipher or null if verification fails.
     */
    suspend fun verifyAndPrepareBiometricCipher(password: CharArray): javax.crypto.Cipher? {
        _loading.value = true
        _verificationError.value = null
        
        try {
            val salt = settingsDataStore.saltFlow.first()
            if (salt == null) {
                _verificationError.value = "Vault is not initialized"
                password.fill('\u0000')
                return null
            }

            // Verify password by attempting unlock
            val testUnlock = vaultRepository.unlock(password.clone())
            if (testUnlock.isFailure) {
                _verificationError.value = "Incorrect master password"
                password.fill('\u0000')
                return null
            }

            // Prepare key/cipher
            return biometricKeyManager.getInitializedCipherForEncryption()
        } catch (e: Exception) {
            _verificationError.value = "Failed to prepare credentials: ${e.message}"
            password.fill('\u0000')
            return null
        } finally {
            _loading.value = false
        }
    }

    /**
     * Encrypts the master password with the authenticated cipher and persists the result.
     */
    fun saveBiometricConfiguration(password: CharArray, authenticatedCipher: javax.crypto.Cipher) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val encryptedBytes = biometricKeyManager.encryptMasterPassword(authenticatedCipher, password)
                val iv = authenticatedCipher.iv
                
                settingsDataStore.saveBiometricData(encryptedBytes, iv)
                settingsDataStore.saveBiometricEnabled(true)
            } catch (e: Exception) {
                _verificationError.value = "Failed to save biometric data: ${e.message}"
            } finally {
                _loading.value = false
                password.fill('\u0000')
            }
        }
    }

    /**
     * Clears biometric authentication keys and configuration.
     */
    fun disableBiometric() {
        viewModelScope.launch {
            settingsDataStore.clearBiometricData()
            biometricKeyManager.deleteBiometricKey()
        }
    }
}
