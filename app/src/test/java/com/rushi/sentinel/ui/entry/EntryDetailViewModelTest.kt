package com.rushi.sentinel.ui.entry

import com.rushi.sentinel.data.repository.VaultRepository
import com.rushi.sentinel.domain.model.Entry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class EntryDetailViewModelTest {

    private val vaultRepository: VaultRepository = mock()
    private lateinit var viewModel: EntryDetailViewModel

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testPasswordMasking_defaultIsMasked() = runTest {
        viewModel = EntryDetailViewModel(vaultRepository)
        assertTrue(viewModel.isPasswordMasked.value)
    }

    @Test
    fun testTogglePasswordMask_unmasksAndPasswordMasksAfter30Seconds() = runTest {
        // Link the main dispatcher to the test scheduler for synchronized virtual time
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)

        viewModel = EntryDetailViewModel(vaultRepository)

        val entry = Entry(
            id = 1L,
            name = "Test Entry",
            username = "user",
            password = "pwd".toByteArray(),
            url = null,
            notes = null,
            categoryId = null,
            createdAt = 0L,
            updatedAt = 0L,
            lastAccessedAt = 0L
        )
        whenever(vaultRepository.getEntryById(any())).thenReturn(flowOf(entry))

        viewModel.loadEntry(1L)
        testScheduler.runCurrent()
        assertTrue(viewModel.isPasswordMasked.value)

        // Toggle mask to reveal password
        viewModel.togglePasswordMask()
        assertFalse(viewModel.isPasswordMasked.value)

        // Advance time by 29 seconds - should still be unmasked
        testScheduler.advanceTimeBy(29000L)
        testScheduler.runCurrent()
        assertFalse(viewModel.isPasswordMasked.value)

        // Advance by remaining 1 second (total 30 seconds) - should auto-mask
        testScheduler.advanceTimeBy(1000L)
        testScheduler.runCurrent()
        assertTrue(viewModel.isPasswordMasked.value)
    }
}
