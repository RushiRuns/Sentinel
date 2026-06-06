# Contract: VaultRepository

## Purpose
The `VaultRepository` is the sole source of truth for vault data. It handles mapping between Room entities and domain models and ensures all database operations are performed on the correct dispatcher.

## Interface (Kotlin)

```kotlin
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
}
```

## Security Constraints
- **Zeroing**: Any `CharArray` passed to `unlock` or `changePassword` MUST be zeroed by the caller in a `finally` block.
- **Dispatcher**: All implementations MUST use `Dispatchers.IO` for database and cryptographic operations.
- **Encapsulation**: The repository MUST NOT expose Room entities (`EntryEntity`, `CategoryEntity`) to the UI layer.
