package com.example.detoxproject.data.local.dao

import androidx.room.*
import com.example.detoxproject.data.local.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements WHERE userId = :userId")
    fun getAllAchievements(userId: Long = 1L): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE userId = :userId")
    suspend fun getAllAchievementsDirect(userId: Long = 1L): List<AchievementEntity>

    @Query("SELECT * FROM achievements WHERE userId = :userId AND isUnlocked = 1 ORDER BY unlockedAt DESC")
    fun getUnlockedAchievements(userId: Long = 1L): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getAchievementById(id: String, userId: Long = 1L): AchievementEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(achievements: List<AchievementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAchievements(achievements: List<AchievementEntity>)

    @Update
    suspend fun updateAchievement(achievement: AchievementEntity)

    @Query("DELETE FROM achievements WHERE userId = :userId AND id = :id")
    suspend fun deleteAchievementById(userId: Long, id: String)
}
