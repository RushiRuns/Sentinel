package com.rushi.sentinel.ui.generator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GeneratorViewModelTest {

    private lateinit var viewModel: GeneratorViewModel

    @Before
    fun setUp() {
        val testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        viewModel = GeneratorViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun startCollecting(scope: kotlinx.coroutines.CoroutineScope, scheduler: kotlinx.coroutines.test.TestCoroutineScheduler) {
        scope.launch(UnconfinedTestDispatcher(scheduler)) { viewModel.password.collect {} }
        scope.launch(UnconfinedTestDispatcher(scheduler)) { viewModel.length.collect {} }
        scope.launch(UnconfinedTestDispatcher(scheduler)) { viewModel.includeUppercase.collect {} }
        scope.launch(UnconfinedTestDispatcher(scheduler)) { viewModel.includeLowercase.collect {} }
        scope.launch(UnconfinedTestDispatcher(scheduler)) { viewModel.includeNumbers.collect {} }
        scope.launch(UnconfinedTestDispatcher(scheduler)) { viewModel.includeSymbols.collect {} }
        scope.launch(UnconfinedTestDispatcher(scheduler)) { viewModel.strength.collect {} }
    }

    @Test
    fun testDefaultState_generatesSecurePasswordOfLength16() = runTest {
        startCollecting(backgroundScope, testScheduler)
        testScheduler.runCurrent()

        val pwd = viewModel.password.value
        assertEquals(16, pwd.length)
        assertEquals(16, viewModel.length.value)
        assertTrue(viewModel.includeUppercase.value)
        assertTrue(viewModel.includeLowercase.value)
        assertTrue(viewModel.includeNumbers.value)
        assertTrue(viewModel.includeSymbols.value)
        assertEquals(PasswordStrength.STRONG, viewModel.strength.value)
    }

    @Test
    fun testLengthAdjustment_generatesCorrectLength() = runTest {
        startCollecting(backgroundScope, testScheduler)
        testScheduler.runCurrent()

        viewModel.setLength(24)
        testScheduler.runCurrent()

        assertEquals(24, viewModel.password.value.length)
        assertEquals(24, viewModel.length.value)

        viewModel.setLength(8)
        testScheduler.runCurrent()

        assertEquals(8, viewModel.password.value.length)
        assertEquals(8, viewModel.length.value)
    }

    @Test
    fun testDisableUppercase_containsNoUppercase() = runTest {
        startCollecting(backgroundScope, testScheduler)
        testScheduler.runCurrent()

        viewModel.toggleUppercase()
        testScheduler.runCurrent()

        assertFalse(viewModel.includeUppercase.value)
        val pwd = viewModel.password.value
        assertTrue(pwd.none { it.isUpperCase() })
    }

    @Test
    fun testDisableLowercase_containsNoLowercase() = runTest {
        startCollecting(backgroundScope, testScheduler)
        testScheduler.runCurrent()

        viewModel.toggleLowercase()
        testScheduler.runCurrent()

        assertFalse(viewModel.includeLowercase.value)
        val pwd = viewModel.password.value
        assertTrue(pwd.none { it.isLowerCase() })
    }

    @Test
    fun testDisableNumbers_containsNoNumbers() = runTest {
        startCollecting(backgroundScope, testScheduler)
        testScheduler.runCurrent()

        viewModel.toggleNumbers()
        testScheduler.runCurrent()

        assertFalse(viewModel.includeNumbers.value)
        val pwd = viewModel.password.value
        assertTrue(pwd.none { it.isDigit() })
    }

    @Test
    fun testDisableSymbols_containsNoSymbols() = runTest {
        startCollecting(backgroundScope, testScheduler)
        testScheduler.runCurrent()

        viewModel.toggleSymbols()
        testScheduler.runCurrent()

        assertFalse(viewModel.includeSymbols.value)
        val pwd = viewModel.password.value
        val symbols = "!@#$%^&*()_-+=[]{}|;:',.<>?/"
        assertTrue(pwd.none { it in symbols })
    }

    @Test
    fun testDisableAllToggles_fallbackEnforcesAtLeastOne() = runTest {
        startCollecting(backgroundScope, testScheduler)
        testScheduler.runCurrent()

        // Turn off upper, lower, numbers. Symbols should remain the last active.
        viewModel.toggleUppercase()
        viewModel.toggleLowercase()
        viewModel.toggleNumbers()
        testScheduler.runCurrent()

        assertTrue(viewModel.includeSymbols.value)

        // Try to toggle symbols (last remaining). It should ignore the click and stay true.
        viewModel.toggleSymbols()
        testScheduler.runCurrent()

        assertTrue(viewModel.includeSymbols.value)
        val symbols = "!@#$%^&*()_-+=[]{}|;:',.<>?/"
        val pwd = viewModel.password.value
        assertTrue(pwd.all { it in symbols })
    }

    @Test
    fun testStrengthCalculation_shortPasswordIsWeak() = runTest {
        startCollecting(backgroundScope, testScheduler)
        testScheduler.runCurrent()

        viewModel.setLength(8)
        testScheduler.runCurrent()

        assertEquals(PasswordStrength.WEAK, viewModel.strength.value)
    }

    @Test
    fun testStrengthCalculation_longPasswordWithFewCharsetsIsMedium() = runTest {
        startCollecting(backgroundScope, testScheduler)
        testScheduler.runCurrent()

        viewModel.setLength(20)
        // Disable upper, lower. Active = numbers, symbols (count = 2)
        viewModel.toggleUppercase()
        viewModel.toggleLowercase()
        testScheduler.runCurrent()

        assertEquals(PasswordStrength.MEDIUM, viewModel.strength.value)
    }
}
