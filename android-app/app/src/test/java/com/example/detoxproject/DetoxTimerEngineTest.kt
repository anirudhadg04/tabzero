package com.example.detoxproject

import com.example.detoxproject.data.local.entity.DetoxSessionEntity
import com.example.detoxproject.domain.engine.DetoxTimerEngine
import org.junit.Assert.*
import org.junit.Test

class DetoxTimerEngineTest {

    @Test
    fun testSessionElapsedCalculation_ActiveState() {
        val startTime = 1000000L
        val session = DetoxSessionEntity(
            id = 1L,
            title = "Morning Deep Work",
            startTime = startTime,
            targetMinutes = 25,
            status = "ACTIVE",
            sessionDate = "2026-10-31"
        )

        // 10 minutes later
        val currentTime = startTime + (10 * 60_000L)
        val elapsedMs = DetoxTimerEngine.calculateElapsedMs(session, currentTime)
        val remainingMs = DetoxTimerEngine.calculateRemainingMs(session, currentTime)
        val progress = DetoxTimerEngine.calculateProgress(session, currentTime)

        assertEquals(10 * 60_000L, elapsedMs)
        assertEquals(15 * 60_000L, remainingMs)
        assertEquals(10f / 25f, progress, 0.001f)
        assertFalse(DetoxTimerEngine.isTargetReached(session, currentTime))
    }

    @Test
    fun testPauseAndResumeAccumulation() {
        val startTime = 1000000L
        val pauseTime = startTime + (5 * 60_000L) // Paused after 5 mins

        val pausedSession = DetoxSessionEntity(
            id = 2L,
            title = "Study Session",
            startTime = startTime,
            targetMinutes = 30,
            status = "PAUSED",
            pausedAt = pauseTime,
            accumulatedPauseMs = 0L,
            sessionDate = "2026-10-31"
        )

        // Even 20 minutes later while still paused, elapsed should strictly freeze at 5 minutes
        val futureTime = startTime + (25 * 60_000L)
        val elapsedWhilePaused = DetoxTimerEngine.calculateElapsedMs(pausedSession, futureTime)
        assertEquals(5 * 60_000L, elapsedWhilePaused)

        // Now resume after 10 minutes of being paused (paused from +5m to +15m)
        val resumeTime = startTime + (15 * 60_000L)
        val accumulatedPause = resumeTime - pauseTime // 10 minutes
        val activeResumedSession = pausedSession.copy(
            status = "ACTIVE",
            pausedAt = null,
            accumulatedPauseMs = accumulatedPause
        )

        // Check 5 minutes after resuming (+20m total wall-clock time)
        val checkTime = startTime + (20 * 60_000L)
        val elapsedAfterResume = DetoxTimerEngine.calculateElapsedMs(activeResumedSession, checkTime)
        // Total active elapsed: 20m wall clock - 10m pause = 10m
        assertEquals(10 * 60_000L, elapsedAfterResume)
        assertEquals(20 * 60_000L, DetoxTimerEngine.calculateRemainingMs(activeResumedSession, checkTime))
    }

    @Test
    fun testTimerAccuracyAfterProcessRestart() {
        // Simulates app process kill: session was saved with startTime and accumulated pause
        val startTime = 2000000L
        val savedSession = DetoxSessionEntity(
            id = 3L,
            title = "Reading Offline",
            startTime = startTime,
            targetMinutes = 45,
            status = "ACTIVE",
            accumulatedPauseMs = 3 * 60_000L, // 3 minutes accumulated pause before crash
            sessionDate = "2026-10-31"
        )

        // Process restarts 20 minutes after startTime
        val restartTime = startTime + (20 * 60_000L)
        val elapsed = DetoxTimerEngine.calculateElapsedMs(savedSession, restartTime)
        // 20 minutes total - 3 minutes pause = 17 minutes elapsed
        assertEquals(17 * 60_000L, elapsed)
        assertEquals(28 * 60_000L, DetoxTimerEngine.calculateRemainingMs(savedSession, restartTime))
    }

    @Test
    fun testTargetReachedAndFormatting() {
        val startTime = 1000000L
        val session = DetoxSessionEntity(
            id = 4L,
            title = "Phone-Free Walk",
            startTime = startTime,
            targetMinutes = 20,
            status = "ACTIVE",
            sessionDate = "2026-10-31"
        )

        val completedTime = startTime + (20 * 60_000L)
        assertTrue(DetoxTimerEngine.isTargetReached(session, completedTime))
        assertEquals("00:00", DetoxTimerEngine.formatMinutesSeconds(DetoxTimerEngine.calculateRemainingMs(session, completedTime)))

        val midTime = startTime + (15 * 60_000L + 30_000L) // 4 min 30 sec left
        val remaining = DetoxTimerEngine.calculateRemainingMs(session, midTime)
        assertEquals("04:30", DetoxTimerEngine.formatMinutesSeconds(remaining))
    }

    @Test
    fun testExpectedEndTimeCalculation_MatchesRemainingCountdownExactly() {
        val startTime = 5000000L
        val session = DetoxSessionEntity(
            id = 5L,
            title = "Notification Sync Test",
            startTime = startTime,
            targetMinutes = 25,
            accumulatedPauseMs = 60_000L, // 1 min pause
            status = "ACTIVE",
            sessionDate = "2026-11-01"
        )

        val expectedEndMs = DetoxTimerEngine.calculateExpectedEndTimeMs(session)
        // Expected end: startTime + pause (60s) + target (25m) = 5,000,000 + 60,000 + 1,500,000 = 6,560,000
        assertEquals(startTime + 60_000L + (25 * 60_000L), expectedEndMs)

        // At currentTime = startTime + 10 mins (plus 1 min pause = 11 mins elapsed wall-clock)
        val currentTime = startTime + (11 * 60_000L)
        val remainingMs = DetoxTimerEngine.calculateRemainingMs(session, currentTime)
        val chronometerRemaining = expectedEndMs - currentTime

        assertEquals(remainingMs, chronometerRemaining)
        assertEquals(15 * 60_000L, chronometerRemaining)
    }
}
