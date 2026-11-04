package com.example.detoxproject

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import com.example.detoxproject.data.integrations.health.HealthConnectManager
import com.example.detoxproject.data.integrations.notifications.SessionAlarmManager
import com.example.detoxproject.ui.MainViewModel
import com.example.detoxproject.ui.navigation.OfflineNavHost
import com.example.detoxproject.ui.navigation.Screen
import com.example.detoxproject.ui.theme.OfflineTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Permissions updated, view model polls automatically
    }

    private val healthConnectLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) {
        viewModel.syncFitnessData()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.detoxproject.ui.theme.ThemeManager.init(this)
        enableEdgeToEdge()

        // Request runtime permissions for step counting & session notifications
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }

        handleIntent(intent)

        setContent {
            OfflineTheme {
                val currentUser by viewModel.currentUser.collectAsState()
                val activeUserId by viewModel.activeUserId.collectAsState()
                val userGoals by viewModel.userGoals.collectAsState()
                val activeSession by viewModel.activeSession.collectAsState()
                val allSessions by viewModel.allSessions.collectAsState()
                val currentTimeMs by viewModel.currentTimeMs.collectAsState()
                val dailyUsage by viewModel.dailyUsage.collectAsState()
                val activitySummary by viewModel.activitySummary.collectAsState()
                val recommendations by viewModel.recommendations.collectAsState()
                val plannedActivities by viewModel.plannedActivities.collectAsState()
                val userMessage by viewModel.userMessage.collectAsState()
                val weather by viewModel.weather.collectAsState()
                val achievements by viewModel.achievements.collectAsState()
                val pastActionsCount by viewModel.pastActionsCount.collectAsState()
                val futureActionsCount by viewModel.futureActionsCount.collectAsState()
                val alarmState by viewModel.alarmState.collectAsState()
                val activeAlarmSessionTitle by viewModel.activeAlarmSessionTitle.collectAsState()
                val activeAlarmDurationMins by viewModel.activeAlarmDurationMins.collectAsState()
                val navigateToScreen by viewModel.navigateToScreen.collectAsState()

                // Fitness & Activity state observables
                val todayActivity by viewModel.todayActivity.collectAsState()
                val weeklyActivity by viewModel.weeklyActivity.collectAsState()
                val activeExerciseSession by viewModel.activeExerciseSession.collectAsState()
                val recentExercises by viewModel.recentExercises.collectAsState()
                val externalHealthMetrics by viewModel.externalHealthMetrics.collectAsState()
                val externalExercises by viewModel.externalExercises.collectAsState()
                val allDailyActivities by viewModel.allDailyActivities.collectAsState()
                val allPlannedActivities by viewModel.allPlannedActivities.collectAsState()

                // Phase 4: Date, Habits & Calendar state observables
                val selectedDate by viewModel.selectedDate.collectAsState()
                val calendarYearMonth by viewModel.calendarYearMonth.collectAsState()
                val selectedDaySummary by viewModel.selectedDaySummary.collectAsState()
                val todayHabitsWithStatus by viewModel.todayHabitsWithStatus.collectAsState()
                val monthCalendarData by viewModel.monthCalendarData.collectAsState()
                val weeklyAnalyticsData by viewModel.weeklyAnalyticsData.collectAsState()
                val quickDetoxPackages by viewModel.quickDetoxPackages.collectAsState()
                val quickDetoxDurationSeconds by viewModel.quickDetoxDurationSeconds.collectAsState()

                // Phase 5: Progression, Streaks & Grace Day state observables
                val userLevelInfo by viewModel.userLevelInfo.collectAsState()
                val allStreaks by viewModel.allStreaks.collectAsState()
                val weeklyGraceStatus by viewModel.weeklyGraceStatus.collectAsState()
                val recentXPTransactions by viewModel.recentXPTransactions.collectAsState()
                val selectedTrendRange by viewModel.selectedTrendRange.collectAsState()
                val trendsAnalyticsData by viewModel.trendsAnalyticsData.collectAsState()
                val pastActions by viewModel.pastActions.collectAsState()
                val futureActions by viewModel.futureActions.collectAsState()
                val mindfulNotes by viewModel.mindfulNotes.collectAsState()

                OfflineNavHost(
                    currentUser = currentUser,
                    activeUserId = activeUserId,
                    userGoals = userGoals,
                    activeSession = activeSession,
                    allSessions = allSessions,
                    currentTimeMs = currentTimeMs,
                    dailyUsage = dailyUsage,
                    activitySummary = activitySummary,
                    recommendations = recommendations,
                    plannedActivities = plannedActivities,
                    onToggleActivity = { viewModel.togglePlannedActivity(it) },
                    userMessage = userMessage,
                    onClearMessage = { viewModel.clearMessage() },
                    weather = weather,
                    achievements = achievements,
                    pastActionsCount = pastActionsCount,
                    futureActionsCount = futureActionsCount,
                    onLogin = { email, pass -> viewModel.login(email, pass) { } },
                    onSignUp = { name, email, pass, confirm -> viewModel.signUp(name, email, pass, confirm) { } },
                    onResetPassword = { email, pass, onDone -> viewModel.resetPassword(email, pass, onDone) },
                    onGetSecurityQuestion = { email, onResult ->
                        lifecycleScope.launch {
                            val res = viewModel.getSecurityQuestion(email)
                            if (res.isSuccess) {
                                val pair = res.getOrNull()!!
                                onResult(true, pair.first, pair.second, null)
                            } else {
                                onResult(false, "", "", res.exceptionOrNull()?.message)
                            }
                        }
                    },
                    onVerifySecurityAnswer = { email, key, ans, onResult ->
                        lifecycleScope.launch {
                            val res = viewModel.verifySecurityAnswer(email, key, ans)
                            if (res.isSuccess) {
                                onResult(true, null)
                            } else {
                                onResult(false, res.exceptionOrNull()?.message)
                            }
                        }
                    },
                    onUpdateAvatar = { viewModel.updateAvatar(it) },
                    onLogout = { viewModel.logout() },
                    onDeleteAccount = { viewModel.deleteAccount() },
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() },
                    onToggleQuickDetox = { viewModel.toggleQuickDetox() },
                    onStartSession = { title, duration, category, allowAudio, restrictedPackages, durationSeconds ->
                        viewModel.startSession(title, duration, category, restrictedPackages, allowAudio, targetSeconds = durationSeconds)
                    },
                    onPauseSession = { viewModel.pauseActiveSession() },
                    onResumeSession = { viewModel.resumeActiveSession() },
                    onCompleteSession = { viewModel.completeActiveSession() },
                    onCancelSession = { viewModel.cancelActiveSession() },
                    onUpdateGoals = { s, m, c, l, u -> viewModel.updateGoals(s, m, c, u, l) },
                    onRefreshWeather = { viewModel.refreshWeather() },
                    onAddActivityToPlanner = { title, duration, category ->
                        viewModel.addPlannedActivity(title, duration, category)
                    },
                    alarmState = alarmState,
                    activeAlarmSessionTitle = activeAlarmSessionTitle,
                    activeAlarmDurationMins = activeAlarmDurationMins,
                    navigateToScreen = navigateToScreen,
                    onClearNavigateTo = { viewModel.navigateTo(null) },
                    onStopAlarm = { viewModel.stopAlarmAudio() },
                    onDismissAndCompleteSession = { viewModel.dismissAlarmAndCompleteSession() },
                    todayActivity = todayActivity,
                    weeklyActivity = weeklyActivity,
                    activeExerciseSession = activeExerciseSession,
                    recentExercises = recentExercises,
                    allPlannedActivities = allPlannedActivities,
                    allDailyActivities = allDailyActivities,
                    externalHealthMetrics = externalHealthMetrics,
                    externalExercises = externalExercises,
                    healthConnectStatus = viewModel.healthConnectStatus,
                    hasHardwareStepSensor = viewModel.hasHardwareStepSensor,
                    onSyncFitness = { viewModel.syncFitnessData() },
                    onUpdateStepGoal = { viewModel.updateDailyStepGoal(it) },
                    onStartExercise = { title, type, notes -> viewModel.startExerciseSession(title, type, notes) },
                    onStopExercise = { notes -> viewModel.stopActiveExercise(notes) },
                    onCancelExercise = { viewModel.cancelActiveExercise() },
                    onOpenHealthConnectSettings = { viewModel.openHealthConnectSettings() },
                    onRequestHealthConnectPermissions = {
                        try {
                            healthConnectLauncher.launch(HealthConnectManager.REQUIRED_PERMISSIONS)
                        } catch (e: Exception) {
                            viewModel.openHealthConnectSettings()
                        }
                    },
                    calendarYearMonth = calendarYearMonth,
                    calendarData = monthCalendarData,
                    selectedDate = selectedDate,
                    selectedDaySummary = selectedDaySummary,
                    weeklyAnalyticsData = weeklyAnalyticsData,
                    todayHabits = todayHabitsWithStatus,
                    onSelectDate = { viewModel.selectDate(it) },
                    onPrevMonth = { viewModel.navigateMonth(-1) },
                    onNextMonth = { viewModel.navigateMonth(1) },
                    onGoToToday = { viewModel.goToToday() },
                    onStepDate = { offset -> viewModel.selectDate(selectedDate.plusDays(offset)) },
                    onToggleHabitForDate = { id, date -> viewModel.toggleHabitForDate(id, date) },
                    onDeleteHabit = { viewModel.deleteHabit(it) },
                    onToggleTask = { viewModel.togglePlannedActivity(it) },
                    onDeleteTask = { viewModel.deleteTask(it) },
                    onAddTask = { title, duration, cat, desc, date ->
                        viewModel.addTaskForDate(title, duration, cat, desc, date)
                    },
                    onCreateHabit = { title, desc, cat, icon, freq, days ->
                        viewModel.createHabit(title, desc, cat, icon, freq, days)
                    },
                    quickDetoxPackages = quickDetoxPackages,
                    onUpdateQuickDetoxPackages = { viewModel.updateQuickDetoxPackages(it) },
                    quickDetoxDurationSeconds = quickDetoxDurationSeconds,
                    onUpdateQuickDetoxDurationSeconds = { viewModel.updateQuickDetoxDurationSeconds(it) },
                    levelInfo = userLevelInfo,
                    allStreaks = allStreaks,
                    weeklyGraceStatus = weeklyGraceStatus,
                    recentXPTransactions = recentXPTransactions,
                    trendsData = trendsAnalyticsData,
                    selectedTrendRange = selectedTrendRange,
                    onSelectTrendRange = { viewModel.selectTrendRange(it) },
                    pastActions = pastActions,
                    futureActions = futureActions,
                    onConsumeGraceDay = { viewModel.consumeGraceDay(it) },
                    mindfulNotes = mindfulNotes,
                    onSaveMindfulNote = { title, content, color -> viewModel.saveMindfulNote(title, content, color) },
                    onDeleteMindfulNote = { viewModel.deleteMindfulNote(it) },
                    onUpdateMindfulNote = { id, title, content, color -> viewModel.updateMindfulNote(id, title, content, color) },
                    onRecordWorkout = { title, type, duration, date, intensity, notes ->
                        viewModel.recordCompletedWorkout(title, type, duration, date, intensity, notes)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppResumed()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("EXTRA_GIVE_UP", false) == true) {
            viewModel.cancelActiveSession()
        }
        if (intent?.action == SessionAlarmManager.ACTION_VIEW_SUMMARY) {
            viewModel.navigateTo(Screen.Progress)
        }
    }
}
