package com.example.detoxproject.ui.screens.discover

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
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
import androidx.compose.ui.window.Dialog
import com.example.detoxproject.data.local.entity.MindfulNoteEntity
import com.example.detoxproject.data.local.entity.WeatherCacheEntity
import com.example.detoxproject.domain.engine.WellnessRecommendation
import com.example.detoxproject.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class CategoryStyle(
    val color: Color,
    val darkColor: Color,
    val icon: ImageVector,
    val label: String
)

private fun getCategoryStyle(category: String): CategoryStyle {
    return when (category.lowercase()) {
        "movement", "walk", "exercise" -> CategoryStyle(
            color = Color(0xFF059669),
            darkColor = Color(0xFF34D399),
            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
            label = "Movement"
        )
        "reading", "learn", "study" -> CategoryStyle(
            color = Color(0xFF4F46E5),
            darkColor = Color(0xFF818CF8),
            icon = Icons.AutoMirrored.Filled.MenuBook,
            label = "Reading"
        )
        "mindfulness", "meditation", "tea" -> CategoryStyle(
            color = Color(0xFF0891B2),
            darkColor = Color(0xFF22D3EE),
            icon = Icons.Default.Spa,
            label = "Mindfulness"
        )
        "nature", "outdoor" -> CategoryStyle(
            color = Color(0xFF65A30D),
            darkColor = Color(0xFFA3E635),
            icon = Icons.Default.Park,
            label = "Nature"
        )
        else -> CategoryStyle(
            color = Color(0xFF9333EA),
            darkColor = Color(0xFFC084FC),
            icon = Icons.Default.SelfImprovement,
            label = category.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    weather: WeatherCacheEntity?,
    recommendations: List<WellnessRecommendation> = emptyList(),
    notes: List<MindfulNoteEntity> = emptyList(),
    onRefreshWeather: () -> Unit,
    onAddActivityToPlanner: (title: String, duration: Int, category: String) -> Unit,
    onSaveNote: (title: String, content: String, colorHex: String) -> Unit = { _, _, _ -> },
    onDeleteNote: (Long) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val isDark = ThemeManager.isDark()
    var showAddNoteDialog by remember { mutableStateOf(false) }

    if (showAddNoteDialog) {
        AddMindfulNoteDialog(
            onSave = { title, content, colorHex ->
                onSaveNote(title, content, colorHex)
                showAddNoteDialog = false
            },
            onDismiss = { showAddNoteDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OfflineBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "more mindful",
                style = Typography.headlineLarge.copy(fontSize = 28.sp, fontWeight = FontWeight.SemiBold),
                color = OfflineTextPrimary
            )

            IconButton(onClick = onRefreshWeather) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh weather",
                    tint = OfflineTextPrimary
                )
            }
        }

        Text(
            text = "Weather-aware offline habits, reflections & personal notes",
            style = Typography.bodyMedium,
            color = OfflineTextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Weather Context Card - Always displayed at top
        val weatherCardBg = if (isDark) OfflineSurface else SoftBeigeLight
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = weatherCardBg),
            border = BorderStroke(1.dp, OfflineBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = weather?.locationName ?: "Current Location",
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = OfflineTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = weather?.conditionText ?: "Partly Cloudy",
                            style = Typography.bodyMedium,
                            color = OfflineTextSecondary
                        )
                    }

                    Text(
                        text = "${weather?.temperature?.toInt() ?: 21}°C",
                        style = Typography.headlineLarge.copy(fontSize = 38.sp, fontWeight = FontWeight.Bold),
                        color = OfflineTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Humidity: ${weather?.humidity ?: 90}%",
                        style = Typography.labelSmall,
                        color = OfflineTextSecondary
                    )
                    Text(
                        text = "Wind: ${weather?.windSpeed ?: 5.3} km/h",
                        style = Typography.labelSmall,
                        color = OfflineTextSecondary
                    )
                    Text(
                        text = if (weather?.isCached == true) "Source: Cached" else "Source: Live Open-Meteo",
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = if (weather?.isCached == true) OfflineTextMuted else (if (isDark) Color(0xFF34D399) else Color(0xFF059669))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Recommended Activities Section
        Text(
            text = "Recommended Activities",
            style = Typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
            color = OfflineTextPrimary
        )

        Spacer(modifier = Modifier.height(14.dp))

        val displayList = if (recommendations.isNotEmpty()) recommendations else listOf(
            WellnessRecommendation("rec_walk", "Neighborhood Walk", "movement", 30, true, "Great weather outside (21.4°C) for an uninterrupted walk without headphones."),
            WellnessRecommendation("rec_read", "Park Bench Reading", "reading", 35, true, "Take a physical paperback to a quiet shaded park."),
            WellnessRecommendation("rec_tea", "Mindful Tea Ritual", "mindfulness", 15, false, "Brew tea and enjoy every sip with your phone face-down.")
        )

        val lightPalette = listOf(SoftGreenLight, SoftBlueLight, SoftTealLight, SoftAmberLight, SoftRoseLight)
        displayList.forEachIndexed { idx, rec ->
            val lightBg = lightPalette[idx % lightPalette.size]
            RecommendationItem(
                title = rec.title,
                category = rec.category,
                durationMinutes = rec.durationMinutes,
                reason = rec.reason,
                isDark = isDark,
                lightBgColor = lightBg,
                onAdd = { onAddActivityToPlanner(rec.title, rec.durationMinutes, rec.category) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Personal Mindful Notes Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mindful Notes",
                    style = Typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                    color = OfflineTextPrimary
                )
                Text(
                    text = "Personal thoughts, intentions & reflections",
                    style = Typography.bodySmall,
                    color = OfflineTextSecondary
                )
            }

            Surface(
                onClick = { showAddNoteDialog = true },
                shape = RoundedCornerShape(10.dp),
                color = OfflineDarkButton
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = OfflineOnDarkButton, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Note",
                        style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = OfflineOnDarkButton
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (notes.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) OfflineSurface else Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, OfflineBorder.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF6366F1).copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.EditNote, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("No Notes Yet", style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = OfflineTextPrimary)
                            Text("Tap '+ Note' above to write down whatever is on your mind.", style = Typography.bodySmall, color = OfflineTextSecondary)
                        }
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                notes.forEach { note ->
                    MindfulNoteCard(
                        note = note,
                        isDark = isDark,
                        onDelete = { onDeleteNote(note.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun RecommendationItem(
    title: String,
    category: String,
    durationMinutes: Int,
    reason: String,
    isDark: Boolean,
    lightBgColor: Color,
    onAdd: () -> Unit
) {
    val catStyle = getCategoryStyle(category)
    val cardBackground = if (isDark) OfflineSurface else lightBgColor

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.dp, OfflineBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row: Category Pill & Duration Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) catStyle.darkColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, if (isDark) catStyle.darkColor.copy(alpha = 0.35f) else Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = catStyle.icon,
                            contentDescription = null,
                            tint = if (isDark) catStyle.darkColor else catStyle.color,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = catStyle.label,
                            style = Typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            color = if (isDark) catStyle.darkColor else catStyle.color
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) OfflineSurfaceVariant else Color.White.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, if (isDark) OfflineBorder else Color.Black.copy(alpha = 0.05f))
                ) {
                    Text(
                        text = "$durationMinutes min",
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isDark) OfflineTextPrimaryDark else Color(0xFF1E293B),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = OfflineTextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = reason,
                style = Typography.bodySmall,
                color = OfflineTextSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OfflineDarkButton,
                    contentColor = OfflineOnDarkButton
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.align(Alignment.End),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = OfflineOnDarkButton,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Add to Planner",
                    style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = OfflineOnDarkButton
                )
            }
        }
    }
}

