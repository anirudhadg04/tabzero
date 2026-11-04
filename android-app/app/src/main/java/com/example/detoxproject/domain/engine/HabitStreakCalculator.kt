package com.example.detoxproject.domain.engine

import com.example.detoxproject.data.local.entity.HabitCompletionEntity
import com.example.detoxproject.data.local.entity.HabitEntity
import java.time.LocalDate

data class HabitStats(
    val habitId: Long,
    val currentStreak: Int,
    val longestStreak: Int,
    val completionRatePercent: Int,
    val totalCompletions: Int,
    val isCompletedToday: Boolean,
    val isScheduledToday: Boolean
)

data class HabitWithStatus(
    val habit: HabitEntity,
    val stats: HabitStats,
    val isCompletedForDate: Boolean = false
)

object HabitStreakCalculator {

    fun parseScheduledDays(habit: HabitEntity): Set<Int> {
        return when (habit.frequency.uppercase()) {
            "DAILY" -> (1..7).toSet()
            "WEEKDAYS" -> (1..5).toSet()
            "WEEKENDS" -> setOf(6, 7)
            "CUSTOM" -> {
                habit.targetDays.split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .filter { it in 1..7 }
                    .toSet().ifEmpty { (1..7).toSet() }
            }
            else -> (1..7).toSet()
        }
    }

    fun isScheduledOn(habit: HabitEntity, date: LocalDate): Boolean {
        val scheduledDays = parseScheduledDays(habit)
        return date.dayOfWeek.value in scheduledDays
    }

    fun calculateStats(
        habit: HabitEntity,
        completions: List<HabitCompletionEntity>,
        today: LocalDate = LocalDate.now()
    ): HabitStats {
        val scheduledDays = parseScheduledDays(habit)
        val completedDates: Set<LocalDate> = completions.mapNotNull { comp ->
            try {
                LocalDate.parse(comp.date, DateManager.ISO_FORMATTER)
            } catch (_: Exception) {
                null
            }
        }.toSet()

        val createdDate = try {
            LocalDate.parse(habit.createdDate, DateManager.ISO_FORMATTER)
        } catch (_: Exception) {
            today
        }.let { if (it.isAfter(today)) today else it }

        val isScheduledToday = today.dayOfWeek.value in scheduledDays
        val isCompletedToday = today in completedDates

        // --- Current Streak ---
        var currentStreak = 0
        var checkDate = if (isScheduledToday && isCompletedToday) {
            currentStreak = 1
            today.minusDays(1)
        } else {
            today.minusDays(1)
        }

        while (!checkDate.isBefore(createdDate)) {
            val scheduled = checkDate.dayOfWeek.value in scheduledDays
            if (scheduled) {
                if (checkDate in completedDates) {
                    currentStreak++
                } else {
                    break // Streak ended
                }
            }
            checkDate = checkDate.minusDays(1)
        }

        // --- Longest Streak & Completion Rate ---
        var longestStreak = 0
        var runningStreak = 0
        var totalScheduledDays = 0
        var totalCompletedScheduled = 0

        var cursor = createdDate
        while (!cursor.isAfter(today)) {
            val scheduled = cursor.dayOfWeek.value in scheduledDays
            if (scheduled) {
                totalScheduledDays++
                if (cursor in completedDates) {
                    totalCompletedScheduled++
                    runningStreak++
                    if (runningStreak > longestStreak) {
                        longestStreak = runningStreak
                    }
                } else {
                    if (cursor != today) {
                        runningStreak = 0
                    }
                }
            }
            cursor = cursor.plusDays(1)
        }

        longestStreak = maxOf(longestStreak, currentStreak)

        val completionRate = if (totalScheduledDays > 0) {
            ((totalCompletedScheduled.toFloat() / totalScheduledDays) * 100).toInt().coerceIn(0, 100)
        } else {
            100
        }

        return HabitStats(
            habitId = habit.id,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            completionRatePercent = completionRate,
            totalCompletions = completions.size,
            isCompletedToday = isCompletedToday,
            isScheduledToday = isScheduledToday
        )
    }
}
