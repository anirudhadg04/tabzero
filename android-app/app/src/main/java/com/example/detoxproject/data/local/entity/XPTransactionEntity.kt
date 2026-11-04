package com.example.detoxproject.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Immutable append-only record of a Zen XP reward, linked reversal, or restoration.
 * Never updated in place, never deleted from the database. Balance derived algebraically:
 * SUM(REWARD) + SUM(RESTORE) - SUM(REVERSAL).
 */
@Entity(
    tableName = "xp_transactions",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["date"]),
        Index(value = ["sourceEventId"]),
        Index(value = ["sourceEventType"]),
        Index(value = ["userId", "sourceEventId"])
    ]
)
data class XPTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val userId: Long = 1L,
    val sourceEventId: String,
    val sourceEventType: String, // HABIT, ALL_HABITS_BONUS, DETOX, STEP_GOAL, EXERCISE, ALL_TASKS_BONUS, ACHIEVEMENT
    val xpAmount: Int, // Positive magnitude (> 0)
    val transactionType: String, // "REWARD", "REVERSAL", or "RESTORE"
    val linkedTransactionId: Long? = null, // Linked to previous transaction in the event chain
    val date: String, // ISO YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis(),
    val description: String
)