@Composable
private fun MindfulNoteCard(
    note: MindfulNoteEntity,
    isDark: Boolean,
    onDelete: () -> Unit
) {
    val noteBgColor = remember(note.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(note.colorHex))
        } catch (_: Exception) {
            Color(0xFFF1F5F9)
        }
    }

    val cardBg = if (isDark) OfflineSurface else noteBgColor
    val dateText = remember(note.createdAt) {
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        sdf.format(Date(note.createdAt))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, if (isDark) noteBgColor.copy(alpha = 0.4f) else OfflineBorder.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = if (isDark) noteBgColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = if (isDark) noteBgColor else Color(0xFF334155),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = note.title.ifBlank { "Reflections" },
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isDark) OfflineTextPrimary else Color(0xFF0F172A)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Note",
                        tint = if (isDark) OfflineTextMuted else Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = note.content,
                style = Typography.bodyMedium,
                color = if (isDark) OfflineTextSecondary else Color(0xFF334155),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = dateText,
                style = Typography.labelSmall.copy(fontSize = 10.sp),
                color = if (isDark) OfflineTextMuted else Color(0xFF64748B)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMindfulNoteDialog(
    onSave: (title: String, content: String, colorHex: String) -> Unit,
    onDismiss: () -> Unit
) {
    val pastelPalette = remember {
        listOf(
            "#F1F5F9", // Soft Slate / Ice (matches 6th screenshot)
            "#E8F5E9", // Soft Mint Green
            "#EDE7F6", // Soft Lavender
            "#E0F2FE", // Soft Sky Blue
            "#FEF3C7", // Soft Warm Cream
            "#FCE7F3", // Soft Blush Rose
            "#CCFBF1"  // Soft Teal
        )
    }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(pastelPalette.random()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = OfflineSurface,
            border = BorderStroke(1.dp, OfflineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New Mindful Note",
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OfflineTextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = OfflineTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. Grateful Today)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("What's on your mind?") },
                    minLines = 4,
                    maxLines = 7,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Card Tint", style = Typography.labelSmall, color = OfflineTextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pastelPalette.forEach { hex ->
                        val isSel = selectedColor == hex
                        val c = remember(hex) { Color(android.graphics.Color.parseColor(hex)) }
                        Surface(
                            onClick = { selectedColor = hex },
                            shape = CircleShape,
                            color = c,
                            border = BorderStroke(if (isSel) 2.5.dp else 1.dp, if (isSel) OfflineAccentPrimary else OfflineBorder.copy(alpha = 0.5f)),
                            modifier = Modifier.size(30.dp)
                        ) {}
                    }
                }

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
                            if (content.isNotBlank()) {
                                onSave(title, content, selectedColor)
                            }
                        },
                        enabled = content.isNotBlank(),
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OfflineDarkButton)
                    ) {
                        Text("Save Note", color = OfflineOnDarkButton)
                    }
                }
            }
        }
    }
}
