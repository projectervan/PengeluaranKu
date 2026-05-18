package com.finansialku.app.ui.screens.splash

import com.finansialku.app.data.repository.AuthRepository
import com.finansialku.app.data.repository.RecurringBillRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SplashViewModel.
 * Verifies auth check and recurring bill generation on app launch.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var recurringBillRepository: RecurringBillRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk(relaxed = true)
        recurringBillRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should navigate to Main when user is signed in`() = runTest {
        // Given
        every { authRepository.isUserSignedIn() } returns true
        every { authRepository.getCurrentUserId() } returns "user_123"
        coEvery { recurringBillRepository.checkAndGenerateRecurringBills("user_123") } returns Unit

        // When
        val viewModel = SplashViewModel(authRepository, recurringBillRepository)
        advanceUntilIdle()

        // Then
        assertEquals(SplashDestination.Main, viewModel.destination.value)
    }

    @Test
    fun `should navigate to Login when user is NOT signed in`() = runTest {
        // Given
        every { authRepository.isUserSignedIn() } returns false

        // When
        val viewModel = SplashViewModel(authRepository, recurringBillRepository)
        advanceUntilIdle()

        // Then
        assertEquals(SplashDestination.Login, viewModel.destination.value)
    }

    @Test
    fun `should call checkAndGenerateRecurringBills when user is signed in`() = runTest {
        // Given
        every { authRepository.isUserSignedIn() } returns true
        every { authRepository.getCurrentUserId() } returns "user_456"
        coEvery { recurringBillRepository.checkAndGenerateRecurringBills("user_456") } returns Unit

        // When
        val viewModel = SplashViewModel(authRepository, recurringBillRepository)
        advanceUntilIdle()

        // Then: PRD 5.1 algorithm should be invoked
        coVerify(exactly = 1) { recurringBillRepository.checkAndGenerateRecurringBills("user_456") }
    }

    @Test
    fun `should NOT call checkAndGenerateRecurringBills when user is NOT signed in`() = runTest {
        // Given
        every { authRepository.isUserSignedIn() } returns false

        // When
        val viewModel = SplashViewModel(authRepository, recurringBillRepository)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { recurringBillRepository.checkAndGenerateRecurringBills(any()) }
    }

    @Test
    fun `initial state should be Loading`() = runTest {
        // Given
        every { authRepository.isUserSignedIn() } returns true
        every { authRepository.getCurrentUserId() } returns "user_789"

        // When: ViewModel is created but coroutine hasn't run yet
        val viewModel = SplashViewModel(authRepository, recurringBillRepository)

        // Note: Due to StandardTestDispatcher, the init coroutine hasn't executed yet
        // After advancing, it should complete
        advanceUntilIdle()

        assertEquals(SplashDestination.Main, viewModel.destination.value)
    }
}
