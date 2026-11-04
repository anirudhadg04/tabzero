package com.example.detoxproject.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted exercise session records (Walking, Running, Cycling, Strength, etc.).
 * Fully independent from digital detox sessions.
 */
@Entity(tableName = "exercise_sessions")
data class ExerciseSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val activityType: String, // "WALKING", "RUNNING", "CYCLING", "STRENGTH", "OTHER"
    val startTime: Long,
    val endTime: Long? = null,
    val durationSeconds: Long = 0L,
    val distanceMeters: Float? = null,
    val steps: Int? = null,
    val calories: Float? = null,
    val notes: String? = null,
    val status: String = "ACTIVE", // "ACTIVE", "COMPLETED", "CANCELLED"
    val sessionDate: String // ISO YYYY-MM-DD
)
