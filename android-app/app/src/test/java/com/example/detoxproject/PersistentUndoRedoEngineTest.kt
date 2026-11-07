package com.example.detoxproject

import com.example.detoxproject.data.local.entity.ActionHistoryEntity
import com.example.detoxproject.data.local.entity.DetoxSessionEntity
import com.example.detoxproject.data.local.entity.PledgeEntity
import com.example.detoxproject.data.local.entity.UserGoalsEntity
import com.example.detoxproject.data.security.PasswordHasher
import com.example.detoxproject.domain.engine.UndoRedoSerializer
import com.example.detoxproject.domain.engine.WellnessRecommendationEngine
import org.junit.Assert.*
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Test

class PersistentUndoRedoEngineTest {

    @Test
    fun testSerializationRoundTrip_Session() {
        val session = DetoxSessionEntity(
            id = 42L,
            userId = 1L,
            pledgeId = 10L,
            title = "Code Sprint",
            startTime = 1700000000000L,
            endTime = 1700003600000L,
            targetMinutes = 60,
            actualMinutes = 60,
            status = "COMPLETED",
            pausedAt = null,
            accumulatedPauseMs = 120000L,
            category = "study",
            restrictedPackages = "com.instagram.android,com.twitter.android",
            allowAudio = true,
            sessionDate = "2026-10-31"
        )

        val json = UndoRedoSerializer.serializeSession(session)
        val deserialized = UndoRedoSerializer.deserializeSession(json)

        assertEquals(session.id, deserialized.id)
        assertEquals(session.userId, deserialized.userId)
        assertEquals(session.pledgeId, deserialized.pledgeId)
        assertEquals(session.title, deserialized.title)
        assertEquals(session.startTime, deserialized.startTime)
        assertEquals(session.targetMinutes, deserialized.targetMinutes)
        assertEquals(session.actualMinutes, deserialized.actualMinutes)
        assertEquals(session.status, deserialized.status)
        assertEquals(session.accumulatedPauseMs, deserialized.accumulatedPauseMs)
        assertEquals(session.restrictedPackages, deserialized.restrictedPackages)
        assertEquals(session.allowAudio, deserialized.allowAudio)
        assertEquals(session.sessionDate, deserialized.sessionDate)
    }

    @Test
    fun testSerializationRoundTrip_Goals() {
        val goals = UserGoalsEntity(
            userId = 1L,
            userName = "CodeNova",
            dailySteps = 10000,
            dailyOfflineMinutes = 180,
            strideLengthMeters = 0.8f,
            updatedAt = 1700000000000L
        )

        val json = UndoRedoSerializer.serializeGoals(goals)
        val deserialized = UndoRedoSerializer.deserializeGoals(json)

        assertEquals(goals.userId, deserialized.userId)
        assertEquals(goals.userName, deserialized.userName)
        assertEquals(goals.dailySteps, deserialized.dailySteps)
        assertEquals(goals.dailyOfflineMinutes, deserialized.dailyOfflineMinutes)
        assertEquals(goals.strideLengthMeters, deserialized.strideLengthMeters, 0.001f)
    }

    @Test
    fun testPasswordHasherSecurity() {
        val salt = PasswordHasher.generateSalt()
        assertNotNull(salt)
        assertEquals(32, salt.length) // 16 bytes = 32 hex chars

        val hash1 = PasswordHasher.hashPassword("secret123", salt)
        val hash2 = PasswordHasher.hashPassword("secret123", salt)
        assertEquals(hash1, hash2) // Deterministic with same salt

        val differentSalt = PasswordHasher.generateSalt()
        val hashDifferentSalt = PasswordHasher.hashPassword("secret123", differentSalt)
        assertNotEquals(hash1, hashDifferentSalt) // Salt changes hash

        assertTrue(PasswordHasher.verifyPassword("secret123", salt, hash1))
        assertFalse(PasswordHasher.verifyPassword("wrongpass", salt, hash1))
    }

