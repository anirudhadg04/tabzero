package com.example.detoxproject.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.detoxproject.data.integrations.notifications.AlarmState
import com.example.detoxproject.ui.theme.*

@Composable
fun SessionCompletionDialog(
    sessionTitle: String,
    durationMinutes: Int,
    alarmState: AlarmState,
    onStopAlarm: () -> Unit,
    onCompleteAndDismiss: () -> Unit,
    onViewSummary: () -> Unit
) {
    Dialog(
        onDismissRequest = {
            onStopAlarm()
            onCompleteAndDismiss()
        },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = OfflineSurface),
            border = BorderStroke(1.dp, OfflineBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Emblem
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(if (alarmState == AlarmState.RINGING) SoftAmber else SoftGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (alarmState == AlarmState.RINGING) Icons.Default.NotificationsActive else Icons.Default.Spa,
                        contentDescription = null,
                        tint = if (alarmState == AlarmState.RINGING) Color(0xFFD97706) else Color(0xFF16A34A),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = if (alarmState == AlarmState.RINGING) "Time's Up!" else "Focus Complete!",
                    style = Typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = OfflineTextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = sessionTitle,
                    style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = OfflineAccentPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "You accomplished $durationMinutes minutes of mindful focus away from screens. Life happens beyond the screen.",
                    style = Typography.bodyMedium,
                    color = OfflineTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Prominent Stop Alarm Button (Visible when ringing)
                if (alarmState == AlarmState.RINGING) {
                    Button(
                        onClick = onStopAlarm,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AlarmOff,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Stop Alarm Audio",
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Primary Complete & Continue Button
                Button(
                    onClick = onCompleteAndDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = OfflineDarkButton),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = OfflineOnDarkButton,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Complete & Record Session",
                        style = Typography.titleMedium.copy(color = OfflineOnDarkButton)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Secondary View Progress Button
                TextButton(
                    onClick = onViewSummary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "View Progress & Milestones",
                        style = Typography.bodyMedium.copy(color = OfflineTextSecondary, fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}
