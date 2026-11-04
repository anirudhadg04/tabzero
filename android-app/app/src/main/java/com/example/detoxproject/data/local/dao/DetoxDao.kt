package com.example.detoxproject.data.local.dao

import androidx.room.*
import com.example.detoxproject.data.local.entity.DetoxSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DetoxDao {
    @Query("SELECT * FROM detox_sessions WHERE userId = :userId ORDER BY startTime DESC")
    fun getAllSessions(userId: Long = 1L): Flow<List<DetoxSessionEntity>>

    @Query("SELECT * FROM detox_sessions WHERE userId = :userId AND (status = 'ACTIVE' OR status = 'PAUSED') ORDER BY startTime DESC LIMIT 1")
    fun getActiveSession(userId: Long = 1L): Flow<DetoxSessionEntity?>

    @Query("SELECT * FROM detox_sessions WHERE userId = :userId AND (status = 'ACTIVE' OR status = 'PAUSED') ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveSessionDirect(userId: Long = 1L): DetoxSessionEntity?

    @Query("SELECT * FROM detox_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): DetoxSessionEntity?

    @Query("SELECT * FROM detox_sessions WHERE userId = :userId AND status = 'COMPLETED'")
    suspend fun getAllCompletedSessionsDirect(userId: Long = 1L): List<DetoxSessionEntity>

    @Query("SELECT * FROM detox_sessions WHERE userId = :userId AND sessionDate = :date AND status = 'COMPLETED'")
    fun getCompletedSessionsForDate(date: String, userId: Long = 1L): Flow<List<DetoxSessionEntity>>

    @Query("SELECT * FROM detox_sessions WHERE userId = :userId AND sessionDate = :date ORDER BY startTime DESC")
    fun getAllSessionsForDate(date: String, userId: Long = 1L): Flow<List<DetoxSessionEntity>>

    @Query("SELECT * FROM detox_sessions WHERE userId = :userId AND sessionDate BETWEEN :startDate AND :endDate ORDER BY startTime ASC")
    suspend fun getSessionsBetweenDatesDirect(startDate: String, endDate: String, userId: Long = 1L): List<DetoxSessionEntity>

    @Query("SELECT SUM(actualMinutes) FROM detox_sessions WHERE userId = :userId AND sessionDate = :date AND status = 'COMPLETED'")
    fun getOfflineMinutesForDate(date: String, userId: Long = 1L): Flow<Int?>

    @Query("SELECT SUM(actualMinutes) FROM detox_sessions WHERE userId = :userId AND sessionDate = :date AND status = 'COMPLETED'")
    suspend fun getOfflineMinutesForDateDirect(date: String, userId: Long = 1L): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: DetoxSessionEntity): Long

    @Update
    suspend fun updateSession(session: DetoxSessionEntity)

    @Delete
    suspend fun deleteSession(session: DetoxSessionEntity)

    @Query("DELETE FROM detox_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)
}
