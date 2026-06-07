package com.rushi.sentinel.ui.settings

import android.content.ContentResolver
import android.net.Uri
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
class BackupRestoreViewModel @Inject constructor(
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun clearMessages() {
        _error.value = null
        _successMessage.value = null
    }

    /**
     * Exports the encrypted backup data to the chosen SAF Uri destination.
     */
    fun exportBackup(contentResolver: ContentResolver, uri: Uri, password: CharArray) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _successMessage.value = null
            
            try {
                val exportResult = vaultRepository.exportBackup(password)
                if (exportResult.isSuccess) {
                    val encryptedBytes = exportResult.getOrThrow()
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(encryptedBytes)
                        outputStream.flush()
                    }
                    _successMessage.value = "Vault backup exported successfully"
                } else {
                    val exception = exportResult.exceptionOrNull()
                    _error.value = "Export failed: ${exception?.message ?: "Unknown error"}"
                }
            } catch (e: Exception) {
                _error.value = "Failed to write backup file: ${e.message}"
            } finally {
                _loading.value = false
                // Wipe the user-supplied password from memory immediately after execution
                password.fill('\u0000')
            }
        }
    }

    /**
     * Imports and restores the encrypted backup data from the chosen SAF Uri source.
     */
    fun importBackup(contentResolver: ContentResolver, uri: Uri, password: CharArray) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _successMessage.value = null

            try {
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _error.value = "Failed to open backup file"
                    _loading.value = false
                    password.fill('\u0000')
                    return@launch
                }

                val backupBytes = inputStream.use { it.readBytes() }
                
                val importResult = vaultRepository.importBackup(backupBytes, password)
                if (importResult.isSuccess) {
                    _successMessage.value = "Vault backup restored successfully"
                } else {
                    val exception = importResult.exceptionOrNull()
                    _error.value = "Restore failed: ${exception?.message ?: "Invalid password or corrupted backup file"}"
                }
            } catch (e: Exception) {
                _error.value = "Failed to read backup file: ${e.message}"
            } finally {
                _loading.value = false
                // Wipe the user-supplied password from memory immediately after execution
                password.fill('\u0000')
            }
        }
    }
}
