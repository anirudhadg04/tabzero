package com.example.detoxproject.ui.screens.settings

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class PermissionDetail(
    val key: String,
    val title: String,
    val shortName: String,
    val icon: ImageVector,
    val whyNeeded: String,
    val functionalityEnabled: String,
    val privacyNote: String,
    val isMandatory: Boolean,
    val requirementType: String
)

object PermissionEducationManager {
    private const val PREFS_NAME = "tabzero_permission_education_prefs"
    private const val KEY_SEEN_PREFIX = "seen_perm_explanation_"

    const val PERM_USAGE_ACCESS = "usage_access"
    const val PERM_ACCESSIBILITY = "accessibility"
    const val PERM_OVERLAY = "overlay"
    const val PERM_NOTIFICATIONS = "notifications"
    const val PERM_EXACT_ALARMS = "exact_alarms"
    const val PERM_ACTIVITY_RECOGNITION = "activity_recognition"
    const val PERM_HEALTH_CONNECT = "health_connect"
    const val PERM_BATTERY_OPTIMIZATION = "battery_optimization"

    fun hasSeenExplanation(context: Context, permKey: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SEEN_PREFIX + permKey, false)
    }

    fun markExplanationSeen(context: Context, permKey: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SEEN_PREFIX + permKey, true).apply()
    }

    fun resetAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    fun getDetail(key: String): PermissionDetail {
        return when (key) {
            PERM_USAGE_ACCESS -> PermissionDetail(
                key = PERM_USAGE_ACCESS,
                title = "Usage Access",
                shortName = "Screen Time & App Monitoring",
                icon = Icons.Default.QueryStats,
                whyNeeded = "tabzero reads on-device application usage statistics to detect when you launch apps designated as distracting during an active focus mode.",
                functionalityEnabled = "Calculates daily device screen-time metrics and powers automatic intervention during detox sessions.",
                privacyNote = "All statistics are processed in real-time in memory and stored locally in your phone's SQLite database. Nothing is sent to any server.",
                isMandatory = false,
                requirementType = "Required for App Blocking & Screen Time"
            )
            PERM_ACCESSIBILITY -> PermissionDetail(
                key = PERM_ACCESSIBILITY,
                title = "App Blocker (Accessibility)",
                shortName = "0ms Instant Interception",
                icon = Icons.Default.Block,
                whyNeeded = "tabzero uses an Accessibility Service exclusively to detect window-state change events when a restricted app is launched, triggering the gentle Zen intervention screen with zero latency.",
                functionalityEnabled = "Provides instant 0ms blocking when restricted apps are launched during an active detox session.",
                privacyNote = "canRetrieveWindowContent is explicitly set to false. tabzero CANNOT read screen text, cannot inspect passwords, cannot view messages, and does not alter window content. Operates 100% locally on your device.",
                isMandatory = false,
                requirementType = "Optional Supplementary Enforcement"
            )
            PERM_OVERLAY -> PermissionDetail(
                key = PERM_OVERLAY,
                title = "Display Over Other Apps",
                shortName = "Zen Intervention Overlay",
                icon = Icons.Default.Layers,
                whyNeeded = "Allows tabzero to display the calming full-screen Zen reminder over top of restricted applications when opened during an active detox session.",
                functionalityEnabled = "Shows the full-screen intervention reminder to pause, breathe, and return to focus.",
                privacyNote = "The overlay is rendered strictly on your device and is only invoked during an active detox session when a restricted app appears in the foreground.",
                isMandatory = false,
                requirementType = "Required for Full-Screen Intervention"
            )
            PERM_NOTIFICATIONS -> PermissionDetail(
                key = PERM_NOTIFICATIONS,
                title = "Notifications",
                shortName = "Timers & Session Chimes",
                icon = Icons.Default.Notifications,
                whyNeeded = "Displays the ongoing countdown notification in your notification shade during focus sessions and plays a soft chime when a session finishes.",
                functionalityEnabled = "Live status bar timer, lockscreen controls, and punctual session completion alerts.",
                privacyNote = "Notifications are generated locally on your phone. No push tokens or remote servers are involved.",
                isMandatory = false,
                requirementType = "Required for Background Countdown Shade"
            )
            PERM_EXACT_ALARMS -> PermissionDetail(
                key = PERM_EXACT_ALARMS,
                title = "Exact Alarms & Reminders",
                shortName = "Punctual Session Alerts",
                icon = Icons.Default.Alarm,
                whyNeeded = "Schedules exact millisecond alarms with Android's AlarmManager so your focus session end chime rings punctually even when the phone is in deep Doze sleep mode.",
                functionalityEnabled = "Guarantees exact-time completion alerts and scheduled habit reminders.",
                privacyNote = "Alarms are managed locally by your device's internal system clock and never transmit data.",
                isMandatory = false,
                requirementType = "Optional Punctuality Enhancement"
            )
            PERM_ACTIVITY_RECOGNITION -> PermissionDetail(
                key = PERM_ACTIVITY_RECOGNITION,
                title = "Physical Activity Tracking",
                shortName = "Step Counter Sensor",
                icon = Icons.Default.DirectionsWalk,
                whyNeeded = "Accesses your phone's built-in step counter hardware sensor to track daily movement and mindful walking progress.",
                functionalityEnabled = "Native daily step counting, distance estimation, and movement streaks.",
                privacyNote = "Step counts are read directly from local hardware registers and never leave your phone.",
                isMandatory = false,
                requirementType = "Required for Step Tracking"
            )
            PERM_HEALTH_CONNECT -> PermissionDetail(
                key = PERM_HEALTH_CONNECT,
                title = "Google Health Connect",
                shortName = "Health Data Sync",
                icon = Icons.Default.Favorite,
                whyNeeded = "Connects to Android's native Health Connect platform to sync steps, active minutes, and calorie estimates across your health apps.",
                functionalityEnabled = "Two-way health syncing with Google Fit, Fitbit, Samsung Health, and other compatible apps.",
                privacyNote = "Read-only access to selected health metrics. All health data remains strictly in your device's Health Connect encrypted storage.",
                isMandatory = false,
                requirementType = "Optional Health Integration"
            )
            PERM_BATTERY_OPTIMIZATION -> PermissionDetail(
                key = PERM_BATTERY_OPTIMIZATION,
                title = "Battery Unrestricted Mode",
                shortName = "Background Execution",
                icon = Icons.Default.BatteryChargingFull,
                whyNeeded = "Exempts tabzero from aggressive OEM battery killers and Android Doze restrictions that might otherwise freeze background step counting or detox timer services.",
                functionalityEnabled = "Uninterrupted timer countdowns, reliable step counting, and continuous habit tracking.",
                privacyNote = "Only allows local background processing on your device; does not affect network activity or battery telemetry.",
                isMandatory = false,
                requirementType = "Recommended for Background Reliability"
            )
            else -> PermissionDetail(
                key = key,
                title = "System Permission",
                shortName = "System Feature",
                icon = Icons.Default.Security,
                whyNeeded = "Required for specific tabzero functionality.",
                functionalityEnabled = "Enables requested feature.",
                privacyNote = "All information stays locally on your device.",
                isMandatory = false,
                requirementType = "Optional Feature"
            )
        }
    }
}
