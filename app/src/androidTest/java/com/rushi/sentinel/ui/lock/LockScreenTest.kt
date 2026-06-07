package com.rushi.sentinel.ui.lock

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.rushi.sentinel.crypto.BiometricKeyManager
import com.rushi.sentinel.data.datastore.SettingsDataStore
import com.rushi.sentinel.data.repository.VaultRepository
import com.rushi.sentinel.domain.model.Category
import com.rushi.sentinel.domain.model.Entry
import com.rushi.sentinel.ui.theme.SentinelTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LockScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var fakeRepository: FakeVaultRepository
    private lateinit var viewModel: LockViewModel

    @Before
    fun setUp() {
        settingsDataStore = SettingsDataStore(context)
        runBlocking {
            settingsDataStore.clearAll()
        }
        fakeRepository = FakeVaultRepository()
        viewModel = LockViewModel(fakeRepository, settingsDataStore, BiometricKeyManager())
    }

    @Test
    fun testSetupMode_displaysSetupFieldsAndSubmits() {
        // Vault has no salt yet, so it should start in setup mode.
        composeTestRule.setContent {
            SentinelTheme {
                LockScreen(viewModel = viewModel)
            }
        }

        // Verify elements on setup screen
        composeTestRule.onNodeWithText("Initialize Vault").assertExists()
        composeTestRule.onNodeWithText("Create Master Password").assertExists()
        composeTestRule.onNodeWithText("Confirm Master Password").assertExists()

        // Enter mismatching passwords
        composeTestRule.onNodeWithText("Create Master Password").performTextInput("password123")
        composeTestRule.onNodeWithText("Confirm Master Password").performTextInput("password456")
        composeTestRule.onNodeWithText("Initialize Vault").performClick()

        // Verify error display
        composeTestRule.onNodeWithText("Passwords do not match").assertExists()
    }

    @Test
    fun testUnlockMode_displaysUnlockFieldsAndFails() = runBlocking {
        // Set an existing salt to trigger unlock mode
        settingsDataStore.saveSalt(ByteArray(16))

        composeTestRule.setContent {
            SentinelTheme {
                LockScreen(viewModel = viewModel)
            }
        }

        // Verify elements on unlock screen
        composeTestRule.onNodeWithText("Decrypt Vault").assertExists()
        composeTestRule.onNodeWithText("Master Password").assertExists()

        // Enter wrong password
        fakeRepository.shouldSucceed = false
        composeTestRule.onNodeWithText("Master Password").performTextInput("WrongPassword")
        composeTestRule.onNodeWithText("Decrypt Vault").performClick()

        // Wait for coroutines and UI update
        composeTestRule.waitForIdle()

        // Verify error is shown
        composeTestRule.onNodeWithText("Incorrect master password").assertExists()
    }

    private class FakeVaultRepository : VaultRepository {
        var shouldSucceed: Boolean = true
        private val _isLocked = MutableStateFlow(true)

        override fun getEntries(): Flow<List<Entry>> = flowOf(emptyList())
        override fun getEntryById(id: Long): Flow<Entry?> = flowOf(null)
        override suspend fun insertEntry(entry: Entry) {}
        override suspend fun updateEntry(entry: Entry) {}
        override suspend fun deleteEntry(entry: Entry) {}
        override fun searchEntries(query: String): Flow<List<Entry>> = flowOf(emptyList())
        override fun getCategories(): Flow<List<Category>> = flowOf(emptyList())
        override suspend fun insertCategory(category: Category) {}
        override suspend fun deleteCategory(category: Category) {}

        override suspend fun unlock(password: CharArray): Result<Unit> {
            return if (shouldSucceed) {
                _isLocked.value = false
                Result.success(Unit)
            } else {
                Result.failure(Exception("Decryption failure"))
            }
        }

        override suspend fun changePassword(
            oldPassword: CharArray,
            newPassword: CharArray
        ): Result<Unit> = Result.success(Unit)

        override fun isLocked(): StateFlow<Boolean> = _isLocked.asStateFlow()

        override fun lock() {
            _isLocked.value = true
        }
    }
}
