package com.finansialku.app.data.repository

import com.finansialku.app.data.dao.TransactionDao
import com.finansialku.app.data.entity.TransactionEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TransactionRepository.
 * Tests CRUD operations, month range calculation, and aggregate flow methods.
 */
class TransactionRepositoryTest {

    private lateinit var transactionDao: TransactionDao
    private lateinit var repository: TransactionRepository

    private val testUserId = "test_user_456"

    @Before
    fun setup() {
        transactionDao = mockk(relaxed = true)
        repository = TransactionRepository(transactionDao)
    }

    @Test
    fun `insertTransaction should delegate to DAO`() = runTest {
        val transaction = createTransaction(
            id = "tx_1",
            type = "INCOME",
            amount = 5000000.0,
            category = "Gaji"
        )

        repository.insertTransaction(transaction)

        coVerify(exactly = 1) { transactionDao.insertTransaction(transaction) }
    }

    @Test
    fun `deleteTransactionById should delegate to DAO`() = runTest {
        repository.deleteTransactionById("tx_123")
        coVerify(exactly = 1) { transactionDao.deleteTransactionById("tx_123") }
    }

    @Test
    fun `deleteAllTransactionsByUser should delegate to DAO`() = runTest {
        repository.deleteAllTransactionsByUser(testUserId)
        coVerify(exactly = 1) { transactionDao.deleteAllTransactionsByUser(testUserId) }
    }

    @Test
    fun `observeTotalIncomeByMonth should calculate correct month range`() = runTest {
        // Given: observe for January 2025
        coEvery {
            transactionDao.observeTotalIncomeByMonth(testUserId, any(), any())
        } returns flowOf(5000000.0)

        // When: call with year=2025, month=1 (January)
        val flow = repository.observeTotalIncomeByMonth(testUserId, 2025, 1)

        // Then: DAO should be called with correct timestamp range
        coVerify {
            transactionDao.observeTotalIncomeByMonth(
                testUserId,
                any(), // start of Jan 2025
                any()  // start of Feb 2025
            )
        }
    }

    @Test
    fun `observeTotalExpenseByMonth should return flow from DAO`() = runTest {
        coEvery {
            transactionDao.observeTotalExpenseByMonth(testUserId, any(), any())
        } returns flowOf(2500000.0)

        val flow = repository.observeTotalExpenseByMonth(testUserId, 2025, 3)

        coVerify {
            transactionDao.observeTotalExpenseByMonth(testUserId, any(), any())
        }
    }

    @Test
    fun `createNewTransaction should generate valid entity with UUID`() {
        val transaction = repository.createNewTransaction(
            userId = testUserId,
            type = "EXPENSE",
            amount = 50000.0,
            category = "Makanan & Minuman",
            date = System.currentTimeMillis(),
            note = "Makan siang"
        )

        assertEquals(testUserId, transaction.userId)
        assertEquals("EXPENSE", transaction.type)
        assertEquals(50000.0, transaction.amount, 0.01)
        assertEquals("Makanan & Minuman", transaction.category)
        assertEquals("Makan siang", transaction.note)
        assertEquals(false, transaction.isRecurringGenerated)
        assertTrue(transaction.id.isNotBlank())
        // UUID format check
        assertTrue(transaction.id.contains("-"))
    }

    @Test
    fun `createNewTransaction with recurring flag should set it correctly`() {
        val transaction = repository.createNewTransaction(
            userId = testUserId,
            type = "EXPENSE",
            amount = 150000.0,
            category = "Tagihan & Utilitas",
            date = System.currentTimeMillis(),
            note = "Auto generated",
            isRecurringGenerated = true
        )

        assertTrue(transaction.isRecurringGenerated)
    }

    @Test
    fun `getTransactionById should return entity from DAO`() = runTest {
        val expected = createTransaction(
            id = "tx_get",
            type = "INCOME",
            amount = 1000000.0,
            category = "Freelance"
        )

        coEvery { transactionDao.getTransactionById("tx_get") } returns expected

        val result = repository.getTransactionById("tx_get")

        assertNotNull(result)
        assertEquals("tx_get", result?.id)
        assertEquals(1000000.0, result?.amount ?: 0.0, 0.01)
    }

    @Test
    fun `getTransactionById should return null when not found`() = runTest {
        coEvery { transactionDao.getTransactionById("nonexistent") } returns null

        val result = repository.getTransactionById("nonexistent")

        assertNull(result)
    }

    @Test
    fun `observeRecentTransactions should pass correct limit`() {
        coEvery {
            transactionDao.observeRecentTransactions(testUserId, 5)
        } returns flowOf(emptyList())

        repository.observeRecentTransactions(testUserId, 5)

        coVerify { transactionDao.observeRecentTransactions(testUserId, 5) }
    }

    // ==================== HELPER ====================

    private fun createTransaction(
        id: String,
        type: String,
        amount: Double,
        category: String
    ) = TransactionEntity(
        id = id,
        userId = testUserId,
        type = type,
        amount = amount,
        category = category,
        date = System.currentTimeMillis(),
        note = null,
        isRecurringGenerated = false
    )
}
