package com.example.detoxproject.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.detoxproject.ui.theme.*

@Composable
fun ManualDurationDialog(
    initialTotalSeconds: Int,
    minSeconds: Int = 5,
    maxSeconds: Int = 720 * 60,
    onDismiss: () -> Unit,
    onConfirm: (totalSeconds: Int) -> Unit
) {
    var hoursText by remember { mutableStateOf((initialTotalSeconds / 3600).toString()) }
    var minsText by remember { mutableStateOf(((initialTotalSeconds % 3600) / 60).toString()) }
    var secsText by remember { mutableStateOf((initialTotalSeconds % 60).toString()) }
    val focusManager = LocalFocusManager.current

    val currentHours = hoursText.toIntOrNull() ?: 0
    val currentMins = minsText.toIntOrNull() ?: 0
    val currentSecs = secsText.toIntOrNull() ?: 0
    val totalSeconds = (currentHours * 3600) + (currentMins * 60) + currentSecs

    val isValid = totalSeconds in minSeconds..maxSeconds
    val errorMessage = when {
        totalSeconds < minSeconds -> "Minimum duration is $minSeconds seconds"
        totalSeconds > maxSeconds -> "Maximum duration is 12 hours"
        else -> null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = OfflineSurface),
            border = BorderStroke(1.dp, OfflineBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = OfflineAccentPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Set Custom Duration",
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                        color = OfflineTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Specify hours, minutes, and seconds",
                    style = Typography.bodySmall,
                    color = OfflineTextSecondary
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Hours, Minutes, and Seconds Input Columns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. HOURS Column
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "HOURS",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = OfflineTextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val next = (currentHours - 1).coerceAtLeast(0)
                                    hoursText = next.toString()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Remove, null, tint = OfflineTextPrimary, modifier = Modifier.size(16.dp))
                            }
                            CleanNumberDisplayBox(
                                value = hoursText,
                                onTap = {
                                    val next = if (currentHours >= 12) 0 else currentHours + 1
                                    hoursText = next.toString()
                                }
                            )
                            IconButton(
                                onClick = {
                                    val next = (currentHours + 1).coerceAtMost(12)
                                    hoursText = next.toString()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, null, tint = OfflineTextPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Text(
                        text = ":",
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = OfflineTextSecondary,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )

                    // 2. MINUTES Column
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "MINUTES",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = OfflineTextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val next = (currentMins - 1).coerceAtLeast(0)
                                    minsText = next.toString()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Remove, null, tint = OfflineTextPrimary, modifier = Modifier.size(16.dp))
                            }
                            CleanNumberDisplayBox(
                                value = minsText,
                                onTap = {
                                    val next = if (currentMins >= 55) 0 else currentMins + 5
                                    minsText = next.toString()
                                }
                            )
                            IconButton(
                                onClick = {
                                    val next = (currentMins + 1).coerceAtMost(59)
                                    minsText = next.toString()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, null, tint = OfflineTextPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Text(
                        text = ":",
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = OfflineTextSecondary,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )

                    // 3. SECONDS Column
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "SECONDS",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = OfflineTextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val next = (currentSecs - 5).coerceAtLeast(0)
                                    secsText = next.toString()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Remove, null, tint = OfflineTextPrimary, modifier = Modifier.size(16.dp))
                            }
                            CleanNumberDisplayBox(
                                value = secsText,
                                onTap = {
                                    val next = if (currentSecs >= 55) 0 else currentSecs + 5
                                    secsText = next.toString()
                                }
                            )
                            IconButton(
                                onClick = {
                                    val next = (currentSecs + 5).coerceAtMost(59)
                                    secsText = next.toString()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, null, tint = OfflineTextPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Presets (Including short second intervals for testing)
                Text(
                    text = "Quick presets",
                    style = Typography.labelSmall,
                    color = OfflineTextSecondary,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(30 to "30s", 60 to "1m", 300 to "5m", 900 to "15m").forEach { (sec, label) ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    hoursText = (sec / 3600).toString()
                                    minsText = ((sec % 3600) / 60).toString()
                                    secsText = (sec % 60).toString()
                                },
                            color = OfflineSurfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = label,
                                style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = OfflineTextPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1500 to "25m", 2700 to "45m", 3600 to "60m", 7200 to "120m").forEach { (sec, label) ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    hoursText = (sec / 3600).toString()
                                    minsText = ((sec % 3600) / 60).toString()
                                    secsText = (sec % 60).toString()
                                },
                            color = OfflineSurfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = label,
                                style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = OfflineTextPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Error / Summary Preview
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = Typography.bodySmall.copy(color = Color(0xFFEF4444)),
                        textAlign = TextAlign.Center
                    )
                } else {
                    val summaryText = buildString {
                        if (currentHours > 0) append("${currentHours}h ")
                        if (currentMins > 0) append("${currentMins}m ")
                        append("${currentSecs}s")
                        append(" ($totalSeconds seconds)")
                    }
                    Text(
                        text = "Total duration: $summaryText",
                        style = Typography.bodySmall.copy(color = OfflineTextSecondary),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = OfflineTextSecondary)
                    }

                    Button(
                        onClick = {
                            if (isValid) {
                                onConfirm(totalSeconds)
                            }
                        },
                        enabled = isValid,
                        colors = ButtonDefaults.buttonColors(containerColor = OfflineDarkButton),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Apply", color = OfflineOnDarkButton)
                    }
                }
            }
        }
    }
}

@Composable
private fun CleanNumberDisplayBox(
    value: String,
    onTap: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(52.dp)
            .height(48.dp)
            .border(1.dp, OfflineBorder, RoundedCornerShape(10.dp))
            .background(OfflineSurface, RoundedCornerShape(10.dp))
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value.ifBlank { "0" },
            style = Typography.titleMedium.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = OfflineTextPrimary
            )
        )
    }
}
