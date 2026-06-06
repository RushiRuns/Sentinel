package com.rushi.sentinel.data.repository

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.rushi.sentinel.data.datastore.SettingsDataStore
import com.rushi.sentinel.data.db.DatabaseHolder
import com.rushi.sentinel.data.db.SentinelDatabase
import com.rushi.sentinel.ui.navigation.VaultLockState
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
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

class VaultRepositoryTest {

    private val databaseHolder: DatabaseHolder = mock()
    private val settingsDataStore: SettingsDataStore = mock()
    private lateinit var repository: VaultRepository

    private val sentinelDatabase: SentinelDatabase = mock()
    private val openHelper: SupportSQLiteOpenHelper = mock()
    private val sqLiteDatabase: SupportSQLiteDatabase = mock()

    @Before
    fun setUp() {
        VaultLockState.lock()
        repository = VaultRepositoryImpl(databaseHolder, settingsDataStore)

        whenever(databaseHolder.getDatabase()).thenReturn(sentinelDatabase)
        whenever(sentinelDatabase.openHelper).thenReturn(openHelper)
        whenever(openHelper.writableDatabase).thenReturn(sqLiteDatabase)
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
}
