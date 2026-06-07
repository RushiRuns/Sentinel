package com.rushi.sentinel.data.repository

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.rushi.sentinel.data.datastore.SettingsDataStore
import com.rushi.sentinel.data.db.DatabaseHolder
import com.rushi.sentinel.data.db.SentinelDatabase
import com.rushi.sentinel.data.db.dao.CategoryDao
import com.rushi.sentinel.data.db.dao.EntryDao
import com.rushi.sentinel.data.db.entity.CategoryEntity
import com.rushi.sentinel.data.db.entity.EntryEntity
import com.rushi.sentinel.ui.navigation.VaultLockState
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.security.SecureRandom
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VaultRepositoryTest {

    private val databaseHolder: DatabaseHolder = mock()
    private val settingsDataStore: SettingsDataStore = mock()
    private lateinit var repository: VaultRepository

    private val sentinelDatabase: SentinelDatabase = mock()
    private val openHelper: SupportSQLiteOpenHelper = mock()
    private val sqLiteDatabase: SupportSQLiteDatabase = mock()
    private val categoryDao: CategoryDao = mock()
    private val entryDao: EntryDao = mock()

    @Before
    fun setUp() {
        VaultLockState.lock()
        repository = VaultRepositoryImpl(databaseHolder, settingsDataStore)

        whenever(databaseHolder.getDatabase()).thenReturn(sentinelDatabase)
        whenever(databaseHolder.getDatabaseFile()).thenReturn(java.io.File("dummy.db"))
        whenever(sentinelDatabase.openHelper).thenReturn(openHelper)
        whenever(openHelper.writableDatabase).thenReturn(sqLiteDatabase)
        whenever(sentinelDatabase.categoryDao()).thenReturn(categoryDao)
        whenever(sentinelDatabase.entryDao()).thenReturn(entryDao)
    }

    @Test
    fun testUnlock_success_withExistingSalt() = runTest {
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        whenever(settingsDataStore.saltFlow).thenReturn(flowOf(salt))

        val password = "CorrectPassword".toCharArray()
        val result = repository.unlock(password)

        assertTrue(result.isSuccess)
        assertFalse(repository.isLocked().value)
        verify(databaseHolder).openDatabase(any())
    }

    @Test
    fun testUnlock_success_firstLaunchGeneratesSalt() = runTest {
        whenever(settingsDataStore.saltFlow).thenReturn(flowOf(null))
        whenever(settingsDataStore.saveSalt(any())).thenAnswer {}

        val password = "NewPassword".toCharArray()
        val result = repository.unlock(password)

        assertTrue(result.isSuccess)
        assertFalse(repository.isLocked().value)
        verify(settingsDataStore).saveSalt(any())
        verify(databaseHolder).openDatabase(any())
    }

    @Test
    fun testUnlock_failure_wrongPassword() = runTest {
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        whenever(settingsDataStore.saltFlow).thenReturn(flowOf(salt))

        // Simulate decryption failure when accessing writableDatabase
        whenever(openHelper.writableDatabase).thenThrow(RuntimeException("Decryption failed"))

        val password = "WrongPassword".toCharArray()
        val result = repository.unlock(password)

        assertTrue(result.isFailure)
        assertTrue(repository.isLocked().value)
        verify(databaseHolder).closeDatabase()
    }

    @Test
    fun testBackupExportAndImport_success() = runTest {
        // Unlock repository
        VaultLockState.unlock()

        // Mock data
        val categories = listOf(CategoryEntity(1L, "Work", 1000L))
        val entries = listOf(
            EntryEntity(
                id = 1L,
                name = "Google",
                username = "rushi",
                password = "MyPassword".toByteArray(Charsets.UTF_8),
                url = "https://google.com",
                notes = "My notes",
                categoryId = 1L,
                isFavorite = true,
                createdAt = 1000L,
                updatedAt = 1000L,
                lastAccessedAt = 1000L
            )
        )

        whenever(categoryDao.getCategories()).thenReturn(flowOf(categories))
        whenever(entryDao.getEntries()).thenReturn(flowOf(entries))

        val password = "BackupPassword123!".toCharArray()

        // Act: Export
        val exportResult = repository.exportBackup(password.clone())
        assertTrue(exportResult.isSuccess)
        val backupBytes = exportResult.getOrThrow()

        // Mock transaction and insertion for Import
        whenever(sentinelDatabase.runInTransaction(any())).thenAnswer { invocation ->
            val runnable = invocation.getArgument<Runnable>(0)
            runnable.run()
            null
        }

        // Act: Import
        val importResult = repository.importBackup(backupBytes, password)
        assertTrue(importResult.isSuccess)

        // Assert
        verify(sentinelDatabase).clearAllTables()
        verify(categoryDao).insertCategory(any())
        verify(entryDao).insertEntry(any())
    }

    @Test
    fun testChangePassword_success() = runTest {
        // Unlock first
        VaultLockState.unlock()

        val oldPassword = "OldPassword".toCharArray()
        val newPassword = "NewPassword".toCharArray()

        // Act
        val result = repository.changePassword(oldPassword, newPassword)

        // Assert
        assertTrue(result.isSuccess)
        verify(sqLiteDatabase).execSQL(org.mockito.kotlin.argThat { this.startsWith("PRAGMA rekey = ") })
        verify(settingsDataStore).saveSalt(any())
        verify(databaseHolder).closeDatabase()
        verify(databaseHolder).openDatabase(any())
    }
}
