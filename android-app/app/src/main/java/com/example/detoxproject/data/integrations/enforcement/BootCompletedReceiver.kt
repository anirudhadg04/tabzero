package com.example.detoxproject.data.integrations.enforcement

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.detoxproject.data.integrations.notifications.ActiveDetoxNotificationManager
import com.example.detoxproject.data.integrations.notifications.DetoxAlarmScheduler
import com.example.detoxproject.data.integrations.notifications.SessionAlarmManager
import com.example.detoxproject.data.local.OfflineDatabase
import com.example.detoxproject.domain.engine.DetoxTimerEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = OfflineDatabase.getInstance(context)
                val activeSession = db.detoxDao().getAnyActiveSessionDirect()

                if (activeSession == null) {
                    return@launch
                }

                val now = System.currentTimeMillis()

                if (activeSession.status == "ACTIVE") {
                    val remainingMs = DetoxTimerEngine.calculateRemainingMs(activeSession, now)

                    if (remainingMs <= 0L) {
                        // Session expired while phone was powered off: mark completed and trigger alarm
                        val completed = activeSession.copy(
                            status = "COMPLETED",
                            actualMinutes = activeSession.targetMinutes,
                            endTime = DetoxTimerEngine.calculateExpectedEndTimeMs(activeSession)
                        )
                        db.detoxDao().updateSession(completed)

                        ActiveDetoxNotificationManager.cancelNotification(context)
                        SessionAlarmManager.triggerAlarm(
                            context = context,
                            sessionId = activeSession.id,
                            sessionTitle = activeSession.title,
                            durationMinutes = activeSession.targetMinutes
                        )
                    } else {
                        // Session is still active: restore enforcement, alarm, and countdown notification
                        val pkgs = activeSession.restrictedPackages.split(',')
                            .map { it.trim() }
                            .filter { it.isNotBlank() }

                        DetoxEnforcementService.start(
                            context = context,
                            packages = pkgs,
                            title = activeSession.title,
                            targetMins = activeSession.targetMinutes,
                            startTime = activeSession.startTime,
                            sessionId = activeSession.id
                        )

                        DetoxAlarmScheduler.scheduleSessionCompletionAlarm(context, activeSession)
                        ActiveDetoxNotificationManager.updateNotification(context, activeSession)
                    }
                } else if (activeSession.status == "PAUSED") {
                    // Re-establish paused status in notification drawer
                    ActiveDetoxNotificationManager.updateNotification(context, activeSession)
                }
            } catch (e: Exception) {
                // Safeguard against OS exceptions during device boot
            } finally {
                pendingResult.finish()
            }
        }
    }
}
