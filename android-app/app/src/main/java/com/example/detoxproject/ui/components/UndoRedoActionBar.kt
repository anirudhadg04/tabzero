package com.example.detoxproject.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.detoxproject.ui.theme.*

@Composable
fun UndoRedoActionBar(
    pastCount: Int,
    futureCount: Int,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onShowHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = OfflineSurfaceVariant.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo control
            Row(
                modifier = Modifier
                    .clickable(enabled = pastCount > 0) { onUndo() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (pastCount > 0) OfflineTextPrimary else OfflineTextMuted.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Undo ($pastCount)",
                    style = Typography.labelLarge.copy(fontSize = 12.sp),
                    color = if (pastCount > 0) OfflineTextPrimary else OfflineTextMuted.copy(alpha = 0.4f)
                )
            }

            // Clickable 5-Step History Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = OfflineSurface.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, OfflineBorder.copy(alpha = 0.4f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onShowHistory() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        tint = OfflineTextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "5-Step History",
                        style = Typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                        color = OfflineTextSecondary
                    )
                }
            }

            // Redo control
            Row(
                modifier = Modifier
                    .clickable(enabled = futureCount > 0) { onRedo() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Redo ($futureCount)",
                    style = Typography.labelLarge.copy(fontSize = 12.sp),
                    color = if (futureCount > 0) OfflineTextPrimary else OfflineTextMuted.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    tint = if (futureCount > 0) OfflineTextPrimary else OfflineTextMuted.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
