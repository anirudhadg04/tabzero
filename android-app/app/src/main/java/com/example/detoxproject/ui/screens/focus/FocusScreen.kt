package com.example.detoxproject.ui.screens.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.detoxproject.data.local.entity.DetoxSessionEntity
import com.example.detoxproject.ui.components.CircularDialSelector
import com.example.detoxproject.ui.components.MinimalPillButton
import com.example.detoxproject.ui.theme.*

@Composable
fun FocusScreen(
    activeSession: DetoxSessionEntity?,
    currentTimeMs: Long,
    initialCategory: String = "focus",
    onStartSession: (title: String, durationMinutes: Int, category: String, allowAudio: Boolean, restrictedPackages: String, durationSeconds: Int) -> Unit,
    onPauseSession: () -> Unit,
    onResumeSession: () -> Unit,
    onCompleteSession: () -> Unit,
    onCancelSession: () -> Unit
) {
    // If an active session is currently running or paused, show the live ActiveSessionScreen
    if (activeSession != null) {
        ActiveSessionScreen(
            session = activeSession,
            currentTimeMs = currentTimeMs,
            onPause = onPauseSession,
            onResume = onResumeSession,
            onComplete = onCompleteSession,
            onCancel = onCancelSession
        )
        return
    }

    var isSelectingApps by remember { mutableStateOf(false) }
    var planTitle by remember(initialCategory) {
        mutableStateOf(if (initialCategory.isNotBlank() && initialCategory != "focus") initialCategory else "study")
    }
    var selectedCategory by remember(initialCategory) {
        mutableStateOf(if (initialCategory.isNotBlank() && initialCategory != "focus") initialCategory else "study")
    }
    var durationMinutes by remember(initialCategory) {
        mutableIntStateOf(
            when (initialCategory.lowercase()) {
                "work" -> 25
                "reading" -> 30
                "personal" -> 20
                "sleep" -> 480
                else -> 25
            }
        )
    }
    var durationSeconds by remember(initialCategory) {
        mutableIntStateOf(durationMinutes * 60)
    }

    if (isSelectingApps) {
        AppSelectionScreen(
            title = planTitle,
            durationMinutes = durationMinutes,
            category = selectedCategory,
            onBack = { isSelectingApps = false },
            onStartDetox = { selectedApps, allowAudio ->
                onStartSession(planTitle, durationMinutes, selectedCategory, allowAudio, selectedApps.joinToString(","), durationSeconds)
                isSelectingApps = false
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OfflineBackground)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top row with options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = OfflineTextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large title: "create a new plan" (Reference Image 1 center screen)
        Text(
            text = "create a new plan",
            style = Typography.headlineLarge.copy(fontSize = 30.sp, fontWeight = FontWeight.SemiBold),
            color = OfflineTextPrimary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Subtitle: "what do you want to focus on?"
        Text(
            text = "what do you want to focus on?",
            style = Typography.bodyLarge,
            color = OfflineTextSecondary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Plan title input box
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = OfflineSurfaceVariant.copy(alpha = 0.7f)
        ) {
            TextField(
                value = planTitle,
                onValueChange = { planTitle = it },
                textStyle = Typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                placeholder = {
                    Text(
                        text = "e.g. study, reading, deep work",
                        color = OfflineTextMuted
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Unified Clock-Face Radial Dial Selector with Hours, Minutes, and Seconds
        CircularDialSelector(
            selectedMinutes = durationMinutes,
            onMinutesChanged = { durationMinutes = it },
            selectedSeconds = durationSeconds,
            onTotalSecondsChanged = {
                durationSeconds = it
                durationMinutes = (it / 60).coerceAtLeast(if (it > 0) 1 else 0)
            },
            minMinutes = 1,
            maxMinutes = 720,
            step = 5
        )

        Spacer(modifier = Modifier.weight(1f))

        // "continue" minimal black pill button
        MinimalPillButton(
            text = "continue",
            enabled = planTitle.isNotBlank(),
            onClick = {
                isSelectingApps = true
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
