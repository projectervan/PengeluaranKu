package com.finansialku.app.di

import android.content.Context
import androidx.room.Room
import com.finansialku.app.data.dao.RecurringBillDao
import com.finansialku.app.data.dao.TransactionDao
import com.finansialku.app.data.dao.UserDao
import com.finansialku.app.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "finansialku_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideTransactionDao(database: AppDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    @Singleton
    fun provideRecurringBillDao(database: AppDatabase): RecurringBillDao {
        return database.recurringBillDao()
    }
}
