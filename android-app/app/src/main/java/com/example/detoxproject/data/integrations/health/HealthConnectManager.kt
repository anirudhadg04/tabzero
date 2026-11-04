package com.example.detoxproject.data.integrations.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Health Connect status descriptor for runtime capability detection.
 */
enum class HealthConnectSdkStatus {
    AVAILABLE,
    UPDATE_REQUIRED,
    UNAVAILABLE
}

data class HealthConnectTodayData(
    val steps: Long = 0L,
    val distanceMeters: Double = 0.0,
    val activeCaloriesKcal: Double = 0.0,
    val hasData: Boolean = false
)

data class ExternalHealthMetrics(
    val latestHeartRateBpm: Int? = null,
    val heartRateRecordedAt: Long? = null,
    val lastSleepDurationMinutes: Long? = null,
    val sleepEndTime: Long? = null,
    val sleepSourceDescription: String? = null,
    val todayActiveCaloriesKcal: Float? = null,
    val isHealthConnectAvailable: Boolean = false,
    val arePermissionsGranted: Boolean = false,
    val dailySleepMinutes: Map<String, Long> = emptyMap()
)

data class ExternalExerciseItem(
    val title: String,
    val exerciseType: String,
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Long
)

class HealthConnectManager(private val context: Context) {

    private val healthConnectClient: HealthConnectClient? by lazy {
        try {
            if (getSdkStatus() == HealthConnectSdkStatus.AVAILABLE) {
                HealthConnectClient.getOrCreate(context)
            } else {
                null
            }
        } catch (e: Throwable) {
            null
        }
    }

