package com.example.detoxproject.ui.screens.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.detoxproject.domain.model.*
import com.example.detoxproject.ui.theme.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TrendsView(
    trendsData: TrendsAnalyticsData?,
    selectedRange: TrendTimeRange,
    onSelectRange: (TrendTimeRange) -> Unit,
    onNavigateToFitness: () -> Unit
) {
    val data = trendsData ?: TrendsAnalyticsData(timeRange = selectedRange)
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    val rangeText = "${data.startDate.format(dateFormatter)} – ${data.endDate.format(dateFormatter)}"

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. Time-Range Filter Chips
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = OfflineSurface,
            border = BorderStroke(1.dp, OfflineBorder.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Time Range",
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = OfflineTextPrimary
                    )
                    Text(
                        text = rangeText,
                        style = Typography.labelSmall,
                        color = OfflineTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TrendTimeRange.values().forEach { range ->
                        val isSelected = range == selectedRange
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectRange(range) },
                            label = {
                                Text(
                                    text = range.label,
                                    style = Typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OfflineDarkButton,
                                selectedLabelColor = OfflineOnDarkButton,
                                containerColor = OfflineSurfaceVariant,
                                labelColor = OfflineTextPrimary
                            ),
                            border = null,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. Movement & Daily Steps Analytics
        MovementTrendCard(movement = data.movement)

        // 3. Digital Detox Focus Analytics
        DetoxTrendCard(detox = data.detox)

        // 4. Habit Consistency & Schedule Analytics
        HabitTrendCard(habits = data.habits)

        // 5. Exercise & Workouts Analytics
        ExerciseTrendCard(exercise = data.exercise)

        // 6. Zen XP Progression & Audit Balance
        XPTrendCard(xp = data.xp)

        // 7. Physical Activity Deep-Link
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable { onNavigateToFitness() },
            color = OfflineSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, OfflineBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = OfflineSurfaceVariant,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.DirectionsRun,
                                contentDescription = null,
                                tint = OfflineTextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Live Fitness & Hardware Steps",
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = OfflineTextPrimary
                        )
                        Text(
                            text = "Step sensors, workouts & energy recovery",
                            style = Typography.bodySmall,
                            color = OfflineTextSecondary
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "View",
                    tint = OfflineTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun MovementTrendCard(movement: MovementTrendAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = OfflineSurface),
        border = BorderStroke(1.dp, OfflineBorder.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF10B981).copy(alpha = 0.14f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.DirectionsWalk,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Movement & Steps",
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = OfflineTextPrimary
                        )
                        Text(
                            text = "Daily step activity & consistency",
                            style = Typography.bodySmall,
                            color = OfflineTextSecondary
                        )
                    }
                }

                ComparisonBadge(changePercent = movement.stepsChangePercent)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3-Metric Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Daily Avg", style = Typography.labelSmall, color = OfflineTextMuted)
                    Text(
                        text = if (movement.averageDailySteps > 0) "%,d".format(movement.averageDailySteps) else "0",
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OfflineTextPrimary
                    )
                }
                Column {
                    Text("Total Steps", style = Typography.labelSmall, color = OfflineTextMuted)
                    Text(
                        text = if (movement.totalSteps > 0) "%,d".format(movement.totalSteps) else "0",
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OfflineTextPrimary
                    )
                }
                Column {
                    Text("Goal Consistency", style = Typography.labelSmall, color = OfflineTextMuted)
                    Text(
                        text = "${movement.goalConsistencyPercent}%",
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF10B981)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            val estimatedKcal = (movement.totalSteps * 0.04f).toInt()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${movement.daysGoalReached} of ${movement.totalDays} days met step goal",
                    style = Typography.bodySmall.copy(fontSize = 11.sp),
                    color = OfflineTextSecondary
                )
                if (estimatedKcal > 0) {
                    Text(
                        text = "≈ %,d kcal [steps only]".format(estimatedKcal),
                        style = Typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFF59E0B)
                    )
                }
            }

            if (movement.dailySteps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                BarTrendChart(
                    data = movement.dailySteps.map { (date, steps) ->
                        val label = if (movement.dailySteps.size <= 7) {
                            date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                        } else {
                            date.dayOfMonth.toString()
                        }
                        BarPoint(label = label, value = steps, isHighlighted = steps >= 8000)
                    },
                    barColor = Color(0xFF10B981)
                )
            }
        }
    }
}

