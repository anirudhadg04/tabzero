package com.example.detoxproject.domain.engine

import com.example.detoxproject.data.local.entity.AchievementEntity
import com.example.detoxproject.data.local.entity.DetoxSessionEntity
import com.example.detoxproject.data.local.entity.PlannedActivityEntity
import com.example.detoxproject.data.local.entity.UserGoalsEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object MilestoneEvaluator {

    val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Calculates current streak in local days where daily offline minutes >= goal.
     */
    fun calculateStreak(
        completedSessions: List<DetoxSessionEntity>,
        dailyOfflineGoalMinutes: Int,
        referenceDate: LocalDate = LocalDate.now()
    ): Int {
        if (completedSessions.isEmpty() || dailyOfflineGoalMinutes <= 0) return 0

        val minutesPerDate = completedSessions
            .filter { it.status == "COMPLETED" }
            .groupBy { it.sessionDate }
            .mapValues { entry -> entry.value.sumOf { it.actualMinutes } }

        var streak = 0
        var checkDate = referenceDate

        val todayMinutes = minutesPerDate[checkDate.format(DATE_FORMATTER)] ?: 0
        if (todayMinutes < dailyOfflineGoalMinutes) {
            checkDate = checkDate.minusDays(1)
        }

        while (true) {
            val dateStr = checkDate.format(DATE_FORMATTER)
            val minutes = minutesPerDate[dateStr] ?: 0
            if (minutes >= dailyOfflineGoalMinutes) {
                streak++
                checkDate = checkDate.minusDays(1)
            } else {
                break
            }
        }

        return streak
    }

    /**
     * Backward-compatible evaluation overload.
     */
    fun evaluateAchievements(
        completedSessions: List<DetoxSessionEntity>,
        completedActivities: List<PlannedActivityEntity>,
        goals: UserGoalsEntity?,
        existingAchievements: List<AchievementEntity>,
        referenceDate: LocalDate = LocalDate.now()
    ): List<AchievementEntity> {
        return evaluateAllAchievements(
            completedSessions = completedSessions,
            completedActivities = completedActivities,
            existingAchievements = existingAchievements,
            goals = goals,
            referenceDate = referenceDate
        )
    }

    fun evaluateAchievements(
        completedSessions: List<DetoxSessionEntity>,
        completedActivities: List<PlannedActivityEntity>,
        existingAchievements: List<AchievementEntity>,
        goals: UserGoalsEntity? = null,
        daysWithStepGoal: Int = 0,
        totalHabitCompletions: Int = 0,
        maxStreak: Int = 0,
        referenceDate: LocalDate = LocalDate.now()
    ): List<AchievementEntity> {
        return evaluateAllAchievements(
            completedSessions = completedSessions,
            completedActivities = completedActivities,
            existingAchievements = existingAchievements,
            goals = goals,
            daysWithStepGoal = daysWithStepGoal,
            totalHabitCompletions = totalHabitCompletions,
            maxStreak = maxStreak,
            referenceDate = referenceDate
        )
    }

    /**
     * Authoritative evaluation for all canonical achievements across 5 categories,
     * including dynamic progression badges and legacy backwards compatibility.
     */
    fun evaluateAllAchievements(
        completedSessions: List<DetoxSessionEntity>,
        completedActivities: List<PlannedActivityEntity>,
        existingAchievements: List<AchievementEntity>,
        goals: UserGoalsEntity? = null,
        daysWithStepGoal: Int = 0,
        totalHabitCompletions: Int = 0,
        maxStreak: Int = 0,
        totalWorkouts: Int = 0,
        userLevel: Int = 1,
        singleDayMaxSteps: Int = 0,
        totalMindfulNotes: Int = 0,
        isGraceUsed: Boolean = false,
        referenceDate: LocalDate = LocalDate.now()
    ): List<AchievementEntity> {
        val offlineGoal = goals?.dailyOfflineMinutes ?: 120
        val detoxStreak = calculateStreak(completedSessions, offlineGoal, referenceDate)
        val effectiveMaxStreak = maxOf(detoxStreak, maxStreak)
        val totalDetoxMinutes = completedSessions.filter { it.status == "COMPLETED" }.sumOf { it.actualMinutes }
        val completedActivityCount = completedActivities.count { it.status == "COMPLETED" }
        val now = System.currentTimeMillis()

        return existingAchievements.map { ach ->
            when (ach.id) {
                // 1. DISCONNECT
                "first_detox" -> {
                    val unlocked = completedSessions.any { it.status == "COMPLETED" }
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = if (unlocked) 1f else 0f
                    )
                }

                "detox_1_hour_master", "hour_free", "one_hour_free" -> {
                    val hasOneHour = completedSessions.any { it.status == "COMPLETED" && it.actualMinutes >= 60 }
                    val longest = completedSessions.filter { it.status == "COMPLETED" }.maxOfOrNull { it.actualMinutes } ?: 0
                    ach.copy(
                        isUnlocked = hasOneHour,
                        unlockedAt = if (hasOneHour) ach.unlockedAt ?: now else null,
                        progress = if (hasOneHour) 1f else (longest.toFloat() / 60f).coerceIn(0f, 1f)
                    )
                }

                "detox_10_hours" -> {
                    val targetMinutes = 600 // 10 hours
                    val unlocked = totalDetoxMinutes >= targetMinutes
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = (totalDetoxMinutes.toFloat() / targetMinutes).coerceIn(0f, 1f)
                    )
                }

                "detox_25_hours" -> {
                    val targetMinutes = 1500 // 25 hours
                    val unlocked = totalDetoxMinutes >= targetMinutes
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = (totalDetoxMinutes.toFloat() / targetMinutes).coerceIn(0f, 1f)
                    )
                }

                "deep_zen_master", "deep_focus" -> {
                    val hasTwoHours = completedSessions.any { it.status == "COMPLETED" && it.actualMinutes >= 120 }
                    val longestSession = completedSessions.filter { it.status == "COMPLETED" }.maxOfOrNull { it.actualMinutes } ?: 0
                    ach.copy(
                        isUnlocked = hasTwoHours,
                        unlockedAt = if (hasTwoHours) ach.unlockedAt ?: now else null,
                        progress = if (hasTwoHours) 1f else (longestSession.toFloat() / 120f).coerceIn(0f, 1f)
                    )
                }

                "detox_100_hours" -> {
                    val targetMinutes = 6000 // 100 hours
                    val unlocked = totalDetoxMinutes >= targetMinutes
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = (totalDetoxMinutes.toFloat() / targetMinutes).coerceIn(0f, 1f)
                    )
                }

                // 2. MOVE
                "step_starter" -> {
                    val unlocked = daysWithStepGoal >= 1
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = if (unlocked) 1f else 0f
                    )
                }

                "step_7_days" -> {
                    val targetDays = 7
                    val unlocked = daysWithStepGoal >= targetDays
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = (daysWithStepGoal.toFloat() / targetDays).coerceIn(0f, 1f)
                    )
                }

                "step_14_days" -> {
                    val targetDays = 14
                    val unlocked = daysWithStepGoal >= targetDays
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = (daysWithStepGoal.toFloat() / targetDays).coerceIn(0f, 1f)
                    )
                }

                "step_10k_day" -> {
                    val effectiveMax = maxOf(singleDayMaxSteps, if (daysWithStepGoal >= 1) (goals?.dailySteps ?: 8000) else 0)
                    val unlocked = effectiveMax >= 10000
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = if (unlocked) 1f else (effectiveMax.toFloat() / 10000f).coerceIn(0f, 1f)
                    )
                }

                "workout_starter" -> {
                    val unlocked = totalWorkouts >= 1
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = if (unlocked) 1f else 0f
                    )
                }

                "workout_5_club" -> {
                    val target = 5
                    val unlocked = totalWorkouts >= target
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = (totalWorkouts.toFloat() / target).coerceIn(0f, 1f)
                    )
                }

                "active_workout_10" -> {
                    val target = 10
                    val unlocked = totalWorkouts >= target
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = (totalWorkouts.toFloat() / target).coerceIn(0f, 1f)
                    )
                }

                "step_30_days" -> {
                    val targetDays = 30
                    val unlocked = daysWithStepGoal >= targetDays
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = (daysWithStepGoal.toFloat() / targetDays).coerceIn(0f, 1f)
                    )
                }

                // 3. RECONNECT
                "first_offline_task", "reconnect_nature", "mindful_reconnect" -> {
                    val unlocked = completedActivityCount >= 1
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = if (unlocked) 1f else 0f
                    )
                }

                "reconnect_5_tasks" -> {
                    val target = 5
                    val unlocked = completedActivityCount >= target
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = (completedActivityCount.toFloat() / target).coerceIn(0f, 1f)
                    )
                }

                "reconnect_10_tasks" -> {
                    val target = 10
                    val unlocked = completedActivityCount >= target
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = (completedActivityCount.toFloat() / target).coerceIn(0f, 1f)
                    )
                }

                "reconnect_25_tasks" -> {
                    val target = 25
                    val unlocked = completedActivityCount >= target
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = (completedActivityCount.toFloat() / target).coerceIn(0f, 1f)
                    )
                }

                "reconnect_50_tasks" -> {
                    val target = 50
                    val unlocked = completedActivityCount >= target
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = (completedActivityCount.toFloat() / target).coerceIn(0f, 1f)
                    )
                }

                // 4. CONSISTENCY
                "grace_guardian" -> {
                    val unlocked = isGraceUsed
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = if (unlocked) 1f else 0f
                    )
                }

                "streak_3_days", "consistency_3", "consistency" -> {
                    val target = 3
                    val unlocked = effectiveMaxStreak >= target
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = (effectiveMaxStreak.toFloat() / target).coerceIn(0f, 1f)
                    )
                }

                "streak_7_days", "habit_champion" -> {
                    val target = 7
                    val unlocked = effectiveMaxStreak >= target
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = (effectiveMaxStreak.toFloat() / target).coerceIn(0f, 1f)
                    )
                }

                "streak_14_days" -> {
                    val target = 14
                    val unlocked = effectiveMaxStreak >= target
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = (effectiveMaxStreak.toFloat() / target).coerceIn(0f, 1f)
                    )
                }

                "streak_30_days" -> {
                    val target = 30
                    val unlocked = effectiveMaxStreak >= target
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = (effectiveMaxStreak.toFloat() / target).coerceIn(0f, 1f)
                    )
                }

                "streak_60_days" -> {
                    val target = 60
                    val unlocked = effectiveMaxStreak >= target
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = (effectiveMaxStreak.toFloat() / target).coerceIn(0f, 1f)
                    )
                }

                "streak_100_days" -> {
                    val target = 100
                    val unlocked = effectiveMaxStreak >= target
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = (effectiveMaxStreak.toFloat() / target).coerceIn(0f, 1f)
                    )
                }

                // 5. PERSONAL GROWTH
                "mindful_note_scribe" -> {
                    val unlocked = totalMindfulNotes >= 1
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = if (unlocked) 1f else 0f
                    )
                }

                "mindful_reflector" -> {
                    val target = 5
                    val unlocked = totalMindfulNotes >= target
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = (totalMindfulNotes.toFloat() / target).coerceIn(0f, 1f)
                    )
                }

                "habits_10" -> {
                    val target = 10
                    val unlocked = totalHabitCompletions >= target
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = (totalHabitCompletions.toFloat() / target).coerceIn(0f, 1f)
                    )
                }

                "habits_25" -> {
                    val target = 25
                    val unlocked = totalHabitCompletions >= target
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = (totalHabitCompletions.toFloat() / target).coerceIn(0f, 1f)
                    )
                }

                "habits_50" -> {
                    val target = 50
                    val unlocked = totalHabitCompletions >= target
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = (totalHabitCompletions.toFloat() / target).coerceIn(0f, 1f)
                    )
                }

                "habits_100" -> {
                    val target = 100
                    val unlocked = totalHabitCompletions >= target
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = (totalHabitCompletions.toFloat() / target).coerceIn(0f, 1f)
                    )
                }

                "habits_200" -> {
                    val target = 200
                    val unlocked = totalHabitCompletions >= target
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = (totalHabitCompletions.toFloat() / target).coerceIn(0f, 1f)
                    )
                }

                "level_5_achiever" -> {
                    val target = 5
                    val unlocked = userLevel >= target
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = (userLevel.toFloat() / target).coerceIn(0f, 1f)
                    )
                }

                "level_10_master" -> {
                    val target = 10
                    val unlocked = userLevel >= target
                    ach.copy(
                        isUnlocked = unlocked,
                        unlockedAt = if (unlocked) ach.unlockedAt ?: now else null,
                        progress = (userLevel.toFloat() / target).coerceIn(0f, 1f)
                    )
                }

                else -> ach
            }
        }
    }
}
