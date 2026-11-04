package com.example.detoxproject.ui.screens.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.detoxproject.data.local.entity.PlannedActivityEntity
import com.example.detoxproject.domain.engine.DateManager
import com.example.detoxproject.domain.engine.HabitWithStatus
import com.example.detoxproject.domain.model.CalendarDayInfo
import com.example.detoxproject.domain.model.CalendarMonthData
import com.example.detoxproject.domain.model.UnifiedDailySummary
import com.example.detoxproject.domain.model.WeeklyAnalyticsData
import com.example.detoxproject.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarMonthView(
    yearMonth: YearMonth,
    calendarData: CalendarMonthData?,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onGoToToday: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = OfflineSurface),
        border = BorderStroke(1.dp, OfflineBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Month Header Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${yearMonth.year}",
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                        color = OfflineTextPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = OfflineSurfaceVariant,
                        modifier = Modifier
                            .clickable { onGoToToday() }
                            .padding(2.dp)
                    ) {
                        Text(
                            text = "Today",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = OfflineTextPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(onClick = onPrevMonth, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Month",
                            tint = OfflineTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onNextMonth, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Month",
                            tint = OfflineTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Day of week headers (Mon..Sun)
            val weekDayHeaders = listOf("M", "T", "W", "T", "F", "S", "S")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                weekDayHeaders.forEach { dayName ->
                    Text(
                        text = dayName,
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = OfflineTextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Days Grid
            val days = calendarData?.days.orEmpty()
            val chunked = days.chunked(7)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                chunked.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        week.forEach { dayInfo ->
                            CalendarDayCell(
                                dayInfo = dayInfo,
                                isSelected = dayInfo.date == selectedDate,
                                onClick = {
                                    if (!dayInfo.isFuture) {
                                        onSelectDate(dayInfo.date)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Indicator Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF16A34A)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Habits", style = Typography.labelSmall.copy(fontSize = 10.sp), color = OfflineTextMuted)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(OfflineAccentCyan))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Detox", style = Typography.labelSmall.copy(fontSize = 10.sp), color = OfflineTextMuted)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(OfflineAccentViolet))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Steps", style = Typography.labelSmall.copy(fontSize = 10.sp), color = OfflineTextMuted)
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayInfo: CalendarDayInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isEnabled = !dayInfo.isFuture
    val textColor = when {
        isSelected -> OfflineOnDarkButton
        dayInfo.isFuture -> OfflineTextMuted.copy(alpha = 0.4f)
        !dayInfo.isCurrentMonth -> OfflineTextMuted.copy(alpha = 0.5f)
        dayInfo.isToday -> OfflineAccentPrimary
        else -> OfflineTextPrimary
    }

    val cellBackground = when {
        isSelected -> OfflineDarkButton
        dayInfo.isToday -> OfflineSurfaceVariant
        else -> Color.Transparent
    }

    val borderModifier = if (dayInfo.isToday && !isSelected) {
        Modifier.border(1.dp, OfflineDarkButton, RoundedCornerShape(12.dp))
    } else Modifier

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(cellBackground)
            .then(borderModifier)
            .clickable(enabled = isEnabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${dayInfo.dayOfMonth}",
                style = Typography.bodySmall.copy(
                    fontWeight = if (isSelected || dayInfo.isToday) FontWeight.Bold else FontWeight.Medium
                ),
                color = textColor
            )

            // Activity Dots (only if not selected to avoid clutter)
            if (!isSelected && (dayInfo.hasHabitActivity || dayInfo.hasDetoxActivity || dayInfo.hasFitnessActivity)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 1.dp)
                ) {
                    if (dayInfo.hasHabitActivity) {
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF16A34A)))
                    }
                    if (dayInfo.hasDetoxActivity) {
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(OfflineAccentCyan))
                    }
                    if (dayInfo.hasFitnessActivity) {
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(OfflineAccentViolet))
                    }
                }
            }
        }
    }
}

