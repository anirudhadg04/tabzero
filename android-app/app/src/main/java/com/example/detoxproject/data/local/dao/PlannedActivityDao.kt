package com.example.detoxproject.data.local.dao

import androidx.room.*
import com.example.detoxproject.data.local.entity.PlannedActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedActivityDao {
    @Query("SELECT * FROM planned_activities WHERE userId = :userId ORDER BY scheduledDate DESC, id DESC")
    fun getAllActivities(userId: Long = 1L): Flow<List<PlannedActivityEntity>>

    @Query("SELECT * FROM planned_activities WHERE userId = :userId AND scheduledDate = :date ORDER BY id ASC")
    fun getActivitiesForDate(date: String, userId: Long = 1L): Flow<List<PlannedActivityEntity>>

    @Query("SELECT * FROM planned_activities WHERE userId = :userId AND scheduledDate = :date ORDER BY id ASC")
    suspend fun getActivitiesForDateDirect(date: String, userId: Long = 1L): List<PlannedActivityEntity>

    @Query("SELECT * FROM planned_activities WHERE userId = :userId")
    suspend fun getAllActivitiesDirect(userId: Long = 1L): List<PlannedActivityEntity>

    @Query("SELECT * FROM planned_activities WHERE userId = :userId AND scheduledDate BETWEEN :startDate AND :endDate ORDER BY scheduledDate ASC")
    suspend fun getActivitiesBetweenDatesDirect(startDate: String, endDate: String, userId: Long = 1L): List<PlannedActivityEntity>

    @Query("SELECT * FROM planned_activities WHERE id = :id LIMIT 1")
    suspend fun getActivityById(id: Long): PlannedActivityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: PlannedActivityEntity): Long

    @Update
    suspend fun updateActivity(activity: PlannedActivityEntity)

    @Delete
    suspend fun deleteActivity(activity: PlannedActivityEntity)

    @Query("DELETE FROM planned_activities WHERE id = :id")
    suspend fun deleteActivityById(id: Long)
}