    companion object {
        val REQUIRED_PERMISSIONS = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class)
        )

        const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"
        const val HEALTH_CONNECT_SYSTEM_PACKAGE = "com.google.android.healthconnect.controller"
    }

    /**
     * Checks whether Health Connect is supported, installed, or needs an update on this device.
     */
    fun getSdkStatus(): HealthConnectSdkStatus {
        return try {
            when (HealthConnectClient.getSdkStatus(context)) {
                HealthConnectClient.SDK_AVAILABLE -> HealthConnectSdkStatus.AVAILABLE
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectSdkStatus.UPDATE_REQUIRED
                else -> HealthConnectSdkStatus.UNAVAILABLE
            }
        } catch (e: Throwable) {
            HealthConnectSdkStatus.UNAVAILABLE
        }
    }

    /**
     * Checks if all required read permissions have been granted by the user.
     */
    suspend fun hasAllPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            granted.containsAll(REQUIRED_PERMISSIONS)
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Checks if at least the core steps/distance permissions are granted.
     */
    suspend fun hasBasicStepPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            granted.contains(HealthPermission.getReadPermission(StepsRecord::class))
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Returns an intent to open Health Connect settings or Google Play Store to install it.
     */
    fun createSettingsOrInstallIntent(): Intent {
        val status = getSdkStatus()
        if (status == HealthConnectSdkStatus.UPDATE_REQUIRED || status == HealthConnectSdkStatus.UNAVAILABLE) {
            return Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$HEALTH_CONNECT_PACKAGE")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }

        // On Android 14+ (API 34), Health Connect is a system component in Settings
        val systemIntent = Intent("android.health.connect.action.HEALTH_HOME_SETTINGS").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (systemIntent.resolveActivity(context.packageManager) != null) {
            return systemIntent
        }

        // Android 13 or standalone Health Connect app
        val legacyIntent = Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return if (legacyIntent.resolveActivity(context.packageManager) != null) {
            legacyIntent
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$HEALTH_CONNECT_PACKAGE")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }

    /**
     * Query today's aggregate steps, distance in meters, and active calories.
     * Guaranteed safe: returns empty data rather than throwing if unavailable or permissions denied.
     */
    suspend fun readTodayAggregate(): HealthConnectTodayData {
        val client = healthConnectClient ?: return HealthConnectTodayData()
        return try {
            val zone = ZoneId.systemDefault()
            val todayStart = LocalDate.now().atStartOfDay(zone).toInstant()
            val now = Instant.now()
            val timeFilter = TimeRangeFilter.between(todayStart, now)

            var steps = 0L
            var distanceMeters = 0.0
            try {
                val stepResponse = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(
                            StepsRecord.COUNT_TOTAL,
                            DistanceRecord.DISTANCE_TOTAL
                        ),
                        timeRangeFilter = timeFilter
                    )
                )
                steps = stepResponse[StepsRecord.COUNT_TOTAL] ?: 0L
                distanceMeters = stepResponse[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
            } catch (e: Throwable) {
                try {
                    val fallbackStep = client.aggregate(
                        AggregateRequest(
                            metrics = setOf(StepsRecord.COUNT_TOTAL),
                            timeRangeFilter = timeFilter
                        )
                    )
                    steps = fallbackStep[StepsRecord.COUNT_TOTAL] ?: 0L
                } catch (ignored: Throwable) {}
            }

            var bestCalories = 0.0
            try {
                val calResponse = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(
                            ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                            TotalCaloriesBurnedRecord.ENERGY_TOTAL
                        ),
                        timeRangeFilter = timeFilter
                    )
                )
                val activeCalories = calResponse[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories
                val totalCalories = calResponse[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories
                bestCalories = activeCalories ?: totalCalories ?: 0.0
            } catch (e: Throwable) {
                try {
                    val singleCal = client.aggregate(
                        AggregateRequest(
                            metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                            timeRangeFilter = timeFilter
                        )
                    )
                    bestCalories = singleCal[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0
                } catch (ignored: Throwable) {}
            }

            HealthConnectTodayData(
                steps = steps,
                distanceMeters = distanceMeters,
                activeCaloriesKcal = bestCalories,
                hasData = steps > 0 || distanceMeters > 0.0 || bestCalories > 0.0
            )
        } catch (e: Throwable) {
            HealthConnectTodayData()
        }
    }

    /**
     * Query external health metrics: recent heart rate, last sleep duration, and active calories.
     * Never fabricates fake metrics. If no records exist, values remain null.
     */
    suspend fun readExternalHealthMetrics(): ExternalHealthMetrics {
        val isAvailable = getSdkStatus() == HealthConnectSdkStatus.AVAILABLE
        val client = healthConnectClient
        if (!isAvailable || client == null) {
            return ExternalHealthMetrics(
                isHealthConnectAvailable = false,
                arePermissionsGranted = false
            )
        }

        val hasPerms = hasAllPermissions()
        if (!hasPerms) {
            return ExternalHealthMetrics(
                isHealthConnectAvailable = true,
                arePermissionsGranted = false
            )
        }

        return try {
            val zone = ZoneId.systemDefault()
            val now = Instant.now()
            val todayStart = LocalDate.now().atStartOfDay(zone).toInstant()
            val past24Hours = now.minus(24, ChronoUnit.HOURS)

            // 1. Heart Rate (most recent within past 24 hours)
            var latestHr: Int? = null
            var hrTime: Long? = null
            try {
                val hrResponse = client.readRecords(
                    ReadRecordsRequest(
                        recordType = HeartRateRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(past24Hours, now),
                        ascendingOrder = false,
                        pageSize = 3
                    )
                )
                val mostRecentRecord = hrResponse.records.firstOrNull()
                val latestSample = mostRecentRecord?.samples?.lastOrNull()
                if (latestSample != null) {
                    latestHr = latestSample.beatsPerMinute.toInt()
                    hrTime = latestSample.time.toEpochMilli()
                }
            } catch (e: Throwable) {
                // Ignore and keep null
            }

            // 2. Sleep (most recent within past 7 days + daily history map)
            var sleepMinutes: Long? = null
            var sleepEnd: Long? = null
            var sleepSource: String? = null
            val sleepHistoryMap = mutableMapOf<String, Long>()
            try {
                val past7Days = now.minus(7, ChronoUnit.DAYS)
                val sleepResponse = client.readRecords(
                    ReadRecordsRequest(
                        recordType = SleepSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(past7Days, now),
                        ascendingOrder = false,
                        pageSize = 10
                    )
                )
                for (record in sleepResponse.records) {
                    val dateKey = record.endTime.atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val dur = ChronoUnit.MINUTES.between(record.startTime, record.endTime)
                    if (dur > 0 && !sleepHistoryMap.containsKey(dateKey)) {
                        sleepHistoryMap[dateKey] = dur
                    }
                }
                val session = sleepResponse.records.firstOrNull()
                if (session != null) {
                    val durationMins = ChronoUnit.MINUTES.between(session.startTime, session.endTime)
                    if (durationMins > 0) {
                        sleepMinutes = durationMins
                        sleepEnd = session.endTime.toEpochMilli()
                        sleepSource = "Health Connect Sleep"
                    }
                }
            } catch (e: Throwable) {
                // Ignore and keep null
            }

            // 3. Active or Total calories burned today
            var caloriesBurned: Float? = null
            try {
                val calResponse = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(
                            ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                            TotalCaloriesBurnedRecord.ENERGY_TOTAL
                        ),
                        timeRangeFilter = TimeRangeFilter.between(todayStart, now)
                    )
                )
                val activeCal = calResponse[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories
                val totalCal = calResponse[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories
                val bestCal = activeCal ?: totalCal
                if (bestCal != null && bestCal > 0) {
                    caloriesBurned = bestCal.toFloat()
                }
            } catch (e: Throwable) {
                // Ignore and keep null
            }

            ExternalHealthMetrics(
                latestHeartRateBpm = latestHr,
                heartRateRecordedAt = hrTime,
                lastSleepDurationMinutes = sleepMinutes,
                sleepEndTime = sleepEnd,
                sleepSourceDescription = sleepSource,
                todayActiveCaloriesKcal = caloriesBurned,
                isHealthConnectAvailable = true,
                arePermissionsGranted = true,
                dailySleepMinutes = sleepHistoryMap
            )
        } catch (e: Throwable) {
            ExternalHealthMetrics(
                isHealthConnectAvailable = true,
                arePermissionsGranted = hasPerms
            )
        }
    }

    /**
     * Read external exercise sessions recorded by other apps (e.g., Pixel Watch, Fitbit, Strava) via Health Connect.
     */
    suspend fun readExternalExerciseSessions(limit: Int = 5): List<ExternalExerciseItem> {
        val client = healthConnectClient ?: return emptyList()
        return try {
            val now = Instant.now()
            val past7Days = now.minus(7, ChronoUnit.DAYS)
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(past7Days, now),
                    ascendingOrder = false,
                    pageSize = limit
                )
            )

            response.records.map { record ->
                val typeName = record.exerciseType.toString().replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                val title = record.title ?: "$typeName Session"
                val duration = ChronoUnit.MINUTES.between(record.startTime, record.endTime)
                ExternalExerciseItem(
                    title = title,
                    exerciseType = typeName,
                    startTime = record.startTime.toEpochMilli(),
                    endTime = record.endTime.toEpochMilli(),
                    durationMinutes = duration
                )
            }
        } catch (e: Throwable) {
            emptyList()
        }
    }
}
