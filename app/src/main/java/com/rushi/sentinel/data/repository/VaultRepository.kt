package com.rushi.sentinel.data.repository

import com.rushi.sentinel.domain.model.Entry
import com.rushi.sentinel.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface VaultRepository {
    // Entries
    fun getEntries(): Flow<List<Entry>>
    fun getEntryById(id: Long): Flow<Entry?>
    suspend fun insertEntry(entry: Entry)
    suspend fun updateEntry(entry: Entry)
    suspend fun deleteEntry(entry: Entry)
    fun searchEntries(query: String): Flow<List<Entry>>

    // Categories
    fun getCategories(): Flow<List<Category>>
    suspend fun insertCategory(category: Category)
    suspend fun deleteCategory(category: Category)

    // Security
    suspend fun unlock(password: CharArray): Result<Unit>
    suspend fun changePassword(oldPassword: CharArray, newPassword: CharArray): Result<Unit>
    fun isLocked(): StateFlow<Boolean>
    fun lock()

    // Backup & Restore
    suspend fun exportBackup(password: CharArray): Result<ByteArray>
    suspend fun importBackup(backupData: ByteArray, password: CharArray): Result<Unit>
}
