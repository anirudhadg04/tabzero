package com.example.detoxproject.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detox_sessions")
data class DetoxSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val userId: Long = 1L,
    val pledgeId: Long? = null,
    val title: String,
    val startTime: Long,
    val endTime: Long? = null,
    val targetMinutes: Int,
    val targetSeconds: Int = targetMinutes * 60,
    val actualMinutes: Int = 0,
    val status: String = "ACTIVE", // ACTIVE, PAUSED, COMPLETED, CANCELLED
    val pausedAt: Long? = null,
    val accumulatedPauseMs: Long = 0L,
    val category: String = "focus",
    val restrictedPackages: String = "",
    val allowAudio: Boolean = false,
    val sessionDate: String // YYYY-MM-DD in local time
)
