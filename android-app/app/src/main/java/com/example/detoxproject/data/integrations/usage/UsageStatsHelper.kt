package com.example.detoxproject.data.integrations.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import java.time.LocalDate
import java.time.ZoneId

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val totalTimeInForegroundMs: Long,
    val category: String // Social, Entertainment, Messengers, Games, Other
)

data class DailyUsageSummary(
    val totalScreenTimeMs: Long,
    val topApps: List<AppUsageInfo>,
    val socialTimeMs: Long,
    val entertainmentTimeMs: Long,
    val hasPermission: Boolean,
    val lastNightRestMinutes: Long? = null
)

object UsageStatsHelper {

    private val IGNORED_PACKAGES = setOf(
        "com.android.systemui",
        "android",
        "com.google.android.permissioncontroller",
        "com.google.android.apps.nexuslauncher",
        "com.android.launcher",
        "com.google.android.inputmethod.latin",
        "com.google.android.apps.turbo"
    )

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun createUsageAccessIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Calculates accurate today's screen time (from 00:00:00 local time today to now)
     * matching Android's Digital Wellbeing by evaluating exact activity lifecycle events.
     */
    fun getDailyUsageSummary(context: Context): DailyUsageSummary {
        if (!hasUsageStatsPermission(context)) {
            return DailyUsageSummary(
                totalScreenTimeMs = 0L,
                topApps = emptyList(),
                socialTimeMs = 0L,
                entertainmentTimeMs = 0L,
                hasPermission = false
            )
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return DailyUsageSummary(0L, emptyList(), 0L, 0L, false)

        val packageManager = context.packageManager
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()

        val appForegroundTimes = mutableMapOf<String, Long>()
        val appStartTimes = mutableMapOf<String, Long>()

        var lastNightRestMins: Long? = null

        try {
            // Query exact usage events for today, starting slightly before midnight to catch sessions running across midnight
            val queryWindowStart = startOfDay - (2 * 3600 * 1000L)
            val events = usageStatsManager.queryEvents(queryWindowStart, now)
            val event = UsageEvents.Event()

            var firstUserEventTime: Long? = null
            var screenIsInteractive = true

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName ?: continue
                val time = event.timeStamp

                if (isIgnoredPackage(pkg)) continue

                if (firstUserEventTime == null && time >= startOfDay && event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    firstUserEventTime = time
                }

                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        // Track when app entered foreground
                        appStartTimes[pkg] = time
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED,
                    UsageEvents.Event.ACTIVITY_STOPPED -> {
                        val startTime = appStartTimes.remove(pkg)
                        if (startTime != null) {
                            val effStart = maxOf(startTime, startOfDay)
                            val effEnd = minOf(time, now)
                            if (effEnd > effStart) {
                                val duration = effEnd - effStart
                                appForegroundTimes[pkg] = (appForegroundTimes[pkg] ?: 0L) + duration
                            }
                        }
                    }
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                        screenIsInteractive = false
                        // Screen locked: close open sessions up to event time
                        for ((activePkg, startTime) in appStartTimes) {
                            val effStart = maxOf(startTime, startOfDay)
                            val effEnd = minOf(time, now)
                            if (effEnd > effStart) {
                                val duration = effEnd - effStart
                                appForegroundTimes[activePkg] = (appForegroundTimes[activePkg] ?: 0L) + duration
                            }
                        }
                        appStartTimes.clear()
                    }
                    UsageEvents.Event.SCREEN_INTERACTIVE -> {
                        screenIsInteractive = true
                    }
                }
            }

            // Close any app session currently active right now
            if (screenIsInteractive) {
                for ((pkg, startTime) in appStartTimes) {
                    val effStart = maxOf(startTime, startOfDay)
                    val effEnd = now
                    if (effEnd > effStart) {
                        val duration = effEnd - effStart
                        appForegroundTimes[pkg] = (appForegroundTimes[pkg] ?: 0L) + duration
                    }
                }
            }

            // Detect overnight rest window:
            // Query 24-hour events to identify the longest period of user inactivity during the night
            val past24hEvents = usageStatsManager.queryEvents(now - (24 * 3600 * 1000L), now)
            var prevUserActivityTime: Long? = null
            var maxOvernightInactivityMs = 0L
            val restEvent = UsageEvents.Event()

            while (past24hEvents.hasNextEvent()) {
                past24hEvents.getNextEvent(restEvent)
                val pkg = restEvent.packageName ?: continue
                if (isIgnoredPackage(pkg)) continue

                if (restEvent.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                    restEvent.eventType == UsageEvents.Event.SCREEN_INTERACTIVE) {
                    val t = restEvent.timeStamp
                    if (prevUserActivityTime != null) {
                        val gap = t - prevUserActivityTime
                        // Sleep window typically spans 3.5 to 13 hours
                        if (gap > maxOvernightInactivityMs && gap in (210 * 60 * 1000L)..(780 * 60 * 1000L)) {
                            maxOvernightInactivityMs = gap
                        }
                    }
                    prevUserActivityTime = t
                }
            }

