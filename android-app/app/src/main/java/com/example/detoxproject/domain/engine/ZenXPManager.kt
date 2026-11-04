package com.example.detoxproject.domain.engine

import androidx.room.withTransaction
import com.example.detoxproject.data.local.OfflineDatabase
import com.example.detoxproject.data.local.entity.XPTransactionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

sealed class RewardResult {
    data class Success(val xpAwarded: Int, val sourceEventId: String, val description: String) : RewardResult()
    data class Capped(val reason: String) : RewardResult()
    data class Ineligible(val reason: String) : RewardResult()
    data class AlreadyAwarded(val sourceEventId: String) : RewardResult()
    data class Failure(val error: String) : RewardResult()
}

/**
 * Authoritative Zen XP append-only event ledger engine.
 *
 * Implements strict append-only semantics:
 * - No transaction is ever updated in place.
 * - No transaction is ever deleted from the database.
 * - Undoing an action appends a REVERSAL transaction.
 * - Redoing an action appends a RESTORE transaction.
 * - Balance is derived dynamically from: SUM(REWARD) + SUM(RESTORE) - SUM(REVERSAL).
 * - Category daily caps are computed from net effective XP for that date and category.
 */
class ZenXPManager(
    private val database: OfflineDatabase,
    var currentUserId: Long = 1L
) {
    private val xpDao = database.xpTransactionDao()

    companion object {
        const val HABIT_XP = 10
        const val MAX_HABIT_XP_PER_DAY = 60

        const val ALL_HABITS_BONUS_XP = 25

        const val DETOX_XP_PER_15_MIN = 5
        const val MAX_DETOX_XP_PER_DAY = 120

        const val STEP_GOAL_XP = 30

        const val EXERCISE_QUALIFYING_MINS = 10
        const val EXERCISE_XP_PER_SESSION = 20
        const val MAX_EXERCISE_XP_PER_DAY = 40

        const val ALL_TASKS_BONUS_XP = 15

        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }

    /**
     * Flow of user's current net Zen XP balance derived strictly from the append-only ledger.
     */
    val totalBalanceFlow: Flow<Int>
        get() = xpDao.getTotalBalance(currentUserId)

    suspend fun getTotalBalanceDirect(): Int {
        return xpDao.getTotalBalanceDirect(currentUserId)
    }

    fun getRecentTransactionsFlow(limit: Int = 50): Flow<List<XPTransactionEntity>> {
        return xpDao.getRecentTransactions(currentUserId, limit)
    }

    // ==========================================
    // REWARD OPERATIONS (WITH CATEGORY CAPS)
    // ==========================================

    /**
     * Awards 10 XP for completing a habit.
     * Enforces max 60 XP per day for habits.
     */
    suspend fun awardHabitCompletion(
        habitId: Long,
        date: String,
        habitTitle: String
    ): RewardResult = database.withTransaction {
        val sourceEventId = "habit_${habitId}_$date"

        val existingReward = xpDao.getRewardBySourceEventDirect(currentUserId, sourceEventId)
        if (existingReward != null) {
            val netXp = xpDao.getNetXPForSourceEventDirect(currentUserId, sourceEventId)
            if (netXp > 0) {
                return@withTransaction RewardResult.AlreadyAwarded(sourceEventId)
            }
            // Restore via appending RESTORE record
            val latestTx = xpDao.getLatestTransactionForSourceEventDirect(currentUserId, sourceEventId)
            val restoreTx = XPTransactionEntity(
                userId = currentUserId,
                sourceEventId = sourceEventId,
                sourceEventType = "HABIT",
                xpAmount = existingReward.xpAmount,
                transactionType = "RESTORE",
                linkedTransactionId = latestTx?.id ?: existingReward.id,
                date = date,
                timestamp = System.currentTimeMillis(),
                description = "Restored habit completion: $habitTitle"
            )
            xpDao.insertTransaction(restoreTx)
            return@withTransaction RewardResult.Success(existingReward.xpAmount, sourceEventId, restoreTx.description)
        }

        // Check daily habit cap from net effective XP
        val currentHabitXp = xpDao.getNetXPForDateAndType(currentUserId, date, "HABIT")
        val remainingCap = (MAX_HABIT_XP_PER_DAY - currentHabitXp).coerceAtLeast(0)
        val awardAmount = minOf(HABIT_XP, remainingCap)

        if (awardAmount <= 0) {
            return@withTransaction RewardResult.Capped("Daily habit XP cap ($MAX_HABIT_XP_PER_DAY XP) reached for $date")
        }

        val tx = XPTransactionEntity(
            userId = currentUserId,
            sourceEventId = sourceEventId,
            sourceEventType = "HABIT",
            xpAmount = awardAmount,
            transactionType = "REWARD",
            date = date,
            timestamp = System.currentTimeMillis(),
            description = "Completed habit: $habitTitle"
        )
        try {
            xpDao.insertTransaction(tx)
            RewardResult.Success(awardAmount, sourceEventId, tx.description)
        } catch (e: Exception) {
            RewardResult.Failure(e.message ?: "Failed to record habit reward")
        }
    }

    /**
     * Awards 25 XP when all active scheduled habits for a date are completed.
     */
    suspend fun awardAllHabitsBonus(
        date: String,
        totalHabitsCount: Int = 1
    ): RewardResult = database.withTransaction {
        if (totalHabitsCount <= 0) {
            return@withTransaction RewardResult.Ineligible("No scheduled habits to complete")
        }

        val sourceEventId = "all_habits_bonus_$date"

        val existingReward = xpDao.getRewardBySourceEventDirect(currentUserId, sourceEventId)
        if (existingReward != null) {
            val netXp = xpDao.getNetXPForSourceEventDirect(currentUserId, sourceEventId)
            if (netXp > 0) {
                return@withTransaction RewardResult.AlreadyAwarded(sourceEventId)
            }
            val latestTx = xpDao.getLatestTransactionForSourceEventDirect(currentUserId, sourceEventId)
            val restoreTx = XPTransactionEntity(
                userId = currentUserId,
                sourceEventId = sourceEventId,
                sourceEventType = "ALL_HABITS_BONUS",
                xpAmount = existingReward.xpAmount,
                transactionType = "RESTORE",
                linkedTransactionId = latestTx?.id ?: existingReward.id,
                date = date,
                timestamp = System.currentTimeMillis(),
                description = "Restored all habits completed bonus for $date"
            )
            xpDao.insertTransaction(restoreTx)
            return@withTransaction RewardResult.Success(existingReward.xpAmount, sourceEventId, restoreTx.description)
        }

        val tx = XPTransactionEntity(
            userId = currentUserId,
            sourceEventId = sourceEventId,
            sourceEventType = "ALL_HABITS_BONUS",
            xpAmount = ALL_HABITS_BONUS_XP,
            transactionType = "REWARD",
            date = date,
            timestamp = System.currentTimeMillis(),
            description = "All $totalHabitsCount habits completed bonus"
        )
        try {
            xpDao.insertTransaction(tx)
            RewardResult.Success(ALL_HABITS_BONUS_XP, sourceEventId, tx.description)
        } catch (e: Exception) {
            RewardResult.Failure(e.message ?: "Failed to record all-habits bonus")
        }
    }

    /**
     * Awards 5 XP per completed 15 minutes of digital detox.
     * Enforces max 120 XP per day for detox sessions.
     */
    suspend fun awardDetoxCompletion(
        sessionId: Long,
        actualMinutes: Int,
        date: String,
        sessionTitle: String
    ): RewardResult = database.withTransaction {
        val earnedUnits = actualMinutes / 15
        if (earnedUnits <= 0) {
            return@withTransaction RewardResult.Ineligible("Session duration ($actualMinutes min) less than 15-minute qualification threshold")
        }

        val grossReward = earnedUnits * DETOX_XP_PER_15_MIN
        val sourceEventId = "detox_session_$sessionId"

        val existingReward = xpDao.getRewardBySourceEventDirect(currentUserId, sourceEventId)
        if (existingReward != null) {
            val netXp = xpDao.getNetXPForSourceEventDirect(currentUserId, sourceEventId)
            if (netXp > 0) {
                return@withTransaction RewardResult.AlreadyAwarded(sourceEventId)
            }
            val latestTx = xpDao.getLatestTransactionForSourceEventDirect(currentUserId, sourceEventId)
            val restoreTx = XPTransactionEntity(
                userId = currentUserId,
                sourceEventId = sourceEventId,
                sourceEventType = "DETOX",
                xpAmount = existingReward.xpAmount,
                transactionType = "RESTORE",
                linkedTransactionId = latestTx?.id ?: existingReward.id,
                date = date,
                timestamp = System.currentTimeMillis(),
                description = "Restored detox session: $sessionTitle"
            )
            xpDao.insertTransaction(restoreTx)
            return@withTransaction RewardResult.Success(existingReward.xpAmount, sourceEventId, restoreTx.description)
        }

        // Check daily detox cap from net effective XP
        val currentDetoxXp = xpDao.getNetXPForDateAndType(currentUserId, date, "DETOX")
        val remainingCap = (MAX_DETOX_XP_PER_DAY - currentDetoxXp).coerceAtLeast(0)
        val awardAmount = minOf(grossReward, remainingCap)

        if (awardAmount <= 0) {
            return@withTransaction RewardResult.Capped("Daily detox XP cap ($MAX_DETOX_XP_PER_DAY XP) reached for $date")
        }

        val tx = XPTransactionEntity(
            userId = currentUserId,
            sourceEventId = sourceEventId,
            sourceEventType = "DETOX",
            xpAmount = awardAmount,
            transactionType = "REWARD",
            date = date,
            timestamp = System.currentTimeMillis(),
            description = "Completed $actualMinutes min detox: $sessionTitle"
        )
        try {
            xpDao.insertTransaction(tx)
            RewardResult.Success(awardAmount, sourceEventId, tx.description)
        } catch (e: Exception) {
            RewardResult.Failure(e.message ?: "Failed to record detox reward")
        }
    }

    /**
     * Awards 30 XP once per calendar day when the user meets their daily step goal.
     */
    suspend fun awardStepGoal(
        date: String,
        steps: Int,
        goalSteps: Int = 8000
    ): RewardResult = database.withTransaction {
        if (steps < goalSteps || goalSteps <= 0) {
            return@withTransaction RewardResult.Ineligible("Steps ($steps) below goal ($goalSteps)")
        }

        val sourceEventId = "step_goal_$date"

        val existingReward = xpDao.getRewardBySourceEventDirect(currentUserId, sourceEventId)
        if (existingReward != null) {
            val netXp = xpDao.getNetXPForSourceEventDirect(currentUserId, sourceEventId)
            if (netXp > 0) {
                return@withTransaction RewardResult.AlreadyAwarded(sourceEventId)
            }
            val latestTx = xpDao.getLatestTransactionForSourceEventDirect(currentUserId, sourceEventId)
            val restoreTx = XPTransactionEntity(
                userId = currentUserId,
                sourceEventId = sourceEventId,
                sourceEventType = "STEP_GOAL",
                xpAmount = existingReward.xpAmount,
                transactionType = "RESTORE",
                linkedTransactionId = latestTx?.id ?: existingReward.id,
                date = date,
                timestamp = System.currentTimeMillis(),
                description = "Restored daily step goal for $date"
            )
            xpDao.insertTransaction(restoreTx)
            return@withTransaction RewardResult.Success(existingReward.xpAmount, sourceEventId, restoreTx.description)
        }

        val tx = XPTransactionEntity(
            userId = currentUserId,
            sourceEventId = sourceEventId,
            sourceEventType = "STEP_GOAL",
            xpAmount = STEP_GOAL_XP,
            transactionType = "REWARD",
            date = date,
            timestamp = System.currentTimeMillis(),
            description = "Daily step goal achieved ($steps / $goalSteps steps)"
        )
        try {
            xpDao.insertTransaction(tx)
            RewardResult.Success(STEP_GOAL_XP, sourceEventId, tx.description)
        } catch (e: Exception) {
            RewardResult.Failure(e.message ?: "Failed to record step goal reward")
        }
    }

    /**
     * Awards 20 XP per qualifying workout session (>= 10 minutes).
     * Enforces max 40 XP per day for exercise sessions.
     */
    suspend fun awardExerciseSession(
        exerciseId: Long,
        durationMinutes: Int,
        date: String,
        exerciseType: String = "Workout",
        exerciseTitle: String = exerciseType
    ): RewardResult = database.withTransaction {
        val resolvedTitle = exerciseTitle.ifBlank { exerciseType.ifBlank { "Workout" } }
        if (durationMinutes < EXERCISE_QUALIFYING_MINS) {
            return@withTransaction RewardResult.Ineligible("Exercise duration ($durationMinutes min) under $EXERCISE_QUALIFYING_MINS min threshold")
        }

        val sourceEventId = "exercise_session_$exerciseId"

        val existingReward = xpDao.getRewardBySourceEventDirect(currentUserId, sourceEventId)
        if (existingReward != null) {
            val netXp = xpDao.getNetXPForSourceEventDirect(currentUserId, sourceEventId)
            if (netXp > 0) {
                return@withTransaction RewardResult.AlreadyAwarded(sourceEventId)
            }
            val latestTx = xpDao.getLatestTransactionForSourceEventDirect(currentUserId, sourceEventId)
            val restoreTx = XPTransactionEntity(
                userId = currentUserId,
                sourceEventId = sourceEventId,
                sourceEventType = "EXERCISE",
                xpAmount = existingReward.xpAmount,
                transactionType = "RESTORE",
                linkedTransactionId = latestTx?.id ?: existingReward.id,
                date = date,
                timestamp = System.currentTimeMillis(),
                description = "Restored exercise workout: $resolvedTitle"
            )
            xpDao.insertTransaction(restoreTx)
            return@withTransaction RewardResult.Success(existingReward.xpAmount, sourceEventId, restoreTx.description)
        }

        // Check daily exercise cap from net effective XP
        val currentExerciseXp = xpDao.getNetXPForDateAndType(currentUserId, date, "EXERCISE")
        val remainingCap = (MAX_EXERCISE_XP_PER_DAY - currentExerciseXp).coerceAtLeast(0)
        val awardAmount = minOf(EXERCISE_XP_PER_SESSION, remainingCap)

        if (awardAmount <= 0) {
            return@withTransaction RewardResult.Capped("Daily exercise XP cap ($MAX_EXERCISE_XP_PER_DAY XP) reached for $date")
        }

        val tx = XPTransactionEntity(
            userId = currentUserId,
            sourceEventId = sourceEventId,
            sourceEventType = "EXERCISE",
            xpAmount = awardAmount,
            transactionType = "REWARD",
            date = date,
            timestamp = System.currentTimeMillis(),
            description = "Completed $durationMinutes min $resolvedTitle workout"
        )
        try {
            xpDao.insertTransaction(tx)
            RewardResult.Success(awardAmount, sourceEventId, tx.description)
        } catch (e: Exception) {
            RewardResult.Failure(e.message ?: "Failed to record exercise reward")
        }
    }

    /**
     * Awards 15 XP once per calendar day when all scheduled real-world tasks are completed.
     */
    suspend fun awardAllTasksBonus(
        date: String,
        tasksCount: Int = 1
    ): RewardResult = database.withTransaction {
        if (tasksCount <= 0) {
            return@withTransaction RewardResult.Ineligible("No scheduled tasks to complete")
        }

        val sourceEventId = "all_tasks_bonus_$date"

        val existingReward = xpDao.getRewardBySourceEventDirect(currentUserId, sourceEventId)
        if (existingReward != null) {
            val netXp = xpDao.getNetXPForSourceEventDirect(currentUserId, sourceEventId)
            if (netXp > 0) {
                return@withTransaction RewardResult.AlreadyAwarded(sourceEventId)
            }
            val latestTx = xpDao.getLatestTransactionForSourceEventDirect(currentUserId, sourceEventId)
            val restoreTx = XPTransactionEntity(
                userId = currentUserId,
                sourceEventId = sourceEventId,
                sourceEventType = "ALL_TASKS_BONUS",
                xpAmount = existingReward.xpAmount,
                transactionType = "RESTORE",
                linkedTransactionId = latestTx?.id ?: existingReward.id,
                date = date,
                timestamp = System.currentTimeMillis(),
                description = "Restored all tasks completed bonus for $date"
            )
            xpDao.insertTransaction(restoreTx)
            return@withTransaction RewardResult.Success(existingReward.xpAmount, sourceEventId, restoreTx.description)
        }

        val tx = XPTransactionEntity(
            userId = currentUserId,
            sourceEventId = sourceEventId,
            sourceEventType = "ALL_TASKS_BONUS",
            xpAmount = ALL_TASKS_BONUS_XP,
            transactionType = "REWARD",
            date = date,
            timestamp = System.currentTimeMillis(),
            description = "All $tasksCount real-world tasks completed bonus"
        )
        try {
            xpDao.insertTransaction(tx)
            RewardResult.Success(ALL_TASKS_BONUS_XP, sourceEventId, tx.description)
        } catch (e: Exception) {
            RewardResult.Failure(e.message ?: "Failed to record all-tasks bonus")
        }
    }

    /**
     * Backward-compatible alias for awardAchievementUnlocked.
     */
    suspend fun awardAchievement(
        achievementId: String,
        xpReward: Int,
        title: String
    ): RewardResult = awardAchievementUnlocked(achievementId, title, xpReward)

    suspend fun awardAchievement(
        achievementId: String,
        title: String,
        xpReward: Int
    ): RewardResult = awardAchievementUnlocked(achievementId, title, xpReward)

    /**
     * Awards the designated one-time XP for unlocking an achievement.
     */
    suspend fun awardAchievementUnlocked(
        achievementId: String,
        title: String,
        xpReward: Int
    ): RewardResult = database.withTransaction {
        if (xpReward <= 0) {
            return@withTransaction RewardResult.Ineligible("Achievement has no XP value")
        }

        val sourceEventId = "achievement_$achievementId"

        val existingReward = xpDao.getRewardBySourceEventDirect(currentUserId, sourceEventId)
        if (existingReward != null) {
            val netXp = xpDao.getNetXPForSourceEventDirect(currentUserId, sourceEventId)
            if (netXp > 0) {
                return@withTransaction RewardResult.AlreadyAwarded(sourceEventId)
            }
            val latestTx = xpDao.getLatestTransactionForSourceEventDirect(currentUserId, sourceEventId)
            val restoreTx = XPTransactionEntity(
                userId = currentUserId,
                sourceEventId = sourceEventId,
                sourceEventType = "ACHIEVEMENT",
                xpAmount = existingReward.xpAmount,
                transactionType = "RESTORE",
                linkedTransactionId = latestTx?.id ?: existingReward.id,
                date = LocalDate.now().format(DATE_FORMATTER),
                timestamp = System.currentTimeMillis(),
                description = "Restored achievement reward: $title"
            )
            xpDao.insertTransaction(restoreTx)
            return@withTransaction RewardResult.Success(existingReward.xpAmount, sourceEventId, restoreTx.description)
        }

        val todayStr = LocalDate.now().format(DATE_FORMATTER)
        val tx = XPTransactionEntity(
            userId = currentUserId,
            sourceEventId = sourceEventId,
            sourceEventType = "ACHIEVEMENT",
            xpAmount = xpReward,
            transactionType = "REWARD",
            date = todayStr,
            timestamp = System.currentTimeMillis(),
            description = "Unlocked achievement: $title"
        )
        try {
            xpDao.insertTransaction(tx)
            RewardResult.Success(xpReward, sourceEventId, tx.description)
        } catch (e: Exception) {
            RewardResult.Failure(e.message ?: "Failed to record achievement reward")
        }
    }

    // ==========================================
    // REVERSAL OPERATIONS (FOR UNDO / ROLLBACK)
    // ==========================================

    /**
     * Reverses an existing reward transaction by appending a linked REVERSAL record.
     * Prevents duplicate reversals if already reversed (net XP <= 0).
     */
    suspend fun reverseReward(
        sourceEventId: String,
        reason: String = "Action undone"
    ): Boolean = database.withTransaction {
        val originalReward = xpDao.getRewardBySourceEventDirect(currentUserId, sourceEventId) ?: return@withTransaction false

        val netXp = xpDao.getNetXPForSourceEventDirect(currentUserId, sourceEventId)
        if (netXp <= 0) {
            return@withTransaction true // Already reversed safely; net XP is 0
        }

        val latestTx = xpDao.getLatestTransactionForSourceEventDirect(currentUserId, sourceEventId)
        val todayStr = LocalDate.now().format(DATE_FORMATTER)
        val reversalTx = XPTransactionEntity(
            userId = currentUserId,
            sourceEventId = sourceEventId,
            sourceEventType = originalReward.sourceEventType,
            xpAmount = originalReward.xpAmount,
            transactionType = "REVERSAL",
            linkedTransactionId = latestTx?.id ?: originalReward.id,
            date = todayStr,
            timestamp = System.currentTimeMillis(),
            description = "Reversal ($reason): ${originalReward.description}"
        )
        try {
            xpDao.insertTransaction(reversalTx)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Restores a reversed reward (for redo operation) by appending a linked RESTORE record.
     * Prevents duplicate restoration if already active (net XP > 0).
     */
    suspend fun restoreReward(sourceEventId: String): Boolean = database.withTransaction {
        val originalReward = xpDao.getRewardBySourceEventDirect(currentUserId, sourceEventId) ?: return@withTransaction false

        val netXp = xpDao.getNetXPForSourceEventDirect(currentUserId, sourceEventId)
        if (netXp > 0) {
            return@withTransaction true // Already restored / active
        }

        val latestTx = xpDao.getLatestTransactionForSourceEventDirect(currentUserId, sourceEventId)
        val todayStr = LocalDate.now().format(DATE_FORMATTER)
        val restoreTx = XPTransactionEntity(
            userId = currentUserId,
            sourceEventId = sourceEventId,
            sourceEventType = originalReward.sourceEventType,
            xpAmount = originalReward.xpAmount,
            transactionType = "RESTORE",
            linkedTransactionId = latestTx?.id ?: originalReward.id,
            date = todayStr,
            timestamp = System.currentTimeMillis(),
            description = "Restored: ${originalReward.description}"
        )
        try {
            xpDao.insertTransaction(restoreTx)
            true
        } catch (_: Exception) {
            false
        }
    }
}
