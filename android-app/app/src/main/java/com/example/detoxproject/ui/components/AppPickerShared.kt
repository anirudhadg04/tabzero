package com.example.detoxproject.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.detoxproject.ui.theme.*

data class AppChoice(
    val packageName: String,
    val name: String,
    val iconEmoji: String = "📱"
)

/**
 * Queries non-redundant, launchable applications installed on the device.
 */
fun getInstalledApps(context: Context): List<AppChoice> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    val resolveInfos = pm.queryIntentActivities(intent, 0)
    return resolveInfos.mapNotNull { resolveInfo ->
        val pkg = resolveInfo.activityInfo.packageName
        if (pkg == context.packageName || pkg == "android") return@mapNotNull null
        val name = resolveInfo.loadLabel(pm)?.toString()?.trim() ?: ""
        if (name.isBlank() || pkg.startsWith("com.android.internal")) return@mapNotNull null

        AppChoice(
            packageName = pkg,
            name = name,
            iconEmoji = when {
                name.contains("Instagram", ignoreCase = true) -> "📷"
                name.contains("WhatsApp", ignoreCase = true) -> "💬"
                name.contains("YouTube", ignoreCase = true) -> "▶"
                name.contains("Chat", ignoreCase = true) || name.contains("GPT", ignoreCase = true) -> "🤖"
                name.contains("Music", ignoreCase = true) || name.contains("Spotify", ignoreCase = true) -> "🎵"
                name.contains("Game", ignoreCase = true) -> "🎮"
                else -> "📱"
            }
        )
    }.distinctBy { it.packageName }
        .sortedBy { it.name.lowercase() }
}

@Composable
fun AppIconTile(
    app: AppChoice,
    pm: android.content.pm.PackageManager,
    modifier: Modifier = Modifier,
    onRemove: () -> Unit
) {
    val appIconBitmap = remember(app.packageName) {
        try {
            val d = pm.getApplicationIcon(app.packageName)
            val b = Bitmap.createBitmap(
                d.intrinsicWidth.coerceAtLeast(48),
                d.intrinsicHeight.coerceAtLeast(48),
                Bitmap.Config.ARGB_8888
            )
            val c = Canvas(b)
            d.setBounds(0, 0, c.width, c.height)
            d.draw(c)
            b.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    Surface(
        modifier = modifier
            .aspectRatio(0.88f)
            .clickable { onRemove() },
        shape = RoundedCornerShape(18.dp),
        color = SoftBlue,
        border = BorderStroke(1.5.dp, OfflineAccentPrimary)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (appIconBitmap != null) {
                    Image(
                        bitmap = appIconBitmap,
                        contentDescription = app.name,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(7.dp))
                    )
                } else {
                    Text(
                        text = app.iconEmoji,
                        fontSize = 24.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = app.name,
                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = OfflineTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Close / deselect badge in corner
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(OfflineSurfaceVariant)
                    .clickable { onRemove() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = OfflineTextSecondary,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

@Composable
fun AddAppTile(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .aspectRatio(0.88f)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = OfflineSurfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.5.dp, OfflineAccentPrimary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(OfflineAccentPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Installed App",
                    tint = OfflineTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Add App",
                style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = OfflineTextPrimary,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstalledAppPickerSheet(
    installedApps: List<AppChoice>,
    selectedPackages: List<String>,
    onTogglePackage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    var dialogSearch by remember { mutableStateOf("") }
    val filtered = remember(installedApps, dialogSearch) {
        installedApps.filter {
            it.name.contains(dialogSearch, ignoreCase = true) || it.packageName.contains(dialogSearch, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = OfflineSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = OfflineBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Select From Installed Apps",
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = OfflineTextPrimary
                    )
                    Text(
                        text = "${selectedPackages.size} app${if (selectedPackages.size == 1) "" else "s"} selected to limit",
                        style = Typography.bodySmall,
                        color = OfflineTextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = OfflineTextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = dialogSearch,
                onValueChange = { dialogSearch = it },
                placeholder = { Text("Search installed apps...", color = OfflineTextMuted) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = OfflineSurfaceVariant,
                    unfocusedContainerColor = OfflineSurfaceVariant,
                    focusedBorderColor = OfflineAccentPrimary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = OfflineTextPrimary,
                    unfocusedTextColor = OfflineTextPrimary
                ),
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = OfflineTextSecondary)
                },
                trailingIcon = {
                    if (dialogSearch.isNotEmpty()) {
                        IconButton(onClick = { dialogSearch = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = OfflineTextSecondary)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp)
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    val isChosen = selectedPackages.contains(app.packageName)
                    val appIconBitmap = remember(app.packageName) {
                        try {
                            val d = pm.getApplicationIcon(app.packageName)
                            val b = Bitmap.createBitmap(
                                d.intrinsicWidth.coerceAtLeast(48),
                                d.intrinsicHeight.coerceAtLeast(48),
                                Bitmap.Config.ARGB_8888
                            )
                            val c = Canvas(b)
                            d.setBounds(0, 0, c.width, c.height)
                            d.draw(c)
                            b.asImageBitmap()
                        } catch (e: Exception) {
                            null
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onTogglePackage(app.packageName) }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (appIconBitmap != null) {
                                Image(
                                    bitmap = appIconBitmap,
                                    contentDescription = app.name,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                    .background(OfflineSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = app.iconEmoji, fontSize = 20.sp)
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = app.name,
                                    style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = OfflineTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = app.packageName,
                                    style = Typography.bodySmall.copy(fontSize = 11.sp),
                                    color = OfflineTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Checkbox(
                            checked = isChosen,
                            onCheckedChange = { onTogglePackage(app.packageName) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = OfflineAccentPrimary,
                                checkmarkColor = Color(0xFF0F172A)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            MinimalPillButton(
                text = "Done (${selectedPackages.size} selected)",
                onClick = onDismiss
            )
        }
    }
}