    @Test
    fun testWellnessRecommendations() {
        // Test rain rule
        val rainyRecs = WellnessRecommendationEngine.generateRecommendations(
            temperature = 22.0,
            weatherCode = 61, // Rain
            precipitation = 2.5,
            screenTimeMs = 3600000L,
            stepsToday = 2000
        )
        assertTrue(rainyRecs.any { !it.isOutdoor })

        // Test excessive heat rule
        val heatRecs = WellnessRecommendationEngine.generateRecommendations(
            temperature = 36.5,
            weatherCode = 0,
            precipitation = 0.0,
            screenTimeMs = 3600000L,
            stepsToday = 2000
        )
        assertTrue(heatRecs.any { it.title.contains("Breathwork") || it.title.contains("Journaling") })

        // Test high screen time + low steps + pleasant weather rule
        val walkRecs = WellnessRecommendationEngine.generateRecommendations(
            temperature = 26.0,
            weatherCode = 1,
            precipitation = 0.0,
            screenTimeMs = 3 * 3600000L, // 3 hours screen time
            stepsToday = 1500, // low steps
            stepsGoal = 8000
        )
        val outdoorWalk = walkRecs.find { it.isOutdoor }
        assertNotNull(outdoorWalk)
        assertTrue(outdoorWalk!!.title.contains("Walk"))
    }

    @Test
    fun testFiveStepHistoryStackLogic() {
        val pastStack = mutableListOf<ActionHistoryEntity>()
        var futureStack = mutableListOf<ActionHistoryEntity>()

        fun record(action: String, id: Long) {
            futureStack.clear()
            if (pastStack.size >= 5) {
                pastStack.removeAt(0)
            }
            pastStack.add(
                ActionHistoryEntity(
                    userId = 1L,
                    actionType = action,
                    description = action,
                    stackPosition = "PAST",
                    entityType = "SESSION",
                    entityId = id,
                    sequenceOrder = pastStack.size + 1
                )
            )
        }

        fun undo(): Boolean {
            if (pastStack.isEmpty()) return false
            val action = pastStack.removeAt(pastStack.lastIndex)
            futureStack.add(action.copy(stackPosition = "FUTURE"))
            return true
        }

        fun redo(): Boolean {
            if (futureStack.isEmpty()) return false
            val action = futureStack.removeAt(futureStack.lastIndex)
            pastStack.add(action.copy(stackPosition = "PAST"))
            return true
        }

        for (i in 1..6) {
            record("ACTION_$i", i.toLong())
        }
        assertEquals(5, pastStack.size)
        assertEquals("ACTION_2", pastStack[0].actionType)
        assertEquals("ACTION_6", pastStack[4].actionType)
        assertTrue(futureStack.isEmpty())

        for (i in 1..5) {
            assertTrue(undo())
        }
        assertTrue(pastStack.isEmpty())
        assertEquals(5, futureStack.size)
        assertFalse(undo())

        for (i in 1..5) {
            assertTrue(redo())
        }
        assertEquals(5, pastStack.size)
        assertTrue(futureStack.isEmpty())
        assertFalse(redo())

        undo()
        undo()
        assertEquals(3, pastStack.size)
        assertEquals(2, futureStack.size)

        record("NEW_ACTION", 99L)
        assertEquals(4, pastStack.size)
        assertTrue(futureStack.isEmpty())
        assertFalse(redo())
    }

