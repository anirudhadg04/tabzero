package com.example.detoxproject.data.integrations.notifications

import android.content.Context

/**
 * Backward compatibility facade delegating to the centralized SessionAlarmManager.
 */
object AlarmNotificationHelper {

    fun triggerSessionFinishedAlarm(context: Context, sessionTitle: String) {
        SessionAlarmManager.triggerAlarm(
            context = context,
            sessionId = System.currentTimeMillis(),
            sessionTitle = sessionTitle,
            durationMinutes = 0
        )
    }
}
