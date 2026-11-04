package com.example.detoxproject.data.integrations.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when (intent.action) {
            SessionAlarmManager.ACTION_DISMISS_ALARM -> {
                SessionAlarmManager.dismissAlarm(context)
            }
        }
    }
}
