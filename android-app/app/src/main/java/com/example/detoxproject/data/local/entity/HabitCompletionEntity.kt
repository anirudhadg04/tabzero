package com.example.detoxproject.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted habit completion record.
 * Tracks completion per habit and local calendar date independently.
 * Prevents duplicate completion records via the unique (habitId, date) composite index.
 */
@Entity(
    tableName = "habit_completions",
    indices = [
        Index(value = ["habitId", "date"], unique = true),
        Index(value = ["date"]),
        Index(value = ["habitId"])
    ]
)
data class HabitCompletionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val habitId: Long,
    val date: String, // ISO YYYY-MM-DD
    val completedAt: Long = System.currentTimeMillis()
)
