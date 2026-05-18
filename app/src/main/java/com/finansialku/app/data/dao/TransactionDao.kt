package com.finansialku.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finansialku.app.data.entity.CategoryExpense
import com.finansialku.app.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    // ==================== CRUD ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransactionById(transactionId: String)

    @Query("DELETE FROM transactions WHERE user_id = :userId")
    suspend fun deleteAllTransactionsByUser(userId: String)

    // ==================== READ (Single) ====================

    @Query("SELECT * FROM transactions WHERE id = :transactionId LIMIT 1")
    suspend fun getTransactionById(transactionId: String): TransactionEntity?

    // ==================== READ (Lists with Flow) ====================

    @Query("""
        SELECT * FROM transactions 
        WHERE user_id = :userId 
        ORDER BY date DESC
    """)
    fun observeAllTransactions(userId: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE user_id = :userId 
            AND date >= :startOfMonth 
            AND date < :endOfMonth 
        ORDER BY date DESC
    """)
    fun observeTransactionsByMonth(
        userId: String,
        startOfMonth: Long,
        endOfMonth: Long
    ): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE user_id = :userId 
        ORDER BY date DESC 
        LIMIT :limit
    """)
    fun observeRecentTransactions(userId: String, limit: Int = 5): Flow<List<TransactionEntity>>

    // ==================== AGGREGATE FUNCTIONS ====================

    /**
     * Total pemasukan (INCOME) untuk bulan tertentu.
     * Digunakan di Dashboard: card "Total Pemasukan Bulanan".
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE user_id = :userId 
            AND type = 'INCOME' 
            AND date >= :startOfMonth 
            AND date < :endOfMonth
    """)
    fun observeTotalIncomeByMonth(
        userId: String,
        startOfMonth: Long,
        endOfMonth: Long
    ): Flow<Double>

    /**
     * Total pengeluaran (EXPENSE) untuk bulan tertentu.
     * Digunakan di Dashboard: card "Total Pengeluaran Bulanan".
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE user_id = :userId 
            AND type = 'EXPENSE' 
            AND date >= :startOfMonth 
            AND date < :endOfMonth
    """)
    fun observeTotalExpenseByMonth(
        userId: String,
        startOfMonth: Long,
        endOfMonth: Long
    ): Flow<Double>

    /**
     * Pengeluaran per kategori untuk bulan tertentu.
     * Digunakan di Dashboard: Pie Chart kontribusi pengeluaran.
     */
    @Query("""
        SELECT category, SUM(amount) AS totalAmount 
        FROM transactions 
        WHERE user_id = :userId 
            AND type = 'EXPENSE' 
            AND date >= :startOfMonth 
            AND date < :endOfMonth 
        GROUP BY category 
        ORDER BY totalAmount DESC
    """)
    fun observeExpenseByCategory(
        userId: String,
        startOfMonth: Long,
        endOfMonth: Long
    ): Flow<List<CategoryExpense>>

    /**
     * Jumlah total transaksi di bulan tertentu.
     * Berguna untuk informasi statistik.
     */
    @Query("""
        SELECT COUNT(*) FROM transactions 
        WHERE user_id = :userId 
            AND date >= :startOfMonth 
            AND date < :endOfMonth
    """)
    fun observeTransactionCountByMonth(
        userId: String,
        startOfMonth: Long,
        endOfMonth: Long
    ): Flow<Int>

    /**
     * Total keseluruhan pemasukan user (all-time).
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE user_id = :userId AND type = 'INCOME'
    """)
    fun observeTotalIncomeAllTime(userId: String): Flow<Double>

    /**
     * Total keseluruhan pengeluaran user (all-time).
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE user_id = :userId AND type = 'EXPENSE'
    """)
    fun observeTotalExpenseAllTime(userId: String): Flow<Double>

    /**
     * Pemasukan per kategori untuk bulan tertentu.
     */
    @Query("""
        SELECT category, SUM(amount) AS totalAmount 
        FROM transactions 
        WHERE user_id = :userId 
            AND type = 'INCOME' 
            AND date >= :startOfMonth 
            AND date < :endOfMonth 
        GROUP BY category 
        ORDER BY totalAmount DESC
    """)
    fun observeIncomeByCategory(
        userId: String,
        startOfMonth: Long,
        endOfMonth: Long
    ): Flow<List<CategoryExpense>>
}
