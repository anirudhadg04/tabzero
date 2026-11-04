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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.detoxproject.data.local.entity.AchievementEntity
import com.example.detoxproject.data.local.entity.ActionHistoryEntity
import com.example.detoxproject.data.local.entity.ExerciseSessionEntity
import com.example.detoxproject.data.local.entity.DetoxSessionEntity
import com.example.detoxproject.domain.engine.DateManager
import com.example.detoxproject.domain.engine.StreakCategoryInfo
import com.example.detoxproject.domain.engine.WeeklyGraceStatus
import com.example.detoxproject.ui.theme.*
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@Composable
fun CreateHabitDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        category: String,
        icon: String,
        frequency: String,
        targetDays: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("mindfulness") }
    var frequency by remember { mutableStateOf("DAILY") }
    val selectedDays = remember { mutableStateListOf(1, 2, 3, 4, 5, 6, 7) }

    val categories = listOf(
        Triple("fitness", "Fitness", "directions_walk"),
        Triple("reading", "Reading", "menu_book"),
        Triple("focus", "Focus Detox", "timer"),
        Triple("sleep", "Sleep", "bedtime"),
        Triple("mindfulness", "Mindfulness", "spa"),
        Triple("outdoor", "Outdoor", "park")
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = OfflineSurface,
            border = BorderStroke(1.dp, OfflineBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Create Daily Habit",
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OfflineTextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = OfflineTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Habit Title (e.g. Read 20 pages)", color = OfflineTextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OfflineTextPrimary,
                        unfocusedTextColor = OfflineTextPrimary,
                        focusedBorderColor = OfflineDarkButton,
                        unfocusedBorderColor = OfflineBorder
                    ),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Intention (optional)", color = OfflineTextSecondary) },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OfflineTextPrimary,
                        unfocusedTextColor = OfflineTextPrimary,
                        focusedBorderColor = OfflineDarkButton,
                        unfocusedBorderColor = OfflineBorder
                    ),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Category",
                    style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = OfflineTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.take(3).forEach { (catId, label, _) ->
                        val isSelected = category == catId
                        FilterChip(
                            selected = isSelected,
                            onClick = { category = catId },
                            label = { Text(label, style = Typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OfflineDarkButton,
                                selectedLabelColor = OfflineOnDarkButton,
                                containerColor = OfflineSurfaceVariant,
                                labelColor = OfflineTextPrimary
                            ),
                            border = null
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.drop(3).forEach { (catId, label, _) ->
                        val isSelected = category == catId
                        FilterChip(
                            selected = isSelected,
                            onClick = { category = catId },
                            label = { Text(label, style = Typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OfflineDarkButton,
                                selectedLabelColor = OfflineOnDarkButton,
                                containerColor = OfflineSurfaceVariant,
                                labelColor = OfflineTextPrimary
                            ),
                            border = null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Frequency",
                    style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = OfflineTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "DAILY" to "Daily",
                        "WEEKDAYS" to "Weekdays",
                        "WEEKENDS" to "Weekends",
                        "CUSTOM" to "Custom"
                    ).forEach { (freqKey, label) ->
                        val isSelected = frequency == freqKey
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) OfflineDarkButton else OfflineSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    frequency = freqKey
                                    when (freqKey) {
                                        "DAILY" -> {
                                            selectedDays.clear()
                                            selectedDays.addAll(listOf(1, 2, 3, 4, 5, 6, 7))
                                        }
                                        "WEEKDAYS" -> {
                                            selectedDays.clear()
                                            selectedDays.addAll(listOf(1, 2, 3, 4, 5))
                                        }
                                        "WEEKENDS" -> {
                                            selectedDays.clear()
                                            selectedDays.addAll(listOf(6, 7))
                                        }
                                    }
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                style = Typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = if (isSelected) OfflineOnDarkButton else OfflineTextPrimary,
                                modifier = Modifier.wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

                if (frequency == "CUSTOM") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Select active days:",
                        style = Typography.labelSmall,
                        color = OfflineTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (d in 1..7) {
                            val isChecked = d in selectedDays
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isChecked) OfflineDarkButton else OfflineSurfaceVariant)
                                    .clickable {
                                        if (isChecked && selectedDays.size > 1) {
                                            selectedDays.remove(d)
                                        } else if (!isChecked) {
                                            selectedDays.add(d)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNames[d - 1],
                                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isChecked) OfflineOnDarkButton else OfflineTextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val chosenIcon = categories.firstOrNull { it.first == category }?.third ?: "spa"
                            val daysStr = selectedDays.sorted().joinToString(",")
                            onConfirm(title.trim(), description.trim(), category, chosenIcon, frequency, daysStr)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OfflineDarkButton),
                    enabled = title.isNotBlank()
                ) {
                    Text(
                        text = "Save Habit",
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = OfflineOnDarkButton
                    )
                }
            }
        }
    }
}

