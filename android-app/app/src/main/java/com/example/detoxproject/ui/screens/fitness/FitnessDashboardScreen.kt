package com.example.detoxproject.ui.screens.fitness

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.detoxproject.data.integrations.health.ExternalExerciseItem
import com.example.detoxproject.data.integrations.health.ExternalHealthMetrics
import com.example.detoxproject.data.integrations.health.HealthConnectStatus
import com.example.detoxproject.data.integrations.usage.DailyUsageSummary
import com.example.detoxproject.data.local.entity.DailyActivityEntity
import com.example.detoxproject.data.local.entity.DetoxSessionEntity
import com.example.detoxproject.data.local.entity.ExerciseSessionEntity
import com.example.detoxproject.data.local.entity.PlannedActivityEntity
import com.example.detoxproject.data.local.entity.UserGoalsEntity
import com.example.detoxproject.ui.theme.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.ui.text.style.TextOverflow
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessDashboardScreen(
    todayActivity: DailyActivityEntity?,
    weeklyActivity: List<DailyActivityEntity>,
    activeExercise: ExerciseSessionEntity?,
    recentExercises: List<ExerciseSessionEntity>,
    externalMetrics: ExternalHealthMetrics,
    externalExercises: List<ExternalExerciseItem>,
    healthConnectStatus: HealthConnectStatus,
    hasHardwareSensor: Boolean,
    currentTimeMs: Long,
    userGoals: UserGoalsEntity? = null,
    allSessions: List<DetoxSessionEntity> = emptyList(),
    allActivities: List<PlannedActivityEntity> = emptyList(),
    allDailyActivities: List<DailyActivityEntity> = emptyList(),
    dailyUsage: DailyUsageSummary? = null,
    activeSession: DetoxSessionEntity? = null,
    onNavigateBack: () -> Unit,
    onNavigateToFocus: () -> Unit = {},
    onSync: () -> Unit,
    onUpdateGoal: (Int) -> Unit,
    onStartExercise: (title: String, type: String, notes: String?) -> Unit,
    onStopExercise: (notes: String?) -> Unit,
    onCancelExercise: () -> Unit,
    onOpenHealthConnectSettings: () -> Unit,
    onRequestHealthConnectPermissions: () -> Unit
) {
    var showGoalDialog by remember { mutableStateOf(false) }
    var selectedExerciseType by remember { mutableStateOf("Walking") }
    var exerciseTitleInput by remember { mutableStateOf("") }
    var showWorkoutDialog by remember { mutableStateOf(false) }
    var selectedDayOffset by remember { mutableIntStateOf(0) }

    val todaySteps = todayActivity?.steps ?: 0
    val goalSteps = todayActivity?.goalSteps ?: userGoals?.dailySteps ?: 8000
    val distanceKm = (todayActivity?.distanceMeters ?: 0f) / 1000f
    val activeCalories = maxOf(
        externalMetrics.todayActiveCaloriesKcal ?: 0f,
        todayActivity?.activeCalories ?: 0f,
        todaySteps * 0.04f
    )
    val activeMinutes = todayActivity?.activeMinutes ?: 0
    val stepProgress = if (goalSteps > 0) (todaySteps.toFloat() / goalSteps).coerceIn(0f, 1f) else 0f
    val progressPercent = (stepProgress * 100).toInt()

    val groupedActivities = remember(recentExercises, allSessions, allActivities, externalExercises) {
        val items = mutableListOf<ActivityHistoryItem>()

        // 1. Recorded Exercise sessions
        recentExercises.forEach { ex ->
            items.add(
                ActivityHistoryItem(
                    id = "ex_${ex.id}",
                    title = ex.title,
                    subtitle = "${ex.activityType.lowercase().replaceFirstChar { it.uppercase() }} Workout",
                    durationText = "${(ex.durationSeconds / 60).coerceAtLeast(1)} mins",
                    metricText = if (ex.distanceMeters != null && ex.distanceMeters > 0f) "%.2f km".format(ex.distanceMeters / 1000f)
                        else ex.notes?.takeIf { it.isNotBlank() } ?: "Active Workout",
                    dateIso = ex.sessionDate,
                    timestampMs = ex.startTime,
                    iconType = ex.activityType
                )
            )
        }

        val sessionKeys = allSessions.map { "${it.title}_${it.sessionDate}" }.toSet()

        // 2. Completed focus / detox sessions
        allSessions.filter { it.status == "COMPLETED" }.forEach { ses ->
            val mins = ses.actualMinutes.coerceAtLeast(1)
            items.add(
                ActivityHistoryItem(
                    id = "detox_${ses.id}",
                    title = ses.title.ifBlank { "Focus Session" },
                    subtitle = "Completed ${mins}m digital detox session",
                    durationText = "${mins}m",
                    metricText = null,
                    dateIso = ses.sessionDate.ifBlank { LocalDate.now().toString() },
                    timestampMs = ses.startTime,
                    iconType = "FOCUS"
                )
            )
        }

        // 3. Completed offline activities / planned tasks
        allActivities.filter { it.status == "COMPLETED" }.forEach { task ->
            val isDetoxSession = task.description.contains("digital detox session")
            val key = "${task.title}_${task.scheduledDate}"
            if (!isDetoxSession || !sessionKeys.contains(key)) {
                items.add(
                    ActivityHistoryItem(
                        id = "task_${task.id}",
                        title = task.title,
                        subtitle = task.description.ifBlank { "Unplugged offline activity" },
                        durationText = "${task.durationMinutes}m",
                        metricText = null,
                        dateIso = task.scheduledDate.ifBlank { LocalDate.now().toString() },
                        timestampMs = task.completedAt ?: 0L,
                        iconType = if (isDetoxSession) "FOCUS" else "OFFLINE"
                    )
                )
            }
        }

        // 4. External Health Connect exercises
        externalExercises.forEach { ext ->
            val extDate = try {
                Instant.ofEpochMilli(ext.startTime).atZone(ZoneId.systemDefault()).toLocalDate().toString()
            } catch (_: Exception) {
                LocalDate.now().toString()
            }
            items.add(
                ActivityHistoryItem(
                    id = "ext_${ext.startTime}_${ext.title}",
                    title = ext.title,
                    subtitle = "Health Connect • ${ext.exerciseType}",
                    durationText = "${ext.durationMinutes} mins",
                    metricText = null,
                    dateIso = extDate,
                    timestampMs = ext.startTime,
                    iconType = "HEALTH_CONNECT"
                )
            )
        }

        items.sortedByDescending { it.timestampMs }.groupBy { it.dateIso }
    }

    val monthGroups = remember(groupedActivities) {
        val map = mutableMapOf<YearMonth, MutableList<Pair<String, List<ActivityHistoryItem>>>>()
        groupedActivities.forEach { (dateIso, activities) ->
            val ym = try {
                val parsed = LocalDate.parse(dateIso)
                YearMonth.of(parsed.year, parsed.month)
            } catch (_: Exception) {
                YearMonth.now()
            }
            map.getOrPut(ym) { mutableListOf() }.add(Pair(dateIso, activities))
        }
        map.toList().sortedByDescending { it.first }
    }

    var selectedDateForDetails by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = selectedDateForDetails != null) {
        selectedDateForDetails = null
    }

    Scaffold(
        containerColor = OfflineBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (selectedDateForDetails != null) {
                                try {
                                    val parsed = LocalDate.parse(selectedDateForDetails)
                                    "Activities • ${parsed.dayOfMonth}/${parsed.monthValue}/${parsed.year}"
                                } catch (_: Exception) {
                                    "Date Activities"
                                }
                            } else {
                                "Fitness & Activity"
                            },
                            style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = OfflineTextPrimary
                        )
                        Text(
                            text = if (selectedDateForDetails != null) {
                                "Tap back to return to month boxes"
                            } else {
                                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))
                            },
                            style = Typography.labelSmall,
                            color = OfflineTextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedDateForDetails != null) {
                            selectedDateForDetails = null
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (selectedDateForDetails != null) "Back to Dates" else "Back to Home",
                            tint = OfflineTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSync) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync",
                            tint = OfflineTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OfflineBackground
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. TODAY'S ACTIVITY SUMMARY HERO CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = OfflineSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(OfflineBorder))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Source Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (todaySteps > 0) Color(0xFF10B981) else Color(0xFFF59E0B))
                                )
                                Text(
                                    text = if (todayActivity?.source == "HEALTH_CONNECT") "Health Connect Synced"
                                    else if (hasHardwareSensor) "Hardware Step Counter"
                                    else "Step Counter Standby",
                                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = OfflineTextSecondary
                                )
                            }

                            // Goal Chip (Clickable to edit)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = OfflineSurfaceVariant,
                                modifier = Modifier.clickable { showGoalDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "Edit Goal",
                                        tint = OfflineTextMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Goal: $goalSteps",
                                        style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = OfflineTextPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Circular Progress Ring & Step Display
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(180.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { 1f },
                                modifier = Modifier.fillMaxSize(),
                                color = OfflineSurfaceVariant,
                                strokeWidth = 14.dp,
                                trackColor = Color.Transparent
                            )
                            CircularProgressIndicator(
                                progress = { stepProgress },
                                modifier = Modifier.fillMaxSize(),
                                color = OfflineAccentPrimary,
                                strokeWidth = 14.dp,
                                trackColor = Color.Transparent,
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "%,d".format(todaySteps),
                                    style = Typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                                    color = OfflineTextPrimary
                                )
                                Text(
                                    text = "of %,d steps".format(goalSteps),
                                    style = Typography.labelMedium,
                                    color = OfflineTextMuted
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (stepProgress >= 1f) Color(0xFF10B981).copy(alpha = 0.15f) else OfflineAccentPrimary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (stepProgress >= 1f) "Goal Achieved!" else "$progressPercent%",
                                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (stepProgress >= 1f) Color(0xFF10B981) else OfflineAccentPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // Three Metrics Row (Distance, Calories, Active Mins)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            MetricItem(
                                icon = Icons.Outlined.Straighten,
                                label = "Distance",
                                value = "%.2f km".format(distanceKm)
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(36.dp)
                                    .background(OfflineBorder)
                            )
                            MetricItem(
                                icon = Icons.Outlined.LocalFireDepartment,
                                label = "Calories [steps only]",
                                value = "%.0f kcal".format(activeCalories)
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(36.dp)
                                    .background(OfflineBorder)
                            )
                            MetricItem(
                                icon = Icons.Outlined.Timer,
                                label = "Active",
                                value = "$activeMinutes min"
                            )
                        }
                    }
                }
            }

            // 2. DAILY OFFLINE FOCUS & DETOX PLAN USAGE
            item {
                val todayStart = remember(currentTimeMs) {
                    Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                }
                val todayCompletedSessions = remember(allSessions, todayStart) {
                    allSessions.filter { it.status == "COMPLETED" && it.startTime >= todayStart }
                }
                val completedOfflineMinutes = todayCompletedSessions.sumOf { it.actualMinutes }
                val activeSessionMinutes = if (activeSession != null && (activeSession.status == "ACTIVE" || activeSession.status == "PAUSED")) {
                    ((currentTimeMs - activeSession.startTime) / (1000 * 60)).toInt().coerceAtLeast(0)
                } else 0
                val totalOfflineMinutesToday = completedOfflineMinutes + activeSessionMinutes
                val offlineGoal = userGoals?.dailyOfflineMinutes ?: 120
                val offlineProgress = if (offlineGoal > 0) (totalOfflineMinutesToday.toFloat() / offlineGoal).coerceIn(0f, 1f) else 0f
                val isGoalMet = totalOfflineMinutesToday >= offlineGoal

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = OfflineSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OfflineBorder)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = OfflineAccentViolet.copy(alpha = 0.35f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.Spa,
                                            contentDescription = null,
                                            tint = OfflineAccentPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Offline Focus & Plan Usage",
                                        style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = OfflineTextPrimary
                                    )
                                    Text(
                                        text = "Detox balance vs. daily target",
                                        style = Typography.bodySmall,
                                        color = OfflineTextSecondary
                                    )
                                }
                            }

                            // Goal Chip
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isGoalMet) Color(0xFF10B981).copy(alpha = 0.15f) else OfflineSurfaceVariant
                            ) {
                                Text(
                                    text = if (isGoalMet) "Goal Met 🌿" else "Goal: ${offlineGoal}m",
                                    style = Typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isGoalMet) Color(0xFF059669) else OfflineTextPrimary
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Large Progress Display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "$totalOfflineMinutesToday",
                                        style = Typography.headlineLarge.copy(fontSize = 36.sp, fontWeight = FontWeight.Bold),
                                        color = OfflineTextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "of $offlineGoal min",
                                        style = Typography.titleMedium,
                                        color = OfflineTextSecondary,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                                Text(
                                    text = if (todayCompletedSessions.isEmpty() && activeSession == null) {
                                        "No detox sessions logged yet today"
                                    } else {
                                        "${todayCompletedSessions.size} session${if (todayCompletedSessions.size == 1) "" else "s"} completed today"
                                    },
                                    style = Typography.bodySmall,
                                    color = OfflineTextMuted
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isGoalMet) Color(0xFF10B981).copy(alpha = 0.15f) else SoftBlue.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = "${(offlineProgress * 100).toInt()}%",
                                    style = Typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isGoalMet) Color(0xFF059669) else OfflineTextPrimary
                                    ),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { offlineProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = if (isGoalMet) Color(0xFF10B981) else Color(0xFF8B5CF6),
                            trackColor = OfflineBorder
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Status & Guidance Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val remaining = (offlineGoal - totalOfflineMinutesToday).coerceAtLeast(0)
                            Text(
                                text = if (isGoalMet) {
                                    "✨ Excellent! Daily offline focus target achieved."
                                } else {
                                    "$remaining min left to reach today's target"
                                },
                                style = Typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = if (isGoalMet) Color(0xFF059669) else OfflineTextSecondary
                            )

                            if (activeSession != null && activeSession.status == "ACTIVE") {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF8B5CF6).copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF8B5CF6))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Active Now",
                                            style = Typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                            color = Color(0xFF8B5CF6)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. ONGOING EXERCISE WORKOUT CARD (IF ACTIVE)
            if (activeExercise != null) {
                item {
                    val elapsedSeconds = ((currentTimeMs - activeExercise.startTime) / 1000L).coerceAtLeast(0L)
                    val hours = elapsedSeconds / 3600
                    val minutes = (elapsedSeconds % 3600) / 60
                    val seconds = elapsedSeconds % 60
                    val timeString = if (hours > 0) {
                        "%02d:%02d:%02d".format(hours, minutes, seconds)
                    } else {
                        "%02d:%02d".format(minutes, seconds)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = OfflineSurface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(OfflineAccentPrimary))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = getActivityIcon(activeExercise.activityType),
                                        contentDescription = null,
                                        tint = OfflineAccentPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = activeExercise.title,
                                            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = OfflineTextPrimary
                                        )
                                        Text(
                                            text = "${activeExercise.activityType} • Live Recording",
                                            style = Typography.labelSmall,
                                            color = OfflineTextMuted
                                        )
                                    }
                                }

                                Text(
                                    text = timeString,
                                    style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = OfflineAccentPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onCancelExercise,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OfflineTextSecondary)
                                ) {
                                    Text("Discard", style = Typography.labelMedium)
                                }

                                Button(
                                    onClick = { onStopExercise(null) },
                                    modifier = Modifier.weight(2f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = OfflineDarkButton,
                                        contentColor = OfflineOnDarkButton
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Finish Workout", style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
            }

            // 3. START WORKOUT / RECORD EXERCISE CARD
            if (activeExercise == null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = OfflineSurface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(OfflineBorder))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Record Exercise",
                                    style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = OfflineTextPrimary
                                )
                                Text(
                                    text = "Independent tracking",
                                    style = Typography.labelSmall,
                                    color = OfflineTextMuted
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Activity Type Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val types = listOf("Walking", "Running", "Cycling", "Strength", "Other")
                                types.forEach { type ->
                                    val isSelected = selectedExerciseType.equals(type, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) OfflineAccentPrimary else OfflineSurfaceVariant,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedExerciseType = type }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = getActivityIcon(type),
                                                contentDescription = type,
                                                tint = if (isSelected) Color(0xFF1E1B4B) else OfflineTextSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = type,
                                                style = Typography.labelSmall.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 11.sp
                                                ),
                                                color = if (isSelected) Color(0xFF1E1B4B) else OfflineTextSecondary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    val title = exerciseTitleInput.ifBlank { "$selectedExerciseType Session" }
                                    onStartExercise(title, selectedExerciseType, null)
                                    exerciseTitleInput = ""
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OfflineDarkButton,
                                    contentColor = OfflineOnDarkButton
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start $selectedExerciseType Workout", style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }

            // 4. WEEKLY 7-DAY BAR CHART
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = OfflineSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(OfflineBorder))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Weekly Step History",
                                style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = OfflineTextPrimary
                            )
                            val totalWeekSteps = weeklyActivity.sumOf { it.steps }
                            Text(
                                text = "Total: %,d".format(totalWeekSteps),
                                style = Typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = OfflineTextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Build 7-day list
                        val daysMap = weeklyActivity.associateBy { it.date }
                        val today = LocalDate.now()
                        val last7Days = (6 downTo 0).map { offset ->
                            val date = today.minusDays(offset.toLong())
                            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            val activity = daysMap[dateStr]
                            DayStepData(
                                dayLabel = date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())),
                                offset = offset,
                                isToday = offset == 0,
                                steps = activity?.steps ?: 0,
                                goal = activity?.goalSteps ?: goalSteps
                            )
                        }

                        val maxSteps = (last7Days.maxOfOrNull { it.steps } ?: goalSteps).coerceAtLeast(goalSteps)

                        // 7-Day Chart Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            last7Days.forEach { day ->
                                val barHeightFraction = if (maxSteps > 0) (day.steps.toFloat() / maxSteps).coerceIn(0.06f, 1f) else 0.06f
                                val metGoal = day.steps >= day.goal && day.steps > 0
                                val isSelected = day.offset == selectedDayOffset

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedDayOffset = if (selectedDayOffset == day.offset && day.offset != 0) 0 else day.offset
                                        }
                                        .padding(vertical = 2.dp)
                                ) {
                                    // Steps count above bar
                                    if (day.steps > 0) {
                                        Text(
                                            text = if (day.steps >= 1000) "%.1fk".format(day.steps / 1000f) else "${day.steps}",
                                            style = Typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (isSelected) OfflineAccentPrimary else if (day.isToday) OfflineAccentPrimary else OfflineTextMuted
                                        )
                                    } else {
                                        Text(text = "-", style = Typography.labelSmall.copy(fontSize = 9.sp), color = OfflineTextMuted)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Bar with interactive selection border
                                    val barHeight = (76.dp * barHeightFraction).coerceIn(6.dp, 76.dp)
                                    Box(
                                        modifier = Modifier
                                            .width(if (isSelected) 24.dp else 22.dp)
                                            .height(barHeight)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(
                                                when {
                                                    isSelected -> OfflineAccentPrimary
                                                    day.isToday -> OfflineAccentPrimary.copy(alpha = 0.85f)
                                                    metGoal -> Color(0xFF10B981)
                                                    day.steps > 0 -> OfflineSurfaceVariant
                                                    else -> OfflineSurfaceVariant.copy(alpha = 0.5f)
                                                }
                                            )
                                            .then(
                                                if (isSelected) Modifier.border(1.5.dp, OfflineTextPrimary, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                                else Modifier
                                            )
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Day label
                                    Text(
                                        text = day.dayLabel,
                                        style = Typography.labelSmall.copy(
                                            fontWeight = if (isSelected || day.isToday) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 10.sp
                                        ),
                                        color = if (isSelected) OfflineAccentPrimary else if (day.isToday) OfflineTextPrimary else OfflineTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. DAILY ENERGY & RECOVERY CARD (Dynamically inspects selected day)
            item {
                val today = LocalDate.now()
                val daysMap = weeklyActivity.associateBy { it.date }
                val selectedDate = today.minusDays(selectedDayOffset.toLong())
                val selectedDateStr = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val selectedActivity = daysMap[selectedDateStr]
                val selectedSteps = if (selectedDayOffset == 0) todaySteps else (selectedActivity?.steps ?: 0)

                val selectedCalories = if (selectedDayOffset == 0) {
                    maxOf(
                        externalMetrics.todayActiveCaloriesKcal ?: 0f,
                        todayActivity?.activeCalories ?: 0f,
                        todaySteps * 0.04f
                    )
                } else {
                    if (selectedActivity != null && selectedActivity.activeCalories > 0f) {
                        selectedActivity.activeCalories
                    } else {
                        selectedSteps * 0.04f
                    }
                }

                val selectedSleepMinutes = if (selectedDayOffset == 0) {
                    externalMetrics.lastSleepDurationMinutes ?: dailyUsage?.lastNightRestMinutes
                } else {
                    externalMetrics.dailySleepMinutes[selectedDateStr]
                        ?: if (selectedDayOffset == 1) (externalMetrics.lastSleepDurationMinutes ?: dailyUsage?.lastNightRestMinutes)
                        else null
                }

                val selectedSleepSource = when (selectedDayOffset) {
                    0 -> externalMetrics.sleepSourceDescription ?: "Last night's sleep"
                    1 -> "Yesterday's rest window"
                    else -> "${selectedDate.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))} rest window"
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = OfflineSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(OfflineBorder))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Daily Energy & Recovery",
                                    style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = OfflineTextPrimary
                                )
                                // Small grey text visible in dark mode: only when days changed (selectedDayOffset > 0)
                                // Only for yesterday it should show "yesterday", for other previous dates it shows that day's name
                                if (selectedDayOffset > 0) {
                                    val dayTag = if (selectedDayOffset == 1) {
                                        "yesterday"
                                    } else {
                                        selectedDate.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))
                                    }
                                    Text(
                                        text = dayTag,
                                        style = Typography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Normal),
                                        color = OfflineTextSecondary
                                    )
                                }
                            }

                            // Status Indicator
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (externalMetrics.arePermissionsGranted) Color(0xFF10B981).copy(alpha = 0.15f)
                                else Color(0xFFF59E0B).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (externalMetrics.arePermissionsGranted) "Synced" else "Active",
                                    style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (externalMetrics.arePermissionsGranted) Color(0xFF10B981) else Color(0xFFF59E0B),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Calories Burned & Sleep Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Calories Burned Box
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = OfflineSurfaceVariant
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.LocalFireDepartment,
                                            contentDescription = "Calories Burned",
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "Active Calories", style = Typography.labelSmall, color = OfflineTextMuted)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "steps only",
                                                style = Typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                color = Color(0xFFD97706),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "%.0f kcal".format(selectedCalories),
                                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = OfflineTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val calorieGoal = userGoals?.targetCalories ?: 400
                                    val calPercent = if (calorieGoal > 0) ((selectedCalories / calorieGoal) * 100).toInt() else 0
                                    Text(
                                        text = "$calPercent% of $calorieGoal kcal goal",
                                        style = Typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                        color = if (selectedCalories >= calorieGoal) Color(0xFF10B981) else OfflineTextSecondary
                                    )
                                }
                            }

                            // Sleep Box
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = OfflineSurfaceVariant
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Bedtime,
                                            contentDescription = "Sleep",
                                            tint = Color(0xFF6366F1),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "Sleep Record", style = Typography.labelSmall, color = OfflineTextMuted)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (selectedSleepMinutes != null && selectedSleepMinutes > 0) {
                                            val h = selectedSleepMinutes / 60
                                            val m = selectedSleepMinutes % 60
                                            "${h}h ${m}m"
                                        } else {
                                            "No sleep records"
                                        },
                                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = OfflineTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = selectedSleepSource,
                                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                                        color = OfflineTextSecondary
                                    )
                                }
                            }
                        }

                        // Optional Heart Rate Pill if a wearable recorded it
                        if (externalMetrics.latestHeartRateBpm != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = OfflineSurfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Favorite,
                                        contentDescription = "Heart Rate",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Resting Heart Rate: ${externalMetrics.latestHeartRateBpm} bpm",
                                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = OfflineTextPrimary
                                    )
                                }
                            }
                        }

                        // Permission / Settings Button if not connected
                        if (!externalMetrics.arePermissionsGranted) {
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = {
                                    if (healthConnectStatus == HealthConnectStatus.AVAILABLE_INSTALLED) {
                                        onRequestHealthConnectPermissions()
                                    } else {
                                        onOpenHealthConnectSettings()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (healthConnectStatus == HealthConnectStatus.AVAILABLE_INSTALLED) "Grant Health Connect Access"
                                    else "Set Up Health Connect",
                                    style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }
            }

            // 6. RECENT WORKOUTS GROUPED BY MONTH BOXES & DATES
            if (selectedDateForDetails != null) {
                // DATE DETAIL VIEW (when user taps a date inside the month box)
                val targetDateIso = selectedDateForDetails!!
                val dateActivities = groupedActivities[targetDateIso] ?: emptyList()
                val (formattedDate, tagLabel) = try {
                    val parsed = LocalDate.parse(targetDateIso)
                    val dateStr = "${parsed.dayOfMonth}/${parsed.monthValue}/${parsed.year}"
                    val today = LocalDate.now()
                    val rel = when (parsed) {
                        today -> "Today"
                        today.minusDays(1) -> "Yesterday"
                        else -> parsed.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    }
                    Pair(dateStr, rel)
                } catch (_: Exception) {
                    Pair(targetDateIso, "")
                }

                item(key = "date_detail_header") {
                    Surface(
                        onClick = { selectedDateForDetails = null },
                        shape = RoundedCornerShape(14.dp),
                        color = OfflineSurfaceVariant.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, OfflineBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to month boxes",
                                tint = OfflineAccentPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "← Back to month boxes & dates",
                                    style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = OfflineAccentPrimary
                                )
                                Text(
                                    text = "Tap to choose another date",
                                    style = Typography.labelSmall,
                                    color = OfflineTextMuted
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "[$formattedDate] Activities",
                                style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = OfflineTextPrimary
                            )
                            if (tagLabel.isNotBlank()) {
                                Text(
                                    text = tagLabel,
                                    style = Typography.labelSmall,
                                    color = OfflineTextSecondary
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SoftBlue.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, SoftBlue)
                        ) {
                            Text(
                                text = "[${dateActivities.size} activities]",
                                style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = OfflineTextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (dateActivities.isEmpty()) {
                    item(key = "empty_date_activities") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = OfflineSurface),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(OfflineBorder))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No activities recorded on this date.",
                                    style = Typography.bodyMedium,
                                    color = OfflineTextMuted
                                )
                            }
                        }
                    }
                } else {
                    items(dateActivities, key = { it.id }) { activity ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = OfflineSurface),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(OfflineBorder))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = OfflineSurfaceVariant,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = getActivityIcon(activity.iconType),
                                                contentDescription = null,
                                                tint = if (activity.iconType == "HEALTH_CONNECT") Color(0xFF10B981) else OfflineAccentPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = activity.title,
                                            style = Typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = OfflineTextPrimary
                                        )
                                        Text(
                                            text = activity.subtitle,
                                            style = Typography.labelSmall,
                                            color = OfflineTextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = OfflineSurfaceVariant.copy(alpha = 0.6f)
                                    ) {
                                        Text(
                                            text = activity.durationText,
                                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = OfflineTextPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    if (activity.metricText != null) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = activity.metricText,
                                            style = Typography.labelSmall,
                                            color = OfflineAccentPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "bottom_back_btn") {
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = { selectedDateForDetails = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, OfflineBorder)
                    ) {
                        Text(
                            text = "← Return to Month Boxes & Dates",
                            style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = OfflineTextPrimary
                        )
                    }
                }
            } else {
                // MONTH BOXES VIEW: {nov [1/11/2026] [2/11/2026]}
                item(key = "recent_workouts_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Workouts",
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = OfflineTextPrimary
                        )
                        Text(
                            text = "Tap a date to inspect",
                            style = Typography.labelSmall,
                            color = OfflineTextMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (monthGroups.isEmpty()) {
                    item(key = "empty_workouts") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = OfflineSurface),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(OfflineBorder))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No workouts recorded yet.\nStart an activity above to build your fitness log.",
                                    style = Typography.bodyMedium,
                                    color = OfflineTextMuted,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    monthGroups.forEach { (yearMonth, datePairs) ->
                        item(key = "month_box_${yearMonth}") {
                            val monthShort = yearMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).lowercase()
                            val monthFull = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                            val totalActivities = datePairs.sumOf { it.second.size }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = OfflineSurface),
                                border = BorderStroke(1.dp, OfflineBorder)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // Month Box Header: {nov}
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = OfflineAccentPrimary.copy(alpha = 0.15f),
                                                border = BorderStroke(1.dp, OfflineAccentPrimary)
                                            ) {
                                                Text(
                                                    text = "{ $monthShort }",
                                                    style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = OfflineAccentPrimary,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                            Text(
                                                text = "$monthFull ${yearMonth.year}",
                                                style = Typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = OfflineTextPrimary
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = OfflineSurfaceVariant
                                        ) {
                                            Text(
                                                text = "$totalActivities activities",
                                                style = Typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                                color = OfflineTextSecondary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = OfflineBorder.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Scrollable list INSIDE the box!
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 240.dp)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        datePairs.forEach { (dateIso, activities) ->
                                            val (formattedDate, relLabel) = try {
                                                val parsed = LocalDate.parse(dateIso)
                                                val dateStr = "${parsed.dayOfMonth}/${parsed.monthValue}/${parsed.year}"
                                                val today = LocalDate.now()
                                                val rel = when (parsed) {
                                                    today -> "Today"
                                                    today.minusDays(1) -> "Yesterday"
                                                    else -> parsed.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                                                }
                                                Pair(dateStr, rel)
                                            } catch (_: Exception) {
                                                Pair(dateIso, "")
                                            }

                                            Card(
                                                onClick = { selectedDateForDetails = dateIso },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(14.dp),
                                                colors = CardDefaults.cardColors(containerColor = OfflineSurfaceVariant.copy(alpha = 0.6f)),
                                                border = BorderStroke(1.dp, OfflineBorder)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Text(
                                                            text = "[$formattedDate]",
                                                            style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = OfflineTextPrimary
                                                        )
                                                        if (relLabel.isNotBlank()) {
                                                            Text(
                                                                text = "• $relLabel",
                                                                style = Typography.labelSmall,
                                                                color = OfflineAccentPrimary
                                                            )
                                                        }
                                                    }

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = SoftBlue.copy(alpha = 0.5f),
                                                            border = BorderStroke(1.dp, SoftBlue)
                                                        ) {
                                                            Text(
                                                                text = "${activities.size} ${if (activities.size == 1) "activity" else "activities"}",
                                                                style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                color = OfflineTextPrimary,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                            )
                                                        }
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                            contentDescription = "View activities",
                                                            tint = OfflineTextSecondary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 7. MONTHLY ACTIVITY SUMMARY CARD (Changes every month with preview & archive)
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    MonthActivitySummaryCard(
                        allDailyActivities = allDailyActivities,
                        allActivities = allActivities,
                        allSessions = allSessions,
                        recentExercises = recentExercises,
                        externalExercises = externalExercises,
                        externalMetrics = externalMetrics,
                        dailyUsage = dailyUsage
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Step Goal Configuration Dialog
    if (showGoalDialog) {
        GoalConfigDialog(
            currentGoal = goalSteps,
            onDismiss = { showGoalDialog = false },
            onSaveGoal = { newGoal ->
                onUpdateGoal(newGoal)
                showGoalDialog = false
            }
        )
    }
}

@Composable
private fun MetricItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OfflineTextMuted,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = OfflineTextPrimary
        )
        Text(
            text = label,
            style = Typography.labelSmall,
            color = OfflineTextMuted
        )
    }
}

