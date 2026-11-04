package com.example.detoxproject.data.local.dao

import androidx.room.*
import com.example.detoxproject.data.local.entity.XPTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface XPTransactionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: XPTransactionEntity): Long

    @Query("SELECT * FROM xp_transactions WHERE userId = :userId AND sourceEventId = :sourceEventId AND transactionType = :transactionType ORDER BY timestamp DESC, id DESC LIMIT 1")
    suspend fun getTransactionBySourceEvent(userId: Long, sourceEventId: String, transactionType: String): XPTransactionEntity?

    @Query("SELECT * FROM xp_transactions WHERE userId = :userId AND sourceEventId = :sourceEventId AND transactionType = 'REWARD' LIMIT 1")
    suspend fun getRewardBySourceEventDirect(userId: Long, sourceEventId: String): XPTransactionEntity?

    @Query("SELECT * FROM xp_transactions WHERE userId = :userId AND sourceEventId = :sourceEventId ORDER BY timestamp DESC, id DESC LIMIT 1")
    suspend fun getLatestTransactionForSourceEventDirect(userId: Long, sourceEventId: String): XPTransactionEntity?

    @Query("SELECT * FROM xp_transactions WHERE userId = :userId AND sourceEventId = :sourceEventId ORDER BY timestamp ASC, id ASC")
    suspend fun getTransactionsForSourceEventDirect(userId: Long, sourceEventId: String): List<XPTransactionEntity>

    @Query("""
        SELECT COALESCE(SUM(
            CASE 
                WHEN transactionType IN ('REWARD', 'RESTORE') THEN xpAmount 
                WHEN transactionType = 'REVERSAL' THEN -xpAmount 
                ELSE 0 
            END
        ), 0)
        FROM xp_transactions 
        WHERE userId = :userId AND sourceEventId = :sourceEventId
    """)
    suspend fun getNetXPForSourceEventDirect(userId: Long, sourceEventId: String): Int

    @Query("SELECT * FROM xp_transactions WHERE userId = :userId ORDER BY timestamp DESC, id DESC")
    fun getAllTransactions(userId: Long = 1L): Flow<List<XPTransactionEntity>>

    @Query("SELECT * FROM xp_transactions WHERE userId = :userId ORDER BY timestamp DESC, id DESC")
    suspend fun getAllTransactionsDirect(userId: Long = 1L): List<XPTransactionEntity>

    @Query("SELECT * FROM xp_transactions WHERE userId = :userId ORDER BY timestamp DESC, id DESC LIMIT :limit")
    fun getRecentTransactions(userId: Long = 1L, limit: Int = 50): Flow<List<XPTransactionEntity>>

    @Query("SELECT * FROM xp_transactions WHERE userId = :userId AND date = :date")
    suspend fun getTransactionsForDate(userId: Long, date: String): List<XPTransactionEntity>

    @Query("SELECT * FROM xp_transactions WHERE userId = :userId AND date = :date ORDER BY timestamp DESC, id DESC")
    fun getTransactionsForDateFlow(userId: Long, date: String): Flow<List<XPTransactionEntity>>

    @Query("SELECT * FROM xp_transactions WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY timestamp ASC, id ASC")
    suspend fun getTransactionsBetweenDatesDirect(startDate: String, endDate: String, userId: Long = 1L): List<XPTransactionEntity>

    @Query("""
        SELECT COALESCE(SUM(
            CASE 
                WHEN transactionType IN ('REWARD', 'RESTORE') THEN xpAmount 
                WHEN transactionType = 'REVERSAL' THEN -xpAmount 
                ELSE 0 
            END
        ), 0)
        FROM xp_transactions 
        WHERE userId = :userId AND date = :date AND sourceEventType = :sourceEventType
    """)
    suspend fun getNetXPForDateAndType(userId: Long, date: String, sourceEventType: String): Int

    @Query("""
        SELECT COALESCE(SUM(
            CASE 
                WHEN transactionType IN ('REWARD', 'RESTORE') THEN xpAmount 
                WHEN transactionType = 'REVERSAL' THEN -xpAmount 
                ELSE 0 
            END
        ), 0)
        FROM xp_transactions 
        WHERE userId = :userId
    """)
    fun getTotalBalance(userId: Long = 1L): Flow<Int>

    @Query("""
        SELECT COALESCE(SUM(
            CASE 
                WHEN transactionType IN ('REWARD', 'RESTORE') THEN xpAmount 
                WHEN transactionType = 'REVERSAL' THEN -xpAmount 
                ELSE 0 
            END
        ), 0)
        FROM xp_transactions 
        WHERE userId = :userId
    """)
    suspend fun getTotalBalanceDirect(userId: Long = 1L): Int
}
