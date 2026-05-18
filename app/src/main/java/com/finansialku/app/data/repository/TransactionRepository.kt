package com.finansialku.app.data.repository

import com.finansialku.app.data.dao.TransactionDao
import com.finansialku.app.data.entity.CategoryExpense
import com.finansialku.app.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {

    // ==================== CRUD ====================

    suspend fun insertTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(transactionId: String) {
        transactionDao.deleteTransactionById(transactionId)
    }

    suspend fun deleteAllTransactionsByUser(userId: String) {
        transactionDao.deleteAllTransactionsByUser(userId)
    }

    suspend fun getTransactionById(transactionId: String): TransactionEntity? {
        return transactionDao.getTransactionById(transactionId)
    }

    // ==================== OBSERVE (Flow) ====================

    fun observeAllTransactions(userId: String): Flow<List<TransactionEntity>> {
        return transactionDao.observeAllTransactions(userId)
    }

    fun observeTransactionsByMonth(
        userId: String,
        year: Int,
        month: Int
    ): Flow<List<TransactionEntity>> {
        val (start, end) = getMonthRange(year, month)
        return transactionDao.observeTransactionsByMonth(userId, start, end)
    }

    fun observeRecentTransactions(userId: String, limit: Int = 5): Flow<List<TransactionEntity>> {
        return transactionDao.observeRecentTransactions(userId, limit)
    }

    // ==================== AGGREGATE FUNCTIONS ====================

    /**
     * Observe total income for a specific month.
     * Used in Dashboard card: "Total Pemasukan Bulanan"
     */
    fun observeTotalIncomeByMonth(
        userId: String,
        year: Int,
        month: Int
    ): Flow<Double> {
        val (start, end) = getMonthRange(year, month)
        return transactionDao.observeTotalIncomeByMonth(userId, start, end)
    }

    /**
     * Observe total expense for a specific month.
     * Used in Dashboard card: "Total Pengeluaran Bulanan"
     */
    fun observeTotalExpenseByMonth(
        userId: String,
        year: Int,
        month: Int
    ): Flow<Double> {
        val (start, end) = getMonthRange(year, month)
        return transactionDao.observeTotalExpenseByMonth(userId, start, end)
    }

    /**
     * Observe expenses grouped by category for a specific month.
     * Used in Dashboard: Pie Chart
     */
    fun observeExpenseByCategory(
        userId: String,
        year: Int,
        month: Int
    ): Flow<List<CategoryExpense>> {
        val (start, end) = getMonthRange(year, month)
        return transactionDao.observeExpenseByCategory(userId, start, end)
    }

    /**
     * Observe income grouped by category for a specific month.
     */
    fun observeIncomeByCategory(
        userId: String,
        year: Int,
        month: Int
    ): Flow<List<CategoryExpense>> {
        val (start, end) = getMonthRange(year, month)
        return transactionDao.observeIncomeByCategory(userId, start, end)
    }

    /**
     * Observe transaction count for a specific month.
     */
    fun observeTransactionCountByMonth(
        userId: String,
        year: Int,
        month: Int
    ): Flow<Int> {
        val (start, end) = getMonthRange(year, month)
        return transactionDao.observeTransactionCountByMonth(userId, start, end)
    }

    /**
     * Observe all-time total income.
     */
    fun observeTotalIncomeAllTime(userId: String): Flow<Double> {
        return transactionDao.observeTotalIncomeAllTime(userId)
    }

    /**
     * Observe all-time total expense.
     */
    fun observeTotalExpenseAllTime(userId: String): Flow<Double> {
        return transactionDao.observeTotalExpenseAllTime(userId)
    }

    // ==================== HELPER: Generate New Transaction ====================

    fun createNewTransaction(
        userId: String,
        type: String,
        amount: Double,
        category: String,
        date: Long,
        note: String? = null,
        isRecurringGenerated: Boolean = false
    ): TransactionEntity {
        return TransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            type = type,
            amount = amount,
            category = category,
            date = date,
            note = note,
            isRecurringGenerated = isRecurringGenerated
        )
    }

    // ==================== UTILITY ====================

    /**
     * Returns start (inclusive) and end (exclusive) timestamps for a given month.
     * month is 1-based (January = 1, December = 12).
     */
    private fun getMonthRange(year: Int, month: Int): Pair<Long, Long> {
        val calStart = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1) // Calendar.MONTH is 0-based
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val calEnd = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, 1) // Start of next month
        }

        return Pair(calStart.timeInMillis, calEnd.timeInMillis)
    }
}