@Composable
private fun DetoxTrendCard(detox: DetoxTrendAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = OfflineSurface),
        border = BorderStroke(1.dp, OfflineBorder.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = OfflineAccentCyan.copy(alpha = 0.14f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Spa,
                                contentDescription = null,
                                tint = OfflineAccentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Digital Detox Focus",
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = OfflineTextPrimary
                        )
                        Text(
                            text = "Offline screen-free duration",
                            style = Typography.bodySmall,
                            color = OfflineTextSecondary
                        )
                    }
                }

                ComparisonBadge(changePercent = detox.detoxChangePercent)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Focus", style = Typography.labelSmall, color = OfflineTextMuted)
                    val hours = detox.totalDetoxMinutes / 60f
                    Text(
                        text = if (detox.totalDetoxMinutes >= 60) "%.1fh".format(hours) else "${detox.totalDetoxMinutes}m",
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OfflineTextPrimary
                    )
                }
                Column {
                    Text("Avg / Day", style = Typography.labelSmall, color = OfflineTextMuted)
                    Text(
                        text = "${detox.averageDailyMinutes}m",
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OfflineTextPrimary
                    )
                }
                Column {
                    Text("Longest Session", style = Typography.labelSmall, color = OfflineTextMuted)
                    Text(
                        text = "${detox.longestSessionMinutes}m",
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OfflineTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${detox.completedSessionCount} verified focus sessions completed",
                style = Typography.bodySmall.copy(fontSize = 11.sp),
                color = OfflineTextSecondary
            )

            if (detox.dailyMinutes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                BarTrendChart(
                    data = detox.dailyMinutes.map { (date, mins) ->
                        val label = if (detox.dailyMinutes.size <= 7) {
                            date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                        } else {
                            date.dayOfMonth.toString()
                        }
                        BarPoint(label = label, value = mins, isHighlighted = mins > 0)
                    },
                    barColor = OfflineAccentCyan
                )
            }
        }
    }
}

@Composable
private fun HabitTrendCard(habits: HabitTrendAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = OfflineSurface),
        border = BorderStroke(1.dp, OfflineBorder.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF97316).copy(alpha = 0.14f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFEA580C),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Habit Completion & Schedules",
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = OfflineTextPrimary
                        )
                        Text(
                            text = "Scheduled opportunities vs actual completions",
                            style = Typography.bodySmall,
                            color = OfflineTextSecondary
                        )
                    }
                }

                ComparisonBadge(changePercent = habits.completionChangePercent)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Completion Rate", style = Typography.labelSmall, color = OfflineTextMuted)
                    Text(
                        text = "${habits.completionPercentage}%",
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF16A34A)
                    )
                }
                Column {
                    Text("Completed", style = Typography.labelSmall, color = OfflineTextMuted)
                    Text(
                        text = "${habits.completedOpportunities}",
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OfflineTextPrimary
                    )
                }
                Column {
                    Text("Scheduled Opps", style = Typography.labelSmall, color = OfflineTextMuted)
                    Text(
                        text = "${habits.totalScheduledOpportunities}",
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OfflineTextPrimary
                    )
                }
            }

            if (habits.topConsistentHabits.isNotEmpty()) {
                var showConsistentHabits by remember { mutableStateOf(false) }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Most Consistent Habits",
                        style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = OfflineTextSecondary
                    )
                    Surface(
                        onClick = { showConsistentHabits = !showConsistentHabits },
                        shape = RoundedCornerShape(8.dp),
                        color = if (showConsistentHabits) OfflineSurfaceVariant else Color.Transparent,
                        border = BorderStroke(1.dp, if (showConsistentHabits) OfflineAccentPrimary else OfflineBorder)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (showConsistentHabits) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = if (showConsistentHabits) OfflineAccentPrimary else OfflineTextPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showConsistentHabits) "Hide" else "View",
                                style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (showConsistentHabits) OfflineAccentPrimary else OfflineTextPrimary
                            )
                        }
                    }
                }

                if (showConsistentHabits) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        habits.topConsistentHabits.forEach { (title, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(OfflineSurfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = title,
                                    style = Typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = OfflineTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$count completions",
                                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFEA580C)
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
private fun ExerciseTrendCard(exercise: ExerciseTrendAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = OfflineSurface),
        border = BorderStroke(1.dp, OfflineBorder.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF3B82F6).copy(alpha = 0.14f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Exercise & Workouts",
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = OfflineTextPrimary
                        )
                        Text(
                            text = "Independent workouts & energy burned",
                            style = Typography.bodySmall,
                            color = OfflineTextSecondary
                        )
                    }
                }

                ComparisonBadge(changePercent = exercise.sessionChangePercent)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Workouts", style = Typography.labelSmall, color = OfflineTextMuted)
                    Text(
                        text = "${exercise.sessionCount}",
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OfflineTextPrimary
                    )
                }
                Column {
                    Text("Total Time", style = Typography.labelSmall, color = OfflineTextMuted)
                    Text(
                        text = "${exercise.totalDurationMinutes}m",
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OfflineTextPrimary
                    )
                }
                Column {
                    Text("Avg Duration", style = Typography.labelSmall, color = OfflineTextMuted)
                    val avgMins = if (exercise.sessionCount > 0) exercise.totalDurationMinutes / exercise.sessionCount else 0
                    Text(
                        text = "${avgMins}m / session",
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OfflineAccentPrimary
                    )
                }
            }

            if (exercise.categoryBreakdown.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Activity Breakdown",
                    style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = OfflineTextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    exercise.categoryBreakdown.forEach { (cat, count) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = OfflineSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "${cat.lowercase().replaceFirstChar { it.uppercase() }}: $count",
                                style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                                color = OfflineTextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No workouts recorded during this time range.",
                    style = Typography.bodySmall,
                    color = OfflineTextMuted
                )
            }
        }
    }
}

