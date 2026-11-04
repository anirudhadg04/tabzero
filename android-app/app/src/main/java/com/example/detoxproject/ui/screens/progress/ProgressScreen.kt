package com.example.detoxproject.ui.screens.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.detoxproject.data.integrations.usage.DailyUsageSummary
import com.example.detoxproject.data.local.entity.AchievementEntity
import com.example.detoxproject.data.local.entity.ActionHistoryEntity
import com.example.detoxproject.data.local.entity.DetoxSessionEntity
import com.example.detoxproject.data.local.entity.ExerciseSessionEntity
import com.example.detoxproject.data.local.entity.XPTransactionEntity
import com.example.detoxproject.domain.engine.AllStreaksInfo
import com.example.detoxproject.domain.engine.StreakCategoryInfo
import com.example.detoxproject.domain.engine.UserLevelInfo
import com.example.detoxproject.domain.engine.WeeklyGraceStatus
import com.example.detoxproject.domain.model.CalendarMonthData
import com.example.detoxproject.domain.model.TrendTimeRange
import com.example.detoxproject.domain.model.TrendsAnalyticsData
import com.example.detoxproject.domain.model.UnifiedDailySummary
import com.example.detoxproject.domain.model.WeeklyAnalyticsData
import com.example.detoxproject.ui.components.UndoRedoActionBar
import com.example.detoxproject.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun ProgressScreen(
    initialTab: Int = 0,
    achievements: List<AchievementEntity>,
    completedSessions: List<DetoxSessionEntity>,
    dailyUsage: DailyUsageSummary,
    dailyOfflineGoal: Int,
    pastActionsCount: Int,
    futureActionsCount: Int,
    levelInfo: UserLevelInfo? = null,
    allStreaks: AllStreaksInfo? = null,
    weeklyGraceStatus: WeeklyGraceStatus? = null,
    recentTransactions: List<XPTransactionEntity> = emptyList(),
    trendsData: TrendsAnalyticsData? = null,
    selectedTrendRange: TrendTimeRange = TrendTimeRange.SEVEN_DAYS,
    onSelectTrendRange: (TrendTimeRange) -> Unit = {},
    pastActions: List<ActionHistoryEntity> = emptyList(),
    futureActions: List<ActionHistoryEntity> = emptyList(),
    onConsumeGraceDay: ((LocalDate) -> Unit)? = null,
    calendarYearMonth: YearMonth = YearMonth.now(),
    calendarData: CalendarMonthData? = null,
    selectedDate: LocalDate = LocalDate.now(),
    selectedDaySummary: UnifiedDailySummary? = null,
    weeklyAnalyticsData: WeeklyAnalyticsData? = null,
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
    recentExercises: List<ExerciseSessionEntity> = emptyList(),
    onRecordWorkout: ((title: String, type: String, durationMinutes: Int, date: LocalDate, intensity: String?, notes: String?) -> Unit)? = null,
    onStartDetox: (() -> Unit)? = null,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onNavigateToFitness: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) } // 0: Calendar History, 1: Trends, 2: Progression & Badges

    LaunchedEffect(initialTab) {
        selectedTab = initialTab
    }
    var showCreateHabitDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showLedgerDialog by remember { mutableStateOf(false) }
    var showActionHistoryDialog by remember { mutableStateOf(false) }

    if (showLedgerDialog) {
        XPHistoryDialog(
            transactions = recentTransactions,
            totalXP = levelInfo?.totalCumulativeXP ?: 0,
            onDismiss = { showLedgerDialog = false }
        )
    }

    if (showActionHistoryDialog) {
        ActionHistoryDialog(
            pastActions = pastActions,
            futureActions = futureActions,
            onDismiss = { showActionHistoryDialog = false }
        )
    }

    if (showCreateHabitDialog) {
        CreateHabitDialog(
            onDismiss = { showCreateHabitDialog = false },
            onConfirm = { title, desc, cat, icon, freq, days ->
                onCreateHabit(title, desc, cat, icon, freq, days)
                showCreateHabitDialog = false
            }
        )
    }

    if (showAddTaskDialog) {
        AddEditTaskDialog(
            scheduledDate = selectedDate,
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, duration, category, description ->
                onAddTask(title, duration, category, description, selectedDate)
                showAddTaskDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OfflineBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = "history & progression",
            style = Typography.headlineLarge.copy(fontSize = 28.sp, fontWeight = FontWeight.SemiBold),
            color = OfflineTextPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Calendar-based history, habits, and Zen progression",
            style = Typography.bodyMedium,
            color = OfflineTextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // History Undo/Redo Action Bar
        UndoRedoActionBar(
            pastCount = pastActionsCount,
            futureCount = futureActionsCount,
            onUndo = onUndo,
            onRedo = onRedo,
            onShowHistory = { showActionHistoryDialog = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Segmented Tab Selector
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = OfflineSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(
                    "Calendar" to 0,
                    "Trends" to 1,
                    "Progression" to 2
                ).forEach { (label, index) ->
                    val isSelected = selectedTab == index
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) OfflineDarkButton else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = index }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            style = Typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) OfflineOnDarkButton else OfflineTextSecondary,
                            modifier = Modifier.wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        when (selectedTab) {
            0 -> {
                // --- TAB 0: CALENDAR HISTORY & DAY INSPECTOR ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showCreateHabitDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = OfflineDarkButton),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = OfflineOnDarkButton,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Habit", color = OfflineOnDarkButton, style = Typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = { showAddTaskDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, OfflineBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OfflineTextPrimary),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = OfflineTextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Task", color = OfflineTextPrimary, style = Typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Monthly Calendar Grid
                CalendarMonthView(
                    yearMonth = calendarYearMonth,
                    calendarData = calendarData,
                    selectedDate = selectedDate,
                    onSelectDate = onSelectDate,
                    onPrevMonth = onPrevMonth,
                    onNextMonth = onNextMonth,
                    onGoToToday = onGoToToday
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Unified Day Detail Inspector Card
                DayDetailHistoryCard(
                    summary = selectedDaySummary,
                    selectedDate = selectedDate,
                    onStepDate = onStepDate,
                    onToggleHabit = { habitId -> onToggleHabitForDate(habitId, selectedDate) },
                    onToggleTask = onToggleTask,
                    onDeleteTask = onDeleteTask,
                    onAddTaskClick = { showAddTaskDialog = true }
                )
            }

            1 -> {
                // --- TAB 1: ANALYTICS & TRENDS ---
                TrendsView(
                    trendsData = trendsData,
                    selectedRange = selectedTrendRange,
                    onSelectRange = onSelectTrendRange,
                    onNavigateToFitness = onNavigateToFitness
                )
            }

            2 -> {
                // --- TAB 2: ZEN PROGRESSION, STREAKS & BADGES HUB ---
                ProgressionHubContent(
                    levelInfo = levelInfo,
                    allStreaks = allStreaks,
                    weeklyGraceStatus = weeklyGraceStatus,
                    achievements = achievements,
                    recentExercises = recentExercises,
                    completedSessions = completedSessions,
                    onRecordWorkout = onRecordWorkout,
                    onStartDetox = onStartDetox,
                    onConsumeGraceDay = onConsumeGraceDay,
                    onOpenLedger = { showLedgerDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private data class StreakDialogData(
    val title: String,
    val subtitle: String,
    val info: StreakCategoryInfo,
    val rule: String,
    val color: Color,
    val icon: ImageVector
)

@Composable
private fun ProgressionHubContent(
    levelInfo: UserLevelInfo?,
    allStreaks: AllStreaksInfo?,
    weeklyGraceStatus: WeeklyGraceStatus?,
    achievements: List<AchievementEntity>,
    recentExercises: List<ExerciseSessionEntity> = emptyList(),
    completedSessions: List<DetoxSessionEntity> = emptyList(),
    onRecordWorkout: ((title: String, type: String, durationMinutes: Int, date: LocalDate, intensity: String?, notes: String?) -> Unit)? = null,
    onStartDetox: (() -> Unit)? = null,
    onConsumeGraceDay: ((LocalDate) -> Unit)? = null,
    onOpenLedger: () -> Unit
) {
    val info = levelInfo ?: UserLevelInfo(1, 0, 100, 0, 0f)
    var activeStreakDialog by remember { mutableStateOf<StreakDialogData?>(null) }
    var showGraceDialog by remember { mutableStateOf(false) }
    var activeAchievementDialog by remember { mutableStateOf<AchievementEntity?>(null) }

    activeStreakDialog?.let { dialogData ->
        StreakDetailDialog(
            streakTitle = dialogData.title,
            streakSubtitle = dialogData.subtitle,
            streakInfo = dialogData.info,
            qualificationRule = dialogData.rule,
            accentColor = dialogData.color,
            icon = dialogData.icon,
            recentWorkouts = recentExercises,
            recentDetoxSessions = completedSessions,
            onRecordWorkout = onRecordWorkout,
            onStartDetox = onStartDetox,
            onDismiss = { activeStreakDialog = null }
        )
    }

    if (showGraceDialog) {
        val grace = weeklyGraceStatus ?: WeeklyGraceStatus(2026, 1, false, null, 1)
        GraceDayDetailDialog(
            graceStatus = grace,
            onUseGraceDay = { date ->
                onConsumeGraceDay?.invoke(date)
                showGraceDialog = false
            },
            onDismiss = { showGraceDialog = false }
        )
    }

    activeAchievementDialog?.let { ach ->
        AchievementDetailDialog(
            achievement = ach,
            onDismiss = { activeAchievementDialog = null }
        )
    }

    // 1. Level & XP Progress Card
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = OfflineSurface,
        border = BorderStroke(1.dp, OfflineBorder.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = OfflineAccentPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "Level",
                                tint = OfflineAccentPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Level ${info.level}",
                            style = Typography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp),
                            color = OfflineTextPrimary
                        )
                        Text(
                            text = "${info.totalCumulativeXP} Total Zen XP",
                            style = Typography.bodySmall.copy(fontSize = 12.sp),
                            color = OfflineTextSecondary
                        )
                    }
                }

                OutlinedButton(
                    onClick = onOpenLedger,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, OfflineAccentPrimary.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Ledger",
                        tint = OfflineAccentPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "XP Ledger",
                        style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = OfflineAccentPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Current Level Progress",
                    style = Typography.labelSmall,
                    color = OfflineTextMuted
                )
                Text(
                    text = "${info.currentXPInLevel} / ${info.xpNeededForNextLevel} XP to Level ${info.level + 1}",
                    style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = OfflineTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { info.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = OfflineAccentPrimary,
                trackColor = OfflineSurfaceVariant
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 2. Four Independent Streaks Hub (Every card is clickable)
    Text(
        text = "Wellness Streaks",
        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
        color = OfflineTextPrimary
    )

    Spacer(modifier = Modifier.height(10.dp))

    val streaks = allStreaks ?: AllStreaksInfo(
        habitStreak = StreakCategoryInfo(0, 0, false),
        detoxStreak = StreakCategoryInfo(0, 0, false),
        movementStreak = StreakCategoryInfo(0, 0, false),
        exerciseStreak = StreakCategoryInfo(0, 0, false)
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StreakCard(
                title = "Habits",
                subtitle = "Schedule-aware",
                streakInfo = streaks.habitStreak,
                icon = Icons.Default.LocalFireDepartment,
                accentColor = Color(0xFFF97316),
                onClick = {
                    activeStreakDialog = StreakDialogData(
                        title = "Habits",
                        subtitle = "Daily Scheduled Goals",
                        info = streaks.habitStreak,
                        rule = "Complete all recurring habits scheduled for today before midnight. Unscheduled days do not break your streak continuity.",
                        color = Color(0xFFF97316),
                        icon = Icons.Default.LocalFireDepartment
                    )
                },
                modifier = Modifier.weight(1f)
            )
            StreakCard(
                title = "Detox",
                subtitle = "Min 1 hour daily",
                streakInfo = streaks.detoxStreak,
                icon = Icons.Default.Spa,
                accentColor = Color(0xFF8B5CF6),
                onClick = {
                    activeStreakDialog = StreakDialogData(
                        title = "Digital Detox",
                        subtitle = "Focus Screen-Free Window",
                        info = streaks.detoxStreak,
                        rule = "Requires a minimum of 1 hour (60 minutes) of completed detox focus sessions within a day to qualify. Abandoned or cancelled sessions do not qualify.",
                        color = Color(0xFF8B5CF6),
                        icon = Icons.Default.Spa
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StreakCard(
                title = "Movement",
                subtitle = "Daily step target",
                streakInfo = streaks.movementStreak,
                icon = Icons.Default.DirectionsWalk,
                accentColor = Color(0xFF10B981),
                onClick = {
                    activeStreakDialog = StreakDialogData(
                        title = "Movement",
                        subtitle = "Daily Step Target",
                        info = streaks.movementStreak,
                        rule = "Reach your configured daily step goal (e.g. 8,000 steps) through walking, running, or everyday movement recorded by device sensors.",
                        color = Color(0xFF10B981),
                        icon = Icons.Default.DirectionsWalk
                    )
                },
                modifier = Modifier.weight(1f)
            )
            StreakCard(
                title = "Workouts",
                subtitle = "Max 2 rest days",
                streakInfo = streaks.exerciseStreak,
                icon = Icons.Default.FitnessCenter,
                accentColor = Color(0xFF3B82F6),
                onClick = {
                    activeStreakDialog = StreakDialogData(
                        title = "Workouts",
                        subtitle = "Exercise Consistency",
                        info = streaks.exerciseStreak,
                        rule = "Log a qualifying exercise session. You are allowed up to 2 consecutive rest days between sessions without resetting your streak.",
                        color = Color(0xFF3B82F6),
                        icon = Icons.Default.FitnessCenter
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 3. Shared Weekly Grace Day Status (Clickable for details)
    val grace = weeklyGraceStatus ?: WeeklyGraceStatus(2026, 1, false, null, 1)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = OfflineSurface,
        border = BorderStroke(1.dp, OfflineBorder.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { showGraceDialog = true }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (grace.isGraceUsedThisWeek) Color(0xFFF59E0B).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Grace Day",
                            tint = if (grace.isGraceUsedThisWeek) Color(0xFFF59E0B) else Color(0xFF10B981),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Weekly Grace Day",
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = OfflineTextPrimary
                    )
                    Text(
                        text = if (grace.isGraceUsedThisWeek) "Used on ${grace.protectedDate} • Tap to view" else "Available (Mon–Sun) • Tap to view",
                        style = Typography.bodySmall.copy(fontSize = 12.sp),
                        color = OfflineTextSecondary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (grace.isGraceUsedThisWeek) Color(0xFFF59E0B).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f)
            ) {
                Text(
                    text = if (grace.isGraceUsedThisWeek) "USED" else "ACTIVE",
                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                    color = if (grace.isGraceUsedThisWeek) Color(0xFFD97706) else Color(0xFF059669),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // 4. Categorized Milestones & Badges with [View] toggle
    var showAllBadges by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Milestones & Badges",
                style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                color = OfflineTextPrimary
            )
            val unlockedTotal = achievements.count { it.isUnlocked }
            Text(
                text = "$unlockedTotal of ${achievements.size} badges unlocked",
                style = Typography.bodySmall,
                color = OfflineTextSecondary
            )
        }
        Surface(
            onClick = { showAllBadges = !showAllBadges },
            shape = RoundedCornerShape(8.dp),
            color = OfflineSurfaceVariant,
            border = BorderStroke(1.dp, OfflineBorder.copy(alpha = 0.8f))
        ) {
            Text(
                text = if (showAllBadges) "Hide" else "View",
                style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = OfflineTextPrimary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    if (!showAllBadges) {
        val previewBadges = achievements.sortedByDescending { it.isUnlocked }.take(3)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            previewBadges.forEach { achievement ->
                AchievementCard(
                    achievement = achievement,
                    onClick = { activeAchievementDialog = it }
                )
            }
        }
    } else {
        val categories = listOf(
            "DISCONNECT" to "Disconnect",
            "MOVE" to "Move & Activity",
            "RECONNECT" to "Reconnect & Life",
            "CONSISTENCY" to "Consistency & Streaks",
            "PERSONAL_GROWTH" to "Personal Growth"
        )

        categories.forEach { (catKey, catTitle) ->
            val catAchievements = achievements.filter { it.category == catKey }
            if (catAchievements.isNotEmpty()) {
                val unlockedCount = catAchievements.count { it.isUnlocked }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = catTitle,
                        style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = OfflineAccentPrimary
                    )
                    Text(
                        text = "$unlockedCount / ${catAchievements.size}",
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = OfflineTextMuted
                    )
                }

                catAchievements.forEach { achievement ->
                    AchievementCard(
                        achievement = achievement,
                        onClick = { activeAchievementDialog = it }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun StreakCard(
    title: String,
    subtitle: String,
    streakInfo: StreakCategoryInfo,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = OfflineSurface,
        border = BorderStroke(1.dp, OfflineBorder.copy(alpha = 0.5f)),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (streakInfo.isQualifiedToday) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Today ✓",
                            style = Typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = Color(0xFF059669),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${streakInfo.currentStreak} days",
                style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                color = OfflineTextPrimary
            )

            Text(
                text = "$title Streak",
                style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = OfflineTextSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Best: ${streakInfo.longestStreak}d • $subtitle",
                style = Typography.bodySmall.copy(fontSize = 11.sp),
                color = OfflineTextMuted
            )
        }
    }
}

@Composable
private fun AchievementCard(
    achievement: AchievementEntity,
    onClick: (AchievementEntity) -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick(achievement) },
        shape = RoundedCornerShape(16.dp),
        color = if (achievement.isUnlocked) OfflineSurfaceVariant.copy(alpha = 0.4f) else OfflineSurface,
        border = BorderStroke(
            1.dp,
            if (achievement.isUnlocked) OfflineAccentPrimary.copy(alpha = 0.3f) else OfflineBorder.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (achievement.isUnlocked) Color(0xFF10B981).copy(alpha = 0.15f) else OfflineSurfaceVariant,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (achievement.category) {
                                "DISCONNECT" -> Icons.Default.Spa
                                "MOVE" -> Icons.Default.DirectionsWalk
                                "RECONNECT" -> Icons.Default.Park
                                "CONSISTENCY" -> Icons.Default.LocalFireDepartment
                                else -> Icons.Default.CheckCircle
                            },
                            contentDescription = achievement.title,
                            tint = if (achievement.isUnlocked) Color(0xFF059669) else OfflineTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = achievement.title,
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                            color = OfflineTextPrimary
                        )
                        if (achievement.xpReward > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = OfflineAccentPrimary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "+${achievement.xpReward} XP",
                                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                    color = OfflineAccentPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = achievement.description,
                        style = Typography.bodySmall.copy(fontSize = 12.sp),
                        color = OfflineTextSecondary
                    )

                    if (!achievement.isUnlocked && achievement.progress > 0f) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { achievement.progress },
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = OfflineAccentPrimary,
                            trackColor = OfflineSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (achievement.isUnlocked) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Unlocked",
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = OfflineTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
