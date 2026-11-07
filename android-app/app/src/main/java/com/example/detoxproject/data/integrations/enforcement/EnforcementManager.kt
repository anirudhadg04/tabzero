package com.example.detoxproject.data.integrations.enforcement

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import com.example.detoxproject.data.integrations.usage.UsageStatsHelper

enum class EnforcementCapability {
    STRICT_SUPPORTED,       // Provisioned as Device Owner / Profile Owner with setPackagesSuspended
    SOFT_ONLY,              // Policy-compliant soft intervention (reminders, notifications, pause UI)
    PERMISSION_REQUIRED,    // Missing Usage Access permission
    UNSUPPORTED             // Environment cannot support monitoring
}

data class EnforcementStatus(
    val capability: EnforcementCapability,
    val isActive: Boolean,
    val restrictedCount: Int,
    val allowAudio: Boolean,
    val statusDescription: String
)

class EnforcementManager(private val context: Context) {

    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    private var isEnforcementActive = false
    private var restrictedPackageList = listOf<String>()
    private var isAudioAllowed = true

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "tabzero. Detox Interventions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Policy-compliant soft reminders during active detox sessions"
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun checkCapability(): EnforcementCapability {
        val isDeviceOwner = dpm?.isDeviceOwnerApp(context.packageName) == true
        if (isDeviceOwner) {
            return EnforcementCapability.STRICT_SUPPORTED
        }

        val hasUsage = UsageStatsHelper.hasUsageStatsPermission(context)
        return if (hasUsage) {
            EnforcementCapability.SOFT_ONLY
        } else {
            EnforcementCapability.PERMISSION_REQUIRED
        }
    }

    fun configureRestrictions(packages: List<String>, allowAudio: Boolean) {
        restrictedPackageList = packages
        isAudioAllowed = allowAudio
    }

    fun startEnforcement(
        packages: List<String> = restrictedPackageList,
        title: String = "Focus Session",
        targetMins: Int = 25,
        startTime: Long = System.currentTimeMillis(),
        sessionId: Long = 0L
    ): Boolean {
        isEnforcementActive = true
        restrictedPackageList = packages
        DetoxEnforcementService.start(context, packages, title, targetMins, startTime, sessionId)
        return true
    }

    fun stopEnforcement() {
        isEnforcementActive = false
        DetoxEnforcementService.stop(context)
    }

    fun getEnforcementStatus(): EnforcementStatus {
        val cap = checkCapability()
        val desc = when (cap) {
            EnforcementCapability.STRICT_SUPPORTED -> "Strict system-level package suspension active."
            EnforcementCapability.SOFT_ONLY -> "Policy-compliant soft intervention active. Discourages digital distraction via mindful prompts."
            EnforcementCapability.PERMISSION_REQUIRED -> "Usage Access permission required to detect foreground distractions."
            EnforcementCapability.UNSUPPORTED -> "Detox monitoring unsupported on this platform."
        }

        return EnforcementStatus(
            capability = cap,
            isActive = isEnforcementActive,
            restrictedCount = restrictedPackageList.size,
            allowAudio = isAudioAllowed,
            statusDescription = desc
        )
    }

    companion object {
        private const val CHANNEL_ID = "tabzero_detox_channel"
    }
}
