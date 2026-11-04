package com.example.detoxproject.data.local.dao

import androidx.room.*
import com.example.detoxproject.data.local.entity.PledgeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PledgeDao {
    @Query("SELECT * FROM pledges WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllPledges(userId: Long = 1L): Flow<List<PledgeEntity>>

    @Query("SELECT * FROM pledges WHERE id = :id LIMIT 1")
    suspend fun getPledgeById(id: Long): PledgeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPledge(pledge: PledgeEntity): Long

    @Update
    suspend fun updatePledge(pledge: PledgeEntity)

    @Delete
    suspend fun deletePledge(pledge: PledgeEntity)

    @Query("DELETE FROM pledges WHERE id = :id")
    suspend fun deletePledgeById(id: Long)
}