@Composable
fun AddEditTaskDialog(
    scheduledDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (title: String, durationMinutes: Int, category: String, description: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableIntStateOf(25) }
    var category by remember { mutableStateOf("mindfulness") }

    val presets = listOf(15, 25, 45, 60, 90)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = OfflineSurface,
            border = BorderStroke(1.dp, OfflineBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Add Daily Task",
                            style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = OfflineTextPrimary
                        )
                        Text(
                            text = "For ${DateManager.formatFriendly(scheduledDate)}",
                            style = Typography.bodySmall,
                            color = OfflineTextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = OfflineTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title (e.g. Write essay, Clean desk)", color = OfflineTextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OfflineTextPrimary,
                        unfocusedTextColor = OfflineTextPrimary,
                        focusedBorderColor = OfflineDarkButton,
                        unfocusedBorderColor = OfflineBorder
                    ),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Notes / Details (optional)", color = OfflineTextSecondary) },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OfflineTextPrimary,
                        unfocusedTextColor = OfflineTextPrimary,
                        focusedBorderColor = OfflineDarkButton,
                        unfocusedBorderColor = OfflineBorder
                    ),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Duration: $durationMinutes minutes",
                    style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = OfflineTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { mins ->
                        val isSelected = durationMinutes == mins
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) OfflineDarkButton else OfflineSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { durationMinutes = mins }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "${mins}m",
                                style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) OfflineOnDarkButton else OfflineTextPrimary,
                                modifier = Modifier.wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onConfirm(title.trim(), durationMinutes, category, description.trim())
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OfflineDarkButton),
                    enabled = title.isNotBlank()
                ) {
                    Text(
                        text = "Add Task",
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = OfflineOnDarkButton
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordWorkoutEntryDialog(
    onSave: (title: String, type: String, durationMinutes: Int, date: LocalDate, intensity: String?, notes: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Running") }
    var durationText by remember { mutableStateOf("30") }
    var selectedIntensity by remember { mutableStateOf("Moderate") }
    var notes by remember { mutableStateOf("") }

    val activityTypes = listOf("Running", "Walking", "Cycling", "Strength", "Yoga", "Other")
    val intensityLevels = listOf("Low", "Moderate", "High")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = OfflineSurface,
            border = BorderStroke(1.dp, OfflineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Record Workout", style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = OfflineTextPrimary)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = OfflineTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Activity Type", style = Typography.labelSmall, color = OfflineTextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activityTypes.take(3).forEach { type ->
                        val isSel = selectedType == type
                        Surface(
                            onClick = { selectedType = type },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) OfflineAccentPrimary else OfflineSurfaceVariant,
                            border = BorderStroke(1.dp, if (isSel) OfflineAccentPrimary else OfflineBorder.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = type,
                                style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isSel) Color.White else OfflineTextPrimary,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activityTypes.drop(3).forEach { type ->
                        val isSel = selectedType == type
                        Surface(
                            onClick = { selectedType = type },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) OfflineAccentPrimary else OfflineSurfaceVariant,
                            border = BorderStroke(1.dp, if (isSel) OfflineAccentPrimary else OfflineBorder.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = type,
                                style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isSel) Color.White else OfflineTextPrimary,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Workout Title (e.g. Morning Run)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Duration (minutes)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Intensity", style = Typography.labelSmall, color = OfflineTextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    intensityLevels.forEach { level ->
                        val isSel = selectedIntensity == level
                        Surface(
                            onClick = { selectedIntensity = level },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) OfflineAccentPrimary else OfflineSurfaceVariant,
                            border = BorderStroke(1.dp, if (isSel) OfflineAccentPrimary else OfflineBorder.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = level,
                                style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isSel) Color.White else OfflineTextPrimary,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val dur = durationText.toIntOrNull()?.coerceAtLeast(1) ?: 30
                            val finalTitle = title.ifBlank { "$selectedType Workout" }
                            onSave(finalTitle, selectedType, dur, LocalDate.now(), selectedIntensity, notes)
                        },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OfflineDarkButton)
                    ) {
                        Text("Save Workout", color = OfflineOnDarkButton)
                    }
                }
            }
        }
    }
}

