package com.example.detoxproject.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.detoxproject.data.integrations.enforcement.DetoxEnforcementService
import com.example.detoxproject.data.integrations.enforcement.EnforcementCapability
import com.example.detoxproject.data.integrations.enforcement.EnforcementManager
import com.example.detoxproject.data.integrations.health.ExternalExerciseItem
import com.example.detoxproject.data.integrations.health.ExternalHealthMetrics
import com.example.detoxproject.data.integrations.health.HealthActivityManager
import com.example.detoxproject.data.integrations.health.HealthConnectStatus
import com.example.detoxproject.data.integrations.health.PhysicalActivitySummary
import com.example.detoxproject.data.integrations.notifications.ActiveDetoxNotificationManager
import com.example.detoxproject.data.integrations.notifications.AlarmState
import com.example.detoxproject.data.integrations.notifications.SessionAlarmManager
import com.example.detoxproject.data.integrations.usage.DailyUsageSummary
import com.example.detoxproject.data.integrations.usage.UsageStatsHelper
import com.example.detoxproject.data.local.entity.*
import com.example.detoxproject.data.repository.FitnessRepository
import com.example.detoxproject.data.repository.WellnessRepository
import com.example.detoxproject.domain.engine.DetoxTimerEngine
import com.example.detoxproject.domain.engine.DateManager
import com.example.detoxproject.domain.engine.HabitWithStatus
import com.example.detoxproject.domain.engine.WellnessRecommendation
import com.example.detoxproject.domain.engine.WellnessRecommendationEngine
import com.example.detoxproject.domain.engine.LevelCalculator
import com.example.detoxproject.domain.engine.UserLevelInfo
import com.example.detoxproject.domain.engine.AllStreaksInfo
import com.example.detoxproject.domain.engine.WeeklyGraceStatus
import com.example.detoxproject.domain.model.*
import com.example.detoxproject.ui.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WellnessRepository.getInstance(application)
    private val healthManager = HealthActivityManager(application)
    private val enforcementManager = EnforcementManager(application)
    val fitnessRepository = FitnessRepository.getInstance(application)

    // Phase 4: Reactive local calendar date manager (single source of truth for day transitions)
    val dateManager = DateManager(application)
    val currentDate: StateFlow<LocalDate> = dateManager.currentDate

    val currentUser: StateFlow<UserEntity?> = repository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeUserId: StateFlow<Long?> = repository.activeUserId

    val userGoals: StateFlow<UserGoalsEntity?> = repository.userGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Fitness & Activity Observables: Dynamically bound to reactive currentDate
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val todayActivity: StateFlow<DailyActivityEntity?> = currentDate.flatMapLatest { date ->
        val dateIso = date.format(DateManager.ISO_FORMATTER)
        fitnessRepository.getActivityFlowForDate(dateIso)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val weeklyActivity: StateFlow<List<DailyActivityEntity>> = currentDate.flatMapLatest { date ->
        fitnessRepository.getWeeklyActivityFlowForEndDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeExerciseSession: StateFlow<ExerciseSessionEntity?> = fitnessRepository.activeExerciseSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentExercises: StateFlow<List<ExerciseSessionEntity>> = fitnessRepository.recentExercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val externalHealthMetrics: StateFlow<ExternalHealthMetrics> = fitnessRepository.externalMetrics

    val externalExercises: StateFlow<List<ExternalExerciseItem>> = fitnessRepository.externalExercises

    val allDailyActivities: StateFlow<List<DailyActivityEntity>> = fitnessRepository.allDailyActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val healthConnectStatus: HealthConnectStatus get() = healthManager.getHealthConnectStatus()

    val hasHardwareStepSensor: Boolean get() = fitnessRepository.stepSensorHelper.hasHardwareSensor()

    val activeSession: StateFlow<DetoxSessionEntity?> = repository.activeSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allSessions: StateFlow<List<DetoxSessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pledges: StateFlow<List<PledgeEntity>> = repository.allPledges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weather: StateFlow<WeatherCacheEntity?> = repository.weatherCache
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val achievements: StateFlow<List<AchievementEntity>> = repository.allAchievements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pastActionsCount: StateFlow<Int> = repository.pastActionsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val futureActionsCount: StateFlow<Int> = repository.futureActionsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Phase 5: Zen XP & Progression State
    val totalXp: StateFlow<Int> = repository.totalXp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val userLevelInfo: StateFlow<UserLevelInfo> = totalXp
        .map { xp -> LevelCalculator.calculateLevel(xp) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            LevelCalculator.calculateLevel(0)
        )

    val recentXPTransactions: StateFlow<List<XPTransactionEntity>> = repository.getRecentXPTransactions(50)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _allStreaks = MutableStateFlow<AllStreaksInfo?>(null)
    val allStreaks: StateFlow<AllStreaksInfo?> = _allStreaks.asStateFlow()

    private val _weeklyGraceStatus = MutableStateFlow<WeeklyGraceStatus?>(null)
    val weeklyGraceStatus: StateFlow<WeeklyGraceStatus?> = _weeklyGraceStatus.asStateFlow()

    fun refreshProgression() {
        viewModelScope.launch {
            try {
                _allStreaks.value = repository.calculateAllStreaks()
                _weeklyGraceStatus.value = repository.getGraceStatus()
            } catch (_: Exception) {
            }
        }
    }

    fun consumeGraceDay(date: LocalDate) {
        viewModelScope.launch {
            val success = repository.consumeGraceDay(date)
            if (success) {
                _userMessage.value = "Grace day applied for ${date.format(DateTimeFormatter.ofPattern("MMM dd"))}"
                refreshProgression()
            } else {
                _userMessage.value = "Weekly grace day already used this week"
            }
        }
    }

    val mindfulNotes: StateFlow<List<com.example.detoxproject.data.local.entity.MindfulNoteEntity>> = repository.getMindfulNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveMindfulNote(title: String, content: String, colorHex: String = "#E8F5E9") {
        viewModelScope.launch {
            repository.saveMindfulNote(title, content, colorHex)
            refreshProgression()
        }
    }

    fun updateMindfulNote(id: Long, title: String, content: String, colorHex: String) {
        viewModelScope.launch {
            repository.updateMindfulNote(id, title, content, colorHex)
            refreshProgression()
        }
    }

    fun deleteMindfulNote(id: Long) {
        viewModelScope.launch {
            repository.deleteMindfulNote(id)
            refreshProgression()
        }
    }

    // Current wall-clock ticker flow pulsing every 1s
    private val _currentTimeMs = MutableStateFlow(System.currentTimeMillis())
    val currentTimeMs: StateFlow<Long> = _currentTimeMs.asStateFlow()

    // Real device usage & step observables
    private val _dailyUsage = MutableStateFlow(UsageStatsHelper.getDailyUsageSummary(application))
    val dailyUsage: StateFlow<DailyUsageSummary> = _dailyUsage.asStateFlow()

    private val _activitySummary = MutableStateFlow(healthManager.getActivitySummary())
    val activitySummary: StateFlow<PhysicalActivitySummary> = _activitySummary.asStateFlow()

    private val _enforcementStatus = MutableStateFlow(enforcementManager.getEnforcementStatus())
    val enforcementStatus = _enforcementStatus.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _quickDetoxPackages = MutableStateFlow<List<String>>(
        com.example.detoxproject.data.local.QuickDetoxPreferences.getRestrictedPackages(getApplication())
    )
    val quickDetoxPackages: StateFlow<List<String>> = _quickDetoxPackages.asStateFlow()

    private val _quickDetoxDurationSeconds = MutableStateFlow<Int>(
        com.example.detoxproject.data.local.QuickDetoxPreferences.getDurationSeconds(getApplication())
    )
    val quickDetoxDurationSeconds: StateFlow<Int> = _quickDetoxDurationSeconds.asStateFlow()

    fun updateQuickDetoxDurationSeconds(seconds: Int) {
        val validSecs = seconds.coerceAtLeast(5)
        com.example.detoxproject.data.local.QuickDetoxPreferences.setDurationSeconds(getApplication(), validSecs)
        _quickDetoxDurationSeconds.value = validSecs
    }

    fun updateQuickDetoxPackages(packages: List<String>) {
        com.example.detoxproject.data.local.QuickDetoxPreferences.setRestrictedPackages(getApplication(), packages)
        _quickDetoxPackages.value = packages

        val active = activeSession.value
        if (active != null && active.status == "ACTIVE" && active.title == "Quick Detox") {
            enforcementManager.startEnforcement(
                packages = packages,
                title = "Quick Detox",
                targetMins = active.targetMinutes,
                startTime = active.startTime
            )
        }
    }

    // Phase 4: Date, Habit, History & Calendar System
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _calendarYearMonth = MutableStateFlow(YearMonth.now())
    val calendarYearMonth: StateFlow<YearMonth> = _calendarYearMonth.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedDaySummary: StateFlow<UnifiedDailySummary?> = _selectedDate.flatMapLatest { date ->
        repository.getUnifiedDailySummary(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val todayHabitsWithStatus: StateFlow<List<HabitWithStatus>> = currentDate.flatMapLatest { today ->
        repository.getUnifiedDailySummary(today).map { it.habits }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActiveHabits: StateFlow<List<HabitEntity>> = repository.allActiveHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHabits: StateFlow<List<HabitEntity>> = repository.allHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _monthCalendarData = MutableStateFlow<CalendarMonthData?>(null)
    val monthCalendarData: StateFlow<CalendarMonthData?> = _monthCalendarData.asStateFlow()

    private val _weeklyAnalyticsData = MutableStateFlow<WeeklyAnalyticsData?>(null)
    val weeklyAnalyticsData: StateFlow<WeeklyAnalyticsData?> = _weeklyAnalyticsData.asStateFlow()

    private val _selectedTrendRange = MutableStateFlow(TrendTimeRange.SEVEN_DAYS)
    val selectedTrendRange: StateFlow<TrendTimeRange> = _selectedTrendRange.asStateFlow()

    private val _trendsAnalyticsData = MutableStateFlow<TrendsAnalyticsData?>(null)
    val trendsAnalyticsData: StateFlow<TrendsAnalyticsData?> = _trendsAnalyticsData.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val pastActions: StateFlow<List<ActionHistoryEntity>> = activeUserId.flatMapLatest { id ->
        if (id != null) repository.database.actionHistoryDao().getPastActions(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val futureActions: StateFlow<List<ActionHistoryEntity>> = activeUserId.flatMapLatest { id ->
        if (id != null) repository.database.actionHistoryDao().getFutureActions(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlannedActivities: StateFlow<List<PlannedActivityEntity>> = repository.allActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val plannedActivities: StateFlow<List<PlannedActivityEntity>> = currentDate.flatMapLatest { date ->
        val dateIso = date.format(DateManager.ISO_FORMATTER)
        repository.getActivitiesForDate(dateIso)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alarmState: StateFlow<AlarmState> = SessionAlarmManager.alarmState
    val activeAlarmSessionTitle: StateFlow<String?> = SessionAlarmManager.activeAlarmSessionTitle
    val activeAlarmDurationMins: StateFlow<Int> = SessionAlarmManager.activeAlarmDurationMins

    private val _navigateToScreen = MutableStateFlow<Screen?>(null)
    val navigateToScreen: StateFlow<Screen?> = _navigateToScreen.asStateFlow()

    fun navigateTo(screen: Screen?) {
        _navigateToScreen.value = screen
    }

    fun stopAlarmAudio() {
        SessionAlarmManager.dismissAlarm(getApplication())
    }

    fun dismissAlarmAndCompleteSession() {
        SessionAlarmManager.dismissAlarm(getApplication())
        SessionAlarmManager.resetToIdle()
        completeActiveSession()
    }

    fun resetAlarmState() {
        SessionAlarmManager.dismissAlarm(getApplication())
        SessionAlarmManager.resetToIdle()
    }

    val recommendations: StateFlow<List<WellnessRecommendation>> = combine(
        weather,
        dailyUsage,
        activitySummary,
        userGoals
    ) { w, usage, act, goals ->
        val temp = w?.temperature ?: 25.0
        val code = w?.weatherCode ?: 1
        val precip = 0.0
        val screenMs = usage.totalScreenTimeMs
        val steps = act.stepsToday
        val goalSteps = goals?.dailySteps ?: 8000

        WellnessRecommendationEngine.generateRecommendations(
            temperature = temp,
            weatherCode = code,
            precipitation = precip,
            screenTimeMs = screenMs,
            stepsToday = steps,
            stepsGoal = goalSteps
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // 1-second pulse for timer & periodic sensor/usage refresh
        viewModelScope.launch {
            var counter = 0
            while (true) {
                delay(1000)
                _currentTimeMs.value = System.currentTimeMillis()
                counter++

                // Check active session timer completion to ring alarm safely
                val session = activeSession.value
                if (session != null && session.status == "ACTIVE") {
                    val remaining = DetoxTimerEngine.calculateRemainingMs(session, _currentTimeMs.value)
                    if (remaining <= 0L) {
                        ActiveDetoxNotificationManager.cancelNotification(getApplication())
                        val triggered = SessionAlarmManager.triggerAlarm(
                            context = getApplication(),
                            sessionId = session.id,
                            sessionTitle = session.title,
                            durationMinutes = session.targetMinutes
                        )
                        if (triggered) {
                            _userMessage.value = "Detox Session Finished! Life happens beyond the screen."
                        }
                        // Cleanly mark session COMPLETED in database and stop enforcement
                        viewModelScope.launch {
                            repository.completeSession(session.id)
                            enforcementManager.stopEnforcement()
                            DetoxEnforcementService.stop(getApplication())
                            com.example.detoxproject.data.integrations.enforcement.DetoxAccessibilityService.stopDetox()
                        }
                    }
                }

                // Refresh sensor, usage, and fitness stats every 10 seconds
                if (counter % 10 == 0) {
                    val usage = UsageStatsHelper.getDailyUsageSummary(getApplication())
                    _dailyUsage.value = usage
                    fitnessRepository.syncTodayActivity(userGoals.value?.dailySteps ?: 8000, usage.lastNightRestMinutes)
                    updatePhysicalActivitySummary(todayActivity.value)
                    _enforcementStatus.value = enforcementManager.getEnforcementStatus()
                }
            }
        }

        viewModelScope.launch {
            repository.syncWeather()
            val usage = UsageStatsHelper.getDailyUsageSummary(getApplication())
            _dailyUsage.value = usage
            fitnessRepository.syncTodayActivity(userGoals.value?.dailySteps ?: 8000, usage.lastNightRestMinutes)
            updatePhysicalActivitySummary(todayActivity.value)
        }

        // Synchronize activity upon date rollover immediately
        viewModelScope.launch {
            currentDate.collectLatest { _ ->
                val goal = userGoals.value?.dailySteps ?: 8000
                fitnessRepository.syncTodayActivity(goal)
                val todayAct = fitnessRepository.getDailyActivityDirect(dateManager.getTodayIso())
                updatePhysicalActivitySummary(todayAct)
            }
        }

        // Synchronize canonical achievements whenever active user is loaded/changed
        viewModelScope.launch {
            repository.activeUserId.collectLatest { uid ->
                if (uid != null) {
                    repository.syncCanonicalAchievements(uid)
                }
            }
        }

        // Keep activitySummary continuously and reactively synchronized with todayActivity (single source of truth)
        viewModelScope.launch {
            combine(todayActivity, externalHealthMetrics) { act, ext ->
                Pair(act, ext)
            }.collect { (act, ext) ->
                val steps = act?.steps ?: 0
                val dist = (act?.distanceMeters ?: 0f) / 1000f
                val cals = maxOf(
                    ext.todayActiveCaloriesKcal ?: 0f,
                    act?.activeCalories ?: 0f,
                    steps * 0.04f
                )
                _activitySummary.value = healthManager.getActivitySummary(
                    knownSteps = steps,
                    knownDistanceKm = dist,
                    knownCalories = cals
                )
            }
        }


        // Phase 5: Progression and streak synchronization
        viewModelScope.launch {
            combine(totalXp, allSessions, todayHabitsWithStatus) { _, _, _ ->
                Unit
            }.collect {
                refreshProgression()
            }
        }

        // Phase 4: Reactively refresh month calendar and weekly analytics
        viewModelScope.launch {
            combine(
                _calendarYearMonth,
                _selectedDate,
                pastActionsCount,
                currentDate
            ) { ym, sel, _, _ -> Pair(ym, sel) }.collectLatest { (ym, sel) ->
                refreshCalendarAndAnalytics(ym, sel)
            }
        }

        // Synchronize live ongoing notification countdown & enforcement service with session state
        viewModelScope.launch {
            activeSession.collect { session ->
                if (session != null && (session.status == "ACTIVE" || session.status == "PAUSED")) {
                    ActiveDetoxNotificationManager.updateNotification(getApplication(), session)
                    if (session.status == "ACTIVE") {
                        val pkgs = session.restrictedPackages.split(',').map { it.trim() }.filter { it.isNotBlank() }
                        enforcementManager.startEnforcement(
                            packages = pkgs,
                            title = session.title,
                            targetMins = session.targetMinutes,
                            startTime = session.startTime
                        )
                    } else {
                        // When paused, stop app blocking but maintain the paused notification
                        enforcementManager.stopEnforcement()
                    }
                } else {
                    // COMPLETED or CANCELLED: cancel active notification
                    ActiveDetoxNotificationManager.cancelNotification(getApplication())
                    enforcementManager.stopEnforcement()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        healthManager.stopListening()
        dateManager.unregister()
    }

    /**
     * Called whenever MainActivity enters foreground (onResume).
     * Refreshes real-time screen time and syncs latest steps immediately.
     */
    fun onAppResumed() {
        viewModelScope.launch {
            dateManager.checkDateRollover()
            val usage = UsageStatsHelper.getDailyUsageSummary(getApplication())
            _dailyUsage.value = usage
            val goal = userGoals.value?.dailySteps ?: 8000
            fitnessRepository.syncTodayActivity(goal, usage.lastNightRestMinutes)
        }
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    private fun updatePhysicalActivitySummary(act: DailyActivityEntity? = todayActivity.value) {
        val steps = act?.steps ?: 0
        val dist = act?.distanceMeters?.div(1000f) ?: ((steps * 0.75f) / 1000f)
        val ext = externalHealthMetrics.value
        val cals = maxOf(
            ext.todayActiveCaloriesKcal ?: 0f,
            act?.activeCalories ?: 0f,
            steps * 0.04f
        )
        _activitySummary.value = healthManager.getActivitySummary(
            knownSteps = steps,
            knownDistanceKm = dist,
            knownCalories = cals
        )
    }

    // --- FITNESS & ACTIVITY ACTIONS ---

    fun syncFitnessData() {
        viewModelScope.launch {
            val usage = UsageStatsHelper.getDailyUsageSummary(getApplication())
            _dailyUsage.value = usage
            fitnessRepository.syncTodayActivity(userGoals.value?.dailySteps ?: 8000, usage.lastNightRestMinutes)
            updatePhysicalActivitySummary(todayActivity.value)
        }
    }

    fun updateDailyStepGoal(newGoal: Int) {
        viewModelScope.launch {
            val uid = activeUserId.value ?: 1L
            fitnessRepository.updateDailyStepGoal(uid, newGoal)
            val currentGoals = userGoals.value
            repository.updateGoals(
                dailySteps = newGoal,
                dailyOfflineMinutes = currentGoals?.dailyOfflineMinutes ?: 120,
                targetCalories = currentGoals?.targetCalories ?: 400,
                userName = currentGoals?.userName ?: "User",
                maxScreenTimeMinutes = currentGoals?.maxScreenTimeMinutes ?: 240
            )
            syncFitnessData()
            _userMessage.value = "Daily step goal updated to %,d steps".format(newGoal)
        }
    }

    fun startExerciseSession(title: String, type: String, notes: String? = null) {
        viewModelScope.launch {
            fitnessRepository.startExerciseSession(title, type, notes)
            _userMessage.value = "Started $type workout"
        }
    }

    fun stopActiveExercise(notes: String? = null) {
        viewModelScope.launch {
            val completed = fitnessRepository.stopActiveExerciseSession(notes)
            if (completed) {
                syncFitnessData()
                _userMessage.value = "Workout completed and logged"
            }
        }
    }

    fun cancelActiveExercise() {
        viewModelScope.launch {
            fitnessRepository.cancelActiveExerciseSession()
            _userMessage.value = "Workout discarded"
        }
    }

    fun recordCompletedWorkout(
        title: String,
        activityType: String,
        durationMinutes: Int,
        date: LocalDate = LocalDate.now(),
        intensity: String? = "MODERATE",
        notes: String? = null
    ) {
        viewModelScope.launch {
            fitnessRepository.recordCompletedWorkout(
                title = title,
                activityType = activityType,
                durationMinutes = durationMinutes,
                date = date,
                intensity = intensity,
                notes = notes
            )
            refreshProgression()
            _userMessage.value = "Recorded $activityType workout ($durationMinutes min)"
        }
    }

    fun openHealthConnectSettings() {
        try {
            val intent = healthManager.createHealthConnectIntent()
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            _userMessage.value = "Unable to open Health Connect settings on this device"
        }
    }

    // --- AUTHENTICATION ---

    fun signUp(name: String, email: String, pass: String, confirmPass: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = repository.registerUser(name, email, pass, confirmPass)
            if (res.isSuccess) {
                _userMessage.value = "Welcome to tabzero., ${res.getOrNull()!!.name}!"
                onComplete(true)
            } else {
                _userMessage.value = res.exceptionOrNull()?.message ?: "Sign up failed"
                onComplete(false)
            }
        }
    }

    fun login(email: String, pass: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = repository.loginUser(email, pass)
            if (res.isSuccess) {
                _userMessage.value = "Welcome back, ${res.getOrNull()!!.name}!"
                onComplete(true)
            } else {
                _userMessage.value = res.exceptionOrNull()?.message ?: "Login failed"
                onComplete(false)
            }
        }
    }

    fun logout() {
        repository.logout()
        _userMessage.value = "Logged out"
    }

    fun deleteAccount(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.deleteCurrentAccount()
            if (success) {
                _userMessage.value = "Account deleted"
            }
            onComplete(success)
        }
    }

    fun resetPassword(email: String, newPass: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = repository.resetPassword(email, newPass)
            if (res.isSuccess) {
                _userMessage.value = "Password updated successfully for $email"
                onComplete(true, "Password updated successfully. You can now sign in.")
            } else {
                val err = res.exceptionOrNull()?.message ?: "Failed to reset password"
                _userMessage.value = err
                onComplete(false, err)
            }
        }
    }

    fun toggleQuickDetox() {
        val active = activeSession.value
        if (active != null && active.status == "ACTIVE") {
            cancelActiveSession()
            _userMessage.value = "Detox Mode deactivated"
        } else {
            val configuredBlocked = _quickDetoxPackages.value.joinToString(",")
            val totalSecs = _quickDetoxDurationSeconds.value.coerceAtLeast(5)
            val mins = (totalSecs + 59) / 60
            startSession("Quick Detox", mins, "focus", configuredBlocked, allowAudio = true, targetSeconds = totalSecs)
            val hours = totalSecs / 3600
            val m = (totalSecs % 3600) / 60
            val s = totalSecs % 60
            val formatted = when {
                hours > 0 && m > 0 -> "${hours}h ${m}m"
                hours > 0 && s > 0 -> "${hours}h ${s}s"
                hours > 0 -> "${hours}h"
                m > 0 && s > 0 -> "${m}m ${s}s"
                m > 0 -> "${m}m"
                else -> "${s}s"
            }
            _userMessage.value = "⚡ Quick Detox activated ($formatted)"
        }
    }


    // --- DETOX SESSIONS ---

    fun startSession(
        title: String,
        targetMinutes: Int,
        category: String = "focus",
        restrictedPackages: String = "",
        allowAudio: Boolean = false,
        targetSeconds: Int = targetMinutes * 60
    ) {
        viewModelScope.launch {
            val res = repository.createAndStartSession(title, targetMinutes, category, restrictedPackages, allowAudio, targetSeconds = targetSeconds)
            if (res.isSuccess) {
                val session = res.getOrNull()!!
                val pkgList = restrictedPackages.split(',').map { it.trim() }.filter { it.isNotBlank() }
                enforcementManager.configureRestrictions(pkgList, allowAudio)
                enforcementManager.startEnforcement(
                    packages = pkgList,
                    title = title,
                    targetMins = targetMinutes,
                    startTime = session.startTime
                )
                _enforcementStatus.value = enforcementManager.getEnforcementStatus()
            } else {
                _userMessage.value = res.exceptionOrNull()?.message ?: "Failed to start session"
            }
        }
    }

    fun pauseActiveSession() {
        viewModelScope.launch {
            val active = activeSession.value ?: return@launch
            val res = repository.pauseSession(active.id)
            enforcementManager.stopEnforcement()
            _enforcementStatus.value = enforcementManager.getEnforcementStatus()
            if (res.isFailure) {
                _userMessage.value = res.exceptionOrNull()?.message
            }
        }
    }

    fun resumeActiveSession() {
        viewModelScope.launch {
            val active = activeSession.value ?: return@launch
            val res = repository.resumeSession(active.id)
            if (res.isSuccess) {
                val session = res.getOrNull()!!
                val pkgList = session.restrictedPackages.split(',').map { it.trim() }.filter { it.isNotBlank() }
                enforcementManager.startEnforcement(
                    packages = pkgList,
                    title = session.title,
                    targetMins = session.targetMinutes,
                    startTime = session.startTime
                )
                _enforcementStatus.value = enforcementManager.getEnforcementStatus()
            } else {
                _userMessage.value = res.exceptionOrNull()?.message
            }
        }
    }

    fun completeActiveSession() {
        viewModelScope.launch {
            ActiveDetoxNotificationManager.cancelNotification(getApplication())
            SessionAlarmManager.dismissAlarm(getApplication())
            SessionAlarmManager.resetToIdle()
            DetoxEnforcementService.stop(getApplication())
            enforcementManager.stopEnforcement()
            _enforcementStatus.value = enforcementManager.getEnforcementStatus()

            val userId = activeUserId.value ?: 1L
            val active = activeSession.value ?: repository.database.detoxDao().getActiveSessionDirect(userId)
            if (active != null) {
                val res = repository.completeSession(active.id)
                if (res.isFailure) {
                    _userMessage.value = res.exceptionOrNull()?.message
                }
            }
        }
    }

    fun cancelActiveSession() {
        viewModelScope.launch {
            ActiveDetoxNotificationManager.cancelNotification(getApplication())
            SessionAlarmManager.dismissAlarm(getApplication())
            SessionAlarmManager.resetToIdle()
            DetoxEnforcementService.stop(getApplication())
            com.example.detoxproject.data.integrations.enforcement.DetoxAccessibilityService.stopDetox()
            enforcementManager.stopEnforcement()
            _enforcementStatus.value = enforcementManager.getEnforcementStatus()

            val userId = activeUserId.value ?: 1L
            val active = activeSession.value ?: repository.database.detoxDao().getActiveSessionDirect(userId)
            if (active != null) {
                val res = repository.cancelSession(active.id)
                if (res.isFailure) {
                    _userMessage.value = res.exceptionOrNull()?.message
                }
            }
        }
    }

    fun undo() {
        viewModelScope.launch {
            val success = repository.undo()
            if (!success) {
                _userMessage.value = "Nothing to undo"
            }
        }
    }

    fun redo() {
        viewModelScope.launch {
            val success = repository.redo()
            if (!success) {
                _userMessage.value = "Nothing to redo"
            }
        }
    }

    fun updateGoals(
        steps: Int,
        offlineMinutes: Int,
        targetCalories: Int = 400,
        userName: String = "User",
        maxScreenTimeMinutes: Int = 240,
        avatar: String? = null
    ) {
        viewModelScope.launch {
            repository.updateGoals(steps, offlineMinutes, targetCalories, userName, maxScreenTimeMinutes, avatar)
            val uid = activeUserId.value ?: 1L
            fitnessRepository.updateDailyStepGoal(uid, steps)
            syncFitnessData()
            _userMessage.value = "Goals updated: %,d steps • %d min offline".format(steps, offlineMinutes)
        }
    }

    fun updateAvatar(avatar: String) {
        viewModelScope.launch {
            repository.updateAvatar(avatar)
        }
    }

    suspend fun getSecurityQuestion(email: String): Result<Pair<String, String>> {
        return repository.getSecurityQuestionForEmail(email)
    }

    suspend fun verifySecurityAnswer(email: String, questionKey: String, answer: String): Result<Boolean> {
        return repository.verifySecurityAnswer(email, questionKey, answer)
    }

    fun refreshWeather() {
        viewModelScope.launch {
            repository.syncWeather()
        }
    }

    fun addPlannedActivity(title: String, duration: Int, category: String) {
        viewModelScope.launch {
            val res = repository.addPlannedActivity(title, duration, category)
            if (res.isSuccess) {
                _userMessage.value = "Added '$title' to your planner"
            }
        }
    }

    fun togglePlannedActivity(activityId: Long) {
        viewModelScope.launch {
            val res = repository.togglePlannedActivity(activityId)
            if (res.isFailure) {
                _userMessage.value = res.exceptionOrNull()?.message
            } else {
                refreshCalendarAndAnalytics()
            }
        }
    }

    // Phase 4: History & Calendar Navigation
    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        val ym = YearMonth.from(date)
        if (ym != _calendarYearMonth.value) {
            _calendarYearMonth.value = ym
        }
    }

    fun navigateMonth(offsetMonths: Long) {
        val newYm = _calendarYearMonth.value.plusMonths(offsetMonths)
        _calendarYearMonth.value = newYm
        if (YearMonth.from(_selectedDate.value) != newYm) {
            val today = currentDate.value
            _selectedDate.value = if (YearMonth.from(today) == newYm) today else newYm.atDay(1)
        }
    }

    fun goToToday() {
        val today = currentDate.value
        _selectedDate.value = today
        _calendarYearMonth.value = YearMonth.from(today)
    }

    fun refreshCalendarAndAnalytics(
        ym: YearMonth = _calendarYearMonth.value,
        selDate: LocalDate = _selectedDate.value
    ) {
        viewModelScope.launch {
            try {
                val cal = repository.getMonthCalendarDataDirect(ym, selDate)
                _monthCalendarData.value = cal
                val weekly = repository.getWeeklyAnalyticsDataDirect(selDate)
                _weeklyAnalyticsData.value = weekly
                val trends = repository.getTrendsAnalyticsDataDirect(_selectedTrendRange.value, selDate)
                _trendsAnalyticsData.value = trends
            } catch (_: Exception) {}
        }
    }

    fun selectTrendRange(range: TrendTimeRange) {
        _selectedTrendRange.value = range
        refreshCalendarAndAnalytics()
    }

    fun toggleHabitForDate(habitId: Long, date: LocalDate) {
        viewModelScope.launch {
            val dateIso = date.format(DateManager.ISO_FORMATTER)
            val res = repository.toggleHabitCompletion(habitId, dateIso)
            if (res.isSuccess) {
                refreshCalendarAndAnalytics()
            } else {
                _userMessage.value = res.exceptionOrNull()?.message
            }
        }
    }

    fun createHabit(
        title: String,
        description: String = "",
        category: String = "mindfulness",
        icon: String = "spa",
        frequency: String = "DAILY",
        targetDays: String = "1,2,3,4,5,6,7"
    ) {
        viewModelScope.launch {
            val res = repository.createHabit(
                title = title,
                description = description,
                category = category,
                icon = icon,
                frequency = frequency,
                targetDays = targetDays
            )
            if (res.isSuccess) {
                _userMessage.value = "Created habit: '$title'"
                refreshCalendarAndAnalytics()
            } else {
                _userMessage.value = res.exceptionOrNull()?.message
            }
        }
    }

    fun updateHabit(habit: HabitEntity) {
        viewModelScope.launch {
            val res = repository.updateHabit(habit)
            if (res.isSuccess) {
                _userMessage.value = "Updated habit: '${habit.title}'"
                refreshCalendarAndAnalytics()
            }
        }
    }

    fun archiveHabit(habitId: Long, isArchived: Boolean) {
        viewModelScope.launch {
            val res = repository.archiveHabit(habitId, isArchived)
            if (res.isSuccess) {
                _userMessage.value = if (isArchived) "Habit archived" else "Habit restored"
                refreshCalendarAndAnalytics()
            }
        }
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            val res = repository.deleteHabit(habitId)
            if (res.isSuccess) {
                _userMessage.value = "Habit deleted"
                refreshCalendarAndAnalytics()
            }
        }
    }

    fun addTaskForDate(
        title: String,
        durationMinutes: Int,
        category: String,
        description: String = "",
        date: LocalDate = _selectedDate.value
    ) {
        viewModelScope.launch {
            val res = repository.addTaskForDate(
                title = title,
                durationMinutes = durationMinutes,
                category = category,
                description = description,
                scheduledDate = date.format(DateManager.ISO_FORMATTER)
            )
            if (res.isSuccess) {
                _userMessage.value = "Added task: '$title'"
                refreshCalendarAndAnalytics()
            } else {
                _userMessage.value = res.exceptionOrNull()?.message
            }
        }
    }

    fun updateTask(task: PlannedActivityEntity) {
        viewModelScope.launch {
            val res = repository.updatePlannedActivity(task)
            if (res.isSuccess) {
                _userMessage.value = "Task updated"
                refreshCalendarAndAnalytics()
            }
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            val res = repository.deletePlannedActivity(taskId)
            if (res.isSuccess) {
                _userMessage.value = "Task deleted"
                refreshCalendarAndAnalytics()
            }
        }
    }
}
