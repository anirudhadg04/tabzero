package com.example.detoxproject.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.detoxproject.data.integrations.health.HealthActivityManager
import com.example.detoxproject.data.integrations.health.HealthConnectStatus
import com.example.detoxproject.data.integrations.usage.UsageStatsHelper
import com.example.detoxproject.ui.theme.*

data class PermissionGuideStatus(
    val hasUsageAccess: Boolean,
    val hasAccessibility: Boolean,
    val hasOverlay: Boolean,
    val hasNotifications: Boolean,
    val hasExactAlarms: Boolean,
    val hasActivityRecognition: Boolean,
    val healthConnectStatus: HealthConnectStatus,
    val isBatteryUnrestricted: Boolean
)

@Composable
fun PermissionPrivacyGuideDialog(
    status: PermissionGuideStatus,
    onDismiss: () -> Unit,
    onRequestPermissionAction: (String) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(26.dp),
            color = OfflineSurface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, OfflineBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(OfflineAccentPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = OfflineAccentPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Permission & Privacy Guide",
                                style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                                color = OfflineTextPrimary
                            )
                            Text(
                                text = "Local-first security & Android permission transparency",
                                style = Typography.labelSmall,
                                color = OfflineTextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(OfflineSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = OfflineTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    // 1. PRIVACY DECLARATION CARD: "Your data. Your device. Your control."
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SoftGreen.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Your data. Your device. Your control.",
                                    style = Typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF1B5E20)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "tabzero is designed to operate locally on your device.\n\nYour detox sessions, habits, progress, and other locally stored application information remain strictly on your device in your phone's SQLite database.\n\nPermissions are used solely to provide the functionality you enable. Internet connectivity is only required for weather information retrieved from Open-Meteo. Core detox functionality operates completely offline without internet.",
                                style = Typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = Color(0xFF1B5E20)
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PrivacyTag("100% Local SQLite")
                                PrivacyTag("Zero Trackers")
                                PrivacyTag("Weather-Only Net")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "LIVE SYSTEM PERMISSIONS AUDIT",
                        style = Typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = OfflineAccentPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. LIVE PERMISSION LIST (All 8 permissions)
                    GuidePermissionItem(
                        detail = PermissionEducationManager.getDetail(PermissionEducationManager.PERM_USAGE_ACCESS),
                        isGranted = status.hasUsageAccess,
                        grantedText = "Granted & Active",
                        notGrantedText = "Not Granted",
                        onConfigure = { onRequestPermissionAction(PermissionEducationManager.PERM_USAGE_ACCESS) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    GuidePermissionItem(
                        detail = PermissionEducationManager.getDetail(PermissionEducationManager.PERM_ACCESSIBILITY),
                        isGranted = status.hasAccessibility,
                        grantedText = "Active (0ms Detection)",
                        notGrantedText = "Not Enabled (Optional)",
                        onConfigure = { onRequestPermissionAction(PermissionEducationManager.PERM_ACCESSIBILITY) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    GuidePermissionItem(
                        detail = PermissionEducationManager.getDetail(PermissionEducationManager.PERM_OVERLAY),
                        isGranted = status.hasOverlay,
                        grantedText = "Allowed",
                        notGrantedText = "Not Allowed",
                        onConfigure = { onRequestPermissionAction(PermissionEducationManager.PERM_OVERLAY) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    GuidePermissionItem(
                        detail = PermissionEducationManager.getDetail(PermissionEducationManager.PERM_NOTIFICATIONS),
                        isGranted = status.hasNotifications,
                        grantedText = "Allowed",
                        notGrantedText = "Denied",
                        onConfigure = { onRequestPermissionAction(PermissionEducationManager.PERM_NOTIFICATIONS) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    GuidePermissionItem(
                        detail = PermissionEducationManager.getDetail(PermissionEducationManager.PERM_EXACT_ALARMS),
                        isGranted = status.hasExactAlarms,
                        grantedText = "Enabled",
                        notGrantedText = "Disabled",
                        onConfigure = { onRequestPermissionAction(PermissionEducationManager.PERM_EXACT_ALARMS) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    GuidePermissionItem(
                        detail = PermissionEducationManager.getDetail(PermissionEducationManager.PERM_ACTIVITY_RECOGNITION),
                        isGranted = status.hasActivityRecognition,
                        grantedText = "Sensor Active",
                        notGrantedText = "Not Granted",
                        onConfigure = { onRequestPermissionAction(PermissionEducationManager.PERM_ACTIVITY_RECOGNITION) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val isHcInstalled = status.healthConnectStatus == HealthConnectStatus.AVAILABLE_INSTALLED
                    GuidePermissionItem(
                        detail = PermissionEducationManager.getDetail(PermissionEducationManager.PERM_HEALTH_CONNECT),
                        isGranted = isHcInstalled,
                        grantedText = "Installed & Linked",
                        notGrantedText = if (status.healthConnectStatus == HealthConnectStatus.UPDATE_REQUIRED) "Update Required" else "Not Setup (Optional)",
                        onConfigure = { onRequestPermissionAction(PermissionEducationManager.PERM_HEALTH_CONNECT) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    GuidePermissionItem(
                        detail = PermissionEducationManager.getDetail(PermissionEducationManager.PERM_BATTERY_OPTIMIZATION),
                        isGranted = status.isBatteryUnrestricted,
                        grantedText = "Unrestricted",
                        notGrantedText = "Optimized by OS",
                        onConfigure = { onRequestPermissionAction(PermissionEducationManager.PERM_BATTERY_OPTIMIZATION) }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 3. ANDROID 13+ RESTRICTED SETTINGS GUIDE
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SoftAmber.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, Color(0xFFB45309).copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFB45309),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Android 13+ Restricted Settings Guide",
                                    style = Typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF92400E)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "On Android 13, 14, and 15, sideloaded applications downloaded outside Google Play have Accessibility greyed out by default (\"Restricted setting\").\n\nTo safely enable Accessibility for 0ms app blocking:\n\n1. Open Android Settings > Apps > tabzero.\n2. Tap the three dots (⋮) in the top-right corner.\n3. Tap \"Allow restricted settings\" and verify with your fingerprint or PIN.\n4. Return to Settings > Accessibility and turn on tabzero App Blocker.\n\nNote: If you prefer not to enable Accessibility, tabzero still provides full app blocking via Usage Access every 300ms.",
                                style = Typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = Color(0xFF78350F)
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFB45309).copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Open tabzero App Info Settings",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun PrivacyTag(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF2E7D32).copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.25f))
    ) {
        Text(
            text = text,
            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
            color = Color(0xFF1B5E20),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun GuidePermissionItem(
    detail: PermissionDetail,
    isGranted: Boolean,
    grantedText: String,
    notGrantedText: String,
    onConfigure: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = OfflineSurfaceVariant),
        border = BorderStroke(1.dp, if (isGranted) Color(0xFF2E7D32).copy(alpha = 0.25f) else OfflineBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isGranted) SoftGreen else SoftAmber),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = detail.icon,
                            contentDescription = null,
                            tint = if (isGranted) Color(0xFF2E7D32) else Color(0xFFB45309),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = detail.title,
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                            color = OfflineTextPrimary
                        )
                        Text(
                            text = detail.shortName,
                            style = Typography.labelSmall.copy(fontSize = 11.sp),
                            color = OfflineTextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isGranted) SoftGreen else SoftAmber,
                    border = BorderStroke(1.dp, if (isGranted) Color(0xFF2E7D32).copy(alpha = 0.3f) else Color(0xFFB45309).copy(alpha = 0.3f))
                ) {
                    Text(
                        text = if (isGranted) grantedText else notGrantedText,
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = if (isGranted) Color(0xFF2E7D32) else Color(0xFFB45309),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = OfflineBorder.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Why needed: ${detail.whyNeeded}",
                    style = Typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                    color = OfflineTextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enables: ${detail.functionalityEnabled}",
                    style = Typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                    color = OfflineTextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Privacy: ${detail.privacyNote}",
                    style = Typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp, color = Color(0xFF2E7D32)),
                    fontWeight = FontWeight.Medium
                )

                if (!isGranted) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onConfigure,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OfflineDarkButton),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Configure / Enable",
                            color = OfflineOnDarkButton,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
