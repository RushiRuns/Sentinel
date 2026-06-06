package com.rushi.sentinel.data.repository

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRepositoryImpl @Inject constructor(
    private val databaseHolder: DatabaseHolder,
    private val settingsDataStore: SettingsDataStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : VaultRepository {

    override fun getEntries(): Flow<List<Entry>> {
        return databaseHolder.getDatabase().entryDao().getEntries().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getEntryById(id: Long): Flow<Entry?> {
        return databaseHolder.getDatabase().entryDao().getEntryById(id).map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun insertEntry(entry: Entry) = withContext(ioDispatcher) {
        databaseHolder.getDatabase().entryDao().insertEntry(entry.toEntity())
        Unit
    }

    override suspend fun updateEntry(entry: Entry) = withContext(ioDispatcher) {
        databaseHolder.getDatabase().entryDao().updateEntry(entry.toEntity())
        Unit
    }

    override suspend fun deleteEntry(entry: Entry) = withContext(ioDispatcher) {
        databaseHolder.getDatabase().entryDao().deleteEntry(entry.toEntity())
        Unit
    }

    override fun searchEntries(query: String): Flow<List<Entry>> {
        return databaseHolder.getDatabase().entryDao().searchEntries(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCategories(): Flow<List<Category>> {
        return databaseHolder.getDatabase().categoryDao().getCategories().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertCategory(category: Category) = withContext(ioDispatcher) {
        databaseHolder.getDatabase().categoryDao().insertCategory(category.toEntity())
        Unit
    }

    override suspend fun deleteCategory(category: Category) = withContext(ioDispatcher) {
        databaseHolder.getDatabase().categoryDao().deleteCategory(category.toEntity())
        Unit
    }

    override suspend fun unlock(password: CharArray): Result<Unit> = withContext(ioDispatcher) {
        try {
            var salt = settingsDataStore.saltFlow.first()
            if (salt == null) {
                // First launch: generate and save a secure 16-byte salt
                salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
                settingsDataStore.saveSalt(salt)
            }

            val derivedKey = KeyDerivation.deriveKey(password, salt)
            try {
                databaseHolder.openDatabase(derivedKey)
                // Access writableDatabase to force validation of encryption key / DB creation
                databaseHolder.getDatabase().openHelper.writableDatabase
                VaultLockState.unlock()
                Result.success(Unit)
            } catch (e: Exception) {
                // Decryption failure or DB initialization error
                databaseHolder.closeDatabase()
                Result.failure(e)
            } finally {
                // Zero out the key in memory immediately after use
                derivedKey.fill(0)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun changePassword(oldPassword: CharArray, newPassword: CharArray): Result<Unit> = withContext(ioDispatcher) {
        Result.success(Unit)
    }

    override fun isLocked(): StateFlow<Boolean> = VaultLockState.isLocked

    override fun lock() {
        databaseHolder.closeDatabase()
        VaultLockState.lock()
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
