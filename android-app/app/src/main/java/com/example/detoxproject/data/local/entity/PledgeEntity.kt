package com.example.detoxproject.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pledges")
data class PledgeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val userId: Long = 1L,
    val title: String,
    val targetMinutes: Int,
    val category: String, // work, personal, reading, sleep, study, custom
    val restrictedPackages: String = "",
    val allowAudio: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
)
