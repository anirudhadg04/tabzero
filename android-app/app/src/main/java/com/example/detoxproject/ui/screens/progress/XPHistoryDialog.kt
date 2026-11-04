package com.example.detoxproject.ui.screens.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.detoxproject.data.local.entity.XPTransactionEntity
import com.example.detoxproject.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun XPHistoryDialog(
    transactions: List<XPTransactionEntity>,
    totalXP: Int,
    onDismiss: () -> Unit
) {
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp)),
            color = OfflineSurface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, OfflineBorder.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = OfflineAccentPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Ledger",
                                    tint = OfflineAccentPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Zen XP Ledger",
                                style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = OfflineTextPrimary
                            )
                            Text(
                                text = "Append-only, fully auditable history",
                                style = Typography.bodySmall.copy(fontSize = 12.sp),
                                color = OfflineTextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = OfflineTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Summary Balance Pill
                Surface(
                    color = OfflineSurfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, OfflineBorder.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CURRENT BALANCE",
                                style = Typography.labelSmall.copy(letterSpacing = 1.sp, fontSize = 10.sp),
                                color = OfflineTextMuted
                            )
                            Text(
                                text = "$totalXP Zen XP",
                                style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = OfflineAccentPrimary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = OfflineAccentPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${transactions.size} records",
                                style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = OfflineAccentPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Transaction List
                if (transactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = OfflineTextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No transactions yet",
                                style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = OfflineTextSecondary
                            )
                            Text(
                                text = "Complete habits, detox sessions, or workouts to earn XP.",
                                style = Typography.bodySmall,
                                color = OfflineTextMuted
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(transactions, key = { it.id }) { tx ->
                            val isReversal = tx.transactionType == "REVERSAL"
                            val timeStr = try {
                                timeFormatter.format(Date(tx.timestamp))
                            } catch (_: Exception) {
                                ""
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isReversal) Color(0xFFEF4444).copy(alpha = 0.05f) else OfflineSurfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isReversal) Color(0xFFEF4444).copy(alpha = 0.25f) else OfflineBorder.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (isReversal) Color(0xFFEF4444).copy(alpha = 0.15f) else OfflineAccentPrimary.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = if (isReversal) "REVERSED" else tx.sourceEventType,
                                                    style = Typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                    color = if (isReversal) Color(0xFFDC2626) else OfflineAccentPrimary,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${tx.date}  $timeStr",
                                                style = Typography.bodySmall.copy(fontSize = 11.sp),
                                                color = OfflineTextMuted
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = tx.description,
                                            style = Typography.bodyMedium.copy(
                                                textDecoration = if (isReversal) TextDecoration.LineThrough else TextDecoration.None
                                            ),
                                            color = if (isReversal) OfflineTextMuted else OfflineTextPrimary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = if (isReversal) "-${tx.xpAmount} XP" else "+${tx.xpAmount} XP",
                                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isReversal) Color(0xFFDC2626) else Color(0xFF16A34A)
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
