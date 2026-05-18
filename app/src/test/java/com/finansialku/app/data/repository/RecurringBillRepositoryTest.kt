package com.finansialku.app.data.repository

import com.finansialku.app.data.dao.RecurringBillDao
import com.finansialku.app.data.dao.TransactionDao
import com.finansialku.app.data.entity.RecurringBillEntity
import com.finansialku.app.data.entity.TransactionEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for RecurringBillRepository.checkAndGenerateRecurringBills()
 *
 * Tests cover the PRD Section 5.1 algorithm:
 * - Bills not yet generated for this month should generate transactions when due_day <= current_day
 * - Bills already generated for this month should NOT generate again
 * - Inactive bills should be skipped
 * - Bills with due_day in the future (current_day < due_day) should NOT generate yet
 */
class RecurringBillRepositoryTest {

    private lateinit var recurringBillDao: RecurringBillDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var repository: RecurringBillRepository

    private val testUserId = "test_user_123"

    @Before
    fun setup() {
        recurringBillDao = mockk(relaxed = true)
        transactionDao = mockk(relaxed = true)
        repository = RecurringBillRepository(recurringBillDao, transactionDao)
    }

    @Test
    fun `should generate transaction when due day has passed and not yet generated this month`() = runTest {
        // Given: A bill with dueDay=1, and today is the 15th, not yet generated
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)

        val bill = createBill(
            id = "bill_1",
            billName = "Internet WiFi",
            amount = 350000.0,
            category = "Tagihan & Utilitas",
            dueDay = 1,
            isActive = true,
            lastGeneratedMonth = null,
            lastGeneratedYear = null
        )

        coEvery { recurringBillDao.getActiveRecurringBills(testUserId) } returns listOf(bill)

        // When
        repository.checkAndGenerateRecurringBills(testUserId)

        // Then: A transaction should be inserted
        val transactionSlot = slot<TransactionEntity>()
        coVerify(exactly = 1) { transactionDao.insertTransaction(capture(transactionSlot)) }

        val transaction = transactionSlot.captured
        assertEquals("EXPENSE", transaction.type)
        assertEquals(350000.0, transaction.amount, 0.01)
        assertEquals("Tagihan & Utilitas", transaction.category)
        assertEquals(testUserId, transaction.userId)
        assertTrue(transaction.isRecurringGenerated)
        assertTrue(transaction.note!!.contains("Internet WiFi"))

