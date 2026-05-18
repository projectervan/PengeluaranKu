package com.finansialku.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finansialku.app.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :transactionId LIMIT 1")
    suspend fun getTransactionById(transactionId: String): TransactionEntity?

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

    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
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

    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
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

    @Query("DELETE FROM transactions WHERE user_id = :userId")
    suspend fun deleteAllTransactionsByUser(userId: String)
}
