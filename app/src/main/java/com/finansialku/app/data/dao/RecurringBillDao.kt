package com.finansialku.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finansialku.app.data.entity.RecurringBillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringBillDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringBill(bill: RecurringBillEntity)

    @Update
    suspend fun updateRecurringBill(bill: RecurringBillEntity)

    @Delete
    suspend fun deleteRecurringBill(bill: RecurringBillEntity)

    @Query("SELECT * FROM recurring_bills WHERE id = :billId LIMIT 1")
    suspend fun getRecurringBillById(billId: String): RecurringBillEntity?

    @Query("SELECT * FROM recurring_bills WHERE user_id = :userId ORDER BY due_day ASC")
    fun observeAllRecurringBills(userId: String): Flow<List<RecurringBillEntity>>

    @Query("SELECT * FROM recurring_bills WHERE user_id = :userId AND is_active = 1")
    suspend fun getActiveRecurringBills(userId: String): List<RecurringBillEntity>

    @Query("""
        UPDATE recurring_bills 
        SET last_generated_month = :month, last_generated_year = :year 
        WHERE id = :billId
    """)
    suspend fun updateLastGenerated(billId: String, month: Int, year: Int)

    @Query("UPDATE recurring_bills SET is_active = :isActive WHERE id = :billId")
    suspend fun updateActiveStatus(billId: String, isActive: Boolean)

    @Query("DELETE FROM recurring_bills WHERE user_id = :userId")
    suspend fun deleteAllBillsByUser(userId: String)
}
