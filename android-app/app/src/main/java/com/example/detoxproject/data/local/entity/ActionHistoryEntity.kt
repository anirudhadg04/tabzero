package com.example.detoxproject.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "action_history")
data class ActionHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val userId: Long = 1L,
    val actionType: String, // CREATE_SESSION, COMPLETE_SESSION, DELETE_SESSION, CREATE_PLEDGE, UPDATE_GOALS, COMPLETE_ACTIVITY
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val stackPosition: String, // "PAST", "PRESENT", "FUTURE"
    val entityType: String, // "SESSION", "PLEDGE", "GOALS", "ACTIVITY"
    val entityId: Long,
    val beforeStateJson: String? = null,
    val afterStateJson: String? = null,
    val sequenceOrder: Int = 0 // Order within the stack
)
