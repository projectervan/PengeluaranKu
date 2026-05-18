package com.finansialku.app.ui.screens.dashboard

import com.finansialku.app.data.entity.CategoryExpense
import com.finansialku.app.data.entity.TransactionEntity
import com.finansialku.app.data.repository.AuthRepository
import com.finansialku.app.data.repository.TransactionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for DashboardViewModel.
 * Verifies correct aggregation and balance calculation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var authRepository: AuthRepository
    private val testDispatcher = StandardTestDispatcher()
    private val testUserId = "dashboard_user"

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        transactionRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)

        every { authRepository.getCurrentUserId() } returns testUserId
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `balance should be income minus expense`() = runTest {
        // Given
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1

        every {
            transactionRepository.observeTotalIncomeByMonth(testUserId, year, month)
        } returns flowOf(5000000.0)

        every {
            transactionRepository.observeTotalExpenseByMonth(testUserId, year, month)
        } returns flowOf(2000000.0)

        every {
            transactionRepository.observeExpenseByCategory(testUserId, year, month)
        } returns flowOf(listOf(CategoryExpense("Makanan", 1000000.0), CategoryExpense("Transport", 1000000.0)))

        every {
            transactionRepository.observeRecentTransactions(testUserId, 5)
        } returns flowOf(emptyList())

        // When
        val viewModel = DashboardViewModel(transactionRepository, authRepository)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(5000000.0, state.totalIncome, 0.01)
        assertEquals(2000000.0, state.totalExpense, 0.01)
        assertEquals(3000000.0, state.balance, 0.01) // 5M - 2M = 3M
    }

    @Test
    fun `balance should be negative when expense exceeds income`() = runTest {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1

        every {
            transactionRepository.observeTotalIncomeByMonth(testUserId, year, month)
        } returns flowOf(1000000.0)

        every {
            transactionRepository.observeTotalExpenseByMonth(testUserId, year, month)
        } returns flowOf(3000000.0)

        every {
            transactionRepository.observeExpenseByCategory(testUserId, year, month)
        } returns flowOf(emptyList())

        every {
            transactionRepository.observeRecentTransactions(testUserId, 5)
        } returns flowOf(emptyList())

        // When
        val viewModel = DashboardViewModel(transactionRepository, authRepository)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(-2000000.0, state.balance, 0.01)
    }

    @Test
    fun `should display correct current month and year`() = runTest {
        val calendar = Calendar.getInstance()
        val expectedMonth = calendar.get(Calendar.MONTH) + 1
        val expectedYear = calendar.get(Calendar.YEAR)

        every {
            transactionRepository.observeTotalIncomeByMonth(testUserId, expectedYear, expectedMonth)
        } returns flowOf(0.0)
        every {
            transactionRepository.observeTotalExpenseByMonth(testUserId, expectedYear, expectedMonth)
        } returns flowOf(0.0)
        every {
            transactionRepository.observeExpenseByCategory(testUserId, expectedYear, expectedMonth)
        } returns flowOf(emptyList())
        every {
            transactionRepository.observeRecentTransactions(testUserId, 5)
        } returns flowOf(emptyList())

        val viewModel = DashboardViewModel(transactionRepository, authRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(expectedMonth, state.currentMonth)
        assertEquals(expectedYear, state.currentYear)
    }

    @Test
    fun `recent transactions should display max 5 items`() = runTest {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1

        val transactions = (1..5).map { i ->
            TransactionEntity(
                id = "tx_$i",
                userId = testUserId,
                type = "EXPENSE",
                amount = (i * 10000).toDouble(),
                category = "Test",
                date = System.currentTimeMillis()
            )
        }

        every {
            transactionRepository.observeTotalIncomeByMonth(testUserId, year, month)
        } returns flowOf(0.0)
        every {
            transactionRepository.observeTotalExpenseByMonth(testUserId, year, month)
        } returns flowOf(0.0)
        every {
            transactionRepository.observeExpenseByCategory(testUserId, year, month)
        } returns flowOf(emptyList())
        every {
            transactionRepository.observeRecentTransactions(testUserId, 5)
        } returns flowOf(transactions)

        val viewModel = DashboardViewModel(transactionRepository, authRepository)
        advanceUntilIdle()

        assertEquals(5, viewModel.uiState.value.recentTransactions.size)
    }
}
