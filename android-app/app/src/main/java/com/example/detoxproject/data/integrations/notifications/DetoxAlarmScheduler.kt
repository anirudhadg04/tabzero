package com.example.detoxproject.data.integrations.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.detoxproject.data.local.entity.DetoxSessionEntity
import com.example.detoxproject.domain.engine.DetoxTimerEngine

object DetoxAlarmScheduler {

    const val ACTION_SESSION_TIMEOUT = "com.example.detoxproject.ACTION_SESSION_TIMEOUT"
    const val EXTRA_SESSION_ID = "extra_session_id"
    const val EXTRA_SESSION_TITLE = "extra_session_title"
    const val EXTRA_TARGET_MINUTES = "extra_target_minutes"

    /**
     * Schedules an exact system alarm for the session completion time.
     * Prevents duplicates by using a deterministic requestCode and cancelling any prior intent.
     */
    fun scheduleSessionCompletionAlarm(context: Context, session: DetoxSessionEntity): Boolean {
        if (session.status != "ACTIVE") return false

        val triggerAtMs = DetoxTimerEngine.calculateExpectedEndTimeMs(session)
        val now = System.currentTimeMillis()
        if (triggerAtMs <= now) {
            return false
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false

        val intent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = ACTION_SESSION_TIMEOUT
            putExtra(EXTRA_SESSION_ID, session.id)
            putExtra(EXTRA_SESSION_TITLE, session.title)
            putExtra(EXTRA_TARGET_MINUTES, session.targetMinutes)
        }

        val requestCode = (session.id % Int.MAX_VALUE).toInt()

        // Cancel previous pending intent to prevent any duplicate alarms
        val existingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (existingIntent != null) {
            alarmManager.cancel(existingIntent)
            existingIntent.cancel()
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Cancels any scheduled completion alarm for the given session ID.
     */
    fun cancelSessionCompletionAlarm(context: Context, sessionId: Long) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, AlarmActionReceiver::class.java).apply {
                action = ACTION_SESSION_TIMEOUT
            }
            val requestCode = (sessionId % Int.MAX_VALUE).toInt()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (e: Exception) {
            // Ignore cancel cleanup errors
        }
    }
}
