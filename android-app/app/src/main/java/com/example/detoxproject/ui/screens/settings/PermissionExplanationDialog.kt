package com.example.detoxproject.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.detoxproject.ui.theme.*

@Composable
fun PermissionExplanationDialog(
    detail: PermissionDetail,
    onContinue: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(26.dp),
            color = OfflineSurface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, OfflineBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header with Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(OfflineAccentPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = detail.icon,
                            contentDescription = null,
                            tint = OfflineAccentPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = detail.title,
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                            color = OfflineTextPrimary
                        )
                        Text(
                            text = detail.shortName,
                            style = Typography.labelSmall,
                            color = OfflineTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Requirement badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = OfflineSurfaceVariant,
                    border = BorderStroke(1.dp, OfflineBorder.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = detail.requirementType,
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                        color = OfflineAccentPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Why needed
                Text(
                    text = "Why tabzero needs this:",
                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                    color = OfflineTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = detail.whyNeeded,
                    style = Typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
                    color = OfflineTextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Functionality enabled
                Text(
                    text = "What it enables:",
                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                    color = OfflineTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = detail.functionalityEnabled,
                    style = Typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
                    color = OfflineTextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Local Privacy Note Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SoftGreen.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = detail.privacyNote,
                            style = Typography.bodySmall.copy(
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = Color(0xFF1B5E20)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, OfflineBorder)
                    ) {
                        Text(
                            text = "Not now",
                            color = OfflineTextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onContinue,
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OfflineDarkButton)
                    ) {
                        Text(
                            text = "Continue to Settings",
                            color = OfflineOnDarkButton,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
