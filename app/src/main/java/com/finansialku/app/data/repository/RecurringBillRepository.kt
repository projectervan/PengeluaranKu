package com.finansialku.app.data.repository

import com.finansialku.app.data.dao.RecurringBillDao
import com.finansialku.app.data.dao.TransactionDao
import com.finansialku.app.data.entity.RecurringBillEntity
import com.finansialku.app.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringBillRepository @Inject constructor(
    private val recurringBillDao: RecurringBillDao,
    private val transactionDao: TransactionDao
) {

    // ==================== CRUD ====================

    suspend fun insertRecurringBill(bill: RecurringBillEntity) {
        recurringBillDao.insertRecurringBill(bill)
    }

    suspend fun updateRecurringBill(bill: RecurringBillEntity) {
        recurringBillDao.updateRecurringBill(bill)
    }

    suspend fun deleteRecurringBill(bill: RecurringBillEntity) {
        recurringBillDao.deleteRecurringBill(bill)
    }

    suspend fun getRecurringBillById(billId: String): RecurringBillEntity? {
        return recurringBillDao.getRecurringBillById(billId)
    }

    fun observeAllRecurringBills(userId: String): Flow<List<RecurringBillEntity>> {
        return recurringBillDao.observeAllRecurringBills(userId)
    }

    suspend fun updateActiveStatus(billId: String, isActive: Boolean) {
        recurringBillDao.updateActiveStatus(billId, isActive)
    }

    suspend fun deleteAllBillsByUser(userId: String) {
        recurringBillDao.deleteAllBillsByUser(userId)
    }

    // ==================== HELPER ====================

    fun createNewRecurringBill(
        userId: String,
        billName: String,
        amount: Double,
        category: String,
        dueDay: Int,
        isActive: Boolean = true
    ): RecurringBillEntity {
        return RecurringBillEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            billName = billName,
            amount = amount,
            category = category,
            dueDay = dueDay,
            isActive = isActive,
            lastGeneratedMonth = null,
            lastGeneratedYear = null
        )
    }

    // ==================== BUSINESS LOGIC: Auto-generate Bills ====================

    /**
     * Implements PRD Section 5.1: checkAndGenerateRecurringBills
     *
     * Called at app launch after Splash Screen. Checks all active recurring bills
     * and generates EXPENSE transactions for those that haven't been generated
     * for the current month yet (when due_day has passed).
     */
    suspend fun checkAndGenerateRecurringBills(userId: String) {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1 // 1-12
        val currentYear = calendar.get(Calendar.YEAR)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        val activeBills = recurringBillDao.getActiveRecurringBills(userId)

        for (bill in activeBills) {
            val alreadyGenerated = bill.lastGeneratedMonth == currentMonth
                    && bill.lastGeneratedYear == currentYear

            if (!alreadyGenerated && currentDay >= bill.dueDay) {
                // 1. Insert as auto-generated EXPENSE transaction
                val transaction = TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    type = "EXPENSE",
                    amount = bill.amount,
                    category = bill.category,
                    date = calendar.timeInMillis,
                    note = "Dibuat otomatis oleh sistem: ${bill.billName}",
                    isRecurringGenerated = true
                )
                transactionDao.insertTransaction(transaction)

                // 2. Update last_generated_month and last_generated_year
                recurringBillDao.updateLastGenerated(bill.id, currentMonth, currentYear)
            }
        }
    }
}
