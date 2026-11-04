package com.example.detoxproject.ui.screens.focus

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.detoxproject.data.local.entity.DetoxSessionEntity
import com.example.detoxproject.domain.engine.DetoxTimerEngine
import com.example.detoxproject.ui.components.MinimalPillButton
import com.example.detoxproject.ui.theme.*

@Composable
fun ActiveSessionScreen(
    session: DetoxSessionEntity,
    currentTimeMs: Long,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val scrollState = rememberScrollState()

    val remainingMs = DetoxTimerEngine.calculateRemainingMs(session, currentTimeMs)
    val elapsedMs = DetoxTimerEngine.calculateElapsedMs(session, currentTimeMs)
    val progress = DetoxTimerEngine.calculateProgress(session, currentTimeMs)
    val isPaused = session.status == "PAUSED"
    val timeFormatted = DetoxTimerEngine.formatMinutesSeconds(remainingMs)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OfflineBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Status Badge Pill
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isPaused) SoftAmber else SoftGreen
        ) {
            Text(
                text = if (isPaused) "SESSION PAUSED" else "FOCUS SESSION ACTIVE",
                style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = if (isPaused) Color(0xFFB45309) else Color(0xFF15803D),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Session Title
        Text(
            text = session.title,
            style = Typography.headlineLarge.copy(fontSize = 32.sp, fontWeight = FontWeight.SemiBold),
            color = OfflineTextPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Target: ${session.targetMinutes} minutes",
            style = Typography.bodyLarge,
            color = OfflineTextSecondary
        )

        Spacer(modifier = Modifier.height(36.dp))

        // Circular Timer Display Area (Reference 1 Aesthetic)
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .background(if (isPaused) SoftBeige else SoftBlue),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeFormatted,
                    style = Typography.headlineLarge.copy(
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp
                    ),
                    color = OfflineTextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isPaused) "Paused" else "Remaining",
                    style = Typography.labelLarge,
                    color = OfflineTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Progress indicator
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = OfflineDarkButton,
            trackColor = OfflineBorder
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Primary Control: Pause or Resume
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isPaused) {
                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = OfflineDarkButton),
                    shape = RoundedCornerShape(27.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Resume", style = Typography.titleMedium.copy(color = Color.White))
                }
            } else {
                OutlinedButton(
                    onClick = onPause,
                    shape = RoundedCornerShape(27.dp),
                    border = BorderStroke(1.5.dp, OfflineTextPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = OfflineTextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pause", style = Typography.titleMedium.copy(color = OfflineTextPrimary))
                }
            }

            // Return to Zen Session Button
            Button(
                onClick = onComplete,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                shape = RoundedCornerShape(27.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Spa,
                    contentDescription = "Return to Zen",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Return to Zen", style = Typography.titleMedium.copy(color = Color.White))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cancel Session Text Button
        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cancel",
                tint = OfflineTextMuted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Cancel Session",
                style = Typography.bodyMedium.copy(color = OfflineTextMuted)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