@Composable
fun StreakDetailDialog(
    streakTitle: String,
    streakSubtitle: String,
    streakInfo: StreakCategoryInfo,
    qualificationRule: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    recentWorkouts: List<ExerciseSessionEntity> = emptyList(),
    recentDetoxSessions: List<DetoxSessionEntity> = emptyList(),
    onRecordWorkout: ((title: String, type: String, durationMinutes: Int, date: LocalDate, intensity: String?, notes: String?) -> Unit)? = null,
    onStartDetox: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var showRecordWorkoutDialog by remember { mutableStateOf(false) }

    if (showRecordWorkoutDialog) {
        RecordWorkoutEntryDialog(
            onSave = { title, type, duration, date, intensity, notes ->
                onRecordWorkout?.invoke(title, type, duration, date, intensity, notes)
                showRecordWorkoutDialog = false
            },
            onDismiss = { showRecordWorkoutDialog = false }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = OfflineSurface,
            border = BorderStroke(1.dp, OfflineBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = accentColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = streakTitle,
                                    tint = accentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "$streakTitle Streak",
                                style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = OfflineTextPrimary
                            )
                            Text(
                                text = streakSubtitle,
                                style = Typography.bodySmall,
                                color = OfflineTextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = OfflineTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = OfflineSurfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, OfflineBorder.copy(alpha = 0.4f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Current Streak", style = Typography.labelSmall, color = OfflineTextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${streakInfo.currentStreak} Days",
                                style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = accentColor
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = OfflineSurfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, OfflineBorder.copy(alpha = 0.4f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Best Record", style = Typography.labelSmall, color = OfflineTextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${streakInfo.longestStreak} Days",
                                style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = OfflineTextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Status Today
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (streakInfo.isQualifiedToday) Color(0xFF10B981).copy(alpha = 0.12f) else OfflineSurfaceVariant,
                    border = BorderStroke(1.dp, if (streakInfo.isQualifiedToday) Color(0xFF10B981).copy(alpha = 0.3f) else OfflineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status Today",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = OfflineTextPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (streakInfo.isQualifiedToday) Color(0xFF10B981) else OfflineBorder
                        ) {
                            Text(
                                text = if (streakInfo.isQualifiedToday) "QUALIFIED ✓" else "PENDING",
                                style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = if (streakInfo.isQualifiedToday) Color.White else OfflineTextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Qualification Rule",
                    style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = OfflineTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = OfflineSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = qualificationRule,
                        style = Typography.bodySmall,
                        color = OfflineTextPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                // --- WORKOUT SPECIFIC SECTION ---
                if (streakTitle.contains("Workout", ignoreCase = true)) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recorded Workouts",
                            style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = OfflineTextPrimary
                        )
                        Surface(
                            onClick = { showRecordWorkoutDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            color = accentColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Record Workout", style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accentColor)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (recentWorkouts.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = OfflineSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No workouts recorded yet. Tap '+ Record Workout' to log what exercise you did (Running, Gym, Cycling, etc.) and maintain your streak.",
                                style = Typography.bodySmall,
                                color = OfflineTextSecondary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            recentWorkouts.take(6).forEach { workout ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = OfflineSurfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, OfflineBorder.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(workout.title, style = Typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = OfflineTextPrimary)
                                            Text("${workout.sessionDate} • ${workout.activityType.lowercase().replaceFirstChar { it.uppercase() }}${workout.notes?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""}", style = Typography.labelSmall, color = OfflineTextSecondary)
                                        }
                                        Text("${workout.durationSeconds / 60}m", style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accentColor)
                                    }
                                }
                            }
                        }
                    }
                }

                // --- DETOX SPECIFIC SECTION ---
                if (streakTitle.contains("Detox", ignoreCase = true)) {
                    Spacer(modifier = Modifier.height(18.dp))
                    val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val todayDetoxMins = recentDetoxSessions
                        .filter { it.sessionDate == todayStr && it.status == "COMPLETED" }
                        .sumOf { it.actualMinutes }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accentColor.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Today's Focus Time", style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = OfflineTextPrimary)
                                Text("$todayDetoxMins / 60 min", style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = accentColor)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (todayDetoxMins.toFloat() / 60f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = accentColor,
                                trackColor = OfflineBorder.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (todayDetoxMins >= 60) "1-hour detox goal reached for today! Streak qualified ✓" else "Need ${(60 - todayDetoxMins).coerceAtLeast(0)} more mins today to qualify streak (min 1 hour).",
                                style = Typography.bodySmall.copy(fontSize = 11.sp),
                                color = if (todayDetoxMins >= 60) Color(0xFF059669) else OfflineTextSecondary
                            )
                        }
                    }

                    if (recentDetoxSessions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Recent Completed Sessions", style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = OfflineTextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            recentDetoxSessions.filter { it.status == "COMPLETED" }.take(4).forEach { ses ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = OfflineSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(ses.title, style = Typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = OfflineTextPrimary)
                                            Text(ses.sessionDate, style = Typography.labelSmall, color = OfflineTextSecondary)
                                        }
                                        Text("${ses.actualMinutes}m", style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accentColor)
                                    }
                                }
                            }
                        }
                    }
                }

                // --- MOVEMENT SPECIFIC SECTION ---
                if (streakTitle.contains("Movement", ignoreCase = true)) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accentColor.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Step & Energy Tracking", style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = OfflineTextPrimary)
                                Text("Walking & Stride", style = Typography.labelSmall, color = OfflineTextSecondary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Every step tracked automatically burns active calories (~40 kcal per 1,000 steps). Hitting your daily target qualifies your streak and records your walking energy in your daily wellness history.",
                                style = Typography.bodySmall.copy(fontSize = 12.sp),
                                color = OfflineTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Weekly Grace Protection",
                    style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = OfflineTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = OfflineSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "This streak is protected by your Shared Weekly Grace Day. If you miss a scheduled day, the grace day will automatically preserve your streak without penalty (1 day per week, Mon–Sun).",
                        style = Typography.bodySmall,
                        color = OfflineTextSecondary,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OfflineDarkButton)
                ) {
                    Text("Got It", color = OfflineOnDarkButton, style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }
}

