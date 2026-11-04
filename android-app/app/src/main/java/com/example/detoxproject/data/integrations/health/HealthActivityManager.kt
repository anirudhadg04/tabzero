package com.example.detoxproject.data.integrations.health

import android.content.Context
import android.content.Intent

enum class HealthConnectStatus {
    AVAILABLE_INSTALLED,
    UPDATE_REQUIRED,
    NOT_INSTALLED,
    UNSUPPORTED_PLATFORM
}

data class PhysicalActivitySummary(
    val stepsToday: Int,
    val estimatedDistanceKm: Float,
    val sourceDescription: String,
    val healthConnectStatus: HealthConnectStatus,
    val hasActivityPermission: Boolean,
    val activeCaloriesKcal: Float = 0f,
    val isFromHealthConnect: Boolean = false
)

/**
 * Bridges health provider status and hardware step counts for tabzero.
 */
class HealthActivityManager(private val context: Context) {
    val stepHelper = StepSensorHelper(context)
    val healthConnect = HealthConnectManager(context)

    companion object {
        const val HEALTH_CONNECT_PACKAGE = HealthConnectManager.HEALTH_CONNECT_PACKAGE
        const val HEALTH_CONNECT_SYSTEM_PACKAGE = HealthConnectManager.HEALTH_CONNECT_SYSTEM_PACKAGE
        const val GOOGLE_FIT_PACKAGE = "com.google.android.apps.fitness"
        const val FITBIT_GOOGLE_HEALTH_PACKAGE = "com.fitbit.FitbitMobile"
    }

    init {
        stepHelper.startListening()
    }

    fun stopListening() {
        stepHelper.stopListening()
    }

    fun getHealthConnectStatus(): HealthConnectStatus {
        return when (healthConnect.getSdkStatus()) {
            HealthConnectSdkStatus.AVAILABLE -> HealthConnectStatus.AVAILABLE_INSTALLED
            HealthConnectSdkStatus.UPDATE_REQUIRED -> HealthConnectStatus.UPDATE_REQUIRED
            HealthConnectSdkStatus.UNAVAILABLE -> {
                if (isPackageInstalled(GOOGLE_FIT_PACKAGE) || isPackageInstalled(FITBIT_GOOGLE_HEALTH_PACKAGE)) {
                    HealthConnectStatus.AVAILABLE_INSTALLED
                } else {
                    HealthConnectStatus.NOT_INSTALLED
                }
            }
        }
    }

    private fun isPackageInstalled(pkgName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(pkgName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun createHealthConnectIntent(): Intent {
        return healthConnect.createSettingsOrInstallIntent()
    }

    fun getActivitySummary(
        knownSteps: Int? = null,
        knownDistanceKm: Float? = null,
        knownCalories: Float? = null
    ): PhysicalActivitySummary {
        val status = stepHelper.getStatus()
        val hcStatus = getHealthConnectStatus()
        val bestSteps = knownSteps ?: status.stepsToday
        val bestDistance = knownDistanceKm ?: ((bestSteps * 0.75f) / 1000f)
        val bestCalories = maxOf(knownCalories ?: 0f, bestSteps * 0.04f)

        val sourceDesc = when {
            hcStatus == HealthConnectStatus.AVAILABLE_INSTALLED -> "Google Health Connect + Android Step Sensor"
            isPackageInstalled(FITBIT_GOOGLE_HEALTH_PACKAGE) -> "Fitbit + Android Step Sensor"
            isPackageInstalled(GOOGLE_FIT_PACKAGE) -> "Google Fit + Android Step Sensor"
            status.hasHardwareSensor -> "Android Hardware Step Counter"
            else -> "Step Counter Sensor Not Available"
        }

        return PhysicalActivitySummary(
            stepsToday = bestSteps,
            estimatedDistanceKm = bestDistance,
            sourceDescription = sourceDesc,
            healthConnectStatus = hcStatus,
            hasActivityPermission = status.hasPermission,
            activeCaloriesKcal = bestCalories
        )
    }
}
