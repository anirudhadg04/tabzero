package com.example.detoxproject.data.integrations.enforcement

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.detoxproject.MainActivity
import com.example.detoxproject.data.integrations.notifications.ActiveDetoxNotificationManager
import com.example.detoxproject.data.integrations.usage.UsageStatsHelper
import com.example.detoxproject.data.local.entity.DetoxSessionEntity
import com.example.detoxproject.ui.screens.intervention.ZenInterventionActivity
import kotlinx.coroutines.*

class DetoxEnforcementService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isMonitoring = false
    private var lastInterventionTimeMs = 0L

    companion object {
        const val ACTION_START_MONITORING = "com.example.detoxproject.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.example.detoxproject.STOP_MONITORING"
        const val EXTRA_RESTRICTED_PACKAGES = "restricted_packages"
        const val EXTRA_SESSION_TITLE = "session_title"
        const val EXTRA_TARGET_MINUTES = "target_minutes"
        const val EXTRA_START_TIME = "start_time"

        private const val CHANNEL_ID = ActiveDetoxNotificationManager.CHANNEL_ID
        private const val NOTIFICATION_ID = ActiveDetoxNotificationManager.ONGOING_NOTIFICATION_ID
        const val INTERVENTION_NOTIFICATION_ID = 2002

        var isServiceRunning = false
            private set

        fun start(context: Context, packages: List<String>, title: String, targetMins: Int, startTime: Long) {
            val intent = Intent(context, DetoxEnforcementService::class.java).apply {
                action = ACTION_START_MONITORING
                putStringArrayListExtra(EXTRA_RESTRICTED_PACKAGES, ArrayList(packages))
                putExtra(EXTRA_SESSION_TITLE, title)
                putExtra(EXTRA_TARGET_MINUTES, targetMins)
                putExtra(EXTRA_START_TIME, startTime)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, DetoxEnforcementService::class.java).apply {
                action = ACTION_STOP_MONITORING
            }
            context.startService(intent)
        }
    }

    private var restrictedPackages = setOf<String>()
    private var sessionTitle = "Focus Session"
    private var targetMinutes = 25
    private var startTimeMs = System.currentTimeMillis()

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_MONITORING -> {
                stopMonitoring()
                DetoxAccessibilityService.stopDetox()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
                isServiceRunning = false
                return START_NOT_STICKY
            }
            ACTION_START_MONITORING -> {
                val pkgs = intent.getStringArrayListExtra(EXTRA_RESTRICTED_PACKAGES) ?: arrayListOf()
                val cleanPkgs = pkgs.map { it.trim().lowercase() }.filter { it.isNotBlank() }
                restrictedPackages = cleanPkgs.toSet()
                sessionTitle = intent.getStringExtra(EXTRA_SESSION_TITLE) ?: "Focus Session"
                targetMinutes = intent.getIntExtra(EXTRA_TARGET_MINUTES, 25)
                startTimeMs = intent.getLongExtra(EXTRA_START_TIME, System.currentTimeMillis())

                // Sync with high-reliability Accessibility Service for 0ms latency interception
                DetoxAccessibilityService.updateRestrictedConfig(
                    active = true,
                    packages = restrictedPackages,
                    title = sessionTitle,
                    targetMins = targetMinutes,
                    startTime = startTimeMs
                )

                startForeground(NOTIFICATION_ID, buildForegroundNotification())
                startMonitoringLoop()
            }
        }
        return START_STICKY
    }

    private fun startMonitoringLoop() {
        if (isMonitoring) return
        isMonitoring = true

        serviceScope.launch {
            while (isActive && isMonitoring) {
                delay(300) // Fast 300ms polling to intercept restricted apps immediately
                try {
                    val foregroundPkg = UsageStatsHelper.getForegroundPackage(this@DetoxEnforcementService)
                    if (foregroundPkg != null && foregroundPkg != packageName) {
                        val isRestricted = isPackageRestricted(foregroundPkg)
                        val now = System.currentTimeMillis()
                        // Intercept immediately if Zen intervention is not active or after short debounce
                        if (isRestricted && (!ZenInterventionActivity.isActivityActive || now - lastInterventionTimeMs > 1000L)) {
                            lastInterventionTimeMs = now
                            launchZenIntervention(foregroundPkg)
                        }
                    }
                } catch (e: Exception) {
                    // Safety check
                }
            }
        }
    }

    private fun isPackageRestricted(pkg: String): Boolean {
        val target = pkg.trim().lowercase()
        if (target == packageName.lowercase() || target == "android" || isSystemUiOrLauncher(target)) {
            return false
        }
        return restrictedPackages.any { r ->
            val clean = r.trim().lowercase()
            target == clean || (clean.isNotBlank() && target.startsWith("$clean."))
        }
    }

    private fun isSystemUiOrLauncher(pkg: String): Boolean {
        return pkg.contains("launcher") ||
                pkg.contains("systemui") ||
                pkg.contains("keyguard") ||
                pkg == "com.google.android.inputmethod.latin" ||
                pkg == "com.android.settings"
    }

    private fun launchZenIntervention(restrictedPkg: String) {
        val targetIntent = Intent(this, ZenInterventionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_RESTRICTED_APP", restrictedPkg)
            putExtra("EXTRA_SESSION_TITLE", sessionTitle)
            putExtra("EXTRA_TARGET_MINUTES", targetMinutes)
            putExtra("EXTRA_START_TIME", startTimeMs)
        }

        // Method 1: Start activity directly
        try {
            startActivity(targetIntent)
        } catch (_: Exception) {}

        // Method 2: High Priority Full-Screen Intent Notification
        // Ensures Android OS automatically pops ZenInterventionActivity over third-party apps
        try {
            val pendingIntent = PendingIntent.getActivity(
                this,
                9999,
                targetIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val channelId = "tabzero_intervention_channel"
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "tabzero. Detox Intervention",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Intercepts restricted apps during active digital detox"
                    setSound(null, null)
                    enableVibration(true)
                }
                notificationManager?.createNotificationChannel(channel)
            }

            val appLabel = try {
                val appInfo = packageManager.getApplicationInfo(restrictedPkg, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                restrictedPkg.substringAfterLast('.')
            }

            val notification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Digital Detox Active")
                .setContentText("$appLabel is restricted. Tap to return to Zen.")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager?.notify(INTERVENTION_NOTIFICATION_ID, notification)
        } catch (_: Exception) {}
    }

    private fun stopMonitoring() {
        isMonitoring = false
        serviceScope.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        stopMonitoring()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "tabzero. Active Detox Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors restricted apps while digital detox is active"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val session = DetoxSessionEntity(
            title = sessionTitle,
            targetMinutes = targetMinutes,
            startTime = startTimeMs,
            status = "ACTIVE",
            sessionDate = ""
        )
        return ActiveDetoxNotificationManager.buildNotification(this, session)
    }
}
