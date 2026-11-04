package com.example.detoxproject.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.detoxproject.R
import com.example.detoxproject.data.integrations.health.PhysicalActivitySummary
import com.example.detoxproject.data.integrations.usage.DailyUsageSummary
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.example.detoxproject.data.local.entity.PlannedActivityEntity
import com.example.detoxproject.data.local.entity.DetoxSessionEntity
import com.example.detoxproject.data.local.entity.UserGoalsEntity
import com.example.detoxproject.data.local.entity.WeatherCacheEntity
import com.example.detoxproject.domain.engine.HabitWithStatus
import com.example.detoxproject.domain.engine.WellnessRecommendation
import com.example.detoxproject.ui.components.UndoRedoActionBar
import com.example.detoxproject.ui.components.UserAvatarView
import com.example.detoxproject.ui.screens.progress.AddEditTaskDialog
import com.example.detoxproject.ui.screens.progress.CreateHabitDialog
import com.example.detoxproject.ui.theme.*
import java.time.LocalDate

@Composable
fun HomeScreen(
    userGoals: UserGoalsEntity?,
    activeSession: DetoxSessionEntity?,
    weather: WeatherCacheEntity?,
    activitySummary: PhysicalActivitySummary?,
    dailyUsage: DailyUsageSummary?,
    recommendation: WellnessRecommendation? = null,
    onAddRecommendationToPlanner: ((WellnessRecommendation) -> Unit)? = null,
    plannedActivities: List<PlannedActivityEntity> = emptyList(),
    todayHabits: List<HabitWithStatus> = emptyList(),
    onToggleHabit: (Long) -> Unit = {},
    onDeleteHabit: (Long) -> Unit = {},
    onCreateHabit: ((String, String, String, String, String, String) -> Unit)? = null,
    onAddTask: ((String, Int, String, String) -> Unit)? = null,
    onToggleActivity: (Long) -> Unit = {},
    onDeleteTask: ((Long) -> Unit)? = null,
    onRefreshWeather: () -> Unit = {},
    pastActionsCount: Int,
    futureActionsCount: Int,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleQuickDetox: () -> Unit = {},
    quickDetoxDurationSeconds: Int = 1800,
    onNavigateToFocus: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onNavigateToFitness: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    levelInfo: com.example.detoxproject.domain.engine.UserLevelInfo? = null,
    onNavigateToProgress: () -> Unit = {},
    todayActivity: com.example.detoxproject.data.local.entity.DailyActivityEntity? = null,
    externalMetrics: com.example.detoxproject.data.integrations.health.ExternalHealthMetrics? = null
) {
    val scrollState = rememberScrollState()
    var showCreateHabitDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }

    if (showCreateHabitDialog && onCreateHabit != null) {
        CreateHabitDialog(
            onDismiss = { showCreateHabitDialog = false },
            onConfirm = { title, desc, cat, icon, freq, days ->
                onCreateHabit(title, desc, cat, icon, freq, days)
                showCreateHabitDialog = false
            }
        )
    }

    if (showAddTaskDialog && onAddTask != null) {
        AddEditTaskDialog(
            scheduledDate = LocalDate.now(),
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, duration, category, description ->
                onAddTask(title, duration, category, description)
                showAddTaskDialog = false
            }
        )
    }

    val screenMinsTotal = (dailyUsage?.totalScreenTimeMs ?: 0L) / (1000 * 60)
    val screenHours = (screenMinsTotal / 60)
    val screenMins = (screenMinsTotal % 60)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OfflineBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Top Bar: Profile avatar + Greeting + tabzero. official logo branding
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onNavigateToProfile() }
            ) {
                UserAvatarView(
                    avatar = userGoals?.avatar,
                    size = 38.dp,
                    defaultName = userGoals?.userName ?: "User"
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "morning,",
                        style = Typography.labelSmall,
                        color = OfflineTextMuted
                    )
                    Text(
                        text = userGoals?.userName ?: "User",
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = OfflineTextPrimary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.tabzero_logo),
                    contentDescription = "tabzero. logo",
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "tabzero.",
                    style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
                    color = OfflineTextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Persistent 5-Step Undo/Redo Action Bar
        UndoRedoActionBar(
            pastCount = pastActionsCount,
            futureCount = futureActionsCount,
            onUndo = onUndo,
            onRedo = onRedo
        )

        if (levelInfo != null) {
            Spacer(modifier = Modifier.height(16.dp))
            HomeProgressionCard(
                levelInfo = levelInfo,
                onNavigateToProgress = onNavigateToProgress
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Large minimalist heading from Reference Image 1
        Text(
            text = "one. two. focus!",
            style = Typography.headlineLarge.copy(fontSize = 32.sp, fontWeight = FontWeight.SemiBold),
            color = OfflineTextPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick "+ Add Plan" Pill (Reference 1)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable { onNavigateToFocus() },
            shape = RoundedCornerShape(24.dp),
            color = OfflineSurfaceVariant.copy(alpha = 0.6f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New plan",
                    tint = OfflineTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1-Tap Instant Quick Detox Toggle (Active / Inactive)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleQuickDetox() },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (activeSession?.status == "ACTIVE") SoftRose else SoftGreen
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, OfflineBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (activeSession?.status == "ACTIVE") Color(0xFFEF4444) else Color(0xFF10B981)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (activeSession?.status == "ACTIVE") Icons.Default.Shield else Icons.Default.Spa,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (activeSession?.status == "ACTIVE") "Detox Mode: ON" else "Detox Mode: OFF",
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = OfflineTextPrimary
                        )
                        val quickDurationLabel = remember(quickDetoxDurationSeconds) {
                            val h = quickDetoxDurationSeconds / 3600
                            val m = (quickDetoxDurationSeconds % 3600) / 60
                            val s = quickDetoxDurationSeconds % 60
                            when {
                                h > 0 && m > 0 -> "${h}h ${m}m"
                                h > 0 && s > 0 -> "${h}h ${s}s"
                                h > 0 -> "${h}h"
                                m > 0 && s > 0 -> "${m}m ${s}s"
                                m > 0 -> "${m}m"
                                else -> "${s}s"
                            }
                        }
                        Text(
                            text = if (activeSession?.status == "ACTIVE") "Apps restricted • Tap to turn OFF" else "Tap to turn ON ($quickDurationLabel Quick Detox)",
                            style = Typography.bodySmall,
                            color = OfflineTextSecondary
                        )
                    }
                }

                Switch(
                    checked = activeSession?.status == "ACTIVE",
                    onCheckedChange = { onToggleQuickDetox() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFEF4444)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4 Category Quadrants (Direct Reproduction of Reference Image 1)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Work (SoftGreen)
            CategoryQuadrant(
                title = "work",
                subtitle = "25m",
                icon = Icons.Outlined.WorkOutline,
                backgroundColor = SoftGreen,
                modifier = Modifier.weight(1f),
                onClick = { onCategoryClick("work") }
            )

            // Personal (SoftBlue)
            CategoryQuadrant(
                title = "personal",
                subtitle = "20m",
                icon = Icons.Outlined.Person,
                backgroundColor = SoftBlue,
                modifier = Modifier.weight(1f),
                onClick = { onCategoryClick("personal") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Reading (SoftBeige)
            CategoryQuadrant(
                title = "reading",
                subtitle = "30m",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                backgroundColor = SoftBeige,
                modifier = Modifier.weight(1f),
                onClick = { onCategoryClick("reading") }
            )

            // Sleep (SoftTeal)
            CategoryQuadrant(
                title = "sleep",
                subtitle = "8h",
                icon = Icons.Outlined.NightsStay,
                backgroundColor = SoftTeal,
                modifier = Modifier.weight(1f),
                onClick = { onCategoryClick("sleep") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Active Detox Session or Quick Status
        if (activeSession != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToFocus() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = OfflineDarkButton)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (activeSession.status == "PAUSED") "DETOX PAUSED" else "DETOX IN PROGRESS",
                            style = Typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = OfflineAccentCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeSession.title,
                            style = Typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = "Target: ${activeSession.targetMinutes} min • Tap to view timer",
                            style = Typography.bodyMedium,
                            color = OfflineBorder
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = OfflineAccentCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Daily Wellness Pulse: Real Sensor Steps, Real Screen Time, Real Direct Open-Meteo Weather
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = OfflineSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, OfflineBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Daily Wellness Pulse",
                    style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = OfflineTextPrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onNavigateToFitness() }
                            .padding(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Steps Today",
                                style = Typography.labelSmall,
                                color = OfflineTextMuted
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "View Fitness",
                                tint = OfflineTextMuted,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        val steps = todayActivity?.steps ?: 0
                        val goal = todayActivity?.goalSteps ?: userGoals?.dailySteps ?: 8000
                        val distanceKm = (todayActivity?.distanceMeters ?: 0f) / 1000f
                        val calories = steps * 0.04f

                        Text(
                            text = "%,d".format(steps),
                            style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = OfflineTextPrimary
                        )
                        Text(
                            text = if (steps > 0) "%.2f km • %.0f kcal [steps]".format(distanceKm, calories) else "Goal: %,d".format(goal),
                            style = Typography.labelSmall,
                            color = OfflineTextSecondary
                        )
                    }

                    Column {
                        Text(
                            text = "Screen Time",
                            style = Typography.labelSmall,
                            color = OfflineTextMuted
                        )
                        val screenTimeDisplay = if (dailyUsage?.hasPermission == true) {
                            if (screenHours > 0) "${screenHours}h ${screenMins}m" else "${screenMins}m"
                        } else {
                            "Pending"
                        }
                        Text(
                            text = screenTimeDisplay,
                            style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = OfflineTextPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Weather",
                                style = Typography.labelSmall,
                                color = OfflineTextMuted
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Weather",
                                tint = OfflineTextSecondary,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { onRefreshWeather() }
                            )
                        }
                        Text(
                            text = "${weather?.temperature?.toInt() ?: 25}°C",
                            style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = OfflineTextPrimary
                        )
                        Text(
                            text = weather?.conditionText?.take(10) ?: "Pleasant",
                            style = Typography.labelSmall,
                            color = OfflineTextSecondary
                        )
                    }
                }
            }
        }

        // Today's Habits & Tasks (Phase 4 Date-Based Engine)
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today's Habits & Tasks",
                style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = OfflineTextPrimary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (onCreateHabit != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = OfflineDarkButton,
                        modifier = Modifier
                            .clickable { showCreateHabitDialog = true }
                            .padding(2.dp)
                    ) {
                        Text(
                            text = "+ Habit",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = OfflineOnDarkButton,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                if (onAddTask != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = OfflineSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, OfflineBorder),
                        modifier = Modifier
                            .clickable { showAddTaskDialog = true }
                            .padding(2.dp)
                    ) {
                        Text(
                            text = "+ Task",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = OfflineTextPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val todayStr = remember { LocalDate.now().toString() }
        val todayTasks = remember(plannedActivities) {
            plannedActivities.filter {
                it.scheduledDate == todayStr && !it.description.contains("digital detox session")
            }
        }

        if (todayHabits.isEmpty() && todayTasks.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = OfflineSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, OfflineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "No habits or tasks scheduled for today.",
                        style = Typography.bodySmall,
                        color = OfflineTextSecondary
                    )
                    Text(
                        text = "Add Habit",
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = OfflineAccentPrimary,
                        modifier = Modifier.clickable { showCreateHabitDialog = true }
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 1. Recurring Habits
                todayHabits.forEach { habitStatus ->
                    val isDone = habitStatus.isCompletedForDate
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (!isDone) Modifier.clickable { onToggleHabit(habitStatus.habit.id) }
                                else Modifier
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDone) SoftGreen.copy(alpha = 0.5f) else OfflineSurface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDone) SoftGreen else OfflineBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = isDone,
                                    enabled = !isDone,
                                    onCheckedChange = { if (!isDone) onToggleHabit(habitStatus.habit.id) }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = habitStatus.habit.title,
                                        style = Typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        color = if (isDone) OfflineTextMuted else OfflineTextPrimary
                                    )
                                    if (habitStatus.habit.description.isNotBlank()) {
                                        Text(
                                            text = habitStatus.habit.description,
                                            style = Typography.bodySmall,
                                            color = OfflineTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (habitStatus.stats.currentStreak > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFEF3C7)
                                    ) {
                                        Text(
                                            text = "🔥 ${habitStatus.stats.currentStreak}d",
                                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFFB45309),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onDeleteHabit(habitStatus.habit.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete habit",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Planned Tasks
                todayTasks.forEach { task ->
                    val isDone = task.status == "COMPLETED"
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleActivity(task.id) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDone) SoftGreen.copy(alpha = 0.5f) else OfflineSurface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDone) SoftGreen else OfflineBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = isDone,
                                    onCheckedChange = { onToggleActivity(task.id) }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = task.title,
                                        style = Typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        color = if (isDone) OfflineTextMuted else OfflineTextPrimary
                                    )
                                    if (task.description.isNotBlank()) {
                                        Text(
                                            text = task.description,
                                            style = Typography.bodySmall,
                                            color = OfflineTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = OfflineSurfaceVariant
                                ) {
                                    Text(
                                        text = "${task.durationMinutes}m",
                                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = OfflineTextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                if (onDeleteTask != null) {
                                    IconButton(
                                        onClick = { onDeleteTask(task.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete task",
                                            tint = OfflineTextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Weather-Aware Offline Recommendation Card (Requirements 7 & 8)
        if (recommendation != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = if (recommendation.isOutdoor) SoftGreen else SoftBlue),
                border = androidx.compose.foundation.BorderStroke(1.dp, OfflineBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (recommendation.isOutdoor) "OUTDOOR RECOMMENDATION" else "INDOOR HABIT",
                            style = Typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                            color = OfflineTextSecondary
                        )
                        Text(
                            text = "${recommendation.durationMinutes} min",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = OfflineTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = recommendation.title,
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = OfflineTextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = recommendation.reason,
                        style = Typography.bodySmall,
                        color = OfflineTextSecondary
                    )

                    if (onAddRecommendationToPlanner != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onAddRecommendationToPlanner(recommendation) },
                            colors = ButtonDefaults.buttonColors(containerColor = OfflineDarkButton),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = OfflineOnDarkButton,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add to Planner", color = OfflineOnDarkButton, style = Typography.labelSmall)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun CategoryQuadrant(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = OfflineTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = subtitle,
                    style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = OfflineTextSecondary
                )
            }

            Text(
                text = title,
                style = Typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = OfflineTextPrimary
            )
        }
    }
}
