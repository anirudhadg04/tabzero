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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.DialogProperties

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
    onDeleteNote: (Long) -> Unit = {},
    onUpdateNote: (id: Long, title: String, content: String, colorHex: String) -> Unit = { _, _, _, _ -> }
) {
    val scrollState = rememberScrollState()
    val isDark = ThemeManager.isDark()
    var isViewerOpen by remember { mutableStateOf(false) }
    var viewerTargetNote by remember { mutableStateOf<MindfulNoteEntity?>(null) }
    var isNotesDropdownExpanded by remember { mutableStateOf(true) }

    if (isViewerOpen) {
        KeepNoteViewerDialog(
            note = viewerTargetNote,
            isDark = isDark,
            onSaveNew = { title, content, colorHex ->
                onSaveNote(title, content, colorHex)
                isNotesDropdownExpanded = true
            },
            onUpdate = { id, title, content, colorHex ->
                onUpdateNote(id, title, content, colorHex)
            },
            onDelete = { id ->
                onDeleteNote(id)
            },
            onDismiss = {
                isViewerOpen = false
                viewerTargetNote = null
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

        // Personal Mindful Notes Section (with [View] dropdown & Google Keep 2-column theme layout)
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
                    text = if (notes.isEmpty()) "Personal thoughts, intentions & reflections" else "${notes.size} ${if (notes.size == 1) "reflection" else "reflections"}",
                    style = Typography.bodySmall,
                    color = OfflineTextSecondary
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // [View] dropdown toggle button
                Surface(
                    onClick = { isNotesDropdownExpanded = !isNotesDropdownExpanded },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isNotesDropdownExpanded) OfflineSurfaceVariant else (if (isDark) OfflineSurface else Color(0xFFE2E8F0)),
                    border = BorderStroke(1.dp, if (isNotesDropdownExpanded) OfflineAccentPrimary else OfflineBorder)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = if (isNotesDropdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = if (isNotesDropdownExpanded) OfflineAccentPrimary else OfflineTextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isNotesDropdownExpanded) "Hide" else "View",
                            style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isNotesDropdownExpanded) OfflineAccentPrimary else OfflineTextPrimary
                        )
                    }
                }

                // [+ Note] Button
                Surface(
                    onClick = {
                        viewerTargetNote = null
                        isViewerOpen = true
                    },
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
        }

        // Dropdown collapsible notes section
        AnimatedVisibility(
            visible = isNotesDropdownExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 14.dp)) {
                if (notes.isEmpty()) {
                    Surface(
                        onClick = {
                            viewerTargetNote = null
                            isViewerOpen = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = if (isDark) OfflineSurface else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, OfflineBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF6366F1).copy(alpha = 0.15f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(imageVector = Icons.Default.EditNote, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No Notes Yet", style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = OfflineTextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Tap '+ Note' above to capture thoughts with colorful themes.", style = Typography.bodySmall, color = OfflineTextSecondary, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    val leftCol = remember(notes) { notes.filterIndexed { i, _ -> i % 2 == 0 } }
                    val rightCol = remember(notes) { notes.filterIndexed { i, _ -> i % 2 == 1 } }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            leftCol.forEach { note ->
                                KeepStyleNoteCard(
                                    note = note,
                                    isDark = isDark,
                                    onClick = {
                                        viewerTargetNote = note
                                        isViewerOpen = true
                                    }
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rightCol.forEach { note ->
                                KeepStyleNoteCard(
                                    note = note,
                                    isDark = isDark,
                                    onClick = {
                                        viewerTargetNote = note
                                        isViewerOpen = true
                                    }
                                )
                            }
                        }
                    }
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

data class NoteTheme(
    val id: String,
    val name: String,
    val lightColor: Color,
    val darkColor: Color,
    val hexCode: String
)

val NOTE_THEMES = listOf(
    NoteTheme("default", "Default", Color(0xFFFFFFFF), Color(0xFF1E293B), "#FFFFFF"),
    NoteTheme("coral", "Coral", Color(0xFFFFE4E6), Color(0xFF4C1D24), "#FFE4E6"),
    NoteTheme("amber", "Amber", Color(0xFFFEF3C7), Color(0xFF452B0E), "#FEF3C7"),
    NoteTheme("sage", "Sage", Color(0xFFDCFCE7), Color(0xFF143E25), "#DCFCE7"),
    NoteTheme("teal", "Teal", Color(0xFFCCFBF1), Color(0xFF13423B), "#CCFBF1"),
    NoteTheme("sky", "Sky", Color(0xFFE0F2FE), Color(0xFF15334D), "#E0F2FE"),
    NoteTheme("lavender", "Lavender", Color(0xFFEDE9FE), Color(0xFF2E1F4D), "#EDE9FE"),
    NoteTheme("blush", "Blush", Color(0xFFFCE7F3), Color(0xFF451935), "#FCE7F3"),
    NoteTheme("slate", "Slate", Color(0xFFF1F5F9), Color(0xFF242C38), "#F1F5F9")
)

fun getNoteColor(hex: String, isDark: Boolean): Color {
    val matched = NOTE_THEMES.find { it.hexCode.equals(hex, ignoreCase = true) }
    if (matched != null) {
        return if (isDark) matched.darkColor else matched.lightColor
    }
    return try {
        val parsed = Color(android.graphics.Color.parseColor(hex))
        if (isDark) {
            Color(
                red = (parsed.red * 0.35f).coerceIn(0f, 1f),
                green = (parsed.green * 0.35f).coerceIn(0f, 1f),
                blue = (parsed.blue * 0.35f).coerceIn(0f, 1f),
                alpha = 1.0f
            )
        } else {
            parsed
        }
    } catch (_: Exception) {
        if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    }
}

@Composable
private fun KeepStyleNoteCard(
    note: MindfulNoteEntity,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val cardColor = remember(note.colorHex, isDark) {
        getNoteColor(note.colorHex, isDark)
    }
    val contentTextColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
    val titleTextColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val dateTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(
            1.dp,
            if (isDark) cardColor.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.08f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (note.title.isNotBlank()) {
                Text(
                    text = note.title,
                    style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                    color = titleTextColor,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (note.content.isNotBlank()) {
                Text(
                    text = note.content,
                    style = Typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                    color = contentTextColor,
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            val dateText = remember(note.updatedAt) {
                val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
                sdf.format(Date(note.updatedAt))
            }

            Text(
                text = dateText,
                style = Typography.labelSmall.copy(fontSize = 10.sp),
                color = dateTextColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeepNoteViewerDialog(
    note: MindfulNoteEntity?,
    isDark: Boolean,
    onSaveNew: (title: String, content: String, colorHex: String) -> Unit,
    onUpdate: (id: Long, title: String, content: String, colorHex: String) -> Unit,
    onDelete: (id: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var selectedColorHex by remember { mutableStateOf(note?.colorHex ?: "#FFFFFF") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val animatedBg by animateColorAsState(
        targetValue = getNoteColor(selectedColorHex, isDark),
        label = "noteBg"
    )

    val titleColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val contentColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
    val placeholderColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF94A3B8)
    val iconTint = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)

    val formattedDate = remember(note?.updatedAt) {
        val dateVal = note?.updatedAt ?: System.currentTimeMillis()
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        sdf.format(Date(dateVal))
    }

    fun saveAndClose() {
        val trimmedTitle = title.trim()
        val trimmedContent = content.trim()
        if (trimmedTitle.isNotBlank() || trimmedContent.isNotBlank()) {
            val finalTitle = trimmedTitle.ifBlank { "Reflections" }
            val finalContent = trimmedContent.ifBlank { trimmedTitle }
            if (note != null) {
                onUpdate(note.id, finalTitle, finalContent, selectedColorHex)
            } else {
                onSaveNew(finalTitle, finalContent, selectedColorHex)
            }
        }
        onDismiss()
    }

    Dialog(
        onDismissRequest = { saveAndClose() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = animatedBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { saveAndClose() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back and save",
                            tint = iconTint
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (note != null) {
                            IconButton(onClick = { showDeleteConfirm = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Note",
                                    tint = if (isDark) Color(0xFFF87171) else Color(0xFFEF4444)
                                )
                            }
                        }

                        IconButton(onClick = { saveAndClose() }) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Done",
                                tint = iconTint
                            )
                        }
                    }
                }

                // Delete Confirmation Banner
                if (showDeleteConfirm && note != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF451A1A) else Color(0xFFFEE2E2),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Delete this note?",
                                style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { showDeleteConfirm = false }) {
                                    Text("Cancel", color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B))
                                }
                                Button(
                                    onClick = {
                                        onDelete(note.id)
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Delete", color = Color.White)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Note Content
                val noteScroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(noteScroll)
                        .padding(horizontal = 8.dp)
                ) {
                    // Title field
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = {
                            Text(
                                text = "Title",
                                style = Typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp),
                                color = placeholderColor
                            )
                        },
                        textStyle = Typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = titleColor,
                            unfocusedTextColor = titleColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Content field
                    TextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = {
                            Text(
                                text = "Note",
                                style = Typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
                                color = placeholderColor
                            )
                        },
                        textStyle = Typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = contentColor,
                            unfocusedTextColor = contentColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 240.dp)
                    )
                }

                // Bottom Theme Bar & Timestamp (Google Keep style)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (note != null) "Edited $formattedDate" else "New Note",
                            style = Typography.labelSmall.copy(fontSize = 11.sp),
                            color = placeholderColor
                        )
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Themes",
                            tint = placeholderColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Theme color picker row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NOTE_THEMES.forEach { theme ->
                            val isSelected = selectedColorHex.equals(theme.hexCode, ignoreCase = true)
                            val displayColor = if (isDark) theme.darkColor else theme.lightColor

                            Surface(
                                onClick = { selectedColorHex = theme.hexCode },
                                shape = CircleShape,
                                color = displayColor,
                                border = BorderStroke(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) (if (isDark) Color.White else Color(0xFF0F172A)) else (if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.15f))
                                ),
                                modifier = Modifier.size(32.dp)
                            ) {
                                if (isSelected) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (isDark) Color.White else Color(0xFF0F172A),
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
