package com.rushi.sentinel.ui.vault

import com.rushi.sentinel.data.repository.VaultRepository
import com.rushi.sentinel.domain.model.Category
import com.rushi.sentinel.domain.model.Entry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class VaultListViewModelTest {

    private val vaultRepository: VaultRepository = mock()
    private lateinit var viewModel: VaultListViewModel

    private val categories = listOf(
        Category(id = 1L, name = "Work", createdAt = 100L),
        Category(id = 2L, name = "Personal", createdAt = 200L)
    )

    private val entries = listOf(
        Entry(
            id = 1L, name = "Google Account", username = "google_user",
            password = "pwd".toByteArray(), url = "https://google.com",
            notes = null, categoryId = 1L, createdAt = 100L, updatedAt = 100L, lastAccessedAt = 100L
        ),
        Entry(
            id = 2L, name = "GitHub Account", username = "github_user",
            password = "pwd".toByteArray(), url = "https://github.com",
            notes = null, categoryId = 1L, createdAt = 200L, updatedAt = 200L, lastAccessedAt = 200L
        ),
        Entry(
            id = 3L, name = "Netflix Account", username = "netflix_user",
            password = "pwd".toByteArray(), url = "https://netflix.com",
            notes = null, categoryId = 2L, createdAt = 300L, updatedAt = 300L, lastAccessedAt = 300L
        )
    )

    @Before
    fun setUp() {
        val testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        whenever(vaultRepository.getCategories()).thenReturn(flowOf(categories))
        whenever(vaultRepository.getEntries()).thenReturn(flowOf(entries))

        viewModel = VaultListViewModel(vaultRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun startCollecting(scope: kotlinx.coroutines.CoroutineScope, scheduler: kotlinx.coroutines.test.TestCoroutineScheduler) {
        scope.launch(UnconfinedTestDispatcher(scheduler)) { viewModel.entries.collect {} }
        scope.launch(UnconfinedTestDispatcher(scheduler)) { viewModel.categories.collect {} }
    }

    @Test
    fun testDefaultState_showsAllEntriesAndCategories() = runTest {
        startCollecting(backgroundScope, testScheduler)
        testScheduler.runCurrent()
        assertEquals(entries, viewModel.entries.value)
        assertEquals(categories, viewModel.categories.value)
    }

    @Test
    fun testSearchQueryFiltering() = runTest {
        startCollecting(backgroundScope, testScheduler)
        testScheduler.runCurrent()
        
        viewModel.setSearchQuery("git")
        testScheduler.runCurrent()
        
        // Should only match GitHub Account
        assertEquals(1, viewModel.entries.value.size)
        assertEquals("GitHub Account", viewModel.entries.value.first().name)
    }

    @Test
    fun testCategoryFiltering() = runTest {
        startCollecting(backgroundScope, testScheduler)
        testScheduler.runCurrent()

        // Select "Work" category (id = 1)
        viewModel.selectCategory(1L)
        testScheduler.runCurrent()

        // Should filter out Netflix (which is Personal, id = 2)
        assertEquals(2, viewModel.entries.value.size)
        assertTrue(viewModel.entries.value.all { it.categoryId == 1L })
    }

    @Test
    fun testCombinedSearchAndCategoryFiltering() = runTest {
        startCollecting(backgroundScope, testScheduler)
        testScheduler.runCurrent()

        // Select "Work" category (id = 1)
        viewModel.selectCategory(1L)
        // Search for "Google"
        viewModel.setSearchQuery("Google")
        testScheduler.runCurrent()

        // Should only return Google Account (which has categoryId = 1 and contains "Google")
        assertEquals(1, viewModel.entries.value.size)
        assertEquals("Google Account", viewModel.entries.value.first().name)
    }

    @Test
    fun testSearchNoMatch_returnsEmptyList() = runTest {
        startCollecting(backgroundScope, testScheduler)
        testScheduler.runCurrent()

        viewModel.setSearchQuery("NotMatchingAnyNameOrUserOrUrl")
        testScheduler.runCurrent()

        assertTrue(viewModel.entries.value.isEmpty())
    }

    private fun assertTrue(value: Boolean) {
        org.junit.Assert.assertTrue(value)
    }
}
