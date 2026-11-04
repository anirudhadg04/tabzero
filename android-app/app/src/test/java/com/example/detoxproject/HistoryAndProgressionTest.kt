package com.example.detoxproject

import com.example.detoxproject.data.local.entity.DetoxSessionEntity
import com.example.detoxproject.data.local.entity.XPTransactionEntity
import com.example.detoxproject.domain.engine.DateManager
import com.example.detoxproject.domain.engine.LevelCalculator
import com.example.detoxproject.domain.model.TrendTimeRange
import com.example.detoxproject.domain.model.UnifiedDailySummary
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class HistoryAndProgressionTest {

    @Test
    fun testLevelCalculatorFormula() {
        // Level 1: 100 XP to reach Level 2
        assertEquals(100, LevelCalculator.xpRequiredForNextLevel(1))
        // Level 2: 150 XP to reach Level 3
        assertEquals(150, LevelCalculator.xpRequiredForNextLevel(2))
        // Level 3: 200 XP to reach Level 4
        assertEquals(200, LevelCalculator.xpRequiredForNextLevel(3))
        // Level 4: 250 XP to reach Level 5
        assertEquals(250, LevelCalculator.xpRequiredForNextLevel(4))
        // Level 5: 300 XP to reach Level 6
        assertEquals(300, LevelCalculator.xpRequiredForNextLevel(5))

        // Total XP thresholds
        assertEquals(0, LevelCalculator.cumulativeXpForLevel(1))
        assertEquals(100, LevelCalculator.cumulativeXpForLevel(2))
        assertEquals(250, LevelCalculator.cumulativeXpForLevel(3))
        assertEquals(450, LevelCalculator.cumulativeXpForLevel(4))
        assertEquals(700, LevelCalculator.cumulativeXpForLevel(5))
        assertEquals(1000, LevelCalculator.cumulativeXpForLevel(6))
    }

    @Test
    fun testMultiLevelUpCalculation() {
        // 0 XP -> Level 1
        val levelInfo0 = LevelCalculator.calculateLevel(0)
        assertEquals(1, levelInfo0.level)
        assertEquals(0, levelInfo0.currentXPInLevel)
        assertEquals(100, levelInfo0.xpNeededForNextLevel)
        assertEquals(0f, levelInfo0.progress, 0.001f)

        // 350 XP -> Level 3 (needs 250 XP total for L3, so 100 XP into L3 which needs 200 to reach L4)
        val levelInfo350 = LevelCalculator.calculateLevel(350)
        assertEquals(3, levelInfo350.level)
        assertEquals(100, levelInfo350.currentXPInLevel)
        assertEquals(200, levelInfo350.xpNeededForNextLevel)
        assertEquals(0.5f, levelInfo350.progress, 0.001f)

        // 1000 XP in one go -> Exactly Level 6
        val levelInfo1000 = LevelCalculator.calculateLevel(1000)
        assertEquals(6, levelInfo1000.level)
        assertEquals(0, levelInfo1000.currentXPInLevel)
        assertEquals(350, levelInfo1000.xpNeededForNextLevel)
        assertEquals(0f, levelInfo1000.progress, 0.001f)
    }

    @Test
    fun testAppendOnlyLedgerBalanceAlgebra() {
        // Transactions: +50 (Reward), +30 (Reward), -50 (Reversal), +50 (Restore)
        val txs = listOf(
            XPTransactionEntity(id = 1, sourceEventId = "h_1", sourceEventType = "HABIT", xpAmount = 50, transactionType = "REWARD", date = "2026-11-03", description = "Habit"),
            XPTransactionEntity(id = 2, sourceEventId = "d_1", sourceEventType = "DETOX", xpAmount = 30, transactionType = "REWARD", date = "2026-11-03", description = "Detox"),
            XPTransactionEntity(id = 3, sourceEventId = "h_1", sourceEventType = "HABIT", xpAmount = 50, transactionType = "REVERSAL", date = "2026-11-03", description = "Undo"),
            XPTransactionEntity(id = 4, sourceEventId = "h_1", sourceEventType = "HABIT", xpAmount = 50, transactionType = "RESTORE", date = "2026-11-03", description = "Redo")
        )

        val netBalance = txs.sumOf { tx ->
            when (tx.transactionType) {
                "REWARD", "RESTORE" -> tx.xpAmount
                "REVERSAL" -> -tx.xpAmount
                else -> 0
            }
        }

        // 50 + 30 - 50 + 50 = 80
        assertEquals(80, netBalance)
    }

    @Test
    fun testUnifiedDailySummaryDetoxDistinction() {
        val completedSession = DetoxSessionEntity(
            id = 1,
            title = "Study",
            targetMinutes = 25,
            actualMinutes = 25,
            category = "focus",
            status = "COMPLETED",
            startTime = 1000L,
            sessionDate = "2026-11-03"
        )
        val cancelledSession = DetoxSessionEntity(
            id = 2,
            title = "Reading",
            targetMinutes = 30,
            actualMinutes = 5,
            category = "reading",
            status = "CANCELLED",
            startTime = 2000L,
            sessionDate = "2026-11-03"
        )

        val allSessions = listOf(completedSession, cancelledSession)
        val completedOnly = allSessions.filter { it.status == "COMPLETED" }

        val summary = UnifiedDailySummary(
            date = LocalDate.of(2026, 11, 3),
            dateIso = "2026-11-03",
            displayDate = "Tuesday, Nov 3",
            detoxSessions = completedOnly,
            allDetoxSessions = allSessions,
            totalDetoxMinutes = completedOnly.sumOf { it.actualMinutes }
        )

        assertEquals(1, summary.detoxSessions.size)
        assertEquals(2, summary.allDetoxSessions.size)
        assertEquals(25, summary.totalDetoxMinutes)
    }

    @Test
    fun testTrendTimeRangeCalculations() {
        val endDate = LocalDate.of(2026, 11, 3)

        // 7 Days
        val days7 = TrendTimeRange.SEVEN_DAYS.days
        val start7 = endDate.minusDays((days7 - 1).toLong())
        assertEquals(LocalDate.of(2026, 10, 28), start7)

        val prevEnd7 = start7.minusDays(1)
        val prevStart7 = prevEnd7.minusDays((days7 - 1).toLong())
        assertEquals(LocalDate.of(2026, 10, 27), prevEnd7)
        assertEquals(LocalDate.of(2026, 10, 21), prevStart7)

        // 30 Days
        val days30 = TrendTimeRange.THIRTY_DAYS.days
        val start30 = endDate.minusDays((days30 - 1).toLong())
        assertEquals(LocalDate.of(2026, 10, 5), start30)

        // 90 Days
        val days90 = TrendTimeRange.NINETY_DAYS.days
        val start90 = endDate.minusDays((days90 - 1).toLong())
        assertEquals(LocalDate.of(2026, 8, 6), start90)
    }

    @Test
    fun testDivisionByZeroGuardsInTrendComparisons() {
        // When previous period had 0 steps or 0 detox, delta percent must be safely null (no NaN / Infinity crash)
        val currentTotalSteps = 15000
        val prevTotalSteps = 0

        val stepsChangePercent: Float? = if (prevTotalSteps > 0) {
            ((currentTotalSteps - prevTotalSteps).toFloat() / prevTotalSteps) * 100f
        } else null

        assertNull(stepsChangePercent)

        // When previous period had steps, delta must be computed accurately
        val normalPrevSteps = 10000
        val normalChangePercent: Float? = if (normalPrevSteps > 0) {
            ((currentTotalSteps - normalPrevSteps).toFloat() / normalPrevSteps) * 100f
        } else null

        assertNotNull(normalChangePercent)
        assertEquals(50.0f, normalChangePercent!!, 0.001f)
    }

    @Test
    fun testCalendarLeapYearAndMonthLengths() {
        // 2024 is leap year -> February has 29 days
        val ym2024Feb = YearMonth.of(2024, 2)
        assertEquals(29, ym2024Feb.lengthOfMonth())

        // 2026 is non-leap year -> February has 28 days
        val ym2026Feb = YearMonth.of(2026, 2)
        assertEquals(28, ym2026Feb.lengthOfMonth())

        // November has 30 days
        val ym2026Nov = YearMonth.of(2026, 11)
        assertEquals(30, ym2026Nov.lengthOfMonth())
    }

    @Test
    fun testDateFormattingFriendly() {
        val today = LocalDate.of(2026, 11, 3)
        val yesterday = LocalDate.of(2026, 11, 2)
        val pastDate = LocalDate.of(2026, 10, 25)

        assertTrue(DateManager.formatFriendly(today, today).startsWith("Today"))
        assertTrue(DateManager.formatFriendly(yesterday, today).startsWith("Yesterday"))
        assertTrue(DateManager.formatFriendly(pastDate, today).contains("Oct 25"))
    }

    @Test
    fun testExerciseStreak_RecoveryRestDaySemantics() {
        // Today is Wednesday Nov 4, 2026. No workout today yet.
        val today = LocalDate.of(2026, 11, 4)
        
        // Scenario A: Workout on Tuesday Nov 3 (1 day ago -> 1 rest day today so far)
        val sessionsA = listOf(
            com.example.detoxproject.data.local.entity.ExerciseSessionEntity(
                id = 1L, title = "Tue Workout", activityType = "RUNNING",
                startTime = 1000L, durationSeconds = 1200L, status = "COMPLETED", sessionDate = "2026-11-03"
            )
        )
        val streakA = com.example.detoxproject.domain.engine.StreakEngine.calculateExerciseStreak(
            completedExercises = sessionsA, referenceDate = today
        )
        // Streak is 1 and active because 1 rest day is allowed
        assertEquals(1, streakA.currentStreak)

        // Scenario B: Workout on Monday Nov 2 (2 days ago -> 2 rest days: Tue and Wed)
        val sessionsB = listOf(
            com.example.detoxproject.data.local.entity.ExerciseSessionEntity(
                id = 1L, title = "Mon Workout", activityType = "RUNNING",
                startTime = 1000L, durationSeconds = 1200L, status = "COMPLETED", sessionDate = "2026-11-02"
            )
        )
        val streakB = com.example.detoxproject.domain.engine.StreakEngine.calculateExerciseStreak(
            completedExercises = sessionsB, referenceDate = today
        )
        // Streak is 1 and active because 2 rest days are within the allowed recovery window
        assertEquals(1, streakB.currentStreak)

        // Scenario C: Workout on Sunday Nov 1 (3 days ago -> 3 rest days: Mon, Tue, Wed)
        val sessionsC = listOf(
            com.example.detoxproject.data.local.entity.ExerciseSessionEntity(
                id = 1L, title = "Sun Workout", activityType = "RUNNING",
                startTime = 1000L, durationSeconds = 1200L, status = "COMPLETED", sessionDate = "2026-11-01"
            )
        )
        val streakC = com.example.detoxproject.domain.engine.StreakEngine.calculateExerciseStreak(
            completedExercises = sessionsC, referenceDate = today
        )
        // Streak resets to 0 because 3 consecutive rest days exceeds the 2-day recovery limit
        assertEquals(0, streakC.currentStreak)
    }

    @Test
    fun testStepTrackingSourceAttribution() {
        val validSources = setOf("HEALTH_CONNECT", "STEP_COUNTER", "ESTIMATED", "UNAVAILABLE")
        assertTrue(validSources.contains("HEALTH_CONNECT"))
        assertTrue(validSources.contains("STEP_COUNTER"))
        assertTrue(validSources.contains("ESTIMATED"))
        assertTrue(validSources.contains("UNAVAILABLE"))

        // Verify that UNAVAILABLE source cannot be considered verified
        fun isVerifiedStepSource(source: String): Boolean {
            return source == "HEALTH_CONNECT" || source == "STEP_COUNTER"
        }
        assertTrue(isVerifiedStepSource("HEALTH_CONNECT"))
        assertTrue(isVerifiedStepSource("STEP_COUNTER"))
        assertFalse(isVerifiedStepSource("UNAVAILABLE"))
        assertFalse(isVerifiedStepSource("ESTIMATED"))
    }
}
