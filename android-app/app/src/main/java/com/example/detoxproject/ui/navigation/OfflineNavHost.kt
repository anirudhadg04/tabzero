package com.example.detoxproject.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.detoxproject.data.integrations.health.ExternalExerciseItem
import com.example.detoxproject.data.integrations.health.ExternalHealthMetrics
import com.example.detoxproject.data.integrations.health.HealthConnectStatus
import com.example.detoxproject.data.integrations.health.PhysicalActivitySummary
import com.example.detoxproject.data.integrations.health.StepSensorStatus
import com.example.detoxproject.data.integrations.notifications.AlarmState
import com.example.detoxproject.data.integrations.usage.DailyUsageSummary
import com.example.detoxproject.data.local.entity.*
import com.example.detoxproject.domain.engine.DetoxTimerEngine
import com.example.detoxproject.domain.engine.HabitWithStatus
import com.example.detoxproject.domain.engine.WellnessRecommendation
import com.example.detoxproject.domain.model.*
import com.example.detoxproject.ui.components.SessionCompletionDialog
import com.example.detoxproject.ui.screens.auth.AuthScreen
import com.example.detoxproject.ui.screens.discover.DiscoverScreen
import com.example.detoxproject.ui.screens.fitness.FitnessDashboardScreen
import com.example.detoxproject.ui.screens.focus.FocusScreen
import com.example.detoxproject.ui.screens.home.HomeScreen
import com.example.detoxproject.ui.screens.profile.ProfileScreen
import com.example.detoxproject.ui.screens.progress.ProgressScreen
import com.example.detoxproject.ui.screens.settings.SettingsScreen
import com.example.detoxproject.ui.theme.OfflineBackground
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun OfflineNavHost(
    currentUser: UserEntity?,
    activeUserId: Long?,
    userGoals: UserGoalsEntity?,
    activeSession: DetoxSessionEntity?,
    allSessions: List<DetoxSessionEntity>,
    currentTimeMs: Long,
    dailyUsage: DailyUsageSummary,
    activitySummary: PhysicalActivitySummary,
    recommendations: List<WellnessRecommendation>,
    plannedActivities: List<PlannedActivityEntity>,
    onToggleActivity: (Long) -> Unit,
    userMessage: String?,
    onClearMessage: () -> Unit,
    weather: WeatherCacheEntity?,
    achievements: List<AchievementEntity>,
    pastActionsCount: Int,
    futureActionsCount: Int,
    onLogin: (email: String, pass: String) -> Unit,
    onSignUp: (name: String, email: String, pass: String, confirmPass: String) -> Unit,
    onResetPassword: (email: String, newPass: String, onComplete: (Boolean, String) -> Unit) -> Unit = { _, _, _ -> },
    onGetSecurityQuestion: (email: String, onResult: (Boolean, String, String, String?) -> Unit) -> Unit = { _, _ -> },
    onVerifySecurityAnswer: (email: String, questionKey: String, answer: String, onResult: (Boolean, String?) -> Unit) -> Unit = { _, _, _, _ -> },
    onUpdateAvatar: (String) -> Unit = {},
    onLogout: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleQuickDetox: () -> Unit = {},
    onStartSession: (title: String, duration: Int, category: String, allowAudio: Boolean, restrictedPackages: String, durationSeconds: Int) -> Unit,
    onPauseSession: () -> Unit,
    onResumeSession: () -> Unit,
    onCompleteSession: () -> Unit,
    onCancelSession: () -> Unit,
    onUpdateGoals: (steps: Int, offlineMinutes: Int, calories: Int, screenLimitMinutes: Int, userName: String) -> Unit,
    onRefreshWeather: () -> Unit,
    onAddActivityToPlanner: (title: String, duration: Int, category: String) -> Unit,
    alarmState: AlarmState = AlarmState.IDLE,
    activeAlarmSessionTitle: String? = null,
    activeAlarmDurationMins: Int = 0,
    navigateToScreen: Screen? = null,
    onClearNavigateTo: () -> Unit = {},
    onStopAlarm: () -> Unit = {},
    onDismissAndCompleteSession: () -> Unit = {},
    todayActivity: DailyActivityEntity? = null,
    weeklyActivity: List<DailyActivityEntity> = emptyList(),
    allDailyActivities: List<DailyActivityEntity> = emptyList(),
    allPlannedActivities: List<PlannedActivityEntity> = emptyList(),
    activeExerciseSession: ExerciseSessionEntity? = null,
    recentExercises: List<ExerciseSessionEntity> = emptyList(),
    externalHealthMetrics: ExternalHealthMetrics = ExternalHealthMetrics(),
    externalExercises: List<ExternalExerciseItem> = emptyList(),
    healthConnectStatus: HealthConnectStatus = HealthConnectStatus.NOT_INSTALLED,
    hasHardwareStepSensor: Boolean = true,
    onSyncFitness: () -> Unit = {},
    onUpdateStepGoal: (Int) -> Unit = {},
    onStartExercise: (title: String, type: String, notes: String?) -> Unit = { _, _, _ -> },
    onStopExercise: (notes: String?) -> Unit = {},
    onCancelExercise: () -> Unit = {},
    onOpenHealthConnectSettings: () -> Unit = {},
    onRequestHealthConnectPermissions: () -> Unit = {},
    // Phase 4: Date, Habits & History Parameters
    calendarYearMonth: YearMonth = YearMonth.now(),
    calendarData: CalendarMonthData? = null,
    selectedDate: LocalDate = LocalDate.now(),
    selectedDaySummary: UnifiedDailySummary? = null,
    weeklyAnalyticsData: WeeklyAnalyticsData? = null,
    todayHabits: List<HabitWithStatus> = emptyList(),
    onSelectDate: (LocalDate) -> Unit = {},
    onPrevMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onGoToToday: () -> Unit = {},
    onStepDate: (Long) -> Unit = {},
    onToggleHabitForDate: (habitId: Long, date: LocalDate) -> Unit = { _, _ -> },
    onToggleTask: (taskId: Long) -> Unit = {},
    onDeleteTask: (taskId: Long) -> Unit = {},
    onAddTask: (title: String, duration: Int, category: String, description: String, date: LocalDate) -> Unit = { _, _, _, _, _ -> },
    onCreateHabit: (title: String, description: String, category: String, icon: String, frequency: String, targetDays: String) -> Unit = { _, _, _, _, _, _ -> },
    quickDetoxPackages: List<String> = emptyList(),
    onUpdateQuickDetoxPackages: (List<String>) -> Unit = {},
    quickDetoxDurationSeconds: Int = 1800,
    onUpdateQuickDetoxDurationSeconds: (Int) -> Unit = {},
    // Phase 5: Progression, Streaks & Grace Day
    levelInfo: com.example.detoxproject.domain.engine.UserLevelInfo? = null,
    allStreaks: com.example.detoxproject.domain.engine.AllStreaksInfo? = null,
    weeklyGraceStatus: com.example.detoxproject.domain.engine.WeeklyGraceStatus? = null,
    recentXPTransactions: List<com.example.detoxproject.data.local.entity.XPTransactionEntity> = emptyList(),
    trendsData: TrendsAnalyticsData? = null,
    selectedTrendRange: TrendTimeRange = TrendTimeRange.SEVEN_DAYS,
    onSelectTrendRange: (TrendTimeRange) -> Unit = {},
    pastActions: List<ActionHistoryEntity> = emptyList(),
    futureActions: List<ActionHistoryEntity> = emptyList(),
    onConsumeGraceDay: ((LocalDate) -> Unit)? = null,
    onRecordWorkout: ((title: String, type: String, durationMinutes: Int, date: LocalDate, intensity: String?, notes: String?) -> Unit)? = null,
    mindfulNotes: List<com.example.detoxproject.data.local.entity.MindfulNoteEntity> = emptyList(),
    onSaveMindfulNote: ((title: String, content: String, colorHex: String) -> Unit)? = null,
    onDeleteMindfulNote: ((Long) -> Unit)? = null
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var pendingFocusCategory by remember { mutableStateOf("focus") }
    var progressInitialTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(navigateToScreen) {
        if (navigateToScreen != null) {
            currentScreen = navigateToScreen
            onClearNavigateTo()
        }
    }

    LaunchedEffect(userMessage) {
        if (userMessage != null) {
            snackbarHostState.showSnackbar(userMessage)
            onClearMessage()
        }
    }

    // In-app Session Completion Dialog with stop alarm and view summary
    val shouldShowCompletionDialog = alarmState == AlarmState.RINGING ||
        (alarmState == AlarmState.DISMISSED && activeSession != null && activeSession.status == "ACTIVE" &&
            DetoxTimerEngine.calculateRemainingMs(activeSession, currentTimeMs) <= 0L)

    if (shouldShowCompletionDialog) {
        SessionCompletionDialog(
            sessionTitle = activeAlarmSessionTitle ?: activeSession?.title ?: "Focus Session",
            durationMinutes = if (activeAlarmDurationMins > 0) activeAlarmDurationMins else (activeSession?.targetMinutes ?: 0),
            alarmState = alarmState,
            onStopAlarm = onStopAlarm,
            onCompleteAndDismiss = onDismissAndCompleteSession,
            onViewSummary = {
                onDismissAndCompleteSession()
                currentScreen = Screen.Progress
            }
        )
    }

    // If no active user session, display the Authentication screen
    if (activeUserId == null || currentUser == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            AuthScreen(
                onLogin = onLogin,
                onSignUp = onSignUp,
                onResetPassword = onResetPassword,
                onGetSecurityQuestion = onGetSecurityQuestion,
                onVerifySecurityAnswer = onVerifySecurityAnswer
            )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    BackHandler(enabled = currentScreen != Screen.Home) {
        if (currentScreen == Screen.Settings) {
            currentScreen = Screen.Profile
        } else {
            currentScreen = Screen.Home
        }
    }

    Scaffold(
        containerColor = OfflineBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            MinimalBottomNav(
                currentRoute = currentScreen.route,
                onNavigate = {
                    if (it == Screen.Progress) {
                        progressInitialTab = 0
                    }
                    currentScreen = it
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.Home -> HomeScreen(
                    userGoals = userGoals,
                    activeSession = activeSession,
                    weather = weather,
                    activitySummary = activitySummary,
                    dailyUsage = dailyUsage,
                    recommendation = recommendations.firstOrNull(),
                    onAddRecommendationToPlanner = { rec ->
                        onAddActivityToPlanner(rec.title, rec.durationMinutes, rec.category)
                    },
                    plannedActivities = plannedActivities,
                    todayHabits = todayHabits,
                    onToggleHabit = { id -> onToggleHabitForDate(id, selectedDate) },
                    onCreateHabit = onCreateHabit,
                    onAddTask = { title, duration, cat, desc -> onAddTask(title, duration, cat, desc, selectedDate) },
                    onToggleActivity = onToggleActivity,
                    onDeleteTask = onDeleteTask,
                    onRefreshWeather = onRefreshWeather,
                    pastActionsCount = pastActionsCount,
                    futureActionsCount = futureActionsCount,
                    onUndo = onUndo,
                    onRedo = onRedo,
                    onToggleQuickDetox = onToggleQuickDetox,
                    quickDetoxDurationSeconds = quickDetoxDurationSeconds,
                    onNavigateToFocus = {
                        pendingFocusCategory = "focus"
                        currentScreen = Screen.Focus
                    },
                    onCategoryClick = { category ->
                        pendingFocusCategory = category
                        currentScreen = Screen.Focus
                    },
                    onNavigateToFitness = { currentScreen = Screen.Fitness },
                    onNavigateToProfile = { currentScreen = Screen.Profile },
                    levelInfo = levelInfo,
                    onNavigateToProgress = {
                        progressInitialTab = 2
                        currentScreen = Screen.Progress
                    },
                    todayActivity = todayActivity,
                    externalMetrics = externalHealthMetrics
                )

                Screen.Fitness -> FitnessDashboardScreen(
                    todayActivity = todayActivity,
                    weeklyActivity = weeklyActivity,
                    activeExercise = activeExerciseSession,
                    recentExercises = recentExercises,
                    externalMetrics = externalHealthMetrics,
                    externalExercises = externalExercises,
                    healthConnectStatus = healthConnectStatus,
                    hasHardwareSensor = hasHardwareStepSensor,
                    currentTimeMs = currentTimeMs,
                    userGoals = userGoals,
                    allSessions = allSessions,
                    allActivities = allPlannedActivities,
                    allDailyActivities = allDailyActivities,
                    dailyUsage = dailyUsage,
                    activeSession = activeSession,
                    onNavigateBack = { currentScreen = Screen.Home },
                    onNavigateToFocus = { currentScreen = Screen.Focus },
                    onSync = onSyncFitness,
                    onUpdateGoal = onUpdateStepGoal,
                    onStartExercise = onStartExercise,
                    onStopExercise = onStopExercise,
                    onCancelExercise = onCancelExercise,
                    onOpenHealthConnectSettings = onOpenHealthConnectSettings,
                    onRequestHealthConnectPermissions = onRequestHealthConnectPermissions
                )

                Screen.Focus -> FocusScreen(
                    activeSession = activeSession,
                    currentTimeMs = currentTimeMs,
                    initialCategory = pendingFocusCategory,
                    onStartSession = onStartSession,
                    onPauseSession = onPauseSession,
                    onResumeSession = onResumeSession,
                    onCompleteSession = onCompleteSession,
                    onCancelSession = onCancelSession
                )

                Screen.Discover -> DiscoverScreen(
                    weather = weather,
                    recommendations = recommendations,
                    notes = mindfulNotes,
                    onRefreshWeather = onRefreshWeather,
                    onAddActivityToPlanner = onAddActivityToPlanner,
                    onSaveNote = { title, content, color -> onSaveMindfulNote?.invoke(title, content, color) },
                    onDeleteNote = { id -> onDeleteMindfulNote?.invoke(id) }
                )

                Screen.Progress -> ProgressScreen(
                    initialTab = progressInitialTab,
                    achievements = achievements,
                    completedSessions = allSessions,
                    dailyUsage = dailyUsage,
                    dailyOfflineGoal = userGoals?.dailyOfflineMinutes ?: 120,
                    pastActionsCount = pastActionsCount,
                    futureActionsCount = futureActionsCount,
                    levelInfo = levelInfo,
                    allStreaks = allStreaks,
                    weeklyGraceStatus = weeklyGraceStatus,
                    recentTransactions = recentXPTransactions,
                    trendsData = trendsData,
                    selectedTrendRange = selectedTrendRange,
                    onSelectTrendRange = onSelectTrendRange,
                    pastActions = pastActions,
                    futureActions = futureActions,
                    onConsumeGraceDay = onConsumeGraceDay,
                    calendarYearMonth = calendarYearMonth,
                    calendarData = calendarData,
                    selectedDate = selectedDate,
                    selectedDaySummary = selectedDaySummary,
                    weeklyAnalyticsData = weeklyAnalyticsData,
                    onSelectDate = onSelectDate,
                    onPrevMonth = onPrevMonth,
                    onNextMonth = onNextMonth,
                    onGoToToday = onGoToToday,
                    onStepDate = onStepDate,
                    onToggleHabitForDate = onToggleHabitForDate,
                    onToggleTask = onToggleTask,
                    onDeleteTask = onDeleteTask,
                    onAddTask = onAddTask,
                    onCreateHabit = onCreateHabit,
                    recentExercises = recentExercises,
                    onRecordWorkout = onRecordWorkout,
                    onStartDetox = { currentScreen = Screen.Focus },
                    onUndo = onUndo,
                    onRedo = onRedo,
                    onNavigateToFitness = { currentScreen = Screen.Fitness }
                )

                Screen.Profile -> ProfileScreen(
                    currentUser = currentUser,
                    userGoals = userGoals,
                    activitySummary = activitySummary,
                    dailyUsage = dailyUsage,
                    weather = weather,
                    onUpdateGoals = onUpdateGoals,
                    onUpdateAvatar = onUpdateAvatar,
                    onNavigateToSettings = { currentScreen = Screen.Settings },
                    onLogout = onLogout
                )

                Screen.Settings -> SettingsScreen(
                    onBack = { currentScreen = Screen.Profile },
                    activitySummary = activitySummary,
                    quickDetoxPackages = quickDetoxPackages,
                    onUpdateQuickDetoxPackages = onUpdateQuickDetoxPackages,
                    quickDetoxDurationSeconds = quickDetoxDurationSeconds,
                    onUpdateQuickDetoxDuration = onUpdateQuickDetoxDurationSeconds
                )

                else -> HomeScreen(
                    userGoals = userGoals,
                    activeSession = activeSession,
                    weather = weather,
                    activitySummary = activitySummary,
                    dailyUsage = dailyUsage,
                    recommendation = recommendations.firstOrNull(),
                    onAddRecommendationToPlanner = { rec ->
                        onAddActivityToPlanner(rec.title, rec.durationMinutes, rec.category)
                    },
                    pastActionsCount = pastActionsCount,
                    futureActionsCount = futureActionsCount,
                    onUndo = onUndo,
                    onRedo = onRedo,
                    onToggleQuickDetox = onToggleQuickDetox,
                    quickDetoxDurationSeconds = quickDetoxDurationSeconds,
                    onNavigateToFocus = { currentScreen = Screen.Focus },
                    onCategoryClick = { currentScreen = Screen.Focus },
                    onNavigateToProfile = { currentScreen = Screen.Profile },
                    todayActivity = todayActivity,
                    externalMetrics = externalHealthMetrics
                )
            }
        }
    }
}
