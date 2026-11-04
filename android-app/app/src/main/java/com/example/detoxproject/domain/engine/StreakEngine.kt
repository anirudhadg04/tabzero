package com.example.detoxproject.domain.engine

import com.example.detoxproject.data.local.entity.DailyActivityEntity
import com.example.detoxproject.data.local.entity.DetoxSessionEntity
import com.example.detoxproject.data.local.entity.ExerciseSessionEntity
import com.example.detoxproject.data.local.entity.HabitCompletionEntity
import com.example.detoxproject.data.local.entity.HabitEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class StreakCategoryInfo(
    val currentStreak: Int,
    val longestStreak: Int,
    val isQualifiedToday: Boolean,
    val isGraceUsedInCurrentStreak: Boolean = false
)

data class AllStreaksInfo(
    val habitStreak: StreakCategoryInfo,
    val detoxStreak: StreakCategoryInfo,
    val movementStreak: StreakCategoryInfo,
    val exerciseStreak: StreakCategoryInfo
)

object StreakEngine {

    val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * 1. Habit Streak:
     * Evaluates consecutive scheduled days where all scheduled active habits were completed.
     * Unscheduled days (no active habits scheduled) skip without breaking the streak or consuming grace.
     * Protected dates in [protectedDates] save a missed scheduled day.
     */
    fun calculateHabitStreak(
        habits: List<HabitEntity>,
        completions: List<HabitCompletionEntity>,
        protectedDates: Set<LocalDate> = emptySet(),
        referenceDate: LocalDate = LocalDate.now()
    ): StreakCategoryInfo {
        val activeHabits = habits.filter { !it.isArchived }
        if (activeHabits.isEmpty()) {
            return StreakCategoryInfo(0, 0, false)
        }

        val completedByDate = completions.groupBy { it.date }
            .mapValues { entry -> entry.value.map { it.habitId }.toSet() }

        fun isDayQualified(date: LocalDate): Boolean? {
            val scheduledForDate = activeHabits.filter { HabitStreakCalculator.isScheduledOn(it, date) }
            if (scheduledForDate.isEmpty()) return null // Unscheduled day: neutral

            val completedIds = completedByDate[date.format(DATE_FORMATTER)] ?: emptySet()
            return scheduledForDate.all { it.id in completedIds }
        }

        val isTodayQualified = isDayQualified(referenceDate) == true

        // Current streak scan backwards
        var currentStreak = 0
        var graceUsed = false
        var checkDate = referenceDate

        // If today is incomplete, start evaluation from yesterday so active streak is not prematurely lost
        if (isDayQualified(checkDate) != true && !protectedDates.contains(checkDate)) {
            checkDate = checkDate.minusDays(1)
        }

        while (true) {
            val status = isDayQualified(checkDate)
            if (status == null) {
                // Neutral day, continue back without incrementing or breaking
                checkDate = checkDate.minusDays(1)
                continue
            }

            if (status) {
                currentStreak++
                checkDate = checkDate.minusDays(1)
            } else if (protectedDates.contains(checkDate)) {
                // Saved by shared grace
                currentStreak++
                graceUsed = true
                checkDate = checkDate.minusDays(1)
            } else {
                break
            }
        }

        // Longest streak scan across 365 days
        var longestStreak = currentStreak
        var runningStreak = 0
        var cursor = referenceDate.minusDays(365)
        while (!cursor.isAfter(referenceDate)) {
            val status = isDayQualified(cursor)
            if (status == true || (status == false && protectedDates.contains(cursor))) {
                runningStreak++
                if (runningStreak > longestStreak) longestStreak = runningStreak
            } else if (status == false) {
                runningStreak = 0
            }
            cursor = cursor.plusDays(1)
        }

        return StreakCategoryInfo(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            isQualifiedToday = isTodayQualified,
            isGraceUsedInCurrentStreak = graceUsed
        )
    }

    /**
     * 2. Detox Streak:
     * Evaluates consecutive days where verified completed detox minutes >= [dailyGoalMinutes].
     * Target requires a minimum of 1 hour (60 minutes) completed detox per day.
     */
    fun calculateDetoxStreak(
        completedSessions: List<DetoxSessionEntity>,
        dailyGoalMinutes: Int = 60,
        protectedDates: Set<LocalDate> = emptySet(),
        referenceDate: LocalDate = LocalDate.now()
    ): StreakCategoryInfo {
        val target = dailyGoalMinutes.coerceAtLeast(60)
        val minutesByDate = completedSessions
            .filter { it.status == "COMPLETED" }
            .groupBy { it.sessionDate }
            .mapValues { entry -> entry.value.sumOf { it.actualMinutes } }

        fun isDayQualified(date: LocalDate): Boolean {
            val minutes = minutesByDate[date.format(DATE_FORMATTER)] ?: 0
            return minutes >= target
        }

        val isTodayQualified = isDayQualified(referenceDate)

        var currentStreak = 0
        var graceUsed = false
        var checkDate = referenceDate

        if (!isDayQualified(checkDate) && !protectedDates.contains(checkDate)) {
            checkDate = checkDate.minusDays(1)
        }

        while (true) {
            if (isDayQualified(checkDate)) {
                currentStreak++
                checkDate = checkDate.minusDays(1)
            } else if (protectedDates.contains(checkDate)) {
                currentStreak++
                graceUsed = true
                checkDate = checkDate.minusDays(1)
            } else {
                break
            }
        }

        var longestStreak = currentStreak
        var runningStreak = 0
        var cursor = referenceDate.minusDays(365)
        while (!cursor.isAfter(referenceDate)) {
            if (isDayQualified(cursor) || protectedDates.contains(cursor)) {
                runningStreak++
                if (runningStreak > longestStreak) longestStreak = runningStreak
            } else {
                runningStreak = 0
            }
            cursor = cursor.plusDays(1)
        }

        return StreakCategoryInfo(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            isQualifiedToday = isTodayQualified,
            isGraceUsedInCurrentStreak = graceUsed
        )
    }

