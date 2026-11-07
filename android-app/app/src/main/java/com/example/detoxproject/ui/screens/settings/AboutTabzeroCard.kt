package com.example.detoxproject.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.detoxproject.BuildConfig
import com.example.detoxproject.ui.theme.*

@Composable
fun AboutTabzeroCard() {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = OfflineSurface),
        border = BorderStroke(1.dp, OfflineBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Card Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(OfflineAccentPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = OfflineAccentPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "About tabzero.",
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = OfflineTextPrimary
                        )
                        Text(
                            text = "Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}) • Local-First",
                            style = Typography.labelSmall.copy(fontSize = 11.sp),
                            color = OfflineTextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = OfflineTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "tabzero. is a mindful digital detox companion designed to help you regain focus, curb compulsive app habits, and cultivate daily physical and mental balance — 100% locally on your device.",
                style = Typography.bodySmall,
                color = OfflineTextSecondary,
                lineHeight = 18.sp
            )

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = OfflineBorder.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. HOW TABZERO WORKS
                    SectionHeader("HOW TABZERO WORKS", Icons.Default.Psychology)
                    Text(
                        text = "• Focus Sessions: Launch deliberate detox sessions with custom countdown timers and selected restricted apps.\n" +
                                "• Smart App Intervention: When a designated distracting app is opened during a session, the Zen Intervention screen pauses you calmly before mindless scrolling begins.\n" +
                                "• Habit Completion & Schedules: Build recurring daily routines, track completions, and view scheduled consistency.\n" +
                                "• Streaks & Zen XP: Earn XP and build streaks across detox sessions, movement goals, and habit completions with weekly Grace Day protection.\n" +
                                "• Physical Movement: Count daily steps and distance using native hardware sensors and optional Health Connect sync.\n" +
                                "• Weather Recommendations: Context-aware outdoor vs indoor suggestions powered by Open-Meteo.\n" +
                                "• Local Persistence: Everything stays in your phone's Room SQLite database.",
                        style = Typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
                        color = OfflineTextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. SMART SYSTEM FEATURES
                    SectionHeader("SMART FEATURES & SERVICES", Icons.Default.SettingsApplications)
                    Text(
                        text = "• Usage Access: Calculates real-time device screen time and monitors distracting app launches.\n" +
                                "• Accessibility Service: Provides 0ms instant window detection without polling. Configured strictly with canRetrieveWindowContent=\"false\" so it never reads screen content or keystrokes.\n" +
                                "• Display Over Other Apps: Renders the full-screen calming Zen reminder when restricted apps are launched.\n" +
                                "• Exact Alarms: Punctual session completion chimes even in deep Android Doze mode.\n" +
                                "• Notifications: Live countdown in notification shade with pause/resume controls.\n" +
                                "• Step Sensor: Direct hardware step counter access for offline step goals.",
                        style = Typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
                        color = OfflineTextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. PRIVACY GUARANTEE
                    SectionHeader("PRIVACY GUARANTEE", Icons.Default.Security)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SoftGreen.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "100% Local-First Architecture",
                                style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No user data, passwords, session histories, habits, notes, or sensor logs are ever uploaded to cloud servers. No analytics SDKs, trackers, or telemetry libraries are bundled in the application. Password authentication uses salted SHA-256 hashing strictly on-device.",
                                style = Typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp, color = Color(0xFF1B5E20))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. OFFLINE FUNCTIONALITY
                    SectionHeader("OFFLINE MODE", Icons.Default.CloudOff)
                    Text(
                        text = "• Fully Offline Capable: All core detox features, app blocking, habits, notes, streaks, undo/redo, and timers work 100% without internet.\n" +
                                "• Weather Service: Open-Meteo is the only feature requiring internet. If offline, the app continues smoothly using cached weather.",
                        style = Typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
                        color = OfflineTextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. THIRD-PARTY CREDITS
                    SectionHeader("CREDITS & ATTRIBUTIONS", Icons.Default.Code)
                    Text(
                        text = "• Weather Forecast Data: Open-Meteo (open-meteo.com, Free Open-Source API)\n" +
                                "• Android Jetpack: Compose, Room SQLite, Navigation, Lifecycle\n" +
                                "• UI Design: Material 3 Design System",
                        style = Typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
                        color = OfflineTextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 6. BUILD & APPLICATION METADATA
                    SectionHeader("APPLICATION BUILD INFO", Icons.Default.Terminal)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = OfflineSurfaceVariant,
                        border = BorderStroke(1.dp, OfflineBorder.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            MetadataRow("Application ID", BuildConfig.APPLICATION_ID)
                            MetadataRow("Version Name", BuildConfig.VERSION_NAME)
                            MetadataRow("Version Code", BuildConfig.VERSION_CODE.toString())
                            MetadataRow("Target Android SDK", "35 (Android 15)")
                            MetadataRow("Minimum Android SDK", "26 (Android 8.0)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OfflineAccentPrimary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            color = OfflineAccentPrimary
        )
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = Typography.labelSmall.copy(fontSize = 11.sp),
            color = OfflineTextSecondary
        )
        Text(
            text = value,
            style = Typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
            color = OfflineTextPrimary
        )
    }
}
