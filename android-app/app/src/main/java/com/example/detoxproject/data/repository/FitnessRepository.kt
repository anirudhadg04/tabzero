package com.example.detoxproject.data.repository

import android.content.Context
import com.example.detoxproject.data.integrations.health.ExternalExerciseItem
import com.example.detoxproject.data.integrations.health.ExternalHealthMetrics
import com.example.detoxproject.data.integrations.health.HealthConnectManager
import com.example.detoxproject.data.integrations.health.HealthConnectSdkStatus
import com.example.detoxproject.data.integrations.health.StepSensorHelper
import com.example.detoxproject.data.local.OfflineDatabase
import com.example.detoxproject.data.local.entity.DailyActivityEntity
import com.example.detoxproject.data.local.entity.ExerciseSessionEntity
import com.example.detoxproject.domain.engine.UndoRedoManager
import com.example.detoxproject.domain.engine.ZenXPManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Unified fitness and physical activity repository for tabzero.
 * Seamlessly coordinates Android Health Connect and native hardware step counters.
 * Guarantees cross-OEM compatibility, automatic midnight rollover, local persistence,
 * and zero fabricated metrics.
 */
class FitnessRepository(
    private val context: Context,
    private val database: OfflineDatabase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    val healthConnectManager = HealthConnectManager(context)
    val stepSensorHelper = StepSensorHelper(context)

    private val dailyActivityDao = database.dailyActivityDao()
    private val exerciseDao = database.exerciseDao()
    private val userGoalsDao = database.userGoalsDao()

    private val zenXpManager = ZenXPManager(database, 1L)
    private val undoRedoManager = UndoRedoManager(database, 1L)

    private val _externalMetrics = MutableStateFlow(ExternalHealthMetrics())
    val externalMetrics: StateFlow<ExternalHealthMetrics> = _externalMetrics.asStateFlow()

    private val _externalExercises = MutableStateFlow<List<ExternalExerciseItem>>(emptyList())
    val externalExercises: StateFlow<List<ExternalExerciseItem>> = _externalExercises.asStateFlow()

    companion object {
        const val DEFAULT_STRIDE_LENGTH_METERS = 0.75f
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        @Volatile
        private var INSTANCE: FitnessRepository? = null

        fun getInstance(context: Context): FitnessRepository {
            return INSTANCE ?: synchronized(this) {
                val db = OfflineDatabase.getInstance(context)
                val repo = FitnessRepository(context.applicationContext, db)
                INSTANCE = repo
                repo
            }
        }
    }

    init {
        // Start native step sensor listening (safe no-op if no hardware sensor or permission)
        stepSensorHelper.startListening()

        // Hook step updates to persist live step changes to Room DB
        stepSensorHelper.onStepsUpdated = { steps, _ ->
            scope.launch {
                persistStepUpdate(steps)
            }
        }
    }

    /**
     * Flow of today's physical activity.
     */
    fun getActivityFlowForDate(dateIso: String): Flow<DailyActivityEntity?> {
        return dailyActivityDao.getDailyActivity(dateIso)
    }

    suspend fun getDailyActivityDirect(dateIso: String): DailyActivityEntity? {
        return dailyActivityDao.getDailyActivityDirect(dateIso)
    }

    fun getTodayActivityFlow(): Flow<DailyActivityEntity?> {
        val todayStr = LocalDate.now().format(DATE_FORMATTER)
        return dailyActivityDao.getDailyActivity(todayStr)
    }

    fun getWeeklyActivityFlowForEndDate(endDate: LocalDate): Flow<List<DailyActivityEntity>> {
        val sixDaysAgo = endDate.minusDays(6)
        return dailyActivityDao.getActivitiesBetween(
            startDate = sixDaysAgo.format(DATE_FORMATTER),
            endDate = endDate.format(DATE_FORMATTER)
        )
    }

    /**
     * Flow of the last 7 days of physical activity for weekly charts.
     */
    fun getWeeklyActivityFlow(): Flow<List<DailyActivityEntity>> {
        val today = LocalDate.now()
        val sixDaysAgo = today.minusDays(6)
        return dailyActivityDao.getActivitiesBetween(
            startDate = sixDaysAgo.format(DATE_FORMATTER),
            endDate = today.format(DATE_FORMATTER)
        )
    }

    /**
     * Flow of all historical daily activities (steps, distance, calories) across all dates.
     */
    val allDailyActivities: Flow<List<DailyActivityEntity>> = dailyActivityDao.getAllActivities()

    /**
     * Flow of currently ongoing active exercise session, if any.
     */
    val activeExerciseSession: Flow<ExerciseSessionEntity?> = exerciseDao.getActiveExerciseSession()

    /**
     * Flow of recent completed exercise sessions.
     */
    val recentExercises: Flow<List<ExerciseSessionEntity>> = exerciseDao.getRecentCompletedExercises(10)

    /**
     * Synchronizes today's activity from either Health Connect (if available and permitted)
     * or the native step counter.
     */
    suspend fun syncTodayActivity(currentGoalSteps: Int = 8000, fallbackRestMinutes: Long? = null) {
        val todayStr = LocalDate.now().format(DATE_FORMATTER)
        val existing = dailyActivityDao.getDailyActivityDirect(todayStr)
        val goal = if (currentGoalSteps > 0) currentGoalSteps else (existing?.goalSteps ?: 8000)

        val existingSteps = existing?.steps ?: 0
        var hcSteps = 0
        var hcDistanceMeters = 0.0
        var hcCaloriesKcal = 0.0
        var hasHealthConnectData = false

        // 1. Try Health Connect aggregate query first
        if (healthConnectManager.getSdkStatus() == HealthConnectSdkStatus.AVAILABLE &&
            healthConnectManager.hasBasicStepPermissions()
        ) {
            val hcData = healthConnectManager.readTodayAggregate()
            if (hcData.hasData || hcData.steps > 0) {
                hcSteps = hcData.steps.toInt()
                hcDistanceMeters = hcData.distanceMeters
                hcCaloriesKcal = hcData.activeCaloriesKcal
                hasHealthConnectData = true
            }
        }

        // 2. Query native hardware step sensor
        val sensorStatus = stepSensorHelper.getStatus(DEFAULT_STRIDE_LENGTH_METERS)
        val sensorSteps = sensorStatus.stepsToday

        // 3. Authoritative step resolution: take the maximum verified step count
        val bestSteps = maxOf(existingSteps, hcSteps, sensorSteps)

        // Calibrate the native sensor baseline if Health Connect or existing has higher steps
        if (bestSteps > sensorSteps && bestSteps > 0) {
            stepSensorHelper.calibrateBaselineWithKnownSteps(bestSteps)
        }

        val distanceMeters = if (hcDistanceMeters > 0.0 && hcSteps >= bestSteps) {
            hcDistanceMeters.toFloat()
        } else {
            bestSteps * DEFAULT_STRIDE_LENGTH_METERS
        }

        val existingCalories = existing?.activeCalories ?: 0f
        val activeCalories = if (hcCaloriesKcal > 0.0) {
            maxOf(hcCaloriesKcal.toFloat(), existingCalories, bestSteps * 0.04f)
        } else {
            maxOf(existingCalories, bestSteps * 0.04f)
        }

        val source = when {
            hasHealthConnectData && hcSteps >= sensorSteps && hcSteps > 0 -> "HEALTH_CONNECT"
            sensorSteps > 0 || (stepSensorHelper.hasHardwareSensor() && stepSensorHelper.hasPermission()) -> "STEP_COUNTER"
            existingSteps > 0 -> existing?.source ?: "STEP_COUNTER"
            else -> "UNAVAILABLE"
        }

        val updated = DailyActivityEntity(
            date = todayStr,
            steps = bestSteps,
            goalSteps = goal,
            distanceMeters = distanceMeters,
            activeCalories = activeCalories,
            activeMinutes = existing?.activeMinutes ?: 0,
            source = source,
            lastUpdated = System.currentTimeMillis()
        )
        dailyActivityDao.insertOrUpdate(updated)

        // Award Step Goal XP if reached with verified tracking source (never when tracking is unavailable or fabricated)
        if (bestSteps >= goal && bestSteps > 0 && (source == "HEALTH_CONNECT" || source == "STEP_COUNTER")) {
            zenXpManager.awardStepGoal(todayStr, bestSteps)
            undoRedoManager.recalculateAggregates()
        }

        // Also refresh external metrics
        refreshExternalHealthMetrics(fallbackRestMinutes, activeCalories)
    }

    private suspend fun persistStepUpdate(steps: Int) {
        val todayStr = LocalDate.now().format(DATE_FORMATTER)
        val distance = steps * DEFAULT_STRIDE_LENGTH_METERS
        val existing = dailyActivityDao.getDailyActivityDirect(todayStr)
        val existingSteps = existing?.steps ?: 0
        if (steps >= existingSteps) {
            val cachedHcCalories = _externalMetrics.value.todayActiveCaloriesKcal ?: 0f
            if (existing == null) {
                val initCalories = maxOf(steps * 0.04f, cachedHcCalories)
                dailyActivityDao.insertOrUpdate(
                    DailyActivityEntity(
                        date = todayStr,
                        steps = steps,
                        goalSteps = 8000,
                        distanceMeters = distance,
                        activeCalories = initCalories,
                        source = "STEP_COUNTER",
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            } else {
                val calories = maxOf(existing.activeCalories, steps * 0.04f, cachedHcCalories)
                dailyActivityDao.updateSteps(
                    date = todayStr,
                    steps = steps,
                    distanceMeters = distance,
                    activeCalories = calories,
                    source = existing.source,
                    lastUpdated = System.currentTimeMillis()
                )
            }

            val goal = existing?.goalSteps ?: 8000
            if (steps >= goal) {
                zenXpManager.awardStepGoal(todayStr, steps)
                undoRedoManager.recalculateAggregates()
            }
        }
    }

    /**
     * Refreshes external health metrics (heart rate, sleep, external sessions).
     */
    suspend fun refreshExternalHealthMetrics(fallbackRestMinutes: Long? = null, fallbackCalories: Float? = null) {
        var metrics = healthConnectManager.readExternalHealthMetrics()

        // Fallback for calories if Health Connect has none recorded or has fewer than verified local calories
        val bestCals = maxOf(metrics.todayActiveCaloriesKcal ?: 0f, fallbackCalories ?: 0f)
        if (bestCals > 0f) {
            metrics = metrics.copy(todayActiveCaloriesKcal = bestCals)
        }

        // Fallback for sleep if Health Connect has no sleep record
        if (metrics.lastSleepDurationMinutes == null && fallbackRestMinutes != null && fallbackRestMinutes > 0L) {
            metrics = metrics.copy(
                lastSleepDurationMinutes = fallbackRestMinutes,
                sleepSourceDescription = "Overnight Rest Window"
            )
        }

        _externalMetrics.value = metrics
        _externalExercises.value = healthConnectManager.readExternalExerciseSessions(5)
    }

    /**
     * Updates the daily step goal for today and persisted user goals.
     */
    suspend fun updateDailyStepGoal(userId: Long, newGoal: Int) {
        val todayStr = LocalDate.now().format(DATE_FORMATTER)
        val existing = dailyActivityDao.getDailyActivityDirect(todayStr)
        if (existing != null) {
            dailyActivityDao.insertOrUpdate(existing.copy(goalSteps = newGoal))
        } else {
            dailyActivityDao.insertOrUpdate(
                DailyActivityEntity(
                    date = todayStr,
                    steps = 0,
                    goalSteps = newGoal,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
        val userGoals = userGoalsDao.getUserGoalsDirect(userId)
        if (userGoals != null) {
            userGoalsDao.insertOrUpdate(userGoals.copy(dailySteps = newGoal))
        }
    }

    /**
     * Starts an active exercise session. Ensures any prior active session is finalized first.
     */
    suspend fun startExerciseSession(
        title: String,
        activityType: String,
        notes: String? = null
    ): Long {
        // Cancel or finalize any existing active exercise
        val currentActive = exerciseDao.getActiveExerciseSessionDirect()
        if (currentActive != null) {
            cancelActiveExerciseSession()
        }

        val todayStr = LocalDate.now().format(DATE_FORMATTER)
        val newSession = ExerciseSessionEntity(
            title = title.ifBlank { "$activityType Workout" },
            activityType = activityType.uppercase(),
            startTime = System.currentTimeMillis(),
            status = "ACTIVE",
            sessionDate = todayStr,
            notes = notes
        )
        return exerciseDao.insertExercise(newSession)
    }

    /**
     * Stops and completes the currently ongoing exercise session.
     * Computes final duration, estimated calories, and increments today's active minutes.
     */
    suspend fun stopActiveExerciseSession(notes: String? = null): Boolean {
        val active = exerciseDao.getActiveExerciseSessionDirect() ?: return false
        val now = System.currentTimeMillis()
        val durationSeconds = ((now - active.startTime) / 1000L).coerceAtLeast(0L)
        val durationMinutes = (durationSeconds / 60).toInt()

        // Calorie burn rate estimation per minute based on activity type
        val calPerMin = when (active.activityType.uppercase()) {
            "RUNNING" -> 11.0f
            "CYCLING" -> 8.5f
            "STRENGTH" -> 6.0f
            "WALKING" -> 4.2f
            else -> 5.0f
        }
        val estimatedCalories = (durationMinutes * calPerMin).coerceAtLeast(1.0f)

        // Estimated distance for walking/running if applicable
        val estimatedDistance: Float? = when (active.activityType.uppercase()) {
            "WALKING" -> (durationMinutes * 80f) // ~80m per min
            "RUNNING" -> (durationMinutes * 170f) // ~170m per min
            "CYCLING" -> (durationMinutes * 350f) // ~350m per min
            else -> null
        }

        val completed = active.copy(
            endTime = now,
            durationSeconds = durationSeconds,
            calories = estimatedCalories,
            distanceMeters = estimatedDistance,
            notes = notes ?: active.notes,
            status = "COMPLETED"
        )
        exerciseDao.updateExercise(completed)

        // Award Zen XP for qualifying workout (min 10 mins, 20 XP each, max 40 XP/day)
        if (durationMinutes >= 10) {
            zenXpManager.awardExerciseSession(
                exerciseId = completed.id,
                durationMinutes = durationMinutes,
                date = completed.sessionDate,
                exerciseTitle = completed.title.ifBlank { "Workout" }
            )
            undoRedoManager.recalculateAggregates()
        }

        // Update active minutes in today's daily activity
        val todayStr = LocalDate.now().format(DATE_FORMATTER)
        val daily = dailyActivityDao.getDailyActivityDirect(todayStr)
        if (daily != null) {
            val updatedActiveMinutes = daily.activeMinutes + durationMinutes
            val updatedCalories = daily.activeCalories + estimatedCalories
            dailyActivityDao.insertOrUpdate(
                daily.copy(
                    activeMinutes = updatedActiveMinutes,
                    activeCalories = updatedCalories,
                    lastUpdated = now
                )
            )
        }

        return true
    }

    /**
     * Records a completed workout directly with date, duration, activity type, and intensity.
     * Instantly qualifies for workout streak and Zen XP rewards (if >= 10 mins).
     */
    suspend fun recordCompletedWorkout(
        title: String,
        activityType: String,
        durationMinutes: Int,
        date: LocalDate = LocalDate.now(),
        intensity: String? = "MODERATE",
        notes: String? = null
    ): Long {
        val durationSeconds = (durationMinutes * 60L).coerceAtLeast(60L)
        val now = System.currentTimeMillis()
        val dateStr = date.format(DATE_FORMATTER)

        val calPerMin = when (activityType.uppercase()) {
            "RUNNING" -> 11.0f
            "CYCLING" -> 8.5f
            "STRENGTH" -> 6.0f
            "WALKING" -> 4.2f
            else -> 5.0f
        }
        val estimatedCalories = (durationMinutes * calPerMin).coerceAtLeast(1.0f)

        val formattedNotes = buildString {
            if (!intensity.isNullOrBlank()) append("Intensity: $intensity")
            if (!notes.isNullOrBlank()) {
                if (isNotEmpty()) append(" • ")
                append(notes)
            }
        }.ifBlank { null }

        val session = ExerciseSessionEntity(
            title = title.ifBlank { "${activityType.lowercase().replaceFirstChar { it.uppercase() }} Workout" },
            activityType = activityType.uppercase(),
            startTime = now - (durationSeconds * 1000L),
            endTime = now,
            durationSeconds = durationSeconds,
            calories = estimatedCalories,
            notes = formattedNotes,
            status = "COMPLETED",
            sessionDate = dateStr
        )

        val insertedId = exerciseDao.insertExercise(session)

        // Award Zen XP for qualifying workout (min 10 mins)
        if (durationMinutes >= 10) {
            zenXpManager.awardExerciseSession(
                exerciseId = insertedId,
                durationMinutes = durationMinutes,
                date = dateStr,
                exerciseTitle = session.title
            )
        }

        // Update active minutes in daily activity
        val daily = dailyActivityDao.getDailyActivityDirect(dateStr)
        if (daily != null) {
            dailyActivityDao.insertOrUpdate(
                daily.copy(
                    activeMinutes = daily.activeMinutes + durationMinutes,
                    activeCalories = daily.activeCalories + estimatedCalories,
                    lastUpdated = now
                )
            )
        } else {
            dailyActivityDao.insertOrUpdate(
                DailyActivityEntity(
                    date = dateStr,
                    steps = 0,
                    goalSteps = 8000,
                    distanceMeters = 0f,
                    activeCalories = estimatedCalories,
                    activeMinutes = durationMinutes,
                    source = "MANUAL_ENTRY",
                    lastUpdated = now
                )
            )
        }

        undoRedoManager.recalculateAggregates()
        return insertedId
    }

    /**
     * Cancels the active exercise session without saving it as completed.
     */
    suspend fun cancelActiveExerciseSession() {
        val active = exerciseDao.getActiveExerciseSessionDirect() ?: return
        exerciseDao.updateExercise(active.copy(status = "CANCELLED", endTime = System.currentTimeMillis()))
    }
}
