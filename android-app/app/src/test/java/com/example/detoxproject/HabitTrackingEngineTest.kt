package com.example.detoxproject

import com.example.detoxproject.data.local.entity.HabitCompletionEntity
import com.example.detoxproject.data.local.entity.HabitEntity
import com.example.detoxproject.domain.engine.HabitStreakCalculator
import com.example.detoxproject.domain.engine.UndoRedoSerializer
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class HabitTrackingEngineTest {

    @Test
    fun testDailyHabitStreak_ConsecutiveCompletions() {
        // Today is Sunday Nov 1, 2026
        val today = LocalDate.of(2026, 11, 1)
        val habit = HabitEntity(
            id = 1L,
            title = "Walk 8,000 steps",
            category = "fitness",
            frequency = "DAILY",
            targetDays = "1,2,3,4,5,6,7",
            createdDate = "2026-10-25"
        )

        // Completed 4 days in a row up to today
        val completions = listOf(
            HabitCompletionEntity(habitId = 1L, date = "2026-11-01"),
            HabitCompletionEntity(habitId = 1L, date = "2026-10-31"),
            HabitCompletionEntity(habitId = 1L, date = "2026-10-30"),
            HabitCompletionEntity(habitId = 1L, date = "2026-10-29")
        )

        val stats = HabitStreakCalculator.calculateStats(habit, completions, today)
        assertEquals(4, stats.currentStreak)
        assertEquals(4, stats.longestStreak)
        assertTrue(stats.isCompletedToday)
        assertTrue(stats.isScheduledToday)
    }

    @Test
    fun testDailyHabitStreak_TodayNotCompletedYet_PreservesYesterdayStreak() {
        // Today is Nov 1, 2026, user hasn't completed today yet
        val today = LocalDate.of(2026, 11, 1)
        val habit = HabitEntity(
            id = 1L,
            title = "Read 20 pages",
            category = "reading",
            frequency = "DAILY",
            targetDays = "1,2,3,4,5,6,7",
            createdDate = "2026-10-25"
        )

        // Completed Oct 29, 30, 31 (3 days)
        val completions = listOf(
            HabitCompletionEntity(habitId = 1L, date = "2026-10-31"),
            HabitCompletionEntity(habitId = 1L, date = "2026-10-30"),
            HabitCompletionEntity(habitId = 1L, date = "2026-10-29")
        )

        val stats = HabitStreakCalculator.calculateStats(habit, completions, today)
        // Streak is still 3 because today is currently in progress
        assertEquals(3, stats.currentStreak)
        assertFalse(stats.isCompletedToday)
    }

    @Test
    fun testDailyHabitStreak_BrokenStreak() {
        val today = LocalDate.of(2026, 11, 1)
        val habit = HabitEntity(
            id = 1L,
            title = "Meditate",
            category = "mindfulness",
            frequency = "DAILY",
            targetDays = "1,2,3,4,5,6,7",
            createdDate = "2026-10-20"
        )

        // Completed Nov 1, but missed Oct 31
        val completions = listOf(
            HabitCompletionEntity(habitId = 1L, date = "2026-11-01"),
            // missed Oct 31
            HabitCompletionEntity(habitId = 1L, date = "2026-10-30"),
            HabitCompletionEntity(habitId = 1L, date = "2026-10-29")
        )

        val stats = HabitStreakCalculator.calculateStats(habit, completions, today)
        assertEquals(1, stats.currentStreak)
        assertEquals(2, stats.longestStreak) // Previous 2-day streak on Oct 29-30
    }

    @Test
    fun testWeekdayHabitStreak_SkipsWeekends() {
        // 2026-10-26 was Monday, 2026-10-30 was Friday, 2026-10-31 was Saturday, 2026-11-01 was Sunday
        val today = LocalDate.of(2026, 11, 1) // Sunday
        val habit = HabitEntity(
            id = 2L,
            title = "Study session",
            category = "focus",
            frequency = "WEEKDAYS",
            targetDays = "1,2,3,4,5",
            createdDate = "2026-10-26"
        )

        // Completed Mon through Fri
        val completions = listOf(
            HabitCompletionEntity(habitId = 2L, date = "2026-10-26"),
            HabitCompletionEntity(habitId = 2L, date = "2026-10-27"),
            HabitCompletionEntity(habitId = 2L, date = "2026-10-28"),
            HabitCompletionEntity(habitId = 2L, date = "2026-10-29"),
            HabitCompletionEntity(habitId = 2L, date = "2026-10-30")
        )

        val stats = HabitStreakCalculator.calculateStats(habit, completions, today)
        // Weekends do not break the weekday streak!
        assertEquals(5, stats.currentStreak)
        assertFalse(stats.isScheduledToday) // Sunday is not scheduled for WEEKDAYS
    }

    @Test
    fun testCompletionRate_NeverCountsFutureDatesOrDatesBeforeCreation() {
        // Habit created on Oct 28 (Wednesday). Today is Oct 31 (Saturday) = 4 days total
        val today = LocalDate.of(2026, 10, 31)
        val habit = HabitEntity(
            id = 3L,
            title = "Sleep on time",
            category = "sleep",
            frequency = "DAILY",
            targetDays = "1,2,3,4,5,6,7",
            createdDate = "2026-10-28"
        )

        // Completed 3 of the 4 days
        val completions = listOf(
            HabitCompletionEntity(habitId = 3L, date = "2026-10-28"),
            HabitCompletionEntity(habitId = 3L, date = "2026-10-29"),
            HabitCompletionEntity(habitId = 3L, date = "2026-10-31")
        )

        val stats = HabitStreakCalculator.calculateStats(habit, completions, today)
        // 3 completed out of 4 scheduled days = 75%
        assertEquals(75, stats.completionRatePercent)
    }

    @Test
    fun testUndoRedoSerialization_HabitAndCompletions() {
        val habit = HabitEntity(
            id = 42L,
            title = "Evening Wind-Down",
            description = "No screens 45 min before sleep",
            category = "sleep",
            icon = "bedtime",
            frequency = "WEEKDAYS",
            targetDays = "1,2,3,4,5",
            isArchived = false,
            createdDate = "2026-11-01"
        )

        val jsonHabit = UndoRedoSerializer.serializeHabit(habit)
        val deserializedHabit = UndoRedoSerializer.deserializeHabit(jsonHabit)
        assertEquals(habit.id, deserializedHabit.id)
        assertEquals(habit.title, deserializedHabit.title)
        assertEquals(habit.category, deserializedHabit.category)
        assertEquals(habit.frequency, deserializedHabit.frequency)
        assertEquals(habit.targetDays, deserializedHabit.targetDays)

        val completion = HabitCompletionEntity(
            id = 101L,
            habitId = 42L,
            date = "2026-11-01",
            completedAt = 123456789L
        )
        val jsonComp = UndoRedoSerializer.serializeHabitCompletion(completion)
        val deserializedComp = UndoRedoSerializer.deserializeHabitCompletion(jsonComp)
        assertEquals(completion.habitId, deserializedComp.habitId)
        assertEquals(completion.date, deserializedComp.date)
    }
}
