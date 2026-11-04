package com.example.detoxproject.data.local.dao

import androidx.room.*
import com.example.detoxproject.data.local.entity.MindfulNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MindfulNoteDao {
    @Query("SELECT * FROM mindful_notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<MindfulNoteEntity>>

    @Query("SELECT * FROM mindful_notes ORDER BY updatedAt DESC")
    suspend fun getAllNotesDirect(): List<MindfulNoteEntity>

    @Query("SELECT * FROM mindful_notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): MindfulNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: MindfulNoteEntity): Long

    @Update
    suspend fun updateNote(note: MindfulNoteEntity)

    @Delete
    suspend fun deleteNote(note: MindfulNoteEntity)

    @Query("DELETE FROM mindful_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)
}
