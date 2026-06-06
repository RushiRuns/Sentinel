package com.rushi.sentinel.ui.categories

import com.rushi.sentinel.data.repository.VaultRepository
import com.rushi.sentinel.domain.model.Category
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesViewModelTest {

    private val vaultRepository: VaultRepository = mock()
    private lateinit var viewModel: CategoriesViewModel

    private val categories = listOf(
        Category(id = 1L, name = "Work", createdAt = 100L),
        Category(id = 2L, name = "Personal", createdAt = 200L)
    )

    @Before
    fun setUp() {
        val testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        whenever(vaultRepository.getCategories()).thenReturn(flowOf(categories))
        viewModel = CategoriesViewModel(vaultRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testCategoriesFlow_exposesRepositoryCategories() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.categories.collect {}
        }
        testScheduler.runCurrent()
        assertEquals(categories, viewModel.categories.value)
        assertNull(viewModel.error.value)
    }

    @Test
    fun testAddCategory_success() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.categories.collect {}
        }
        testScheduler.runCurrent()

        viewModel.addCategory("Finance")
        testScheduler.runCurrent()

        verify(vaultRepository).insertCategory(any())
        assertNull(viewModel.error.value)
    }

    @Test
    fun testAddCategory_blankName_setsError() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.categories.collect {}
        }
        testScheduler.runCurrent()

        viewModel.addCategory("   ")
        testScheduler.runCurrent()

        assertNotNull(viewModel.error.value)
        assertEquals("Category name cannot be empty", viewModel.error.value)
    }

    @Test
    fun testAddCategory_duplicateName_setsError() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.categories.collect {}
        }
        testScheduler.runCurrent()

        // Attempt to add "WORK" (case insensitive duplicate)
        viewModel.addCategory("WORK")
        testScheduler.runCurrent()

        assertNotNull(viewModel.error.value)
        assertEquals("Category 'WORK' already exists", viewModel.error.value)
    }

    @Test
    fun testDeleteCategory_callsRepository() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.categories.collect {}
        }
        testScheduler.runCurrent()
        val categoryToDelete = categories.first()

        viewModel.deleteCategory(categoryToDelete)
        testScheduler.runCurrent()

        verify(vaultRepository).deleteCategory(categoryToDelete)
        assertNull(viewModel.error.value)
    }
}
