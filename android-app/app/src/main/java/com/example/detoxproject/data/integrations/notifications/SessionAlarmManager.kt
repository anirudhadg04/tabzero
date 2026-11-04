package com.example.detoxproject.data.integrations.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.detoxproject.MainActivity
import com.example.detoxproject.data.integrations.enforcement.DetoxEnforcementService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AlarmState {
    IDLE,
    RINGING,
    DISMISSED
}

object SessionAlarmManager {

    const val ALARM_CHANNEL_ID = "tabzero_session_alarm_channel"
    const val ALARM_NOTIFICATION_ID = 2002

    const val ACTION_DISMISS_ALARM = "com.example.detoxproject.ACTION_DISMISS_ALARM"
    const val ACTION_VIEW_SUMMARY = "com.example.detoxproject.ACTION_VIEW_SUMMARY"
    const val EXTRA_SESSION_TITLE = "extra_session_title"
    const val EXTRA_SESSION_DURATION = "extra_session_duration"

    private val _alarmState = MutableStateFlow(AlarmState.IDLE)
    val alarmState: StateFlow<AlarmState> = _alarmState.asStateFlow()

    private val _activeAlarmSessionTitle = MutableStateFlow<String?>(null)
    val activeAlarmSessionTitle: StateFlow<String?> = _activeAlarmSessionTitle.asStateFlow()

    private val _activeAlarmDurationMins = MutableStateFlow(0)
    val activeAlarmDurationMins: StateFlow<Int> = _activeAlarmDurationMins.asStateFlow()

    private val triggeredSessionIds = mutableSetOf<Long>()

    private var activeRingtone: Ringtone? = null
    private var isVibrating = false

    @Synchronized
    fun hasTriggered(sessionId: Long): Boolean {
        return triggeredSessionIds.contains(sessionId)
    }

    @Synchronized
    fun triggerAlarm(
        context: Context? = null,
        sessionId: Long,
        sessionTitle: String,
        durationMinutes: Int
    ): Boolean {
        // Prevent duplicate alarms for the same session ID
        if (triggeredSessionIds.contains(sessionId)) {
            return false
        }
        triggeredSessionIds.add(sessionId)

        _activeAlarmSessionTitle.value = sessionTitle
        _activeAlarmDurationMins.value = durationMinutes
        _alarmState.value = AlarmState.RINGING

        if (context != null) {
            try {
                // Stop enforcement service and ongoing notification so it is cleanly replaced by the completion alarm
                DetoxEnforcementService.stop(context)
                ActiveDetoxNotificationManager.cancelNotification(context)
                createNotificationChannel(context)
                startAudio(context)
                startVibration(context)
                postCompletionNotification(context, sessionTitle, durationMinutes)
            } catch (e: Exception) {
                // Safeguard against headless or restricted device environments
            }
        }

        return true
    }

    @Synchronized
    fun dismissAlarm(context: Context? = null) {
        if (context != null) {
            try {
                stopAudio()
                stopVibration(context)

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(ALARM_NOTIFICATION_ID)
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        } else {
            stopAudio()
        }

        _alarmState.value = AlarmState.DISMISSED
    }

    @Synchronized
    fun resetToIdle() {
        _alarmState.value = AlarmState.IDLE
        _activeAlarmSessionTitle.value = null
        _activeAlarmDurationMins.value = 0
    }

    @Synchronized
    fun clearTriggeredSessions() {
        triggeredSessionIds.clear()
    }

    private fun startAudio(context: Context) {
        try {
            stopAudio()
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            val ringtone = RingtoneManager.getRingtone(context, alarmUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ringtone?.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            ringtone?.play()
            activeRingtone = ringtone
        } catch (e: Exception) {
            activeRingtone = null
        }
    }

    private fun stopAudio() {
        try {
            activeRingtone?.stop()
        } catch (e: Exception) {
            // Ignore cleanup errors
        } finally {
            activeRingtone = null
        }
    }

    private fun startVibration(context: Context) {
        try {
            val pattern = longArrayOf(0, 600, 300, 600, 300, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, -1)
            }
            isVibrating = true
        } catch (e: Exception) {
            isVibrating = false
        }
    }

    private fun stopVibration(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.cancel()
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.cancel()
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
        } finally {
            isVibrating = false
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "tabzero. Detox Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Audible alarm and notifications when focus sessions complete"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 1000)
                setSound(soundUri, audioAttributes)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun postCompletionNotification(
        context: Context,
        sessionTitle: String,
        durationMinutes: Int
    ) {
        // Cancel the ongoing countdown notification so it never persists after completion
        ActiveDetoxNotificationManager.cancelNotification(context)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        // 1. Content Intent: Tapping notification opens MainActivity to show in-app completion sheet
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            101,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Action: Dismiss Alarm (Handled by BroadcastReceiver without opening app)
        val dismissIntent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = ACTION_DISMISS_ALARM
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            102,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Action: View Summary (Navigates to progress in MainActivity)
        val summaryIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_VIEW_SUMMARY
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_SESSION_TITLE, sessionTitle)
            putExtra(EXTRA_SESSION_DURATION, durationMinutes)
        }
        val summaryPendingIntent = PendingIntent.getActivity(
            context,
            103,
            summaryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Focus Session Completed! 🌿")
            .setContentText("'$sessionTitle' completed ($durationMinutes min). Life happens beyond the screen.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss Alarm", dismissPendingIntent)
            .addAction(android.R.drawable.ic_menu_view, "View Summary", summaryPendingIntent)
            .build()

        manager.notify(ALARM_NOTIFICATION_ID, notification)
    }
}