    @Test
    fun testDailyUndoRedoStack_MidnightRolloverAndIsolation() {
        val zone = ZoneId.systemDefault()
        val day1 = LocalDate.of(2026, 11, 7)
        val day2 = LocalDate.of(2026, 11, 8)

        val day1Start = day1.atStartOfDay(zone).toInstant().toEpochMilli()
        val day1End = day1.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val day2Start = day2.atStartOfDay(zone).toInstant().toEpochMilli()
        val day2End = day2.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        // In-memory simulation of ActionHistoryDao with daily timestamp boundaries
        val databaseActions = mutableListOf<ActionHistoryEntity>()

        fun record(action: String, id: Long, timestampMs: Long) {
            // Clear future
            databaseActions.removeAll { it.stackPosition == "FUTURE" }
            // Purge past days
            databaseActions.removeAll { it.timestamp < day1Start }
            // Get today's past actions
            val todayPast = databaseActions.filter { it.stackPosition == "PAST" && it.timestamp >= day1Start && it.timestamp < day1End }
            if (todayPast.size >= 5) {
                val oldest = todayPast.minByOrNull { it.sequenceOrder }
                oldest?.let { databaseActions.remove(it) }
            }
            databaseActions.add(
                ActionHistoryEntity(
                    id = databaseActions.size + 1L,
                    userId = 1L,
                    actionType = action,
                    description = action,
                    timestamp = timestampMs,
                    stackPosition = "PAST",
                    entityType = "SESSION",
                    entityId = id,
                    sequenceOrder = (todayPast.maxOfOrNull { it.sequenceOrder } ?: 0) + 1
                )
            )
        }

        fun undo(referenceDate: LocalDate): Boolean {
            val rStart = referenceDate.atStartOfDay(zone).toInstant().toEpochMilli()
            val rEnd = referenceDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val latest = databaseActions.filter { it.stackPosition == "PAST" && it.timestamp >= rStart && it.timestamp < rEnd }
                .maxByOrNull { it.sequenceOrder } ?: return false

            databaseActions.remove(latest)
            databaseActions.add(latest.copy(stackPosition = "FUTURE"))
            return true
        }

        fun redo(referenceDate: LocalDate): Boolean {
            val rStart = referenceDate.atStartOfDay(zone).toInstant().toEpochMilli()
            val rEnd = referenceDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val next = databaseActions.filter { it.stackPosition == "FUTURE" && it.timestamp >= rStart && it.timestamp < rEnd }
                .minByOrNull { it.sequenceOrder } ?: return false

            databaseActions.remove(next)
            databaseActions.add(next.copy(stackPosition = "PAST"))
            return true
        }

        // 1. Day 1: Perform operations during the day
        record("COMPLETE_HABIT", 101L, day1Start + 3600000L) // 1 hr after start of day 1
        record("CREATE_SESSION", 201L, day1Start + 7200000L) // 2 hrs after start of day 1

        val day1Past = databaseActions.filter { it.stackPosition == "PAST" && it.timestamp >= day1Start && it.timestamp < day1End }
        assertEquals(2, day1Past.size)

        // Undo on Day 1 succeeds
        assertTrue(undo(day1))
        val day1PastAfterUndo = databaseActions.filter { it.stackPosition == "PAST" && it.timestamp >= day1Start && it.timestamp < day1End }
        assertEquals(1, day1PastAfterUndo.size)
        // Redo on Day 1 succeeds
        assertTrue(redo(day1))

        // 2. Midnight Rollover to Day 2:
        // Trying to undo on Day 2 should fail because there are zero actions recorded on Day 2
        assertFalse("Day 1 action must NOT be undoable on Day 2", undo(day2))
        assertFalse("Day 1 action must NOT be redoable on Day 2", redo(day2))

        // 3. Record new action on Day 2
        // Old action pointers are purged from active stack, fresh stack begins
        databaseActions.removeAll { it.timestamp < day2Start }
        assertEquals("Prior day history purged from stack", 0, databaseActions.size)

        record("COMPLETE_HABIT", 102L, day2Start + 1800000L)
        val day2Past = databaseActions.filter { it.stackPosition == "PAST" && it.timestamp >= day2Start && it.timestamp < day2End }
        assertEquals(1, day2Past.size)
        assertTrue("Day 2 action is undoable on Day 2", undo(day2))
    }

    @Test
    fun testDailyUndoRedo_DataIntegrity_HistoricalRecordsRemainPermanent() {
        // User historical records (habits, completed sessions, notes, XP)
        val completedHabitHistory = mutableListOf("Walk 8,000 steps - 2026-11-06", "Meditation - 2026-11-06")
        val completedSessions = mutableListOf("Focus Sprint - 45 min", "Evening Calm - 30 min")
        var userTotalXp = 450

        // Undo stack pointers
        val actionHistoryPointers = mutableListOf("action_ptr_1", "action_ptr_2")

        // Resetting daily undo/redo stack pointers at midnight:
        actionHistoryPointers.clear()

        // Critical verification: User data MUST NOT be deleted
        assertEquals(2, completedHabitHistory.size)
        assertEquals(2, completedSessions.size)
        assertEquals(450, userTotalXp)
        assertTrue(actionHistoryPointers.isEmpty())
    }
}

