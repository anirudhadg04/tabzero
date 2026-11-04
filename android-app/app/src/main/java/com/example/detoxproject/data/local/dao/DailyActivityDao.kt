package com.example.detoxproject.data.local.dao

import androidx.room.*
import com.example.detoxproject.data.local.entity.DailyActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyActivityDao {

    @Query("SELECT * FROM daily_activities WHERE date = :date LIMIT 1")
    fun getDailyActivity(date: String): Flow<DailyActivityEntity?>

    @Query("SELECT * FROM daily_activities WHERE date = :date LIMIT 1")
    suspend fun getDailyActivityDirect(date: String): DailyActivityEntity?

    @Query("SELECT * FROM daily_activities ORDER BY date DESC")
    fun getAllActivities(): Flow<List<DailyActivityEntity>>

    @Query("SELECT * FROM daily_activities ORDER BY date DESC")
    suspend fun getAllActivitiesDirect(): List<DailyActivityEntity>

    @Query("SELECT * FROM daily_activities ORDER BY date DESC LIMIT :limit")
    fun getRecentActivities(limit: Int): Flow<List<DailyActivityEntity>>

    @Query("SELECT * FROM daily_activities WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getActivitiesBetween(startDate: String, endDate: String): Flow<List<DailyActivityEntity>>

    @Query("SELECT * FROM daily_activities WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getActivitiesBetweenDirect(startDate: String, endDate: String): List<DailyActivityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(activity: DailyActivityEntity)

    @Query("""
        UPDATE daily_activities 
        SET steps = :steps, distanceMeters = :distanceMeters, activeCalories = :activeCalories, source = :source, lastUpdated = :lastUpdated 
        WHERE date = :date
    """)
    suspend fun updateSteps(date: String, steps: Int, distanceMeters: Float, activeCalories: Float, source: String, lastUpdated: Long): Int
}