@Composable
fun DayDetailHistoryCard(
    summary: UnifiedDailySummary?,
    selectedDate: LocalDate,
    onStepDate: (offsetDays: Long) -> Unit,
    onToggleHabit: (habitId: Long) -> Unit,
    onDeleteHabit: (Long) -> Unit = {},
    onToggleTask: (taskId: Long) -> Unit,
    onDeleteTask: (taskId: Long) -> Unit,
    onAddTaskClick: () -> Unit
) {
    if (summary == null) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = OfflineSurface),
        border = BorderStroke(1.dp, OfflineBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Day Navigation Stepper Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onStepDate(-1) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Day",
                        tint = OfflineTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = summary.displayDate,
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = OfflineTextPrimary
                    )
                    Text(
                        text = summary.dateIso,
                        style = Typography.labelSmall,
                        color = OfflineTextSecondary
                    )
                }

                IconButton(
                    onClick = { onStepDate(1) },
                    enabled = !summary.isToday,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Day",
                        tint = if (!summary.isToday) OfflineTextPrimary else OfflineTextMuted.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Metrics 6-Pill Grid (2 Rows of 3)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricPill(
                        label = "Habits",
                        value = "${summary.habitsCompletedCount}/${summary.habitsTotalCount}",
                        color = SoftGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricPill(
                        label = "Tasks",
                        value = "${summary.tasksCompletedCount}/${summary.tasksTotalCount}",
                        color = SoftBlue,
                        modifier = Modifier.weight(1f)
                    )
                    val steps = summary.dailyActivity?.steps ?: 0
                    val goalReached = steps >= summary.stepGoalTarget && steps > 0
                    MetricPill(
                        label = if (goalReached) "Steps ✓" else "Steps",
                        value = if (steps > 0) "%,d".format(steps) else "0",
                        color = if (goalReached) SoftGreen else SoftBeige,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricPill(
                        label = "Detox",
                        value = "${summary.totalDetoxMinutes}m",
                        color = SoftTeal,
                        modifier = Modifier.weight(1f)
                    )
                    MetricPill(
                        label = "Workouts",
                        value = "${summary.exerciseSessions.size}",
                        color = SoftBlue.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f)
                    )
                    val xpText = "${if (summary.netXpForDate >= 0) "+" else ""}${summary.netXpForDate} XP"
                    MetricPill(
                        label = "Zen XP",
                        value = xpText,
                        color = OfflineAccentPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- MOVEMENT & PHYSICAL ACTIVITY SECTION ---
            val stepsRecorded = summary.dailyActivity?.steps ?: 0
            val distanceKm = (summary.dailyActivity?.distanceMeters ?: 0f) / 1000f
            val caloriesBurned = stepsRecorded * 0.04f
            val stepGoal = summary.stepGoalTarget.coerceAtLeast(1)
            val stepFraction = (stepsRecorded.toFloat() / stepGoal.toFloat()).coerceIn(0f, 1f)
            val isGoalMet = stepsRecorded >= stepGoal

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DirectionsWalk,
                        contentDescription = null,
                        tint = if (isGoalMet) Color(0xFF16A34A) else OfflineTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Physical Movement",
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = OfflineTextPrimary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val sourceLabel = when (summary.dailyActivity?.source) {
                        "HEALTH_CONNECT" -> "Health Connect"
                        "STEP_COUNTER" -> "Hardware Sensor"
                        "ESTIMATED" -> "Estimated"
                        "UNAVAILABLE" -> "Unavailable"
                        else -> if (stepsRecorded > 0) "Recorded" else "No Data"
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = OfflineSurfaceVariant
                    ) {
                        Text(
                            text = sourceLabel,
                            style = Typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                            color = OfflineTextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (isGoalMet) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Goal Reached ✓",
                                style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = Color(0xFF059669),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = OfflineSurfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, OfflineBorder.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (stepsRecorded > 0) "%,d / %,d steps".format(stepsRecorded, stepGoal) else "No steps recorded",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = OfflineTextPrimary
                        )
                        Text(
                            text = "${(stepFraction * 100).toInt()}%",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isGoalMet) Color(0xFF16A34A) else OfflineTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { stepFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isGoalMet) Color(0xFF10B981) else OfflineAccentPrimary,
                        trackColor = OfflineBorder.copy(alpha = 0.4f)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (distanceKm > 0f) "%.2f km distance".format(distanceKm) else "Estimated 0.00 km",
                            style = Typography.bodySmall.copy(fontSize = 11.sp),
                            color = OfflineTextSecondary
                        )
                        Text(
                            text = "%.0f kcal [steps only]".format(caloriesBurned),
                            style = Typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFF59E0B)
                        )
                        Text(
                            text = "${(stepsRecorded * 0.04).toInt()} active min",
                            style = Typography.bodySmall.copy(fontSize = 11.sp),
                            color = OfflineTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- RECURRING HABITS SECTION ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Habits for this Day",
                    style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = OfflineTextPrimary
                )
                Text(
                    text = "${summary.habitsCompletedCount} of ${summary.habitsTotalCount} done",
                    style = Typography.labelSmall,
                    color = OfflineTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (summary.habits.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = OfflineSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No recurring habits scheduled for this day.",
                        style = Typography.bodySmall,
                        color = OfflineTextSecondary,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    summary.habits.forEach { habitStatus ->
                        val isDone = habitStatus.isCompletedForDate
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDone) SoftGreen.copy(alpha = 0.5f) else OfflineSurfaceVariant,
                            border = BorderStroke(1.dp, if (isDone) SoftGreen else OfflineBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (!isDone) Modifier.clickable { onToggleHabit(habitStatus.habit.id) }
                                    else Modifier
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
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
                                    Spacer(modifier = Modifier.width(8.dp))
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalFireDepartment,
                                            contentDescription = "Streak",
                                            tint = Color(0xFFEA580C),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "${habitStatus.stats.currentStreak}d",
                                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFFEA580C)
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
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- DAILY TASKS SECTION ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Planned Tasks",
                    style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = OfflineTextPrimary
                )
                IconButton(onClick = onAddTaskClick, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Task",
                        tint = OfflineTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (summary.tasks.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = OfflineSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "No tasks planned for this day.",
                            style = Typography.bodySmall,
                            color = OfflineTextSecondary
                        )
                        Text(
                            text = "+ Add Task",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = OfflineAccentPrimary,
                            modifier = Modifier.clickable { onAddTaskClick() }
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    summary.tasks.forEach { task ->
                        val isDone = task.status == "COMPLETED"
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDone) SoftGreen.copy(alpha = 0.5f) else OfflineSurfaceVariant,
                            border = BorderStroke(1.dp, if (isDone) SoftGreen else OfflineBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleTask(task.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = isDone,
                                        onCheckedChange = { onToggleTask(task.id) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
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

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = OfflineBorder.copy(alpha = 0.4f)
                                    ) {
                                        Text(
                                            text = "${task.durationMinutes}m",
                                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = OfflineTextSecondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    IconButton(
                                        onClick = { onDeleteTask(task.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Task",
                                            tint = OfflineTextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- DETOX SESSIONS SECTION ---
            val allDetoxList = if (summary.allDetoxSessions.isNotEmpty()) summary.allDetoxSessions else summary.detoxSessions
            if (allDetoxList.isNotEmpty()) {
                var showDetoxSessions by remember(summary.date) { mutableStateOf(false) }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Detox Focus Sessions (${summary.totalDetoxMinutes} min)",
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = OfflineTextPrimary
                        )
                        Text(
                            text = "${allDetoxList.size} session${if (allDetoxList.size > 1) "s" else ""}",
                            style = Typography.labelSmall,
                            color = OfflineTextSecondary
                        )
                    }
                    Surface(
                        onClick = { showDetoxSessions = !showDetoxSessions },
                        shape = RoundedCornerShape(8.dp),
                        color = OfflineSurfaceVariant,
                        border = BorderStroke(1.dp, OfflineBorder.copy(alpha = 0.8f))
                    ) {
                        Text(
                            text = if (showDetoxSessions) "Hide" else "View",
                            style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = OfflineTextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                if (showDetoxSessions) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    allDetoxList.forEach { session ->
                        val isCompleted = session.status == "COMPLETED"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCompleted) SoftTeal.copy(alpha = 0.5f) else OfflineSurfaceVariant,
                            border = BorderStroke(1.dp, if (isCompleted) SoftTeal else OfflineBorder.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (isCompleted) Color(0xFF0D9488) else Color(0xFFEA580C),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = session.title,
                                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = OfflineTextPrimary
                                        )
                                        Text(
                                            text = "${session.category.lowercase().replaceFirstChar { it.uppercase() }} • ${if (isCompleted) "Completed" else "Cancelled / Ended early"}",
                                            style = Typography.labelSmall,
                                            color = if (isCompleted) OfflineTextSecondary else Color(0xFFEA580C)
                                        )
                                    }
                                }
                                Text(
                                    text = "${session.actualMinutes}m",
                                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = OfflineTextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

            // --- EXERCISE & WORKOUTS SECTION ---
            if (summary.exerciseSessions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Workouts & Exercises",
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = OfflineTextPrimary
                    )
                    Text(
                        text = "${summary.totalExerciseMinutes} min total",
                        style = Typography.labelSmall,
                        color = OfflineTextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    summary.exerciseSessions.forEach { workout ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SoftBlue.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, SoftBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = workout.title.ifBlank { workout.activityType },
                                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = OfflineTextPrimary
                                        )
                                        val effortText = workout.notes?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: " • Active"
                                        Text(
                                            text = "${workout.activityType.lowercase().replaceFirstChar { it.uppercase() }}$effortText",
                                            style = Typography.labelSmall,
                                            color = OfflineTextSecondary
                                        )
                                    }
                                }
                                Text(
                                    text = "${workout.durationSeconds / 60}m",
                                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = OfflineTextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // --- ZEN XP ACTIVITY SECTION ---
            if (summary.xpTransactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Zen XP Earned & Adjusted",
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = OfflineTextPrimary
                    )
                    Text(
                        text = "${if (summary.netXpForDate >= 0) "+" else ""}${summary.netXpForDate} XP Net",
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (summary.netXpForDate >= 0) OfflineAccentPrimary else Color(0xFFEF4444)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    summary.xpTransactions.forEach { tx ->
                        val isReward = tx.transactionType == "REWARD"
                        val isReversal = tx.transactionType == "REVERSAL"
                        val isRestore = tx.transactionType == "RESTORE"

                        val badgeColor = when {
                            isReward -> Color(0xFF16A34A)
                            isReversal -> Color(0xFFEF4444)
                            else -> Color(0xFF3B82F6)
                        }
                        val prefix = if (isReversal) "-" else "+"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(badgeColor.copy(alpha = 0.08f))
                                .border(1.dp, badgeColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.description.ifBlank { tx.sourceEventType },
                                    style = Typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = OfflineTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${tx.transactionType} • ${timeFmt.format(Date(tx.timestamp))}",
                                    style = Typography.labelSmall.copy(fontSize = 10.sp),
                                    color = badgeColor
                                )
                            }
                            Text(
                                text = "$prefix${tx.xpAmount} XP",
                                style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = badgeColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyAnalyticsCard(weeklyData: WeeklyAnalyticsData?) {
    if (weeklyData == null || weeklyData.days.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = OfflineSurface),
        border = BorderStroke(1.dp, OfflineBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "7-Day Activity Trends",
                style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = OfflineTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Daily focus minutes and step totals over the last 7 days",
                style = Typography.bodySmall,
                color = OfflineTextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Trend Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Avg Focus / Day", style = Typography.labelSmall, color = OfflineTextMuted)
                    Text(
                        "${weeklyData.averageFocusMinutesPerDay} min",
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OfflineTextPrimary
                    )
                }
                Column {
                    Text("Avg Steps / Day", style = Typography.labelSmall, color = OfflineTextMuted)
                    Text(
                        "%,d".format(weeklyData.averageStepsPerDay),
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OfflineTextPrimary
                    )
                }
                Column {
                    Text("Habit Consistency", style = Typography.labelSmall, color = OfflineTextMuted)
                    Text(
                        "${weeklyData.weeklyHabitCompletionRate}%",
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OfflineTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Visual 7-Day Bar Chart
            val maxMinutes = weeklyData.days.maxOfOrNull { it.detoxMinutes }?.coerceAtLeast(60) ?: 60

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyData.days.forEach { day ->
                    val fraction = (day.detoxMinutes.toFloat() / maxMinutes.toFloat()).coerceIn(0.05f, 1f)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (day.detoxMinutes > 0) "${day.detoxMinutes}m" else "-",
                            style = Typography.labelSmall.copy(fontSize = 9.sp),
                            color = OfflineTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (day.isToday) OfflineDarkButton else OfflineAccentCyan)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = day.dayLabel,
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (day.isToday) OfflineTextPrimary else OfflineTextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyAnalyticsCard(calendarData: CalendarMonthData?) {
    if (calendarData == null) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SoftBlue),
        border = BorderStroke(1.dp, OfflineBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "${calendarData.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} Overview",
                style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = OfflineTextPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Active Days", style = Typography.labelSmall, color = OfflineTextSecondary)
                    Text(
                        "${calendarData.totalActiveDays} days",
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OfflineTextPrimary
                    )
                }

                Column {
                    Text("Habit Rate", style = Typography.labelSmall, color = OfflineTextSecondary)
                    Text(
                        "${calendarData.monthlyConsistencyPercent}%",
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OfflineTextPrimary
                    )
                }

                Column {
                    Text("Focus Hours", style = Typography.labelSmall, color = OfflineTextSecondary)
                    val hours = calendarData.totalMonthlyFocusMinutes / 60f
                    Text(
                        "%.1fh".format(hours),
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OfflineTextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = Typography.labelSmall.copy(fontSize = 10.sp),
                color = OfflineTextSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                color = OfflineTextPrimary
            )
        }
    }
}
