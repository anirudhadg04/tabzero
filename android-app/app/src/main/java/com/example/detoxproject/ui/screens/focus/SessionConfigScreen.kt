package com.example.detoxproject.ui.screens.focus

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.detoxproject.ui.components.MinimalPillButton
import com.example.detoxproject.ui.theme.*

@Composable
fun SessionConfigScreen(
    title: String,
    durationMinutes: Int,
    category: String,
    onBack: () -> Unit,
    onStartSession: (title: String, duration: Int, category: String, allowAudio: Boolean) -> Unit
) {
    var allowAudio by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OfflineBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Top navigation bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = OfflineTextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "session configuration",
                style = Typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = OfflineTextPrimary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Session Overview Card (Reference 1 Aesthetic)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SoftBlue),
            border = BorderStroke(1.dp, OfflineBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Selected Pledge",
                    style = Typography.labelSmall,
                    color = OfflineTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title.replaceFirstChar { it.uppercase() },
                    style = Typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = OfflineTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Target Duration",
                        style = Typography.bodyMedium,
                        color = OfflineTextSecondary
                    )
                    Text(
                        text = "$durationMinutes minutes",
                        style = Typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = OfflineTextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Music-friendly detox configuration (Prescribed in Section 5.5)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = OfflineSurface),
            border = BorderStroke(1.dp, OfflineBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = OfflineTextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Audio-Friendly Detox",
                                style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = OfflineTextPrimary
                            )
                            Text(
                                text = "Allow Spotify & music apps during session",
                                style = Typography.bodyMedium.copy(fontSize = 13.sp),
                                color = OfflineTextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = allowAudio,
                        onCheckedChange = { allowAudio = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = OfflineDarkButton
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Policy-compliant Soft Intervention status card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SoftGreen),
            border = BorderStroke(1.dp, OfflineBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = OfflineTextPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Policy-Compliant Soft Intervention",
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                        color = OfflineTextPrimary
                    )
                    Text(
                        text = "Encourages screen discipline via conscious notification and full-screen pause reminders.",
                        style = Typography.bodyMedium.copy(fontSize = 13.sp),
                        color = OfflineTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        MinimalPillButton(
            text = "Start Detox Session",
            onClick = {
                onStartSession(title, durationMinutes, category, allowAudio)
            }
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}
