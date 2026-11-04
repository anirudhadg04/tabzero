package com.example.detoxproject.data.local.dao

import androidx.room.*
import com.example.detoxproject.data.local.entity.HabitCompletionEntity
import com.example.detoxproject.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits WHERE userId = :userId AND isArchived = 0 ORDER BY id ASC")
    fun getAllActiveHabits(userId: Long = 1L): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE userId = :userId ORDER BY isArchived ASC, id ASC")
    fun getAllHabits(userId: Long = 1L): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE userId = :userId AND isArchived = 0 ORDER BY id ASC")
    suspend fun getAllActiveHabitsDirect(userId: Long = 1L): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE userId = :userId ORDER BY isArchived ASC, id ASC")
    suspend fun getAllHabitsDirect(userId: Long = 1L): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getHabitById(id: Long): HabitEntity?

    @Query("SELECT COUNT(*) FROM habits WHERE userId = :userId")
    suspend fun getHabitCount(userId: Long = 1L): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(habits: List<HabitEntity>)

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("UPDATE habits SET isArchived = :isArchived WHERE id = :id")
    suspend fun setHabitArchived(id: Long, isArchived: Boolean)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabit(id: Long)

    // --- Habit Completions ---

    @Query("SELECT * FROM habit_completions WHERE date = :date")
    fun getCompletionsForDate(date: String): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE date = :date")
    suspend fun getCompletionsForDateDirect(date: String): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY date ASC")
    fun getCompletionsForHabit(habitId: Long): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY date ASC")
    suspend fun getAllCompletionsForHabitDirect(habitId: Long): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getCompletionsBetweenDatesDirect(startDate: String, endDate: String): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions ORDER BY date ASC")
    suspend fun getAllCompletionsDirect(): List<HabitCompletionEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM habit_completions WHERE habitId = :habitId AND date = :date)")
    suspend fun isHabitCompleted(habitId: Long, date: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: HabitCompletionEntity): Long

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND date = :date")
    suspend fun deleteCompletion(habitId: Long, date: String): Int

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId")
    suspend fun deleteAllCompletionsForHabit(habitId: Long)
}
