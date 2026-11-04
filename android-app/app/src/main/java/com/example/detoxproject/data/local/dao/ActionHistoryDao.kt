package com.example.detoxproject.data.local.dao

import androidx.room.*
import com.example.detoxproject.data.local.entity.ActionHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionHistoryDao {
    @Query("SELECT * FROM action_history WHERE userId = :userId AND stackPosition = 'PAST' ORDER BY sequenceOrder DESC")
    fun getPastActions(userId: Long = 1L): Flow<List<ActionHistoryEntity>>

    @Query("SELECT * FROM action_history WHERE userId = :userId AND stackPosition = 'PAST' ORDER BY sequenceOrder DESC")
    suspend fun getPastActionsDirect(userId: Long = 1L): List<ActionHistoryEntity>

    @Query("SELECT * FROM action_history WHERE userId = :userId AND stackPosition = 'FUTURE' ORDER BY sequenceOrder ASC")
    fun getFutureActions(userId: Long = 1L): Flow<List<ActionHistoryEntity>>

    @Query("SELECT * FROM action_history WHERE userId = :userId AND stackPosition = 'FUTURE' ORDER BY sequenceOrder ASC")
    suspend fun getFutureActionsDirect(userId: Long = 1L): List<ActionHistoryEntity>

    @Query("SELECT * FROM action_history WHERE userId = :userId AND stackPosition = 'PAST' ORDER BY sequenceOrder DESC LIMIT 1")
    suspend fun getLatestPastAction(userId: Long = 1L): ActionHistoryEntity?

    @Query("SELECT * FROM action_history WHERE userId = :userId AND stackPosition = 'FUTURE' ORDER BY sequenceOrder ASC LIMIT 1")
    suspend fun getNextFutureAction(userId: Long = 1L): ActionHistoryEntity?

    @Query("SELECT COUNT(*) FROM action_history WHERE userId = :userId AND stackPosition = 'PAST'")
    fun getPastCount(userId: Long = 1L): Flow<Int>

    @Query("SELECT COUNT(*) FROM action_history WHERE userId = :userId AND stackPosition = 'FUTURE'")
    fun getFutureCount(userId: Long = 1L): Flow<Int>

    @Query("SELECT * FROM action_history WHERE userId = :userId ORDER BY sequenceOrder DESC, id DESC LIMIT :limit")
    fun getRecentActions(userId: Long = 1L, limit: Int = 5): Flow<List<ActionHistoryEntity>>

    @Query("SELECT * FROM action_history WHERE userId = :userId ORDER BY sequenceOrder DESC, id DESC LIMIT :limit")
    suspend fun getRecentActionsDirect(userId: Long = 1L, limit: Int = 5): List<ActionHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: ActionHistoryEntity): Long

    @Update
    suspend fun updateAction(action: ActionHistoryEntity)

    @Delete
    suspend fun deleteAction(action: ActionHistoryEntity)

    @Query("DELETE FROM action_history WHERE userId = :userId AND stackPosition = 'FUTURE'")
    suspend fun clearFuture(userId: Long = 1L)

    @Query("DELETE FROM action_history WHERE userId = :userId AND id IN (SELECT id FROM action_history WHERE userId = :userId AND stackPosition = 'PAST' ORDER BY sequenceOrder ASC LIMIT :excessCount)")
    suspend fun trimOldestPast(excessCount: Int, userId: Long = 1L)

    @Query("DELETE FROM action_history WHERE userId = :userId")
    suspend fun clearAll(userId: Long = 1L)
}