@Composable
fun GraceDayDetailDialog(
    graceStatus: WeeklyGraceStatus,
    onUseGraceDay: (LocalDate) -> Unit = {},
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = OfflineSurface,
            border = BorderStroke(1.dp, OfflineBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (graceStatus.isGraceUsedThisWeek) Color(0xFFF59E0B).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Grace Day",
                                    tint = if (graceStatus.isGraceUsedThisWeek) Color(0xFFF59E0B) else Color(0xFF10B981),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Weekly Grace Day",
                                style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = OfflineTextPrimary
                            )
                            Text(
                                text = "Streak protection safeguard",
                                style = Typography.bodySmall,
                                color = OfflineTextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = OfflineTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (graceStatus.isGraceUsedThisWeek) Color(0xFFF59E0B).copy(alpha = 0.1f) else Color(0xFF10B981).copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, if (graceStatus.isGraceUsedThisWeek) Color(0xFFF59E0B).copy(alpha = 0.3f) else Color(0xFF10B981).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Status for Week ${graceStatus.weekNumber}, ${graceStatus.year}",
                                style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = OfflineTextPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (graceStatus.isGraceUsedThisWeek) Color(0xFFF59E0B) else Color(0xFF10B981)
                            ) {
                                Text(
                                    text = if (graceStatus.isGraceUsedThisWeek) "USED" else "ACTIVE & READY",
                                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (graceStatus.isGraceUsedThisWeek) {
                            Text(
                                text = "Protected Missed Date: ${graceStatus.protectedDate}",
                                style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFD97706)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your streaks across Habits, Detox, Movement, and Workouts were preserved for this day.",
                                style = Typography.bodySmall,
                                color = OfflineTextSecondary
                            )
                        } else {
                            Text(
                                text = "1 Protected Missed Day Available",
                                style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF059669)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "If you miss a scheduled goal any day this week (Mon–Sun), your grace day will automatically activate to preserve your streaks.",
                                style = Typography.bodySmall,
                                color = OfflineTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Grace Day Principles",
                    style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = OfflineTextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "• One Shared Day: Protects all 4 streaks simultaneously for the same missed calendar date.",
                        style = Typography.bodySmall,
                        color = OfflineTextPrimary
                    )
                    Text(
                        text = "• Resets Weekly: New grace day granted every Monday at 00:00. Unused days do not accumulate.",
                        style = Typography.bodySmall,
                        color = OfflineTextPrimary
                    )
                    Text(
                        text = "• Pure Integrity: Grace protects your streak continuity, but awards 0 fabricated XP.",
                        style = Typography.bodySmall,
                        color = OfflineTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (!graceStatus.isGraceUsedThisWeek) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.08f))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "Use Grace Day for Today?",
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = OfflineTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This will maintain and protect all your active streaks (Habits, Detox, Movement, Workouts) for today even if daily targets weren't met.",
                            style = Typography.bodySmall,
                            color = OfflineTextSecondary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, OfflineBorder)
                            ) {
                                Text("Cancel", color = OfflineTextPrimary)
                            }
                            Button(
                                onClick = {
                                    onUseGraceDay(LocalDate.now())
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                            ) {
                                Text("Use Today", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OfflineDarkButton)
                    ) {
                        Text("Understood", color = OfflineOnDarkButton, style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementDetailDialog(
    achievement: AchievementEntity,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = OfflineSurface,
            border = BorderStroke(1.dp, OfflineBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (achievement.isUnlocked) Color(0xFF10B981).copy(alpha = 0.15f) else OfflineSurfaceVariant,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (achievement.category) {
                                        "DISCONNECT" -> Icons.Default.Spa
                                        "MOVE" -> Icons.Default.DirectionsWalk
                                        "RECONNECT" -> Icons.Default.Park
                                        "CONSISTENCY" -> Icons.Default.LocalFireDepartment
                                        else -> Icons.Default.WorkspacePremium
                                    },
                                    contentDescription = achievement.title,
                                    tint = if (achievement.isUnlocked) Color(0xFF059669) else OfflineTextMuted,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = achievement.title,
                                style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = OfflineTextPrimary
                            )
                            Text(
                                text = achievement.category.lowercase().replaceFirstChar { it.uppercase() },
                                style = Typography.bodySmall,
                                color = OfflineTextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = OfflineTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (achievement.isUnlocked) Color(0xFF10B981).copy(alpha = 0.1f) else OfflineSurfaceVariant,
                    border = BorderStroke(1.dp, if (achievement.isUnlocked) Color(0xFF10B981).copy(alpha = 0.3f) else OfflineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (achievement.isUnlocked) "UNLOCKED ✓" else "LOCKED",
                            style = Typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (achievement.isUnlocked) Color(0xFF059669) else OfflineTextSecondary
                            )
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = OfflineAccentPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "+${achievement.xpReward} Zen XP",
                                style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = OfflineAccentPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Description",
                    style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = OfflineTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = achievement.description,
                    style = Typography.bodyMedium,
                    color = OfflineTextPrimary
                )

                if (achievement.isUnlocked && achievement.unlockedAt != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Unlocked Date",
                        style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = OfflineTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val dateFmt = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
                    Text(
                        text = dateFmt.format(Date(achievement.unlockedAt)),
                        style = Typography.bodySmall,
                        color = Color(0xFF16A34A)
                    )
                }

                if (!achievement.isUnlocked) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Progress: ${(achievement.progress * 100).toInt()}%",
                        style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = OfflineTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { achievement.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = OfflineAccentPrimary,
                        trackColor = OfflineSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OfflineDarkButton)
                ) {
                    Text("Close", color = OfflineOnDarkButton, style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }
}

@Composable
fun ActionHistoryDialog(
    pastActions: List<ActionHistoryEntity>,
    futureActions: List<ActionHistoryEntity>,
    onDismiss: () -> Unit
) {
    val timeFmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = OfflineSurface,
            border = BorderStroke(1.dp, OfflineBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
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
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Action History",
                                    tint = OfflineAccentPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "5-Step Action History",
                                style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = OfflineTextPrimary
                            )
                            Text(
                                text = "Persistent undo/redo stack state",
                                style = Typography.bodySmall,
                                color = OfflineTextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = OfflineTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stack summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = OfflineSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Undoable Actions", style = Typography.labelSmall, color = OfflineTextMuted)
                            Text("${pastActions.size}", style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = OfflineTextPrimary)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = OfflineSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Redoable Actions", style = Typography.labelSmall, color = OfflineTextMuted)
                            Text("${futureActions.size}", style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = OfflineTextPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Recent Operations (Past & Future)",
                    style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = OfflineTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                val allRecent = (pastActions + futureActions).sortedByDescending { it.sequenceOrder }
                if (allRecent.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = OfflineSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No recent actions in the undo stack.",
                            style = Typography.bodySmall,
                            color = OfflineTextMuted,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        allRecent.take(5).forEach { action ->
                            val isPast = action.stackPosition == "PAST"
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isPast) OfflineSurfaceVariant.copy(alpha = 0.7f) else OfflineSurfaceVariant.copy(alpha = 0.3f),
                                border = BorderStroke(1.dp, if (isPast) OfflineBorder else OfflineBorder.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = action.description,
                                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = OfflineTextPrimary
                                        )
                                        Text(
                                            text = "${action.actionType} • ${timeFmt.format(Date(action.timestamp))}",
                                            style = Typography.labelSmall.copy(fontSize = 10.sp),
                                            color = OfflineTextSecondary
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isPast) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF3B82F6).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (isPast) "DONE" else "UNDONE",
                                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                            color = if (isPast) Color(0xFF059669) else Color(0xFF2563EB),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OfflineDarkButton)
                ) {
                    Text("Close", color = OfflineOnDarkButton, style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }
}