@Composable
private fun XPTrendCard(xp: XPTrendAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = OfflineSurface),
        border = BorderStroke(1.dp, OfflineBorder.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = OfflineAccentPrimary.copy(alpha = 0.14f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = OfflineAccentPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Zen XP & Progression",
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = OfflineTextPrimary
                        )
                        Text(
                            text = "Auditable append-only ledger flow",
                            style = Typography.bodySmall,
                            color = OfflineTextSecondary
                        )
                    }
                }

                ComparisonBadge(changePercent = xp.netXPChangePercent)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Net XP Change", style = Typography.labelSmall, color = OfflineTextMuted)
                    Text(
                        text = "${if (xp.netChange >= 0) "+" else ""}${xp.netChange} XP",
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (xp.netChange >= 0) OfflineAccentPrimary else Color(0xFFEF4444)
                    )
                }
                Column {
                    Text("Rewards", style = Typography.labelSmall, color = OfflineTextMuted)
                    Text(
                        text = "+${xp.totalEarned}",
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF16A34A)
                    )
                }
                Column {
                    Text("Reversals", style = Typography.labelSmall, color = OfflineTextMuted)
                    Text(
                        text = "-${xp.totalReversed}",
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (xp.totalReversed > 0) Color(0xFFEF4444) else OfflineTextMuted
                    )
                }
            }

            if (xp.sourceBreakdown.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "XP Breakdown by Source",
                    style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = OfflineTextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    xp.sourceBreakdown.forEach { (source, netAmount) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (source) {
                                    "HABIT" -> "Daily Habits"
                                    "ALL_HABITS_BONUS" -> "All Habits Bonus"
                                    "DETOX" -> "Digital Detox"
                                    "STEP_GOAL" -> "Step Goal"
                                    "EXERCISE" -> "Workouts"
                                    "ALL_TASKS_BONUS" -> "Task Bonus"
                                    "ACHIEVEMENT" -> "Achievements"
                                    else -> source
                                },
                                style = Typography.bodySmall,
                                color = OfflineTextPrimary
                            )
                            Text(
                                text = "${if (netAmount >= 0) "+" else ""}$netAmount XP",
                                style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (netAmount >= 0) OfflineAccentPrimary else Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }
        }
    }
}

data class BarPoint(val label: String, val value: Int, val isHighlighted: Boolean)

@Composable
private fun BarTrendChart(
    data: List<BarPoint>,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val maxValue = data.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { point ->
                val fraction = (point.value.toFloat() / maxValue.toFloat()).coerceIn(0.04f, 1f)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .width(if (data.size <= 7) 18.dp else 6.dp)
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(if (point.isHighlighted) barColor else barColor.copy(alpha = 0.3f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (data.size <= 14) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                data.forEach { point ->
                    Text(
                        text = point.label,
                        style = Typography.labelSmall.copy(fontSize = 9.sp),
                        color = OfflineTextMuted,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonBadge(changePercent: Float?) {
    if (changePercent == null) return

    val isPositive = changePercent >= 0f
    val color = if (isPositive) Color(0xFF16A34A) else Color(0xFFEA580C)
    val prefix = if (isPositive) "+" else ""

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "${prefix}%.1f%% vs prior".format(changePercent),
                style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                color = color
            )
        }
    }
}
