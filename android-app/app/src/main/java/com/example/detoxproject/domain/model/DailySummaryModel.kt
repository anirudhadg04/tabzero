package com.example.detoxproject.domain.model

import com.example.detoxproject.data.local.entity.DailyActivityEntity
import com.example.detoxproject.data.local.entity.DetoxSessionEntity
import com.example.detoxproject.data.local.entity.ExerciseSessionEntity
import com.example.detoxproject.data.local.entity.PlannedActivityEntity
import com.example.detoxproject.data.local.entity.XPTransactionEntity
import com.example.detoxproject.domain.engine.HabitWithStatus
import java.time.LocalDate
import java.time.YearMonth

/**
 * Unified day summary combining all wellness pillars:
 * Habits, Tasks, Daily Physical Activity, Focus Sessions, Exercises, and Zen XP transactions.
 */
data class UnifiedDailySummary(
    val date: LocalDate,
    val dateIso: String,
    val displayDate: String,
    val habits: List<HabitWithStatus> = emptyList(),
    val habitsCompletedCount: Int = 0,
    val habitsTotalCount: Int = 0,
    val tasks: List<PlannedActivityEntity> = emptyList(),
    val tasksCompletedCount: Int = 0,
    val tasksTotalCount: Int = 0,
    val dailyActivity: DailyActivityEntity? = null,
    val detoxSessions: List<DetoxSessionEntity> = emptyList(),
    val allDetoxSessions: List<DetoxSessionEntity> = emptyList(),
    val totalDetoxMinutes: Int = 0,
    val exerciseSessions: List<ExerciseSessionEntity> = emptyList(),
    val totalExerciseMinutes: Int = 0,
    val xpTransactions: List<XPTransactionEntity> = emptyList(),
    val netXpForDate: Int = 0,
    val stepGoalTarget: Int = 8000,
    val isToday: Boolean = false,
    val isPast: Boolean = false,
    val isFuture: Boolean = false
)

data class CalendarDayInfo(
    val date: LocalDate,
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val isFuture: Boolean,
    val hasHabitActivity: Boolean = false,
    val hasDetoxActivity: Boolean = false,
    val hasFitnessActivity: Boolean = false,
    val completionScore: Float = 0f // 0f to 1f
)

data class CalendarMonthData(
    val yearMonth: YearMonth,
    val days: List<CalendarDayInfo>,
    val totalActiveDays: Int = 0,
    val monthlyConsistencyPercent: Int = 0,
    val totalMonthlyFocusMinutes: Int = 0,
    val totalMonthlySteps: Int = 0
)

data class WeeklyTrendDay(
    val date: LocalDate,
    val dayLabel: String,
    val detoxMinutes: Int = 0,
    val steps: Int = 0,
    val habitsCompleted: Int = 0,
    val habitsTotal: Int = 0,
    val isToday: Boolean = false
)

data class WeeklyAnalyticsData(
    val days: List<WeeklyTrendDay> = emptyList(),
    val averageFocusMinutesPerDay: Int = 0,
    val averageStepsPerDay: Int = 0,
    val weeklyHabitCompletionRate: Int = 0
)

enum class TrendTimeRange(val days: Int, val label: String) {
    SEVEN_DAYS(7, "7 Days"),
    THIRTY_DAYS(30, "30 Days"),
    NINETY_DAYS(90, "90 Days")
}

data class MovementTrendAnalytics(
    val dailySteps: List<Pair<LocalDate, Int>> = emptyList(),
    val averageDailySteps: Int = 0,
    val totalSteps: Int = 0,
    val daysGoalReached: Int = 0,
    val totalDays: Int = 7,
    val goalConsistencyPercent: Int = 0,
    val prevTotalSteps: Int = 0,
    val stepsChangePercent: Float? = null
)

data class DetoxTrendAnalytics(
    val dailyMinutes: List<Pair<LocalDate, Int>> = emptyList(),
    val totalDetoxMinutes: Int = 0,
    val averageDailyMinutes: Int = 0,
    val completedSessionCount: Int = 0,
    val longestSessionMinutes: Int = 0,
    val goalConsistencyPercent: Int = 0,
    val prevTotalDetoxMinutes: Int = 0,
    val detoxChangePercent: Float? = null
)

data class HabitTrendAnalytics(
    val totalScheduledOpportunities: Int = 0,
    val completedOpportunities: Int = 0,
    val completionPercentage: Int = 0,
    val dailyCompletionTrend: List<Pair<LocalDate, Float>> = emptyList(),
    val topConsistentHabits: List<Pair<String, Int>> = emptyList(),
    val prevCompletionPercentage: Int = 0,
    val completionChangePercent: Float? = null
)

data class ExerciseTrendAnalytics(
    val sessionCount: Int = 0,
    val totalDurationMinutes: Int = 0,
    val activeCaloriesBurned: Int = 0,
    val categoryBreakdown: Map<String, Int> = emptyMap(),
    val prevSessionCount: Int = 0,
    val sessionChangePercent: Float? = null
)

data class XPTrendAnalytics(
    val totalEarned: Int = 0,
    val totalReversed: Int = 0,
    val totalRestored: Int = 0,
    val netChange: Int = 0,
    val sourceBreakdown: Map<String, Int> = emptyMap(),
    val dailyNetXP: List<Pair<LocalDate, Int>> = emptyList(),
    val prevNetChange: Int = 0,
    val netXPChangePercent: Float? = null
)

data class TrendsAnalyticsData(
    val timeRange: TrendTimeRange = TrendTimeRange.SEVEN_DAYS,
    val startDate: LocalDate = LocalDate.now().minusDays(6),
    val endDate: LocalDate = LocalDate.now(),
    val movement: MovementTrendAnalytics = MovementTrendAnalytics(),
    val detox: DetoxTrendAnalytics = DetoxTrendAnalytics(),
    val habits: HabitTrendAnalytics = HabitTrendAnalytics(),
    val exercise: ExerciseTrendAnalytics = ExerciseTrendAnalytics(),
    val xp: XPTrendAnalytics = XPTrendAnalytics()
)
