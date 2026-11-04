package com.example.detoxproject.data.local.dao

import androidx.room.*
import com.example.detoxproject.data.local.entity.GraceDayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GraceDayDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGraceDay(graceDay: GraceDayEntity): Long

    @Query("SELECT * FROM grace_days WHERE userId = :userId AND year = :year AND weekNumber = :weekNumber LIMIT 1")
    suspend fun getGraceDayForWeekDirect(userId: Long, year: Int, weekNumber: Int): GraceDayEntity?

    @Query("SELECT * FROM grace_days WHERE userId = :userId AND year = :year AND weekNumber = :weekNumber LIMIT 1")
    fun getGraceDayForWeek(userId: Long, year: Int, weekNumber: Int): Flow<GraceDayEntity?>

    @Query("SELECT * FROM grace_days WHERE userId = :userId AND protectedDate = :date LIMIT 1")
    suspend fun getGraceDayForDateDirect(userId: Long, date: String): GraceDayEntity?

    @Query("SELECT * FROM grace_days WHERE userId = :userId ORDER BY year DESC, weekNumber DESC")
    fun getAllGraceDays(userId: Long = 1L): Flow<List<GraceDayEntity>>

    @Query("SELECT * FROM grace_days WHERE userId = :userId ORDER BY year DESC, weekNumber DESC")
    suspend fun getAllGraceDaysDirect(userId: Long = 1L): List<GraceDayEntity>

    @Query("DELETE FROM grace_days WHERE userId = :userId AND protectedDate = :date")
    suspend fun deleteGraceDayForDate(userId: Long, date: String)
}
