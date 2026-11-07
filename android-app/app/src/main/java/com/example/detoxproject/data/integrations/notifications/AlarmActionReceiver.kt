package com.example.detoxproject.data.integrations.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.detoxproject.data.integrations.enforcement.DetoxAccessibilityService
import com.example.detoxproject.data.integrations.enforcement.DetoxEnforcementService
import com.example.detoxproject.data.local.OfflineDatabase
import com.example.detoxproject.domain.engine.DetoxTimerEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when (intent.action) {
            SessionAlarmManager.ACTION_DISMISS_ALARM -> {
                SessionAlarmManager.dismissAlarm(context)
            }
            DetoxAlarmScheduler.ACTION_SESSION_TIMEOUT -> {
                val sessionId = intent.getLongExtra(DetoxAlarmScheduler.EXTRA_SESSION_ID, -1L)
                val sessionTitle = intent.getStringExtra(DetoxAlarmScheduler.EXTRA_SESSION_TITLE) ?: "Focus Session"
                val durationMinutes = intent.getIntExtra(DetoxAlarmScheduler.EXTRA_TARGET_MINUTES, 25)

                if (sessionId != -1L) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = OfflineDatabase.getInstance(context)
                            val session = db.detoxDao().getSessionById(sessionId)
                            if (session != null && session.status == "ACTIVE") {
                                val completedSession = session.copy(
                                    status = "COMPLETED",
                                    actualMinutes = session.targetMinutes,
                                    endTime = DetoxTimerEngine.calculateExpectedEndTimeMs(session)
                                )
                                db.detoxDao().updateSession(completedSession)

                                // Stop enforcement service & ongoing notification
                                DetoxEnforcementService.stop(context)
                                DetoxAccessibilityService.stopDetox()
                                ActiveDetoxNotificationManager.cancelNotification(context)

                                // Trigger completion alarm audio, vibration, and notification
                                SessionAlarmManager.triggerAlarm(
                                    context = context,
                                    sessionId = sessionId,
                                    sessionTitle = sessionTitle,
                                    durationMinutes = durationMinutes
                                )
                            }
                        } catch (e: Exception) {
                            // Guard against unhandled receiver exceptions
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        }
    }
}
