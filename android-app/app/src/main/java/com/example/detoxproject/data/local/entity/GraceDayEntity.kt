package com.example.detoxproject.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted record of consumed shared weekly grace days.
 * Enforces exactly one grace day per calendar week (Monday to Sunday) per user.
 */
@Entity(
    tableName = "grace_days",
    indices = [
        Index(value = ["userId", "year", "weekNumber"], unique = true),
        Index(value = ["userId", "protectedDate"], unique = true)
    ]
)
data class GraceDayEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val userId: Long = 1L,
    val year: Int,
    val weekNumber: Int, // ISO week 1..53
    val protectedDate: String, // ISO YYYY-MM-DD
    val consumedAt: Long = System.currentTimeMillis(),
    val reason: String = "" // e.g. "Rest day / habit protection"
)
