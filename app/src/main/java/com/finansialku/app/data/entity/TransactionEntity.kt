package com.finansialku.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["date"]),
        Index(value = ["type"]),
        Index(value = ["user_id", "type", "date"])
    ]
)
data class TransactionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // UUID

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "type")
    val type: String, // "INCOME" or "EXPENSE"

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "date")
    val date: Long, // Timestamp in millis

    @ColumnInfo(name = "note")
    val note: String? = null,

    @ColumnInfo(name = "is_recurring_generated")
    val isRecurringGenerated: Boolean = false
)

/**
 * Data class for aggregate query result: expense grouped by category.
 * Used for Pie Chart on Dashboard.
 */
data class CategoryExpense(
    val category: String,
    val totalAmount: Double
)
