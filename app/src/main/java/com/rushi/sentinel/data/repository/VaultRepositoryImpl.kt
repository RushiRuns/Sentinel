package com.rushi.sentinel.data.repository

import android.util.Base64
import com.rushi.sentinel.crypto.KeyDerivation
import com.rushi.sentinel.data.datastore.SettingsDataStore
import com.rushi.sentinel.data.db.DatabaseHolder
import com.rushi.sentinel.data.db.entity.EntryEntity
import com.rushi.sentinel.data.db.entity.CategoryEntity
import com.rushi.sentinel.domain.model.Category
import com.rushi.sentinel.domain.model.Entry
import com.rushi.sentinel.ui.navigation.VaultLockState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Singleton
class VaultRepositoryImpl @Inject constructor(
    private val databaseHolder: DatabaseHolder,
    private val settingsDataStore: SettingsDataStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : VaultRepository {

    override fun getEntries(): Flow<List<Entry>> = flow {
        try {
            emitAll(databaseHolder.getDatabase().entryDao().getEntries().map { entities ->
                entities.map { it.toDomain() }
            }.catch { emit(emptyList()) })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getEntryById(id: Long): Flow<Entry?> = flow {
        try {
            emitAll(databaseHolder.getDatabase().entryDao().getEntryById(id).map { entity ->
                entity?.toDomain()
            }.catch { emit(null) })
        } catch (e: Exception) {
            emit(null)
        }
    }

    override suspend fun insertEntry(entry: Entry) = withContext(ioDispatcher) {
        try {
            databaseHolder.getDatabase().entryDao().insertEntry(entry.toEntity())
        } catch (e: Exception) {
            // Log or handle error - for now just prevent crash
        }
        Unit
    }

    override suspend fun updateEntry(entry: Entry) = withContext(ioDispatcher) {
        try {
            databaseHolder.getDatabase().entryDao().updateEntry(entry.toEntity())
        } catch (e: Exception) {
            // Handle error
        }
        Unit
    }

    override suspend fun deleteEntry(entry: Entry) = withContext(ioDispatcher) {
        try {
            databaseHolder.getDatabase().entryDao().deleteEntry(entry.toEntity())
        } catch (e: Exception) {
            // Handle error
        }
        Unit
    }

    override fun searchEntries(query: String): Flow<List<Entry>> = flow {
        try {
            emitAll(databaseHolder.getDatabase().entryDao().searchEntries(query).map { entities ->
                entities.map { it.toDomain() }
            }.catch { emit(emptyList()) })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getCategories(): Flow<List<Category>> = flow {
        try {
            emitAll(databaseHolder.getDatabase().categoryDao().getCategories().map { entities ->
                entities.map { it.toDomain() }
            }.catch { emit(emptyList()) })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun insertCategory(category: Category) = withContext(ioDispatcher) {
        try {
            databaseHolder.getDatabase().categoryDao().insertCategory(category.toEntity())
        } catch (e: Exception) {
            // Handle error
        }
        Unit
    }

    override suspend fun deleteCategory(category: Category) = withContext(ioDispatcher) {
        try {
            databaseHolder.getDatabase().categoryDao().deleteCategory(category.toEntity())
        } catch (e: Exception) {
            // Handle error
        }
        Unit
    }

    override suspend fun unlock(password: CharArray): Result<Unit> = withContext(ioDispatcher) {
        try {
            var salt = settingsDataStore.saltFlow.first()
            val dbFile = databaseHolder.getDatabaseFile()

            if (salt == null) {
                if (dbFile.exists() && dbFile.length() > 0) {
                    // Database exists but salt is missing — stale unencrypted DB from a pre-SQLCipher
                    // install. Wipe it so the user can start fresh.
                    android.util.Log.w("VaultRepository", "Stale DB with no salt — deleting incompatible file.")
                    dbFile.delete()
                }
                // First launch (or after wipe): generate and save a secure 16-byte salt.
                salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
                settingsDataStore.saveSalt(salt)
                android.util.Log.d("VaultRepository", "Generated new salt for first launch.")
            }

            val derivedKey = KeyDerivation.deriveKey(password, salt)
            try {
                databaseHolder.openDatabase(derivedKey)
                // Access writableDatabase to force validation of encryption key / DB creation
                databaseHolder.getDatabase().openHelper.writableDatabase
                VaultLockState.unlock()
                android.util.Log.d("VaultRepository", "Vault unlocked successfully.")
                Result.success(Unit)
            } catch (e: android.database.sqlite.SQLiteException) {
                databaseHolder.closeDatabase()
                // SQLite error code 26 = SQLITE_NOTADB — the existing file is not an SQLCipher
                // database (e.g. a stale plain-SQLite file from before encryption was enabled).
                // Wipe the incompatible file + saved salt and surface a clear error so the UI
                // can tell the user their vault has been reset.
                if (e.message?.contains("(code 26)") == true || e.message?.contains("file is not a database") == true) {
                    android.util.Log.w("VaultRepository", "Incompatible DB detected (code 26) — wiping and resetting vault.")
                    if (dbFile.exists()) dbFile.delete()
                    settingsDataStore.clearAll()
                    return@withContext Result.failure(
                        IllegalStateException("VAULT_RESET: An incompatible database file was detected and has been removed. Please re-enter your password to create a fresh vault.")
                    )
                }
                android.util.Log.e("VaultRepository", "Unlock failed: ${e.message}")
                Result.failure(e)
            } catch (e: Exception) {
                android.util.Log.e("VaultRepository", "Unlock failed: ${e.message}")
                databaseHolder.closeDatabase()
                Result.failure(e)
            } finally {
                // Zero out the key in memory immediately after use
                derivedKey.fill(0)
            }
        } catch (e: Exception) {
            android.util.Log.e("VaultRepository", "Unlock error: ${e.message}")
            Result.failure(e)
        } finally {
            password.fill('\u0000')
        }
    }

    override suspend fun changePassword(oldPassword: CharArray, newPassword: CharArray): Result<Unit> = withContext(ioDispatcher) {
        var newKey: ByteArray? = null
        try {
            if (isLocked().value) {
                return@withContext Result.failure(IllegalStateException("Vault is locked"))
            }

            // Generate fresh 16-byte random salt
            val newSalt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
            
            // Derive new key
            newKey = KeyDerivation.deriveKey(newPassword, newSalt)
            
            // Format new key as hex string for SQLCipher PRAGMA rekey
            val newKeyHex = newKey.joinToString("") { "%02x".format(it) }
            
            // Rekey database
            val db = databaseHolder.getDatabase()
            db.openHelper.writableDatabase.execSQL("PRAGMA rekey = \"x'$newKeyHex'\"")
            
            // Save new salt
            settingsDataStore.saveSalt(newSalt)
            
            // Close old database connection and re-open with new key
            databaseHolder.closeDatabase()
            databaseHolder.openDatabase(newKey)
            
            // Access writableDatabase once to verify the key works
            databaseHolder.getDatabase().openHelper.writableDatabase
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            newKey?.fill(0)
            oldPassword.fill('\u0000')
            newPassword.fill('\u0000')
        }
    }

    override fun isLocked(): StateFlow<Boolean> = VaultLockState.isLocked

    override fun lock() {
        databaseHolder.closeDatabase()
        VaultLockState.lock()
    }

    override suspend fun exportBackup(password: CharArray): Result<ByteArray> = withContext(ioDispatcher) {
        try {
            if (isLocked().value) {
                return@withContext Result.failure(IllegalStateException("Vault is locked"))
            }

            // Retrieve all entities
            val categoriesList = databaseHolder.getDatabase().categoryDao().getCategories().first()
            val entriesList = databaseHolder.getDatabase().entryDao().getEntries().first()

            val backupCategories = categoriesList.map {
                BackupCategory(it.id, it.name, it.createdAt)
            }
            val backupEntries = entriesList.map {
                BackupEntry(
                    id = it.id,
                    name = it.name,
                    username = it.username,
                    password = it.password.clone(), // Clone to safely zero later
                    url = it.url,
                    notes = it.notes,
                    categoryId = it.categoryId,
                    isFavorite = it.isFavorite,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                    lastAccessedAt = it.lastAccessedAt
                )
            }

            val backupContent = BackupContent(
                version = 1,
                categories = backupCategories,
                entries = backupEntries
            )

            var jsonBytes: ByteArray? = null
            try {
                val jsonString = Json.encodeToString(BackupContent.serializer(), backupContent)
                jsonBytes = jsonString.toByteArray(Charsets.UTF_8)

                val payload = com.rushi.sentinel.crypto.BackupCrypto.encrypt(jsonBytes, password)

                val encryptedBackupPayload = EncryptedBackupPayload(
                    version = 1,
                    salt = Base64.encodeToString(payload.salt, Base64.NO_WRAP),
                    iv = Base64.encodeToString(payload.iv, Base64.NO_WRAP),
                    ciphertext = Base64.encodeToString(payload.ciphertext, Base64.NO_WRAP)
                )

                val resultJson = Json.encodeToString(EncryptedBackupPayload.serializer(), encryptedBackupPayload)
                Result.success(resultJson.toByteArray(Charsets.UTF_8))
            } finally {
                // Wipe sensitive data
                jsonBytes?.fill(0)
                backupEntries.forEach { it.password.fill(0) }
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            password.fill('\u0000')
        }
    }

    override suspend fun importBackup(backupData: ByteArray, password: CharArray): Result<Unit> = withContext(ioDispatcher) {
        var decryptedBytes: ByteArray? = null
        var backupContent: BackupContent? = null
        try {
            if (isLocked().value) {
                return@withContext Result.failure(IllegalStateException("Vault is locked"))
            }

            val payloadJsonString = String(backupData, Charsets.UTF_8)
            val payloadJson = Json.decodeFromString(EncryptedBackupPayload.serializer(), payloadJsonString)

            val salt = Base64.decode(payloadJson.salt, Base64.NO_WRAP)
            val iv = Base64.decode(payloadJson.iv, Base64.NO_WRAP)
            val ciphertext = Base64.decode(payloadJson.ciphertext, Base64.NO_WRAP)

            val payload = com.rushi.sentinel.crypto.BackupCrypto.EncryptedPayload(salt, iv, ciphertext)

            decryptedBytes = com.rushi.sentinel.crypto.BackupCrypto.decrypt(payload, password)

            val decryptedJsonString = String(decryptedBytes, Charsets.UTF_8)
            backupContent = Json.decodeFromString(BackupContent.serializer(), decryptedJsonString)

            val db = databaseHolder.getDatabase()

            db.runInTransaction {
                db.clearAllTables()

                // Insert categories first
                backupContent.categories.forEach { backupCat ->
                    db.categoryDao().insertCategory(
                        CategoryEntity(
                            id = backupCat.id,
                            name = backupCat.name,
                            createdAt = backupCat.createdAt
                        )
                    )
                }

                // Insert entries
                backupContent.entries.forEach { backupEntry ->
                    db.entryDao().insertEntry(
                        EntryEntity(
                            id = backupEntry.id,
                            name = backupEntry.name,
                            username = backupEntry.username,
                            password = backupEntry.password,
                            url = backupEntry.url,
                            notes = backupEntry.notes,
                            categoryId = backupEntry.categoryId,
                            isFavorite = backupEntry.isFavorite,
                            createdAt = backupEntry.createdAt,
                            updatedAt = backupEntry.updatedAt,
                            lastAccessedAt = backupEntry.lastAccessedAt
                        )
                    )
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            // Wiping decrypted bytes & backup passwords in memory
            decryptedBytes?.fill(0)
            backupContent?.entries?.forEach { it.password.fill(0) }
            password.fill('\u0000')
        }
    }

    override suspend fun updateEntryAccess(entryId: Long) = withContext(ioDispatcher) {
        try {
            val db = databaseHolder.getDatabase()
            val entryFlow = db.entryDao().getEntryById(entryId)
            val entryEntity = entryFlow.first()
            if (entryEntity != null) {
                db.entryDao().updateEntry(
                    entryEntity.copy(lastAccessedAt = System.currentTimeMillis())
                )
            }
        } catch (e: Exception) {
            // Suppress error
        }
        Unit
    }

    // Mapper helper extensions
    private fun CategoryEntity.toDomain(): Category = Category(
        id = id,
        name = name,
        createdAt = createdAt
    )

    private fun Category.toEntity(): CategoryEntity = CategoryEntity(
        id = id,
        name = name,
        createdAt = createdAt
    )

    private fun EntryEntity.toDomain(): Entry = Entry(
        id = id,
        name = name,
        username = username,
        password = password,
        url = url,
        notes = notes,
        categoryId = categoryId,
        isFavorite = isFavorite,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastAccessedAt = lastAccessedAt
    )

    private fun Entry.toEntity(): EntryEntity = EntryEntity(
        id = id,
        name = name,
        username = username,
        password = password,
        url = url,
        notes = notes,
        categoryId = categoryId,
        isFavorite = isFavorite,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastAccessedAt = lastAccessedAt
    )
}

@kotlinx.serialization.Serializable
data class BackupContent(
    val version: Int = 1,
    val categories: List<BackupCategory>,
    val entries: List<BackupEntry>
)

@kotlinx.serialization.Serializable
data class BackupCategory(
    val id: Long,
    val name: String,
    val createdAt: Long
)

@kotlinx.serialization.Serializable
data class BackupEntry(
    val id: Long,
    val name: String,
    val username: String,
    val password: ByteArray,
    val url: String?,
    val notes: String?,
    val categoryId: Long?,
    val isFavorite: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val lastAccessedAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BackupEntry

        if (id != other.id) return false
        if (name != other.name) return false
        if (username != other.username) return false
        if (!password.contentEquals(other.password)) return false
        if (url != other.url) return false
        if (notes != other.notes) return false
        if (categoryId != other.categoryId) return false
        if (isFavorite != other.isFavorite) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false
        if (lastAccessedAt != other.lastAccessedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + username.hashCode()
        result = 31 * result + password.contentHashCode()
        result = 31 * result + (url?.hashCode() ?: 0)
        result = 31 * result + (notes?.hashCode() ?: 0)
        result = 31 * result + (categoryId?.hashCode() ?: 0)
        result = 31 * result + isFavorite.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        result = 31 * result + lastAccessedAt.hashCode()
        return result
    }
}

@kotlinx.serialization.Serializable
data class EncryptedBackupPayload(
    val version: Int,
    val salt: String,
    val iv: String,
    val ciphertext: String
)
