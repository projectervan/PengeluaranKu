package com.finansialku.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.finansialku.app.data.dao.RecurringBillDao
import com.finansialku.app.data.dao.TransactionDao
import com.finansialku.app.data.dao.UserDao
import com.finansialku.app.data.entity.RecurringBillEntity
import com.finansialku.app.data.entity.TransactionEntity
import com.finansialku.app.data.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        TransactionEntity::class,
        RecurringBillEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao
    abstract fun recurringBillDao(): RecurringBillDao
}
