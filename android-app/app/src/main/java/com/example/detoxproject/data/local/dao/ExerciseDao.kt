package com.example.detoxproject.data.local.dao

import androidx.room.*
import com.example.detoxproject.data.local.entity.ExerciseSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercise_sessions WHERE status = 'ACTIVE' ORDER BY startTime DESC LIMIT 1")
    fun getActiveExerciseSession(): Flow<ExerciseSessionEntity?>

    @Query("SELECT * FROM exercise_sessions WHERE status = 'ACTIVE' ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveExerciseSessionDirect(): ExerciseSessionEntity?

    @Query("SELECT * FROM exercise_sessions WHERE status = 'COMPLETED' ORDER BY startTime DESC LIMIT :limit")
    fun getRecentCompletedExercises(limit: Int): Flow<List<ExerciseSessionEntity>>

    @Query("SELECT * FROM exercise_sessions WHERE status = 'COMPLETED' ORDER BY startTime DESC")
    suspend fun getAllCompletedExercisesDirect(): List<ExerciseSessionEntity>

    @Query("SELECT * FROM exercise_sessions WHERE sessionDate = :date ORDER BY startTime DESC")
    fun getExercisesForDate(date: String): Flow<List<ExerciseSessionEntity>>

    @Query("SELECT * FROM exercise_sessions WHERE sessionDate BETWEEN :startDate AND :endDate ORDER BY startTime ASC")
    suspend fun getExercisesBetweenDatesDirect(startDate: String, endDate: String): List<ExerciseSessionEntity>

    @Query("SELECT * FROM exercise_sessions WHERE id = :id LIMIT 1")
    suspend fun getExerciseById(id: Long): ExerciseSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(session: ExerciseSessionEntity): Long

    @Update
    suspend fun updateExercise(session: ExerciseSessionEntity)

    @Query("DELETE FROM exercise_sessions WHERE id = :id")
    suspend fun deleteExercise(id: Long)
}
