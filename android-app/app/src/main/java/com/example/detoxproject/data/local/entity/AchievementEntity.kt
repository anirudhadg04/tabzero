package com.example.detoxproject.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "achievements",
    primaryKeys = ["userId", "id"]
)
data class AchievementEntity(
    val userId: Long = 1L,
    val id: String, // e.g. "first_detox", "step_starter", "deep_focus", "consistency"
    val title: String,
    val description: String,
    val iconName: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val progress: Float = 0f, // 0f to 1f
    val category: String = "DISCONNECT", // DISCONNECT, MOVE, RECONNECT, CONSISTENCY, PERSONAL_GROWTH
    val xpReward: Int = 0
)
