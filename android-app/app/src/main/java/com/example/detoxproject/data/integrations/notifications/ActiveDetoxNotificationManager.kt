package com.example.detoxproject.data.integrations.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.detoxproject.MainActivity
import com.example.detoxproject.R
import com.example.detoxproject.data.local.entity.DetoxSessionEntity
import com.example.detoxproject.domain.engine.DetoxTimerEngine

/**
 * Manages the ongoing active digital detox notification.
 * Provides a native live countdown chronometer synchronized with DetoxTimerEngine,
 * supporting ACTIVE, PAUSED, RESUMED, COMPLETED, and CANCELLED states.
 */
object ActiveDetoxNotificationManager {

    const val CHANNEL_ID = "tabzero_active_detox_channel"
    const val ONGOING_NOTIFICATION_ID = 2001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "tabzero. Digital Detox Active",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Live countdown and ongoing status of active digital detox sessions"
                setShowBadge(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun buildNotification(context: Context, session: DetoxSessionEntity): Notification {
        createNotificationChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            201,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val logoBitmap = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.tabzero_logo)
        } catch (e: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("tabzero. — Digital Detox Active")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pendingIntent)

        if (logoBitmap != null) {
            builder.setLargeIcon(logoBitmap)
        }

        if (session.status == "ACTIVE") {
            val endTimeMs = DetoxTimerEngine.calculateExpectedEndTimeMs(session)
            val sessionTitle = session.title.ifBlank { "Focus Session" }
            builder.setContentText("Focusing on '$sessionTitle'.")
            builder.setSubText("Distracting apps will be blocked.")
            builder.setWhen(endTimeMs)
            builder.setUsesChronometer(true)
            builder.setChronometerCountDown(true)
            builder.setShowWhen(true)

            val bigText = NotificationCompat.BigTextStyle()
                .setBigContentTitle("tabzero. — Digital Detox Active")
                .bigText("Focusing on '$sessionTitle'.\nDistracting apps will be blocked.")
            builder.setStyle(bigText)
        } else if (session.status == "PAUSED") {
            val remainingMs = DetoxTimerEngine.calculateRemainingMs(session)
            val formatted = DetoxTimerEngine.formatMinutesSeconds(remainingMs)
            val sessionTitle = session.title.ifBlank { "Focus Session" }
            builder.setContentText("Focusing on '$sessionTitle'.")
            builder.setSubText("Session paused • $formatted remaining")
            builder.setUsesChronometer(false)
            builder.setShowWhen(false)

            val bigText = NotificationCompat.BigTextStyle()
                .setBigContentTitle("tabzero. — Digital Detox Active")
                .bigText("Focusing on '$sessionTitle' (Session paused • $formatted remaining).\nDistracting apps will be blocked.")
            builder.setStyle(bigText)
        }

        return builder.build()
    }

    fun updateNotification(context: Context, session: DetoxSessionEntity?) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (session == null || session.status == "COMPLETED" || session.status == "CANCELLED") {
                cancelNotification(context)
                return
            }

            val notification = buildNotification(context, session)
            manager.notify(ONGOING_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // Guard against permissions / background execution edge-cases
        }
    }

    fun cancelNotification(context: Context) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            manager.cancel(ONGOING_NOTIFICATION_ID)
        } catch (e: Exception) {
            // Ignore cancel cleanup errors
        }
    }
}
