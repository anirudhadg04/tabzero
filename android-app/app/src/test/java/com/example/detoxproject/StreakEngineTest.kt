package com.example.detoxproject

import com.example.detoxproject.data.local.entity.DailyActivityEntity
import com.example.detoxproject.data.local.entity.DetoxSessionEntity
import com.example.detoxproject.data.local.entity.ExerciseSessionEntity
import com.example.detoxproject.data.local.entity.HabitCompletionEntity
import com.example.detoxproject.data.local.entity.HabitEntity
import com.example.detoxproject.domain.engine.StreakEngine
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class StreakEngineTest {

    @Test
    fun testHabitStreak_ConsecutiveScheduledDays() {
        val today = LocalDate.of(2026, 11, 3) // Tuesday
        val habit = HabitEntity(
            id = 1L,
            userId = 1L,
            title = "Morning Meditation",
            frequency = "DAILY",
            targetDays = "1,2,3,4,5,6,7",
            createdDate = "2026-10-01"
        )
        val habits = listOf(habit)

        // Completed Sun (Nov 1), Mon (Nov 2), Tue (Nov 3)
        val completions = listOf(
            HabitCompletionEntity(habitId = 1L, date = "2026-11-01"),
            HabitCompletionEntity(habitId = 1L, date = "2026-11-02"),
            HabitCompletionEntity(habitId = 1L, date = "2026-11-03")
        )

        val streak = StreakEngine.calculateHabitStreak(
            habits = habits,
            completions = completions,
            referenceDate = today
        )

        assertEquals(3, streak.currentStreak)
        assertEquals(3, streak.longestStreak)
        assertTrue(streak.isQualifiedToday)
    }

    @Test
    fun testHabitStreak_UnscheduledDayDoesNotBreakStreak() {
        // Monday Nov 2, 2026. Habit scheduled only on WEEKENDS (Sat=6, Sun=7)
        val refDate = LocalDate.of(2026, 11, 2) // Monday
        val weekendHabit = HabitEntity(
            id = 1L,
            userId = 1L,
            title = "Weekend Trail Run",
            frequency = "WEEKENDS",
            targetDays = "6,7",
            createdDate = "2026-10-01"
        )
        // Completed Sat Oct 31 & Sun Nov 1
        val completions = listOf(
            HabitCompletionEntity(habitId = 1L, date = "2026-10-31"),
            HabitCompletionEntity(habitId = 1L, date = "2026-11-01")
        )

        val streak = StreakEngine.calculateHabitStreak(
            habits = listOf(weekendHabit),
            completions = completions,
            referenceDate = refDate // Monday: unscheduled day
        )

        // Streak is 2 because Sat and Sun were completed, and Monday is unscheduled (neutral)
        assertEquals(2, streak.currentStreak)
    }

    @Test
    fun testHabitStreak_ProtectedDateRescuesMissedDay() {
        val today = LocalDate.of(2026, 11, 3)
        val dailyHabit = HabitEntity(
            id = 1L,
            userId = 1L,
            title = "Read 30 mins",
            frequency = "DAILY",
            targetDays = "1,2,3,4,5,6,7",
            createdDate = "2026-10-01"
        )

        // Completed Nov 1, missed Nov 2, completed Nov 3
        val completions = listOf(
            HabitCompletionEntity(habitId = 1L, date = "2026-11-01"),
            HabitCompletionEntity(habitId = 1L, date = "2026-11-03")
        )

        // Without grace, current streak would only be 1 (today)
        val streakWithoutGrace = StreakEngine.calculateHabitStreak(
            habits = listOf(dailyHabit),
            completions = completions,
            referenceDate = today
        )
        assertEquals(1, streakWithoutGrace.currentStreak)

        // With grace protecting Nov 2
        val streakWithGrace = StreakEngine.calculateHabitStreak(
            habits = listOf(dailyHabit),
            completions = completions,
            protectedDates = setOf(LocalDate.of(2026, 11, 2)),
            referenceDate = today
        )
        // 2 active completed days + 1 protected day = 2 completed streak days preserved
        assertTrue(streakWithGrace.currentStreak >= 2)
        assertTrue(streakWithGrace.isGraceUsedInCurrentStreak)
    }

    @Test
    fun testDetoxStreak_TargetAchievedConsecutiveDays() {
        val today = LocalDate.of(2026, 11, 3)
        val sessions = listOf(
            DetoxSessionEntity(id = 1L, title = "Detox 1", startTime = 1000L, targetMinutes = 60, actualMinutes = 60, status = "COMPLETED", sessionDate = "2026-11-01"),
            DetoxSessionEntity(id = 2L, title = "Detox 2", startTime = 2000L, targetMinutes = 75, actualMinutes = 75, status = "COMPLETED", sessionDate = "2026-11-02"),
            DetoxSessionEntity(id = 3L, title = "Detox 3", startTime = 3000L, targetMinutes = 60, actualMinutes = 60, status = "COMPLETED", sessionDate = "2026-11-03")
        )

        val streak = StreakEngine.calculateDetoxStreak(
            completedSessions = sessions,
            dailyGoalMinutes = 60,
            referenceDate = today
        )

        assertEquals(3, streak.currentStreak)
        assertEquals(3, streak.longestStreak)
        assertTrue(streak.isQualifiedToday)
    }

    @Test
    fun testMovementStreak_StepGoalReachedConsecutiveDays() {
        val today = LocalDate.of(2026, 11, 3)
        val activities = listOf(
            DailyActivityEntity(date = "2026-11-01", steps = 10000, goalSteps = 8000),
            DailyActivityEntity(date = "2026-11-02", steps = 8500, goalSteps = 8000),
            DailyActivityEntity(date = "2026-11-03", steps = 9200, goalSteps = 8000)
        )

        val streak = StreakEngine.calculateMovementStreak(
            dailyActivities = activities,
            referenceDate = today
        )

        assertEquals(3, streak.currentStreak)
        assertTrue(streak.isQualifiedToday)
    }

    @Test
    fun testExerciseStreak_ToleratesUpToTwoRestDays() {
        val today = LocalDate.of(2026, 11, 4) // Wednesday
        // Workout on Sunday (Nov 1) and Wednesday (Nov 4) with Mon (Nov 2) and Tue (Nov 3) as 2 rest days
        val sessions = listOf(
            ExerciseSessionEntity(id = 1L, title = "Sunday Long Run", activityType = "RUNNING", startTime = 1000L, durationSeconds = 1800L, status = "COMPLETED", sessionDate = "2026-11-01"),
            ExerciseSessionEntity(id = 2L, title = "Wednesday Workout", activityType = "STRENGTH", startTime = 2000L, durationSeconds = 1200L, status = "COMPLETED", sessionDate = "2026-11-04")
        )

        val streak = StreakEngine.calculateExerciseStreak(
            completedExercises = sessions,
            referenceDate = today
        )

        // Streak maintained across 2 rest days
        assertEquals(2, streak.currentStreak)
        assertTrue(streak.isQualifiedToday)
    }

    @Test
    fun testExerciseStreak_ThreeRestDaysBreaksStreak() {
        val today = LocalDate.of(2026, 11, 5) // Thursday
        // Workout on Sunday (Nov 1) and Thursday (Nov 5) -> Mon, Tue, Wed = 3 rest days (exceeds 2-day recovery limit)
        val sessions = listOf(
            ExerciseSessionEntity(id = 1L, title = "Sunday Long Run", activityType = "RUNNING", startTime = 1000L, durationSeconds = 1800L, status = "COMPLETED", sessionDate = "2026-11-01"),
            ExerciseSessionEntity(id = 2L, title = "Thursday Workout", activityType = "STRENGTH", startTime = 2000L, durationSeconds = 1200L, status = "COMPLETED", sessionDate = "2026-11-05")
        )

        val streak = StreakEngine.calculateExerciseStreak(
            completedExercises = sessions,
            referenceDate = today
        )

        // Sunday was broken by 3 rest days, so current streak restarted at 1 on Thursday
        assertEquals(1, streak.currentStreak)
    }
}
