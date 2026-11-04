package com.example.detoxproject

import com.example.detoxproject.data.integrations.notifications.AlarmState
import com.example.detoxproject.data.integrations.notifications.SessionAlarmManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SessionAlarmManagerTest {

    @Before
    fun setUp() {
        SessionAlarmManager.resetToIdle()
        SessionAlarmManager.clearTriggeredSessions()
    }

    @Test
    fun testInitialStateIsIdle() {
        assertEquals(AlarmState.IDLE, SessionAlarmManager.alarmState.value)
        assertNull(SessionAlarmManager.activeAlarmSessionTitle.value)
        assertEquals(0, SessionAlarmManager.activeAlarmDurationMins.value)
    }

    @Test
    fun testTriggerAlarm_TransitionsToRingingAndSetsMetadata() {
        val triggered = SessionAlarmManager.triggerAlarm(
            context = null,
            sessionId = 101L,
            sessionTitle = "Deep Work Focus",
            durationMinutes = 25
        )

        assertTrue("First trigger must return true", triggered)
        assertEquals(AlarmState.RINGING, SessionAlarmManager.alarmState.value)
        assertEquals("Deep Work Focus", SessionAlarmManager.activeAlarmSessionTitle.value)
        assertEquals(25, SessionAlarmManager.activeAlarmDurationMins.value)
        assertTrue(SessionAlarmManager.hasTriggered(101L))
    }

    @Test
    fun testDuplicateTrigger_IsIdempotentAndIgnored() {
        // Trigger first time
        val firstTrigger = SessionAlarmManager.triggerAlarm(
            context = null,
            sessionId = 202L,
            sessionTitle = "Reading Session",
            durationMinutes = 30
        )
        assertTrue(firstTrigger)

        // Attempt second trigger with same sessionId
        val secondTrigger = SessionAlarmManager.triggerAlarm(
            context = null,
            sessionId = 202L,
            sessionTitle = "Duplicate Reading",
            durationMinutes = 30
        )
        assertFalse("Duplicate trigger for same session ID must return false", secondTrigger)
        assertEquals("Title must remain the original", "Reading Session", SessionAlarmManager.activeAlarmSessionTitle.value)
    }

    @Test
    fun testDismissAlarm_TransitionsToDismissed() {
        SessionAlarmManager.triggerAlarm(
            context = null,
            sessionId = 303L,
            sessionTitle = "Walking Session",
            durationMinutes = 15
        )
        assertEquals(AlarmState.RINGING, SessionAlarmManager.alarmState.value)

        SessionAlarmManager.dismissAlarm(context = null)
        assertEquals(AlarmState.DISMISSED, SessionAlarmManager.alarmState.value)
    }

    @Test
    fun testResetToIdle_ClearsStateAndMetadata() {
        SessionAlarmManager.triggerAlarm(
            context = null,
            sessionId = 404L,
            sessionTitle = "Nap Time",
            durationMinutes = 20
        )
        SessionAlarmManager.dismissAlarm(context = null)
        assertEquals(AlarmState.DISMISSED, SessionAlarmManager.alarmState.value)

        SessionAlarmManager.resetToIdle()
        assertEquals(AlarmState.IDLE, SessionAlarmManager.alarmState.value)
        assertNull(SessionAlarmManager.activeAlarmSessionTitle.value)
        assertEquals(0, SessionAlarmManager.activeAlarmDurationMins.value)
    }

    @Test
    fun testMultipleDistinctSessionsCanTriggerSequentially() {
        val firstResult = SessionAlarmManager.triggerAlarm(
            context = null,
            sessionId = 501L,
            sessionTitle = "Morning Routine",
            durationMinutes = 10
        )
        assertTrue(firstResult)
        assertEquals("Morning Routine", SessionAlarmManager.activeAlarmSessionTitle.value)

        SessionAlarmManager.dismissAlarm(context = null)
        SessionAlarmManager.resetToIdle()

        val secondResult = SessionAlarmManager.triggerAlarm(
            context = null,
            sessionId = 502L,
            sessionTitle = "Afternoon Study",
            durationMinutes = 45
        )
        assertTrue(secondResult)
        assertEquals("Afternoon Study", SessionAlarmManager.activeAlarmSessionTitle.value)
        assertEquals(45, SessionAlarmManager.activeAlarmDurationMins.value)
        assertEquals(AlarmState.RINGING, SessionAlarmManager.alarmState.value)
    }
}
