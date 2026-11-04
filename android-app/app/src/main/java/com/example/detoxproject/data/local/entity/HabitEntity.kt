package com.example.detoxproject.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted recurring habit definition.
 * Tracks recurring behaviors (steps, reading, detox, exercise, meditation, sleep)
 * with configurable frequency and schedule.
 */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val userId: Long = 1L,
    val title: String,
    val description: String = "",
    val category: String = "mindfulness", // fitness, focus, reading, health, sleep, outdoor, mindfulness
    val icon: String = "spa", // directions_walk, timer, menu_book, bedtime, spa, fitness_center, park
    val frequency: String = "DAILY", // DAILY, WEEKDAYS, WEEKENDS, CUSTOM
    val targetDays: String = "1,2,3,4,5,6,7", // Comma-separated DayOfWeek integers (1=Mon, 7=Sun)
    val isArchived: Boolean = false,
    val createdDate: String, // ISO YYYY-MM-DD
    val createdAt: Long = System.currentTimeMillis()
)
