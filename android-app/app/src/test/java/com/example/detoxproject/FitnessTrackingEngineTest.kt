package com.example.detoxproject

import com.example.detoxproject.data.local.entity.DailyActivityEntity
import com.example.detoxproject.data.local.entity.ExerciseSessionEntity
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class FitnessTrackingEngineTest {

    @Test
    fun testDailyActivityEntity_DefaultsAndProgress() {
        val today = LocalDate.now().toString()
        val activity = DailyActivityEntity(
            date = today,
            steps = 6000,
            goalSteps = 8000,
            distanceMeters = 4500f,
            activeCalories = 240f,
            activeMinutes = 45,
            source = "STEP_COUNTER"
        )

        assertEquals(today, activity.date)
        assertEquals(6000, activity.steps)
        assertEquals(8000, activity.goalSteps)
        assertEquals(4500f, activity.distanceMeters, 0.01f)
        assertEquals("STEP_COUNTER", activity.source)

        val progress = activity.steps.toFloat() / activity.goalSteps
        assertEquals(0.75f, progress, 0.001f)
    }

    @Test
    fun testExerciseSessionEntity_CreationAndCompletion() {
        val today = LocalDate.now().toString()
        val startTime = System.currentTimeMillis() - 1800000L // 30 mins ago
        val activeSession = ExerciseSessionEntity(
            title = "Morning Jog",
            activityType = "RUNNING",
            startTime = startTime,
            status = "ACTIVE",
            sessionDate = today
        )

        assertEquals("ACTIVE", activeSession.status)
        assertEquals("RUNNING", activeSession.activityType)

        val endTime = System.currentTimeMillis()
        val durationSeconds = (endTime - startTime) / 1000L
        val durationMinutes = (durationSeconds / 60).toInt()
        val calPerMin = 11.0f // Running burn rate
        val estimatedCalories = durationMinutes * calPerMin

        val completedSession = activeSession.copy(
            endTime = endTime,
            durationSeconds = durationSeconds,
            calories = estimatedCalories,
            status = "COMPLETED"
        )

        assertEquals("COMPLETED", completedSession.status)
        assertTrue(completedSession.durationSeconds >= 1700L)
        assertTrue(completedSession.calories != null && completedSession.calories!! > 0f)
    }

    @Test
    fun testDistanceEstimation_FromSteps() {
        val strideMeters = 0.75f
        val steps = 8000
        val distanceKm = (steps * strideMeters) / 1000f

        assertEquals(6.0f, distanceKm, 0.001f)
    }

    @Test
    fun testStepSensorMath_NormalIncrement() {
        val baseline = 10000
        val currentRaw = 14500
        val todaySteps = (currentRaw - baseline).coerceAtLeast(0)

        assertEquals(4500, todaySteps)
    }

    @Test
    fun testStepSensorMath_RebootRecoveryPreservesSteps() {
        // Device was at 14500 raw, with baseline 10000 -> 4500 steps today
        val previousTodaySteps = 4500
        // Device reboot occurs: raw counter resets to small number, e.g. 120
        val rawAfterReboot = 120
        // Reboot adjustment: newBaseline = rawAfterReboot - previousTodaySteps
        val newBaseline = rawAfterReboot - previousTodaySteps
        // Now calculate today's steps with new baseline:
        val todaySteps = (rawAfterReboot - newBaseline).coerceAtLeast(0)

        assertEquals(4500, todaySteps)

        // When user takes 50 more steps after reboot:
        val subsequentRaw = 170
        val updatedTodaySteps = (subsequentRaw - newBaseline).coerceAtLeast(0)
        assertEquals(4550, updatedTodaySteps)
    }

    @Test
    fun testMidnightRollover_ResetsTodayStepsAndReanchorsBaseline() {
        val yesterdayStr = "2026-10-31"
        val todayStr = "2026-11-01"
        assertNotEquals(yesterdayStr, todayStr)

        val rawAtMidnight = 25000
        // New day baseline becomes the raw count at the start of the new day
        val todayBaseline = rawAtMidnight
        val todayStepsAtStart = (rawAtMidnight - todayBaseline).coerceAtLeast(0)

        assertEquals(0, todayStepsAtStart)

        // User walks 300 steps in the morning:
        val morningRaw = 25300
        val morningSteps = (morningRaw - todayBaseline).coerceAtLeast(0)
        assertEquals(300, morningSteps)
    }
}
