package com.example.detoxproject.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "planned_activities")
data class PlannedActivityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val userId: Long = 1L,
    val title: String,
    val description: String,
    val category: String, // outdoor, indoor, reading, mindfulness, walk
    val durationMinutes: Int,
    val scheduledDate: String, // YYYY-MM-DD
    val status: String = "PLANNED", // PLANNED, COMPLETED, SKIPPED
    val weatherCondition: String? = null,
    val completedAt: Long? = null
)
