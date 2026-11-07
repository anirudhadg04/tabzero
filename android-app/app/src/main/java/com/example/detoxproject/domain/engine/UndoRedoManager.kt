package com.example.detoxproject.domain.engine

import androidx.room.withTransaction
import com.example.detoxproject.data.local.OfflineDatabase
import com.example.detoxproject.data.local.entity.*
import java.time.LocalDate
import java.time.ZoneId

class UndoRedoManager(
    private val database: OfflineDatabase,
    var currentUserId: Long = 1L
) {
    private val historyDao = database.actionHistoryDao()
    private val detoxDao = database.detoxDao()
    private val pledgeDao = database.pledgeDao()
    private val goalsDao = database.userGoalsDao()
    private val activityDao = database.plannedActivityDao()
    private val achievementDao = database.achievementDao()
    private val habitDao = database.habitDao()

    val zenXpManager = ZenXPManager(database, currentUserId)

    fun updateUserId(userId: Long) {
        currentUserId = userId
        zenXpManager.currentUserId = userId
    }

    fun getTodayRange(referenceDate: LocalDate = LocalDate.now()): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = referenceDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = referenceDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return Pair(start, end)
    }

    /**
     * Records a new state-changing operation into the persistent 5-step daily stack.
     * Automatically clears actions from prior days, enforces the 5-item limit for today,
     * and clears the redo (future) stack.
     */
    suspend fun recordOperation(
        actionType: String,
        description: String,
        entityType: String,
        entityId: Long,
        beforeJson: String?,
        afterJson: String?,
        referenceDate: LocalDate = LocalDate.now()
    ) = database.withTransaction {
        val (startOfDay, endOfDay) = getTodayRange(referenceDate)

        // Automatically purge any undo/redo actions from prior days to reset daily stack
        historyDao.purgePastDaysHistory(startOfDay, currentUserId)
        historyDao.clearFuture(currentUserId)

        val pastActions = historyDao.getTodayPastActionsDirect(currentUserId, startOfDay, endOfDay)
        if (pastActions.size >= 5) {
            val excess = pastActions.size - 4
            historyDao.trimOldestTodayPast(excess, startOfDay, endOfDay, currentUserId)
        }

        val nextOrder = (pastActions.maxOfOrNull { it.sequenceOrder } ?: 0) + 1
        historyDao.insertAction(
            ActionHistoryEntity(
                userId = currentUserId,
                actionType = actionType,
                description = description,
                timestamp = System.currentTimeMillis(),
                stackPosition = "PAST",
                entityType = entityType,
                entityId = entityId,
                beforeStateJson = beforeJson,
                afterStateJson = afterJson,
                sequenceOrder = nextOrder
            )
        )
    }

    /**
     * Undoes the most recent operation from PAST, moves it to FUTURE,
     * reverses associated XP in the append-only ledger, and recalculates dependent aggregates.
     */
    suspend fun undo(referenceDate: LocalDate = LocalDate.now()): Boolean = database.withTransaction {
        val (startOfDay, endOfDay) = getTodayRange(referenceDate)
        val latest = historyDao.getLatestTodayPastAction(currentUserId, startOfDay, endOfDay) ?: return@withTransaction false

        if (latest.timestamp < startOfDay || latest.timestamp >= endOfDay) {
            return@withTransaction false
        }

        when (latest.entityType) {
            "SESSION" -> {
                when (latest.actionType) {
                    "CREATE_SESSION" -> {
                        detoxDao.deleteSessionById(latest.entityId)
                    }
                    "UPDATE_SESSION" -> {
                        latest.beforeStateJson?.let { jsonStr ->
                            val entity = UndoRedoSerializer.deserializeSession(jsonStr)
                            detoxDao.insertSession(entity.copy(userId = currentUserId))
                        }
                    }
                    "COMPLETE_SESSION" -> {
                        latest.beforeStateJson?.let { jsonStr ->
                            val entity = UndoRedoSerializer.deserializeSession(jsonStr)
                            detoxDao.insertSession(entity.copy(userId = currentUserId))
                        }
                        zenXpManager.reverseReward("detox_${latest.entityId}", "Detox session undone")
                    }
                    "DELETE_SESSION" -> {
                        latest.beforeStateJson?.let { jsonStr ->
                            val entity = UndoRedoSerializer.deserializeSession(jsonStr)
                            detoxDao.insertSession(entity.copy(userId = currentUserId))
                        }
                    }
                }
            }
            "PLEDGE" -> {
                when (latest.actionType) {
                    "CREATE_PLEDGE" -> {
                        pledgeDao.deletePledgeById(latest.entityId)
                    }
                    "UPDATE_PLEDGE" -> {
                        latest.beforeStateJson?.let { jsonStr ->
                            val entity = UndoRedoSerializer.deserializePledge(jsonStr)
                            pledgeDao.insertPledge(entity.copy(userId = currentUserId))
                        }
                    }
                    "DELETE_PLEDGE" -> {
                        latest.beforeStateJson?.let { jsonStr ->
                            val entity = UndoRedoSerializer.deserializePledge(jsonStr)
                            pledgeDao.insertPledge(entity.copy(userId = currentUserId))
                        }
                    }
                }
            }
            "GOALS" -> {
                latest.beforeStateJson?.let { jsonStr ->
                    val entity = UndoRedoSerializer.deserializeGoals(jsonStr)
                    goalsDao.insertOrUpdate(entity.copy(userId = currentUserId))
                }
            }
            "ACTIVITY" -> {
                when (latest.actionType) {
                    "CREATE_ACTIVITY" -> {
                        activityDao.deleteActivityById(latest.entityId)
                    }
                    "COMPLETE_ACTIVITY" -> {
                        latest.beforeStateJson?.let { jsonStr ->
                            val entity = UndoRedoSerializer.deserializeActivity(jsonStr)
                            activityDao.insertActivity(entity.copy(userId = currentUserId))
                            zenXpManager.reverseReward("all_tasks_${entity.scheduledDate}", "Task completion undone")
                        }
                    }
                    "UNCOMPLETE_ACTIVITY", "UPDATE_ACTIVITY" -> {
                        latest.beforeStateJson?.let { jsonStr ->
                            val entity = UndoRedoSerializer.deserializeActivity(jsonStr)
                            activityDao.insertActivity(entity.copy(userId = currentUserId))
                        }
                    }
                    "DELETE_ACTIVITY" -> {
                        latest.beforeStateJson?.let { jsonStr ->
                            val entity = UndoRedoSerializer.deserializeActivity(jsonStr)
                            activityDao.insertActivity(entity.copy(userId = currentUserId))
                        }
                    }
                }
            }
            "HABIT" -> {
                when (latest.actionType) {
                    "CREATE_HABIT" -> {
                        habitDao.deleteHabit(latest.entityId)
                    }
                    "UPDATE_HABIT", "ARCHIVE_HABIT" -> {
                        latest.beforeStateJson?.let { jsonStr ->
                            val entity = UndoRedoSerializer.deserializeHabit(jsonStr)
                            habitDao.insertHabit(entity.copy(userId = currentUserId))
                        }
                    }
                }
            }
            "HABIT_COMPLETION" -> {
                when (latest.actionType) {
                    "COMPLETE_HABIT" -> {
                        latest.afterStateJson?.let { jsonStr ->
                            val comp = UndoRedoSerializer.deserializeHabitCompletion(jsonStr)
                            habitDao.deleteCompletion(comp.habitId, comp.date)
                            zenXpManager.reverseReward("habit_${comp.habitId}_${comp.date}", "Habit completion undone")
                            zenXpManager.reverseReward("all_habits_${comp.date}", "All-habits bonus undone")
                        }
                    }
                    "UNCOMPLETE_HABIT" -> {
                        latest.beforeStateJson?.let { jsonStr ->
                            val comp = UndoRedoSerializer.deserializeHabitCompletion(jsonStr)
                            habitDao.insertCompletion(comp)
                            zenXpManager.restoreReward("habit_${comp.habitId}_${comp.date}")
                        }
                    }
                }
            }
        }

        historyDao.updateAction(latest.copy(stackPosition = "FUTURE"))
        recalculateAggregates()
        true
    }

    /**
     * Redoes the next operation from FUTURE, moves it to PAST,
     * restores reversed XP in the ledger, and recalculates dependent aggregates.
     */
    suspend fun redo(referenceDate: LocalDate = LocalDate.now()): Boolean = database.withTransaction {
        val (startOfDay, endOfDay) = getTodayRange(referenceDate)
        val next = historyDao.getNextTodayFutureAction(currentUserId, startOfDay, endOfDay) ?: return@withTransaction false

        if (next.timestamp < startOfDay || next.timestamp >= endOfDay) {
            return@withTransaction false
        }

        when (next.entityType) {
            "SESSION" -> {
                when (next.actionType) {
                    "CREATE_SESSION" -> {
                        next.afterStateJson?.let { jsonStr ->
                            val entity = UndoRedoSerializer.deserializeSession(jsonStr)
                            detoxDao.insertSession(entity.copy(userId = currentUserId))
                        }
                    }
                    "UPDATE_SESSION" -> {
                        next.afterStateJson?.let { jsonStr ->
                            val entity = UndoRedoSerializer.deserializeSession(jsonStr)
                            detoxDao.insertSession(entity.copy(userId = currentUserId))
                        }
                    }
                    "COMPLETE_SESSION" -> {
                        next.afterStateJson?.let { jsonStr ->
                            val entity = UndoRedoSerializer.deserializeSession(jsonStr)
                            detoxDao.insertSession(entity.copy(userId = currentUserId))
                        }
                        zenXpManager.restoreReward("detox_${next.entityId}")
                    }
                    "DELETE_SESSION" -> {
                        detoxDao.deleteSessionById(next.entityId)
                    }
                }
            }
            "PLEDGE" -> {
                when (next.actionType) {
                    "CREATE_PLEDGE" -> {
                        next.afterStateJson?.let { jsonStr ->
                            val entity = UndoRedoSerializer.deserializePledge(jsonStr)
                            pledgeDao.insertPledge(entity.copy(userId = currentUserId))
                        }
                    }
                    "UPDATE_PLEDGE" -> {
                        next.afterStateJson?.let { jsonStr ->
                            val entity = UndoRedoSerializer.deserializePledge(jsonStr)
                            pledgeDao.insertPledge(entity.copy(userId = currentUserId))
                        }
                    }
                    "DELETE_PLEDGE" -> {
                        pledgeDao.deletePledgeById(next.entityId)
                    }
                }
            }
            "GOALS" -> {
                next.afterStateJson?.let { jsonStr ->
                    val entity = UndoRedoSerializer.deserializeGoals(jsonStr)
                    goalsDao.insertOrUpdate(entity.copy(userId = currentUserId))
                }
            }
            "ACTIVITY" -> {
                when (next.actionType) {
                    "CREATE_ACTIVITY" -> {
                        next.afterStateJson?.let { jsonStr ->
                            val entity = UndoRedoSerializer.deserializeActivity(jsonStr)
                            activityDao.insertActivity(entity.copy(userId = currentUserId))
                        }
                    }
                    "COMPLETE_ACTIVITY" -> {
                        next.afterStateJson?.let { jsonStr ->
                            val entity = UndoRedoSerializer.deserializeActivity(jsonStr)
                            activityDao.insertActivity(entity.copy(userId = currentUserId))
                            zenXpManager.restoreReward("all_tasks_${entity.scheduledDate}")
                        }
                    }
                    "UNCOMPLETE_ACTIVITY", "UPDATE_ACTIVITY" -> {
                        next.afterStateJson?.let { jsonStr ->
                            val entity = UndoRedoSerializer.deserializeActivity(jsonStr)
                            activityDao.insertActivity(entity.copy(userId = currentUserId))
                        }
                    }
                    "DELETE_ACTIVITY" -> {
                        activityDao.deleteActivityById(next.entityId)
                    }
                }
            }
            "HABIT" -> {
                when (next.actionType) {
                    "CREATE_HABIT" -> {
                        next.afterStateJson?.let { jsonStr ->
                            val entity = UndoRedoSerializer.deserializeHabit(jsonStr)
                            habitDao.insertHabit(entity.copy(userId = currentUserId))
                        }
                    }
                    "UPDATE_HABIT", "ARCHIVE_HABIT" -> {
                        next.afterStateJson?.let { jsonStr ->
                            val entity = UndoRedoSerializer.deserializeHabit(jsonStr)
                            habitDao.insertHabit(entity.copy(userId = currentUserId))
                        }
                    }
                }
            }
            "HABIT_COMPLETION" -> {
                when (next.actionType) {
                    "COMPLETE_HABIT" -> {
                        next.afterStateJson?.let { jsonStr ->
                            val comp = UndoRedoSerializer.deserializeHabitCompletion(jsonStr)
                            habitDao.insertCompletion(comp)
                            zenXpManager.restoreReward("habit_${comp.habitId}_${comp.date}")
                            zenXpManager.restoreReward("all_habits_${comp.date}")
                        }
                    }
                    "UNCOMPLETE_HABIT" -> {
                        next.afterStateJson?.let { jsonStr ->
                            val comp = UndoRedoSerializer.deserializeHabitCompletion(jsonStr)
                            habitDao.deleteCompletion(comp.habitId, comp.date)
                            zenXpManager.reverseReward("habit_${comp.habitId}_${comp.date}", "Habit undone on redo")
                            zenXpManager.reverseReward("all_habits_${comp.date}", "All-habits bonus undone on redo")
                        }
                    }
                }
            }
        }

        historyDao.updateAction(next.copy(stackPosition = "PAST"))
        recalculateAggregates()
        true
    }

    /**
     * Recalculates streaks and milestone achievements from raw underlying records,
     * maintaining consistency in the Zen XP ledger.
     */
    suspend fun recalculateAggregates() {
        val completedSessions = detoxDao.getAllCompletedSessionsDirect(currentUserId)
        val goals = goalsDao.getUserGoalsDirect(currentUserId)
        val dailyGoalMinutes = goals?.dailyOfflineMinutes ?: 120

        val allActivities = activityDao.getAllActivitiesDirect(currentUserId)
        val completedActivities = allActivities.filter { it.status == "COMPLETED" }

        val existingAchievements = achievementDao.getAllAchievementsDirect(currentUserId)
        if (existingAchievements.isEmpty()) return

        val habits = habitDao.getAllActiveHabitsDirect(currentUserId)
        val allCompletions = habitDao.getAllCompletionsDirect()
        val totalHabitCompletions = allCompletions.size

        val dailyActivities = database.dailyActivityDao().getAllActivitiesDirect()
        val daysWithStepGoal = dailyActivities.count { it.steps >= it.goalSteps }

        val exerciseSessions = database.exerciseDao().getAllCompletedExercisesDirect()

        val allStreaks = StreakEngine.calculateAllStreaks(
            habits = habits,
            completions = allCompletions,
            completedSessions = completedSessions,
            dailyActivities = dailyActivities,
            completedExercises = exerciseSessions,
            dailyOfflineGoalMinutes = dailyGoalMinutes
        )

        val maxStreak = maxOf(
            allStreaks.habitStreak.longestStreak,
            allStreaks.detoxStreak.longestStreak,
            allStreaks.movementStreak.longestStreak,
            allStreaks.exerciseStreak.longestStreak
        )

        val totalWorkouts = exerciseSessions.count { it.status == "COMPLETED" }
        val totalMindfulNotes = database.mindfulNoteDao().getAllNotesDirect().size
        val hasUsedGrace = database.graceDayDao().getAllGraceDaysDirect(currentUserId).isNotEmpty()
        val totalXp = database.xpTransactionDao().getTotalBalanceDirect(currentUserId)
        val userLevel = LevelCalculator.calculateLevel(totalXp).level
        val singleDayMaxSteps = dailyActivities.maxOfOrNull { it.steps } ?: 0

        val updated = MilestoneEvaluator.evaluateAllAchievements(
            completedSessions = completedSessions,
            completedActivities = completedActivities,
            existingAchievements = existingAchievements,
            goals = goals,
            daysWithStepGoal = daysWithStepGoal,
            totalHabitCompletions = totalHabitCompletions,
            maxStreak = maxStreak,
            totalWorkouts = totalWorkouts,
            userLevel = userLevel,
            singleDayMaxSteps = singleDayMaxSteps,
            totalMindfulNotes = totalMindfulNotes,
            isGraceUsed = hasUsedGrace
        )

        val existingMap = existingAchievements.associateBy { it.id }

        updated.forEach { ach ->
            val oldAch = existingMap[ach.id]
            if (ach.isUnlocked && (oldAch == null || !oldAch.isUnlocked)) {
                // Newly unlocked: award XP
                if (ach.xpReward > 0) {
                    zenXpManager.awardAchievement(ach.id, ach.xpReward, ach.title)
                }
            } else if (!ach.isUnlocked && (oldAch != null && oldAch.isUnlocked)) {
                // Relocked due to undo: reverse XP
                zenXpManager.reverseReward("achievement_${ach.id}", "Achievement relocked via undo")
            }
            achievementDao.updateAchievement(ach.copy(userId = currentUserId))
        }
    }
}