            if (maxOvernightInactivityMs > 0) {
                lastNightRestMins = maxOvernightInactivityMs / (1000 * 60)
            }
        } catch (e: Exception) {
            // Graceful fallback to queryUsageStats
        }

        // Fallback or sanity check: if events yielded 0, fall back to queryAndAggregateUsageStats filtered to today
        if (appForegroundTimes.isEmpty()) {
            try {
                val aggregated = usageStatsManager.queryAndAggregateUsageStats(startOfDay, now)
                val elapsedToday = (now - startOfDay).coerceAtLeast(1L)
                for ((pkg, stats) in aggregated) {
                    if (isIgnoredPackage(pkg)) continue
                    if (stats.lastTimeUsed >= startOfDay && stats.totalTimeInForeground > 0L) {
                        val validTime = stats.totalTimeInForeground.coerceAtMost(elapsedToday)
                        if (validTime > 0L) {
                            appForegroundTimes[pkg] = validTime
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        val elapsedToday = (now - startOfDay).coerceAtLeast(1L)
        var totalMs = 0L
        var socialMs = 0L
        var entertainmentMs = 0L
        val appList = mutableListOf<AppUsageInfo>()

        for ((pkg, rawForegroundTime) in appForegroundTimes) {
            val foregroundTime = rawForegroundTime.coerceAtMost(elapsedToday)
            if (foregroundTime <= 0L) continue
            totalMs += foregroundTime

            val category = categorizePackage(pkg)
            if (category == "Social") socialMs += foregroundTime
            if (category == "Entertainment") entertainmentMs += foregroundTime

            if (foregroundTime >= 10_000L) {
                val appName = try {
                    val appInfo = packageManager.getApplicationInfo(pkg, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    pkg.substringAfterLast('.')
                }

                appList.add(
                    AppUsageInfo(
                        packageName = pkg,
                        appName = appName,
                        totalTimeInForegroundMs = foregroundTime,
                        category = category
                    )
                )
            }
        }

        // Cap totalMs to the elapsed milliseconds since midnight today
        totalMs = totalMs.coerceAtMost(elapsedToday)

        val sorted = appList.sortedByDescending { it.totalTimeInForegroundMs }.take(10)

        return DailyUsageSummary(
            totalScreenTimeMs = totalMs,
            topApps = sorted,
            socialTimeMs = socialMs,
            entertainmentTimeMs = entertainmentMs,
            hasPermission = true,
            lastNightRestMinutes = lastNightRestMins
        )
    }

    private fun isIgnoredPackage(pkg: String): Boolean {
        val lower = pkg.lowercase()
        return IGNORED_PACKAGES.contains(lower) ||
                lower.contains("launcher") ||
                lower.contains("keyguard") ||
                lower == "com.android.systemui"
    }

    /**
     * Retrieves the package name currently in the foreground by querying recent usage events.
     */
    fun getForegroundPackage(context: Context): String? {
        if (!hasUsageStatsPermission(context)) return null
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null

        val now = System.currentTimeMillis()
        var lastResumedPkg: String? = null
        var lastEventTime = 0L

        // 1. Query events in the last 60 seconds to detect active foreground transitions
        try {
            val events = usageStatsManager.queryEvents(now - 60_000L, now)
            if (events != null) {
                val event = UsageEvents.Event()
                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    val pkg = event.packageName ?: continue
                    if (isIgnoredPackage(pkg)) continue

                    if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED && event.timeStamp >= lastEventTime) {
                        lastResumedPkg = pkg
                        lastEventTime = event.timeStamp
                    }
                }
            }
        } catch (_: Exception) {}

        if (lastResumedPkg != null) {
            return lastResumedPkg
        }

        // 2. Fallback: Query recent usage stats from INTERVAL_BEST
        try {
            val statsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                now - 60_000L,
                now
            )
            val mostRecent = statsList
                ?.filter { !isIgnoredPackage(it.packageName) && it.totalTimeInForeground > 0L }
                ?.maxByOrNull { it.lastTimeUsed }

            if (mostRecent != null && (now - mostRecent.lastTimeUsed) < 30_000L) {
                return mostRecent.packageName
            }
        } catch (_: Exception) {}

        return null
    }

    fun categorizePackage(packageName: String): String {
        val lower = packageName.lowercase()
        return when {
            lower.contains("instagram") || lower.contains("facebook") || lower.contains("twitter") ||
            lower.contains("tiktok") || lower.contains("snapchat") || lower.contains("threads") ||
            lower.contains("reddit") || lower.contains("pinterest") -> "Social"

            lower.contains("youtube") || lower.contains("netflix") || lower.contains("twitch") ||
            lower.contains("primevideo") || lower.contains("disney") || lower.contains("hotstar") -> "Entertainment"

            lower.contains("whatsapp") || lower.contains("telegram") || lower.contains("discord") ||
            lower.contains("signal") || lower.contains("viber") || lower.contains("messenger") -> "Messengers"

            lower.contains("game") || lower.contains("pubg") || lower.contains("candycrush") ||
            lower.contains("roblox") || lower.contains("clash") -> "Games"

            else -> "Other"
        }
    }
}
