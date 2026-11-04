package com.example.detoxproject.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted personal notes and reflections created in the More Mindful tab.
 * Includes a soft pastel color code for aesthetic visual display.
 */
@Entity(tableName = "mindful_notes")
data class MindfulNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val content: String,
    val colorHex: String = "#E8F5E9",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
