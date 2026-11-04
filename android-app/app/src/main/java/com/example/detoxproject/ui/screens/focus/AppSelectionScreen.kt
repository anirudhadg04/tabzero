package com.example.detoxproject.ui.screens.focus

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import com.example.detoxproject.ui.components.*
import com.example.detoxproject.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreen(
    title: String,
    durationMinutes: Int,
    category: String,
    onBack: () -> Unit,
    onStartDetox: (selectedPackages: List<String>, allowAudio: Boolean) -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    var searchQuery by remember { mutableStateOf("") }
    var allowAudio by remember { mutableStateOf(true) }
    var showAppPickerDialog by remember { mutableStateOf(false) }
    var showSelectedAppsGrid by remember { mutableStateOf(false) }

    // Query non-redundant, launchable applications installed on the device
    val installedApps = remember { getInstalledApps(context) }

    // Initialize with actual installed distracting apps on this device
    val selectedPackages = remember(installedApps) {
        val popularDistractions = listOf(
            "com.instagram.android",
            "com.instapro.android",
            "com.whatsapp",
            "com.google.android.youtube",
            "com.snapchat.android",
            "com.twitter.android",
            "com.facebook.katana",
            "com.zhiliaoapp.musically"
        )
        val initial = installedApps.filter { popularDistractions.contains(it.packageName) }
            .map { it.packageName }
            .take(3)

        mutableStateListOf<String>().apply {
            if (initial.isNotEmpty()) {
                addAll(initial)
            }
        }
    }

    // The Page Card displays ONLY the currently selected apps instead of hardcoded predefined ones
    val selectedApps = remember(selectedPackages.toList(), installedApps, searchQuery) {
        selectedPackages.mapNotNull { pkg ->
            installedApps.find { it.packageName == pkg }
                ?: AppChoice(packageName = pkg, name = pkg.substringAfterLast("."), iconEmoji = "📱")
        }.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    // Installed app picker bottom sheet
    if (showAppPickerDialog) {
        InstalledAppPickerSheet(
            installedApps = installedApps,
            selectedPackages = selectedPackages.toList(),
            onTogglePackage = { pkg ->
                if (selectedPackages.contains(pkg)) {
                    selectedPackages.remove(pkg)
                } else {
                    selectedPackages.add(pkg)
                }
            },
            onDismiss = { showAppPickerDialog = false }
        )
    }

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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = OfflineTextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Screen title
        Text(
            text = "select apps to limit",
            style = Typography.headlineLarge.copy(fontSize = 32.sp, fontWeight = FontWeight.SemiBold),
            color = OfflineTextPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "discourage apps from distracting you while you're focusing",
            style = Typography.bodyMedium,
            color = OfflineTextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Interactive Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search selected apps...", color = OfflineTextMuted) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
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
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = OfflineTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = OfflineTextSecondary)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Summary row showing how many apps selected + [view] button
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = OfflineSurface),
            border = BorderStroke(1.dp, OfflineBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Restricted Apps",
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = OfflineTextPrimary
                        )
                        Text(
                            text = "${selectedPackages.size} apps selected to limit",
                            style = Typography.bodySmall,
                            color = OfflineTextSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { showSelectedAppsGrid = !showSelectedAppsGrid },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showSelectedAppsGrid) OfflineSurfaceVariant else OfflineDarkButton
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (showSelectedAppsGrid) "hide" else "view",
                                style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (showSelectedAppsGrid) OfflineTextPrimary else OfflineOnDarkButton
                            )
                        }
                    }
                }

                // When user clicks [view], reveals the current add apps button and the selected apps
                if (showSelectedAppsGrid) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = OfflineBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Prominent Add Apps button shown immediately when [view] is clicked
                    Button(
                        onClick = { showAppPickerDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OfflineDarkButton)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Apps",
                            tint = OfflineOnDarkButton,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "+ Add / Manage Apps",
                            style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = OfflineOnDarkButton
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val allItemsCount = selectedApps.size + 1 // selected apps + Add App tile
                    val totalRows = (allItemsCount + 3) / 4

                    for (rowIndex in 0 until totalRows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (colIndex in 0 until 4) {
                                val itemIndex = rowIndex * 4 + colIndex
                                if (itemIndex < selectedApps.size) {
                                    val app = selectedApps[itemIndex]
                                    AppIconTile(
                                        app = app,
                                        pm = pm,
                                        modifier = Modifier.weight(1f),
                                        onRemove = {
                                            selectedPackages.remove(app.packageName)
                                        }
                                    )
                                } else if (itemIndex == selectedApps.size) {
                                    // The single integrated "+" Tile to add from installed apps
                                    AddAppTile(
                                        modifier = Modifier.weight(1f),
                                        onClick = { showAppPickerDialog = true }
                                    )
                                } else {
                                    // Empty slot to keep 4-column alignment
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (selectedApps.isEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = OfflineSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No selected apps match '$searchQuery'"
                                else "No apps limited yet. Tap '+ Add App' to pick apps you want restricted.",
                                style = Typography.bodySmall,
                                color = OfflineTextSecondary,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Start button with dark mode & light mode high-contrast text
        MinimalPillButton(
            text = "start",
            onClick = {
                onStartDetox(selectedPackages.toList(), allowAudio)
            }
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}