    /**
     * 3. Movement Streak:
     * Evaluates consecutive days where daily verified step goal was reached (steps >= goalSteps && steps > 0).
     */
    fun calculateMovementStreak(
        dailyActivities: List<DailyActivityEntity>,
        protectedDates: Set<LocalDate> = emptySet(),
        referenceDate: LocalDate = LocalDate.now()
    ): StreakCategoryInfo {
        val activityByDate = dailyActivities.associateBy { it.date }

        fun isDayQualified(date: LocalDate): Boolean {
            val act = activityByDate[date.format(DATE_FORMATTER)] ?: return false
            return act.steps >= act.goalSteps && act.steps > 0
        }

        val isTodayQualified = isDayQualified(referenceDate)

        var currentStreak = 0
        var graceUsed = false
        var checkDate = referenceDate

        if (!isDayQualified(checkDate) && !protectedDates.contains(checkDate)) {
            checkDate = checkDate.minusDays(1)
        }

        while (true) {
            if (isDayQualified(checkDate)) {
                currentStreak++
                checkDate = checkDate.minusDays(1)
            } else if (protectedDates.contains(checkDate)) {
                currentStreak++
                graceUsed = true
                checkDate = checkDate.minusDays(1)
            } else {
                break
            }
        }

        var longestStreak = currentStreak
        var runningStreak = 0
        var cursor = referenceDate.minusDays(365)
        while (!cursor.isAfter(referenceDate)) {
            if (isDayQualified(cursor) || protectedDates.contains(cursor)) {
                runningStreak++
                if (runningStreak > longestStreak) longestStreak = runningStreak
            } else {
                runningStreak = 0
            }
            cursor = cursor.plusDays(1)
        }

        return StreakCategoryInfo(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            isQualifiedToday = isTodayQualified,
            isGraceUsedInCurrentStreak = graceUsed
        )
    }

    /**
     * 4. Exercise Streak:
     * Evaluates active workout momentum without penalizing healthy rest days.
     * A qualifying workout requires duration >= 10 minutes (600s).
     * Normal recovery intervals allow up to 2 rest days between workout days.
     * If a rest gap exceeds 2 days and is not protected by grace, the streak breaks.
     */
    fun calculateExerciseStreak(
        completedExercises: List<ExerciseSessionEntity>,
        protectedDates: Set<LocalDate> = emptySet(),
        referenceDate: LocalDate = LocalDate.now()
    ): StreakCategoryInfo {
        val exerciseDates = completedExercises
            .filter { it.status == "COMPLETED" && it.durationSeconds >= 600 }
            .mapNotNull {
                try {
                    LocalDate.parse(it.sessionDate, DATE_FORMATTER)
                } catch (_: Exception) {
                    null
                }
            }
            .toSet()

        val isTodayQualified = referenceDate in exerciseDates

        // Scan backwards measuring consecutive active workout milestones
        var currentStreak = if (isTodayQualified) 1 else 0
        var graceUsed = false
        var checkDate = referenceDate.minusDays(1)

        var restDayGap = if (isTodayQualified) 0 else 1
        while (true) {
            val exercised = checkDate in exerciseDates
            if (exercised) {
                currentStreak++
                restDayGap = 0
                checkDate = checkDate.minusDays(1)
            } else if (protectedDates.contains(checkDate)) {
                currentStreak++
                graceUsed = true
                restDayGap = 0
                checkDate = checkDate.minusDays(1)
            } else {
                restDayGap++
                if (restDayGap > 2) {
                    // Maximum allowed rest gap exceeded (more than 2 consecutive rest days)
                    break
                }
                checkDate = checkDate.minusDays(1)
            }

            // Guard against infinite loop
            if (checkDate.isBefore(referenceDate.minusDays(365))) break
        }

        // Longest streak
        var longestStreak = currentStreak
        var runningStreak = 0
        var runningGap = 0
        var cursor = referenceDate.minusDays(365)
        while (!cursor.isAfter(referenceDate)) {
            val exercised = cursor in exerciseDates || protectedDates.contains(cursor)
            if (exercised) {
                runningStreak++
                runningGap = 0
                if (runningStreak > longestStreak) longestStreak = runningStreak
            } else {
                runningGap++
                if (runningGap > 2) {
                    runningStreak = 0
                }
            }
            cursor = cursor.plusDays(1)
        }

        return StreakCategoryInfo(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            isQualifiedToday = isTodayQualified,
            isGraceUsedInCurrentStreak = graceUsed
        )
    }

    /**
     * Evaluates all four streaks concurrently.
     */
    fun calculateAllStreaks(
        habits: List<HabitEntity>,
        completions: List<HabitCompletionEntity>,
        completedSessions: List<DetoxSessionEntity>,
        dailyActivities: List<DailyActivityEntity>,
        completedExercises: List<ExerciseSessionEntity>,
        dailyOfflineGoalMinutes: Int = 60,
        protectedDates: Set<LocalDate> = emptySet(),
        referenceDate: LocalDate = LocalDate.now()
    ): AllStreaksInfo {
        return AllStreaksInfo(
            habitStreak = calculateHabitStreak(habits, completions, protectedDates, referenceDate),
            detoxStreak = calculateDetoxStreak(completedSessions, dailyOfflineGoalMinutes.coerceAtLeast(60), protectedDates, referenceDate),
            movementStreak = calculateMovementStreak(dailyActivities, protectedDates, referenceDate),
            exerciseStreak = calculateExerciseStreak(completedExercises, protectedDates, referenceDate)
        )
    }
}
