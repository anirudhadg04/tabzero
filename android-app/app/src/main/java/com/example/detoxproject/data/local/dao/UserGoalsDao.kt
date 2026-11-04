package com.example.detoxproject.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.detoxproject.data.local.entity.UserGoalsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserGoalsDao {
    @Query("SELECT * FROM user_goals WHERE userId = :userId LIMIT 1")
    fun getUserGoals(userId: Long = 1L): Flow<UserGoalsEntity?>

    @Query("SELECT * FROM user_goals WHERE userId = :userId LIMIT 1")
    suspend fun getUserGoalsDirect(userId: Long = 1L): UserGoalsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(goals: UserGoalsEntity)

    @Query("UPDATE user_goals SET avatar = :avatar, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateAvatar(userId: Long, avatar: String, updatedAt: Long = System.currentTimeMillis())

    @Update
    suspend fun update(goals: UserGoalsEntity)
}
