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
import com.example.detoxproject.data.integrations.notifications.ActiveDetoxNotificationManager
import com.example.detoxproject.data.integrations.notifications.DetoxAlarmScheduler
import com.example.detoxproject.data.integrations.notifications.SessionAlarmManager
import com.example.detoxproject.data.integrations.usage.UsageStatsHelper
import com.example.detoxproject.data.local.OfflineDatabase
import com.example.detoxproject.data.local.entity.DetoxSessionEntity
import com.example.detoxproject.domain.engine.DetoxTimerEngine
import com.example.detoxproject.ui.screens.intervention.ZenInterventionActivity
import kotlinx.coroutines.*

class DetoxEnforcementService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isMonitoring = false
    private var lastInterventionTimeMs = 0L
    private var currentSessionId = 0L

    companion object {
        const val ACTION_START_MONITORING = "com.example.detoxproject.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.example.detoxproject.STOP_MONITORING"
        const val EXTRA_RESTRICTED_PACKAGES = "restricted_packages"
        const val EXTRA_SESSION_TITLE = "session_title"
        const val EXTRA_TARGET_MINUTES = "target_minutes"
        const val EXTRA_START_TIME = "start_time"
        const val EXTRA_SESSION_ID = "session_id"

        private const val CHANNEL_ID = ActiveDetoxNotificationManager.CHANNEL_ID
        private const val NOTIFICATION_ID = ActiveDetoxNotificationManager.ONGOING_NOTIFICATION_ID
        const val INTERVENTION_NOTIFICATION_ID = 2002

        var isServiceRunning = false
            private set

        fun start(
            context: Context,
            packages: List<String>,
            title: String,
            targetMins: Int,
            startTime: Long,
            sessionId: Long = 0L
        ) {
            val intent = Intent(context, DetoxEnforcementService::class.java).apply {
                action = ACTION_START_MONITORING
                putStringArrayListExtra(EXTRA_RESTRICTED_PACKAGES, ArrayList(packages))
                putExtra(EXTRA_SESSION_TITLE, title)
                putExtra(EXTRA_TARGET_MINUTES, targetMins)
                putExtra(EXTRA_START_TIME, startTime)
                putExtra(EXTRA_SESSION_ID, sessionId)
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
        if (intent == null) {
            // Android OS restarted the service after process death (START_STICKY)
            // Immediately satisfy Android's foreground service start requirement
            startForeground(NOTIFICATION_ID, buildForegroundNotification())
            recoverActiveSession()
            return START_STICKY
        }

        when (intent.action) {
            ACTION_STOP_MONITORING -> {
                stopMonitoring()
                DetoxAccessibilityService.stopDetox()
                if (currentSessionId != 0L) {
                    DetoxAlarmScheduler.cancelSessionCompletionAlarm(this, currentSessionId)
                }
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
                currentSessionId = intent.getLongExtra(EXTRA_SESSION_ID, 0L)

                // Sync with high-reliability Accessibility Service for 0ms latency interception
                DetoxAccessibilityService.updateRestrictedConfig(
                    active = true,
                    packages = restrictedPackages,
                    title = sessionTitle,
                    targetMins = targetMinutes,
                    startTime = startTimeMs
                )

                val session = DetoxSessionEntity(
                    id = currentSessionId,
                    title = sessionTitle,
                    targetMinutes = targetMinutes,
                    startTime = startTimeMs,
                    status = "ACTIVE",
                    restrictedPackages = cleanPkgs.joinToString(","),
                    sessionDate = ""
                )

                // Schedule exact alarm with AlarmManager
                if (currentSessionId != 0L) {
                    DetoxAlarmScheduler.scheduleSessionCompletionAlarm(this, session)
                }

                startForeground(NOTIFICATION_ID, buildForegroundNotification())
                startMonitoringLoop()
            }
        }
        return START_STICKY
    }

    private fun recoverActiveSession() {
        serviceScope.launch {
            try {
                val db = OfflineDatabase.getInstance(this@DetoxEnforcementService)
                val activeSession = db.detoxDao().getAnyActiveSessionDirect()

                if (activeSession == null || activeSession.status != "ACTIVE") {
                    // No active session requires enforcement
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
                    return@launch
                }

                val now = System.currentTimeMillis()
                val remainingMs = DetoxTimerEngine.calculateRemainingMs(activeSession, now)

                if (remainingMs <= 0L) {
                    // Session expired while process was dead: mark COMPLETED safely
                    val completed = activeSession.copy(
                        status = "COMPLETED",
                        actualMinutes = activeSession.targetMinutes,
                        endTime = DetoxTimerEngine.calculateExpectedEndTimeMs(activeSession)
                    )
                    db.detoxDao().updateSession(completed)

                    ActiveDetoxNotificationManager.cancelNotification(this@DetoxEnforcementService)
                    SessionAlarmManager.triggerAlarm(
                        context = this@DetoxEnforcementService,
                        sessionId = activeSession.id,
                        sessionTitle = activeSession.title,
                        durationMinutes = activeSession.targetMinutes
                    )

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
                    return@launch
                }

                // Restore active session state
                currentSessionId = activeSession.id
                sessionTitle = activeSession.title.ifBlank { "Focus Session" }
                targetMinutes = activeSession.targetMinutes
                startTimeMs = activeSession.startTime
                val cleanPkgs = activeSession.restrictedPackages.split(',')
                    .map { it.trim().lowercase() }
                    .filter { it.isNotBlank() }
                restrictedPackages = cleanPkgs.toSet()

                // Sync with Accessibility Service
                DetoxAccessibilityService.updateRestrictedConfig(
                    active = true,
                    packages = restrictedPackages,
                    title = sessionTitle,
                    targetMins = targetMinutes,
                    startTime = startTimeMs
                )

                // Re-schedule exact alarm with AlarmManager
                DetoxAlarmScheduler.scheduleSessionCompletionAlarm(this@DetoxEnforcementService, activeSession)

                // Update notification with accurate countdown chronometer
                val notification = ActiveDetoxNotificationManager.buildNotification(this@DetoxEnforcementService, activeSession)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.notify(NOTIFICATION_ID, notification)

                startMonitoringLoop()
            } catch (e: Exception) {
                // Safeguard against DB or OS exceptions
            }
        }
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
            id = currentSessionId,
            title = sessionTitle,
            targetMinutes = targetMinutes,
            startTime = startTimeMs,
            status = "ACTIVE",
            sessionDate = ""
        )
        return ActiveDetoxNotificationManager.buildNotification(this, session)
    }
}