        // And: last_generated should be updated
        coVerify(exactly = 1) {
            recurringBillDao.updateLastGenerated("bill_1", currentMonth, currentYear)
        }
    }

    @Test
    fun `should NOT generate transaction when already generated this month`() = runTest {
        // Given: Bill already generated for current month
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)

        val bill = createBill(
            id = "bill_2",
            billName = "Kost",
            amount = 1500000.0,
            category = "Kost / Sewa",
            dueDay = 1,
            isActive = true,
            lastGeneratedMonth = currentMonth,
            lastGeneratedYear = currentYear
        )

        coEvery { recurringBillDao.getActiveRecurringBills(testUserId) } returns listOf(bill)

        // When
        repository.checkAndGenerateRecurringBills(testUserId)

        // Then: No transaction should be inserted
        coVerify(exactly = 0) { transactionDao.insertTransaction(any()) }
        coVerify(exactly = 0) { recurringBillDao.updateLastGenerated(any(), any(), any()) }
    }

    @Test
    fun `should NOT generate transaction when due day has not yet come`() = runTest {
        // Given: A bill with dueDay=31 (future)
        val bill = createBill(
            id = "bill_3",
            billName = "Cicilan Motor",
            amount = 900000.0,
            category = "Cicilan",
            dueDay = 31, // Always in the future unless today is the 31st
            isActive = true,
            lastGeneratedMonth = null,
            lastGeneratedYear = null
        )

        // Only test this if today is NOT the 31st
        val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        if (today < 31) {
            coEvery { recurringBillDao.getActiveRecurringBills(testUserId) } returns listOf(bill)

            // When
            repository.checkAndGenerateRecurringBills(testUserId)

            // Then: No transaction should be inserted (due day hasn't come)
            coVerify(exactly = 0) { transactionDao.insertTransaction(any()) }
        }
    }

    @Test
    fun `should only process active bills`() = runTest {
        // Given: getActiveRecurringBills already filters by is_active = true
        // But let's verify the DAO method called is correct
        coEvery { recurringBillDao.getActiveRecurringBills(testUserId) } returns emptyList()

        // When
        repository.checkAndGenerateRecurringBills(testUserId)

        // Then: Should call getActiveRecurringBills (not getAllBills)
        coVerify(exactly = 1) { recurringBillDao.getActiveRecurringBills(testUserId) }
        coVerify(exactly = 0) { transactionDao.insertTransaction(any()) }
    }

    @Test
    fun `should generate transactions for multiple eligible bills`() = runTest {
        // Given: 3 bills, 2 eligible (dueDay <= today), 1 already generated
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        val bill1 = createBill(
            id = "bill_a",
            billName = "WiFi",
            amount = 300000.0,
            category = "Internet & WiFi",
            dueDay = 1, // Already passed
            isActive = true,
            lastGeneratedMonth = null,
            lastGeneratedYear = null
        )

        val bill2 = createBill(
            id = "bill_b",
            billName = "PDAM",
            amount = 100000.0,
            category = "Tagihan & Utilitas",
            dueDay = 1, // Already passed
            isActive = true,
            lastGeneratedMonth = null,
            lastGeneratedYear = null
        )

        val bill3 = createBill(
            id = "bill_c",
            billName = "Listrik",
            amount = 200000.0,
            category = "Tagihan & Utilitas",
            dueDay = 1,
            isActive = true,
            lastGeneratedMonth = currentMonth, // Already generated
            lastGeneratedYear = currentYear
        )

        coEvery { recurringBillDao.getActiveRecurringBills(testUserId) } returns listOf(bill1, bill2, bill3)

        // When
        repository.checkAndGenerateRecurringBills(testUserId)

        // Then: Only 2 transactions should be generated (bill1 and bill2)
        coVerify(exactly = 2) { transactionDao.insertTransaction(any()) }
        coVerify(exactly = 1) { recurringBillDao.updateLastGenerated("bill_a", currentMonth, currentYear) }
        coVerify(exactly = 1) { recurringBillDao.updateLastGenerated("bill_b", currentMonth, currentYear) }
        coVerify(exactly = 0) { recurringBillDao.updateLastGenerated("bill_c", any(), any()) }
    }

    @Test
    fun `should generate for bill from previous year not yet generated this month`() = runTest {
        // Given: Bill was last generated last year December
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)

        val bill = createBill(
            id = "bill_prev_year",
            billName = "Asuransi",
            amount = 500000.0,
            category = "Asuransi",
            dueDay = 1,
            isActive = true,
            lastGeneratedMonth = 12,
            lastGeneratedYear = currentYear - 1
        )

        coEvery { recurringBillDao.getActiveRecurringBills(testUserId) } returns listOf(bill)

        // When
        repository.checkAndGenerateRecurringBills(testUserId)

        // Then: Should generate since last generated was a different month/year
        coVerify(exactly = 1) { transactionDao.insertTransaction(any()) }
        coVerify(exactly = 1) {
            recurringBillDao.updateLastGenerated("bill_prev_year", currentMonth, currentYear)
        }
    }

    @Test
    fun `generated transaction should have correct note format`() = runTest {
        val bill = createBill(
            id = "bill_note_test",
            billName = "Token Listrik",
            amount = 200000.0,
            category = "Tagihan & Utilitas",
            dueDay = 1,
            isActive = true,
            lastGeneratedMonth = null,
            lastGeneratedYear = null
        )

        coEvery { recurringBillDao.getActiveRecurringBills(testUserId) } returns listOf(bill)

        // When
        repository.checkAndGenerateRecurringBills(testUserId)

        // Then
        val transactionSlot = slot<TransactionEntity>()
        coVerify { transactionDao.insertTransaction(capture(transactionSlot)) }
        assertEquals("Dibuat otomatis oleh sistem: Token Listrik", transactionSlot.captured.note)
    }

    @Test
    fun `createNewRecurringBill should create entity with correct defaults`() {
        val bill = repository.createNewRecurringBill(
            userId = testUserId,
            billName = "Test Bill",
            amount = 100000.0,
            category = "Lainnya",
            dueDay = 15
        )

        assertEquals(testUserId, bill.userId)
        assertEquals("Test Bill", bill.billName)
        assertEquals(100000.0, bill.amount, 0.01)
        assertEquals("Lainnya", bill.category)
        assertEquals(15, bill.dueDay)
        assertTrue(bill.isActive)
        assertEquals(null, bill.lastGeneratedMonth)
        assertEquals(null, bill.lastGeneratedYear)
        assertTrue(bill.id.isNotBlank())
    }

    // ==================== HELPER ====================

    private fun createBill(
        id: String,
        billName: String,
        amount: Double,
        category: String,
        dueDay: Int,
        isActive: Boolean,
        lastGeneratedMonth: Int?,
        lastGeneratedYear: Int?
    ) = RecurringBillEntity(
        id = id,
        userId = testUserId,
        billName = billName,
        amount = amount,
        category = category,
        dueDay = dueDay,
        isActive = isActive,
        lastGeneratedMonth = lastGeneratedMonth,
        lastGeneratedYear = lastGeneratedYear
    )
}
