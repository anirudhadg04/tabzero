package com.example.detoxproject.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_goals")
data class UserGoalsEntity(
    @PrimaryKey
    val userId: Long = 1L,
    val userName: String = "User",
    val dailySteps: Int = 8000,
    val dailyOfflineMinutes: Int = 120,
    val strideLengthMeters: Float = 0.75f,
    val targetCalories: Int = 400,
    val maxScreenTimeMinutes: Int = 240,
    val avatar: String = "🦉",
    val updatedAt: Long = System.currentTimeMillis()
)
