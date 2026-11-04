package com.example.detoxproject.data.integrations.enforcement

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.detoxproject.ui.screens.intervention.ZenInterventionActivity

/**
 * High-reliability accessibility service for tabzero. detox enforcement.
 * Intercepts foreground application window changes with 0ms latency.
 * Being an AccessibilityService, Android grants it authority to launch
 * ZenInterventionActivity over third-party applications without background activity restrictions.
 */
class DetoxAccessibilityService : AccessibilityService() {

    companion object {
        var isServiceActive: Boolean = false
            private set

        private var restrictedPackages: Set<String> = emptySet()
        private var sessionTitle: String = "Focus Session"
        private var targetMinutes: Int = 25
        private var startTimeMs: Long = 0L
        private var isDetoxActive: Boolean = false
        private var lastInterventionTimeMs: Long = 0L

        fun updateRestrictedConfig(
            active: Boolean,
            packages: Set<String>,
            title: String = "Focus Session",
            targetMins: Int = 25,
            startTime: Long = System.currentTimeMillis()
        ) {
            isDetoxActive = active
            restrictedPackages = packages.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
            sessionTitle = title
            targetMinutes = targetMins
            startTimeMs = startTime
        }

        fun stopDetox() {
            isDetoxActive = false
            restrictedPackages = emptySet()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceActive = true
        serviceInfo = serviceInfo?.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 80
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !isDetoxActive || restrictedPackages.isEmpty()) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkgName = event.packageName?.toString() ?: return
        val target = pkgName.trim().lowercase()

        // Ignore tabzero itself, android OS, system UI, launchers, input methods
        if (target == packageName.lowercase() ||
            target == "android" ||
            target.contains("systemui") ||
            target.contains("launcher") ||
            target.contains("keyguard") ||
            target == "com.android.settings" ||
            target == "com.google.android.inputmethod.latin"
        ) {
            return
        }

        // Strict restriction check: only packages explicitly configured by the user
        val isRestricted = restrictedPackages.any { r ->
            val clean = r.trim().lowercase()
            target == clean || (clean.isNotBlank() && target.startsWith("$clean."))
        }

        val now = System.currentTimeMillis()
        if (isRestricted && (!ZenInterventionActivity.isActivityActive || (now - lastInterventionTimeMs > 1200L))) {
            lastInterventionTimeMs = now

            // Immediately launch ZenInterventionActivity directly to front
            val targetIntent = Intent(this, ZenInterventionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("EXTRA_RESTRICTED_APP", pkgName)
                putExtra("EXTRA_SESSION_TITLE", sessionTitle)
                putExtra("EXTRA_TARGET_MINUTES", targetMinutes)
                putExtra("EXTRA_START_TIME", startTimeMs)
            }
            startActivity(targetIntent)
        }
    }

    override fun onInterrupt() {
        // Required callback
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceActive = false
    }
}
