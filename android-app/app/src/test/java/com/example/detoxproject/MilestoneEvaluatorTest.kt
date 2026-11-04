package com.example.detoxproject

import com.example.detoxproject.data.local.entity.AchievementEntity
import com.example.detoxproject.data.local.entity.DetoxSessionEntity
import com.example.detoxproject.data.local.entity.PlannedActivityEntity
import com.example.detoxproject.data.local.entity.UserGoalsEntity
import com.example.detoxproject.domain.engine.MilestoneEvaluator
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class MilestoneEvaluatorTest {

    @Test
    fun testStreakCalculation_ConsecutiveDays() {
        val today = LocalDate.of(2026, 10, 31)
        val sessions = listOf(
            DetoxSessionEntity(
                id = 1L, title = "Session 1", startTime = 1000L, targetMinutes = 60,
                actualMinutes = 120, status = "COMPLETED", sessionDate = "2026-10-31"
            ),
            DetoxSessionEntity(
                id = 2L, title = "Session 2", startTime = 2000L, targetMinutes = 60,
                actualMinutes = 130, status = "COMPLETED", sessionDate = "2026-10-30"
            ),
            DetoxSessionEntity(
                id = 3L, title = "Session 3", startTime = 3000L, targetMinutes = 60,
                actualMinutes = 150, status = "COMPLETED", sessionDate = "2026-10-29"
            )
        )

        val streak = MilestoneEvaluator.calculateStreak(sessions, 120, today)
        assertEquals(3, streak)
    }

    @Test
    fun testStreakCalculation_BrokenStreak() {
        val today = LocalDate.of(2026, 10, 31)
        val sessions = listOf(
            DetoxSessionEntity(
                id = 1L, title = "Session 1", startTime = 1000L, targetMinutes = 60,
                actualMinutes = 120, status = "COMPLETED", sessionDate = "2026-10-31"
            ),
            // Missing 2026-10-30
            DetoxSessionEntity(
                id = 3L, title = "Session 3", startTime = 3000L, targetMinutes = 60,
                actualMinutes = 150, status = "COMPLETED", sessionDate = "2026-10-29"
            )
        )

        val streak = MilestoneEvaluator.calculateStreak(sessions, 120, today)
        assertEquals(1, streak)
    }

    @Test
    fun testAchievementEvaluation() {
        val today = LocalDate.of(2026, 10, 31)
        val initialAchievements = listOf(
            AchievementEntity(id = "first_detox", title = "First Detox", description = "First session", iconName = "spa"),
            AchievementEntity(id = "hour_free", title = "One Hour Free", description = "60 mins offline", iconName = "timer"),
            AchievementEntity(id = "deep_focus", title = "Deep Focus", description = "120 mins offline", iconName = "psychology"),
            AchievementEntity(id = "consistency_3", title = "Consistency", description = "3-day streak", iconName = "fire"),
            AchievementEntity(id = "reconnect_nature", title = "Mindful Reconnect", description = "Habit completed", iconName = "park")
        )

        val sessions = listOf(
            DetoxSessionEntity(
                id = 1L, title = "2 Hour Session", startTime = 1000L, targetMinutes = 120,
                actualMinutes = 125, status = "COMPLETED", sessionDate = "2026-10-31"
            )
        )

        val activities = listOf(
            PlannedActivityEntity(
                id = 1L, title = "Nature Walk", description = "", category = "outdoor",
                durationMinutes = 30, scheduledDate = "2026-10-31", status = "COMPLETED"
            )
        )

        val goals = UserGoalsEntity(dailyOfflineMinutes = 120)
        val evaluated = MilestoneEvaluator.evaluateAchievements(sessions, activities, goals, initialAchievements, today)

        val firstDetox = evaluated.first { it.id == "first_detox" }
        val hourFree = evaluated.first { it.id == "hour_free" }
        val deepFocus = evaluated.first { it.id == "deep_focus" }
        val nature = evaluated.first { it.id == "reconnect_nature" }

        assertTrue(firstDetox.isUnlocked)
        assertTrue(hourFree.isUnlocked)
        assertTrue(deepFocus.isUnlocked)
        assertTrue(nature.isUnlocked)
    }
}
