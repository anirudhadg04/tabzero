package com.example.detoxproject.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted daily physical activity records.
 * Maintains daily totals, walking distance, calories, and source attribution.
 */
@Entity(tableName = "daily_activities")
data class DailyActivityEntity(
    @PrimaryKey
    val date: String, // ISO YYYY-MM-DD
    val steps: Int = 0,
    val goalSteps: Int = 8000,
    val distanceMeters: Float = 0f,
    val activeCalories: Float = 0f,
    val activeMinutes: Int = 0,
    val source: String = "STEP_COUNTER", // "HEALTH_CONNECT" or "STEP_COUNTER"
    val lastUpdated: Long = System.currentTimeMillis()
)
