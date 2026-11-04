package com.example.detoxproject.data.integrations.health

import android.Manifest
import android.os.Build
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import java.time.LocalDate

data class StepSensorStatus(
    val hasHardwareSensor: Boolean,
    val hasPermission: Boolean,
    val stepsToday: Int,
    val estimatedDistanceKm: Float
)

/**
 * Native hardware step counter manager.
 * Uses Android's Sensor.TYPE_STEP_COUNTER with genuine step counting,
 * automatic midnight rollover, and device reboot detection.
 * Never fabricates or fakes step counts.
 */
class StepSensorHelper(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val stepSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val prefs = context.getSharedPreferences("tabzero_step_tracker_v2", Context.MODE_PRIVATE)

    var onStepsUpdated: ((todaySteps: Int, rawTotal: Int) -> Unit)? = null

    companion object {
        private const val KEY_DATE = "last_tracked_date"
        private const val KEY_BASELINE = "day_baseline_steps"
        private const val KEY_LAST_RAW = "last_raw_sensor_total"
        private const val KEY_TODAY_STEPS = "today_steps_count"
        private const val DEFAULT_STRIDE_METERS = 0.75f
    }

    fun hasHardwareSensor(): Boolean = stepSensor != null

    fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun startListening() {
        if (hasPermission() && stepSensor != null) {
            try {
                sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
            } catch (e: Throwable) {
                // Ignore gracefully if sensor registration fails on particular OEM
            }
        }
    }

    fun stopListening() {
        try {
            sensorManager?.unregisterListener(this)
        } catch (e: Throwable) {
            // Ignore
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return
        val rawTotal = event.values.firstOrNull()?.toInt() ?: return
        if (rawTotal < 0) return

        val todayStr = LocalDate.now().toString()
        val lastDate = prefs.getString(KEY_DATE, "")
        val lastRaw = prefs.getInt(KEY_LAST_RAW, -1)
        var baseline = prefs.getInt(KEY_BASELINE, -1)
        var todaySteps = prefs.getInt(KEY_TODAY_STEPS, 0)

        // Case 1: Date has rolled over (new day)
        if (lastDate != todayStr) {
            // Baseline is now the current raw sensor value for the beginning of today
            baseline = rawTotal
            todaySteps = 0
            prefs.edit()
                .putString(KEY_DATE, todayStr)
                .putInt(KEY_BASELINE, baseline)
                .putInt(KEY_TODAY_STEPS, 0)
                .putInt(KEY_LAST_RAW, rawTotal)
                .apply()
        } else {
            // Same day: check for device reboot
            // If rawTotal < lastRaw, the device has rebooted since last event
            if (lastRaw > 0 && rawTotal < lastRaw) {
                // Device rebooted: current rawTotal starts near 0.
                // Preserve steps taken earlier today before reboot by offsetting baseline
                baseline = rawTotal - todaySteps
                prefs.edit().putInt(KEY_BASELINE, baseline).apply()
            } else if (baseline == -1) {
                // First initialization on this day
                baseline = rawTotal - todaySteps
                prefs.edit().putInt(KEY_BASELINE, baseline).apply()
            }

            todaySteps = (rawTotal - baseline).coerceAtLeast(0)
            prefs.edit()
                .putInt(KEY_TODAY_STEPS, todaySteps)
                .putInt(KEY_LAST_RAW, rawTotal)
                .apply()
        }

        onStepsUpdated?.invoke(todaySteps, rawTotal)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op for step counter
    }

    /**
     * Checks if date rollover occurred while the app was closed or sensor inactive.
     */
    private fun checkDateRollover() {
        val todayStr = LocalDate.now().toString()
        val lastDate = prefs.getString(KEY_DATE, "")
        if (!lastDate.isNullOrBlank() && lastDate != todayStr) {
            // Date changed while inactive: reset today's steps to 0
            // and baseline will be re-anchored on next sensor event
            prefs.edit()
                .putString(KEY_DATE, todayStr)
                .putInt(KEY_TODAY_STEPS, 0)
                .putInt(KEY_BASELINE, -1)
                .apply()
        }
    }

    fun getStatus(strideLengthMeters: Float = DEFAULT_STRIDE_METERS): StepSensorStatus {
        checkDateRollover()
        val currentSteps = prefs.getInt(KEY_TODAY_STEPS, 0)
        val distanceKm = (currentSteps * strideLengthMeters) / 1000f

        return StepSensorStatus(
            hasHardwareSensor = hasHardwareSensor(),
            hasPermission = hasPermission(),
            stepsToday = currentSteps,
            estimatedDistanceKm = distanceKm
        )
    }

    /**
     * Calibrates baseline using verified daily steps from Health Connect / system health.
     * Guarantees that opening the app late in the day does not lose the steps walked earlier today.
     */
    fun calibrateBaselineWithKnownSteps(knownSteps: Int) {
        if (knownSteps <= 0) return
        val current = prefs.getInt(KEY_TODAY_STEPS, 0)
        if (knownSteps > current) {
            val lastRaw = prefs.getInt(KEY_LAST_RAW, -1)
            val todayStr = LocalDate.now().toString()
            val edit = prefs.edit().putString(KEY_DATE, todayStr).putInt(KEY_TODAY_STEPS, knownSteps)
            if (lastRaw > 0) {
                edit.putInt(KEY_BASELINE, lastRaw - knownSteps)
            }
            edit.apply()
        }
    }
}
