package com.finansialku.app.ui.screens.transactions

import com.finansialku.app.data.entity.TransactionEntity
import com.finansialku.app.data.repository.AuthRepository
import com.finansialku.app.data.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TransactionsViewModel.
 * Tests form validation, CRUD, and month navigation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: TransactionsViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val testUserId = "txn_test_user"

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        transactionRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)

        every { authRepository.getCurrentUserId() } returns testUserId
        every {
            transactionRepository.observeTransactionsByMonth(testUserId, any(), any())
        } returns flowOf(emptyList())

        viewModel = TransactionsViewModel(transactionRepository, authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `form validation should fail when amount is empty`() = runTest {
        viewModel.initNewTransaction()
        viewModel.updateFormCategory("Makanan & Minuman")
        // amount is empty
        viewModel.saveTransaction()
        advanceUntilIdle()

        val formState = viewModel.formState.value
        assertEquals("Nominal harus minimal Rp 1", formState.error)
        assertFalse(formState.saveSuccess)
    }

    @Test
    fun `form validation should fail when amount is zero`() = runTest {
        viewModel.initNewTransaction()
        viewModel.updateFormAmount("0")
        viewModel.updateFormCategory("Makanan & Minuman")
        viewModel.saveTransaction()
        advanceUntilIdle()

        val formState = viewModel.formState.value
        assertEquals("Nominal harus minimal Rp 1", formState.error)
    }

    @Test
    fun `form validation should fail when category is empty`() = runTest {
        viewModel.initNewTransaction()
        viewModel.updateFormAmount("50000")
        // category is empty
        viewModel.saveTransaction()
        advanceUntilIdle()

        val formState = viewModel.formState.value
        assertEquals("Kategori tidak boleh kosong", formState.error)
    }

    @Test
    fun `form validation should pass with valid inputs`() = runTest {
        viewModel.initNewTransaction()
        viewModel.updateFormAmount("50000")
        viewModel.updateFormCategory("Makanan & Minuman")
        viewModel.updateFormType("EXPENSE")

        coEvery { transactionRepository.createNewTransaction(any(), any(), any(), any(), any(), any()) } returns
                TransactionEntity(
                    id = "new_id",
                    userId = testUserId,
                    type = "EXPENSE",
                    amount = 50000.0,
                    category = "Makanan & Minuman",
                    date = System.currentTimeMillis()
                )

        viewModel.saveTransaction()
        advanceUntilIdle()

        val formState = viewModel.formState.value
        assertTrue(formState.saveSuccess)
    }

    @Test
    fun `updateFormAmount should only allow digits`() = runTest {
        viewModel.initNewTransaction()
        viewModel.updateFormAmount("123abc456")

        assertEquals("123456", viewModel.formState.value.amount)
    }

    @Test
    fun `updateFormType should reset category`() = runTest {
        viewModel.initNewTransaction()
        viewModel.updateFormCategory("Gaji")
        viewModel.updateFormType("EXPENSE")

        assertEquals("EXPENSE", viewModel.formState.value.type)
        assertEquals("", viewModel.formState.value.category)
    }

    @Test
    fun `deleteTransaction should call repository`() = runTest {
        val transaction = TransactionEntity(
            id = "tx_del",
            userId = testUserId,
            type = "EXPENSE",
            amount = 30000.0,
            category = "Makanan",
            date = System.currentTimeMillis()
        )

        viewModel.deleteTransaction(transaction)
        advanceUntilIdle()

        coVerify(exactly = 1) { transactionRepository.deleteTransaction(transaction) }
    }

    @Test
    fun `initNewTransaction should reset form state`() = runTest {
        viewModel.updateFormAmount("99999")
        viewModel.updateFormCategory("Test")

        viewModel.initNewTransaction()

        val form = viewModel.formState.value
        assertEquals("", form.amount)
        assertEquals("", form.category)
        assertEquals("EXPENSE", form.type)
        assertFalse(form.isEditing)
    }

    @Test
    fun `initEditTransaction should populate form`() = runTest {
        val transaction = TransactionEntity(
            id = "tx_edit",
            userId = testUserId,
            type = "INCOME",
            amount = 5000000.0,
            category = "Gaji",
            date = 1700000000000L,
            note = "Gaji bulanan"
        )

        viewModel.initEditTransaction(transaction)

        val form = viewModel.formState.value
        assertEquals("tx_edit", form.id)
        assertEquals("INCOME", form.type)
        assertEquals("5000000", form.amount)
        assertEquals("Gaji", form.category)
        assertEquals(1700000000000L, form.date)
        assertEquals("Gaji bulanan", form.note)
        assertTrue(form.isEditing)
    }
}
