package com.rushi.sentinel.data.datastore

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vault_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    private val context: Context
) {
    companion object {
        private val KEY_SALT = stringPreferencesKey("salt")
        private val KEY_AUTO_LOCK_TIMEOUT = longPreferencesKey("auto_lock_timeout_ms")
        private val KEY_CLIPBOARD_CLEAR_TIMEOUT = longPreferencesKey("clipboard_clear_timeout_ms")
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val KEY_FAILED_ATTEMPT_LIMIT = intPreferencesKey("failed_attempt_limit")
        private val KEY_BIOMETRIC_CIPHERTEXT = stringPreferencesKey("biometric_ciphertext")
        private val KEY_BIOMETRIC_IV = stringPreferencesKey("biometric_iv")
        
        private const val DEFAULT_AUTO_LOCK_TIMEOUT = 300000L // 5 minutes
        private const val DEFAULT_CLIPBOARD_TIMEOUT = 30000L  // 30 seconds
        private const val DEFAULT_FAILED_LIMIT = 5
    }

    val saltFlow: Flow<ByteArray?> = context.dataStore.data.map { preferences ->
        preferences[KEY_SALT]?.let { Base64.decode(it, Base64.NO_WRAP) }
    }

    val autoLockTimeoutFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTO_LOCK_TIMEOUT] ?: DEFAULT_AUTO_LOCK_TIMEOUT
    }

    val clipboardClearTimeoutFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[KEY_CLIPBOARD_CLEAR_TIMEOUT] ?: DEFAULT_CLIPBOARD_TIMEOUT
    }

    val biometricEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_BIOMETRIC_ENABLED] ?: false
    }

    val failedAttemptLimitFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_FAILED_ATTEMPT_LIMIT] ?: DEFAULT_FAILED_LIMIT
    }

    val biometricCiphertextFlow: Flow<ByteArray?> = context.dataStore.data.map { preferences ->
        preferences[KEY_BIOMETRIC_CIPHERTEXT]?.let { Base64.decode(it, Base64.NO_WRAP) }
    }

    val biometricIvFlow: Flow<ByteArray?> = context.dataStore.data.map { preferences ->
        preferences[KEY_BIOMETRIC_IV]?.let { Base64.decode(it, Base64.NO_WRAP) }
    }

    suspend fun saveSalt(salt: ByteArray) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SALT] = Base64.encodeToString(salt, Base64.NO_WRAP)
        }
    }

    suspend fun saveAutoLockTimeout(timeoutMs: Long) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_LOCK_TIMEOUT] = timeoutMs
        }
    }

    suspend fun saveClipboardClearTimeout(timeoutMs: Long) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CLIPBOARD_CLEAR_TIMEOUT] = timeoutMs
        }
    }

    suspend fun saveBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun saveBiometricData(ciphertext: ByteArray, iv: ByteArray) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BIOMETRIC_CIPHERTEXT] = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
            preferences[KEY_BIOMETRIC_IV] = Base64.encodeToString(iv, Base64.NO_WRAP)
        }
    }

    suspend fun clearBiometricData() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_BIOMETRIC_CIPHERTEXT)
            preferences.remove(KEY_BIOMETRIC_IV)
            preferences[KEY_BIOMETRIC_ENABLED] = false
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