@Composable
private fun GoalConfigDialog(
    currentGoal: Int,
    onDismiss: () -> Unit,
    onSaveGoal: (Int) -> Unit
) {
    val presets = listOf(4000, 6000, 8000, 10000, 12000)
    var selectedGoal by remember { mutableIntStateOf(currentGoal) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Daily Step Goal",
                style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = OfflineTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Select your daily target steps:",
                    style = Typography.bodySmall,
                    color = OfflineTextSecondary
                )

                presets.forEach { preset ->
                    val isSelected = selectedGoal == preset
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) OfflineAccentPrimary else OfflineSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedGoal = preset }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "%,d steps".format(preset),
                                style = Typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) Color(0xFF1E1B4B) else OfflineTextPrimary
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF1E1B4B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveGoal(selectedGoal) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = OfflineDarkButton,
                    contentColor = OfflineOnDarkButton
                )
            ) {
                Text("Save Target")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = OfflineTextSecondary)
            }
        },
        containerColor = OfflineSurface
    )
}

private data class ActivityHistoryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val durationText: String,
    val metricText: String?,
    val dateIso: String,
    val timestampMs: Long,
    val iconType: String
)

@Composable
fun MonthActivitySummaryCard(
    allDailyActivities: List<DailyActivityEntity>,
    allActivities: List<PlannedActivityEntity>,
    allSessions: List<DetoxSessionEntity>,
    recentExercises: List<ExerciseSessionEntity>,
    externalExercises: List<ExternalExerciseItem>,
    externalMetrics: ExternalHealthMetrics,
    dailyUsage: DailyUsageSummary?
) {
    var selectedYearMonth by remember { mutableStateOf(YearMonth.now()) }
    val currentMonth = remember { YearMonth.now() }
    val monthPrefix = remember(selectedYearMonth) {
        val m = if (selectedYearMonth.monthValue < 10) "0${selectedYearMonth.monthValue}" else "${selectedYearMonth.monthValue}"
        "${selectedYearMonth.year}-$m"
    }
    val monthName = remember(selectedYearMonth) {
        selectedYearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    }
    val monthShort = remember(selectedYearMonth) {
        selectedYearMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).lowercase()
    }
    val isCurrentMonth = selectedYearMonth == currentMonth

    // Calculate aggregated metrics for the selected month:
    val monthDailyRecords = remember(allDailyActivities, monthPrefix) {
        allDailyActivities.filter { it.date.startsWith(monthPrefix) }
    }
    val totalSteps = remember(monthDailyRecords) {
        monthDailyRecords.sumOf { it.steps }
    }
    val totalCalories = remember(monthDailyRecords) {
        monthDailyRecords.sumOf { it.activeCalories.toDouble() }.toFloat()
    }
    val totalDistanceKm = remember(monthDailyRecords) {
        (monthDailyRecords.sumOf { it.distanceMeters.toDouble() } / 1000f).toFloat()
    }

    // Workouts & focus activities in this month
    val monthWorkoutsCount = remember(allSessions, recentExercises, allActivities, externalExercises, monthPrefix) {
        val detoxCount = allSessions.count { it.status == "COMPLETED" && it.sessionDate.startsWith(monthPrefix) }
        val exCount = recentExercises.count { it.status == "COMPLETED" && it.sessionDate.startsWith(monthPrefix) }
        val taskCount = allActivities.count { it.status == "COMPLETED" && it.scheduledDate.startsWith(monthPrefix) && !it.description.contains("digital detox session") }
        val extCount = externalExercises.count {
            try {
                val d = Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                d.startsWith(monthPrefix)
            } catch (_: Exception) { false }
        }
        detoxCount + exCount + taskCount + extCount
    }

    // Recorded exercises breakdown in this month
    val exerciseBreakdown = remember(allSessions, recentExercises, allActivities, externalExercises, monthPrefix) {
        val breakdown = mutableMapOf<String, Int>()
        recentExercises.filter { it.sessionDate.startsWith(monthPrefix) }.forEach {
            val name = "${it.activityType.lowercase().replaceFirstChar { c -> c.uppercase() }} Workout"
            breakdown[name] = (breakdown[name] ?: 0) + 1
        }
        allSessions.filter { it.status == "COMPLETED" && it.sessionDate.startsWith(monthPrefix) }.forEach {
            val name = "${it.title.ifBlank { "Focus" }} (${it.category})"
            breakdown[name] = (breakdown[name] ?: 0) + 1
        }
        allActivities.filter { it.status == "COMPLETED" && it.scheduledDate.startsWith(monthPrefix) && !it.description.contains("digital detox session") }.forEach {
            val name = it.title.ifBlank { "Offline Activity" }
            breakdown[name] = (breakdown[name] ?: 0) + 1
        }
        externalExercises.forEach {
            val d = try { Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).toLocalDate().toString() } catch (_: Exception) { "" }
            if (d.startsWith(monthPrefix)) {
                val name = "Health Connect: ${it.exerciseType}"
                breakdown[name] = (breakdown[name] ?: 0) + 1
            }
        }
        breakdown.toList().sortedByDescending { it.second }
    }

    // Sleep info
    val sleepText = remember(externalMetrics, dailyUsage) {
        val sleepMins = externalMetrics.lastSleepDurationMinutes ?: dailyUsage?.lastNightRestMinutes
        if (sleepMins != null && sleepMins > 0) {
            "${sleepMins / 60}h ${sleepMins % 60}m"
        } else {
            "7h 30m avg"
        }
    }

    // Screen time info
    val screenTimeText = remember(dailyUsage) {
        val totalMs = dailyUsage?.totalScreenTimeMs ?: 0L
        val hours = totalMs / (1000 * 60 * 60)
        val mins = (totalMs / (1000 * 60)) % 60
        if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = OfflineSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(OfflineBorder))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Top Header with Month Navigator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = OfflineAccentPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCurrentMonth) "[$monthShort activities] summary" else "[$monthShort activities] preview",
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = OfflineTextPrimary
                        )
                    }
                    Text(
                        text = if (isCurrentMonth) "Running summary of the whole month" else "Archived monthly activity preview",
                        style = Typography.labelSmall,
                        color = OfflineTextMuted
                    )
                }

                // Month switcher arrows
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { selectedYearMonth = selectedYearMonth.minusMonths(1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Month",
                            tint = OfflineTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "${selectedYearMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${selectedYearMonth.year}",
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = OfflineTextPrimary
                    )
                    IconButton(
                        onClick = { selectedYearMonth = selectedYearMonth.plusMonths(1) },
                        modifier = Modifier.size(32.dp),
                        enabled = selectedYearMonth < currentMonth.plusMonths(1)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Month",
                            tint = if (selectedYearMonth < currentMonth.plusMonths(1)) OfflineTextSecondary else OfflineTextMuted.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Grid of Key Metrics: Workouts, Steps, Calories
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MonthlyMetricBox(
                    icon = Icons.Outlined.FitnessCenter,
                    label = "Workouts",
                    value = "$monthWorkoutsCount",
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                MonthlyMetricBox(
                    icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
                    label = "Steps",
                    value = if (totalSteps > 0) "%,d".format(totalSteps) else "0",
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                MonthlyMetricBox(
                    icon = Icons.Outlined.LocalFireDepartment,
                    label = "Calories [steps]",
                    value = if (totalCalories > 0f) "%.0f kcal".format(totalCalories) else "0 kcal",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MonthlyMetricBox(
                    icon = Icons.Outlined.Bedtime,
                    label = "Sleep",
                    value = sleepText,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                MonthlyMetricBox(
                    icon = Icons.Outlined.PhoneAndroid,
                    label = "Screen Time",
                    value = screenTimeText,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                MonthlyMetricBox(
                    icon = Icons.Outlined.Straighten,
                    label = "Distance",
                    value = "%.2f km".format(totalDistanceKm),
                    modifier = Modifier.weight(1f)
                )
            }

            // Recorded Exercises breakdown
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Recorded Exercises & Activities ($monthName)",
                style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = OfflineTextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (exerciseBreakdown.isEmpty()) {
                Text(
                    text = "No activities logged for $monthName yet.",
                    style = Typography.bodySmall,
                    color = OfflineTextMuted
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    exerciseBreakdown.take(6).forEach { (name, count) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = name,
                                style = Typography.bodySmall,
                                color = OfflineTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SoftBlue.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "$count ${if (count == 1) "session" else "sessions"}",
                                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = OfflineTextPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyMetricBox(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = OfflineSurfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, OfflineBorder.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OfflineAccentPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = Typography.labelSmall,
                    color = OfflineTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = OfflineTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class DayStepData(
    val dayLabel: String,
    val offset: Int,
    val isToday: Boolean,
    val steps: Int,
    val goal: Int
)

private fun getActivityIcon(activityType: String): ImageVector {
    return when (activityType.uppercase()) {
        "WALKING" -> Icons.Outlined.DirectionsWalk
        "RUNNING" -> Icons.Outlined.DirectionsRun
        "CYCLING" -> Icons.Outlined.DirectionsBike
        "STRENGTH" -> Icons.Outlined.FitnessCenter
        "FOCUS", "DETOX" -> Icons.Outlined.HourglassTop
        "OFFLINE", "MINDFULNESS" -> Icons.Outlined.Spa
        "HEALTH_CONNECT" -> Icons.Outlined.Favorite
        else -> Icons.Outlined.SelfImprovement
    }
}
