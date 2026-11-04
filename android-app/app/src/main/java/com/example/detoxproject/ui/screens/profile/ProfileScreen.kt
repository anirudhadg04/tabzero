package com.example.detoxproject.ui.screens.profile

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.detoxproject.data.integrations.health.HealthActivityManager
import com.example.detoxproject.data.integrations.health.HealthConnectStatus
import com.example.detoxproject.data.integrations.health.PhysicalActivitySummary
import com.example.detoxproject.data.integrations.usage.DailyUsageSummary
import com.example.detoxproject.data.integrations.usage.UsageStatsHelper
import com.example.detoxproject.data.local.entity.UserEntity
import com.example.detoxproject.data.local.entity.UserGoalsEntity
import com.example.detoxproject.data.local.entity.WeatherCacheEntity
import com.example.detoxproject.ui.components.ImageCropDialog
import com.example.detoxproject.ui.components.UserAvatarView
import com.example.detoxproject.ui.theme.*
import java.io.File

@Composable
fun ProfileScreen(
    currentUser: UserEntity?,
    userGoals: UserGoalsEntity?,
    activitySummary: PhysicalActivitySummary,
    dailyUsage: DailyUsageSummary? = null,
    weather: WeatherCacheEntity? = null,
    onUpdateGoals: (steps: Int, offlineMinutes: Int, calories: Int, screenLimitMinutes: Int, userName: String) -> Unit,
    onUpdateAvatar: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var name by remember(userGoals, currentUser) { mutableStateOf(userGoals?.userName ?: currentUser?.name ?: "User") }
    var tempName by remember(name) { mutableStateOf(name) }
    var showEditNameDialog by remember { mutableStateOf(false) }

    var stepsText by remember(userGoals) { mutableStateOf((userGoals?.dailySteps ?: 8000).toString()) }
    var offlineText by remember(userGoals) { mutableStateOf((userGoals?.dailyOfflineMinutes ?: 120).toString()) }
    var caloriesText by remember(userGoals) { mutableStateOf((userGoals?.targetCalories ?: 400).toString()) }
    var screenLimitText by remember(userGoals) { mutableStateOf((userGoals?.maxScreenTimeMinutes ?: 240).toString()) }

    var selectedAvatar by remember(userGoals?.avatar) { mutableStateOf(userGoals?.avatar ?: "🦉") }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var cropImageUri by remember { mutableStateOf<Uri?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }

    val presetAvatars = listOf("🦉", "🌿", "🧘", "⚡", "🌸", "🏔️", "🌊", "🚀")

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            cropImageUri = uri
            showCropDialog = true
        }
    }

    var showLogoutDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val screenMinsTotal = (dailyUsage?.totalScreenTimeMs ?: 0L) / (1000 * 60)
    val screenHours = (screenMinsTotal / 60)
    val screenMins = (screenMinsTotal % 60)

    if (showAvatarPicker) {
        AlertDialog(
            onDismissRequest = { showAvatarPicker = false },
            title = {
                Text(
                    text = "Choose Profile Avatar",
                    style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = OfflineTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Pick an avatar emblem or choose your own photo from gallery.",
                        style = Typography.bodySmall,
                        color = OfflineTextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        presetAvatars.take(4).forEach { avatar ->
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(if (selectedAvatar == avatar) OfflineAccentPrimary else OfflineSurfaceVariant)
                                    .clickable {
                                        selectedAvatar = avatar
                                        onUpdateAvatar(avatar)
                                        showAvatarPicker = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = avatar, fontSize = 24.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        presetAvatars.drop(4).forEach { avatar ->
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(if (selectedAvatar == avatar) OfflineAccentPrimary else OfflineSurfaceVariant)
                                    .clickable {
                                        selectedAvatar = avatar
                                        onUpdateAvatar(avatar)
                                        showAvatarPicker = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = avatar, fontSize = 24.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Gallery photo picker button (+)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = OfflineSurfaceVariant.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, OfflineBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { photoPickerLauncher.launch("image/*") }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(OfflineDarkButton),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Upload from gallery",
                                    tint = OfflineOnDarkButton,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Upload from Gallery (+)",
                                    style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = OfflineTextPrimary
                                )
                                Text(
                                    text = "Choose your own custom profile picture",
                                    style = Typography.labelSmall,
                                    color = OfflineTextSecondary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAvatarPicker = false }) {
                    Text("Close", color = OfflineTextPrimary)
                }
            }
        )
    }

    if (showCropDialog && cropImageUri != null) {
        ImageCropDialog(
            imageUri = cropImageUri!!,
            onDismiss = {
                showCropDialog = false
                cropImageUri = null
            },
            onCropConfirmed = { croppedBitmap ->
                try {
                    val avatarsDir = File(context.filesDir, "avatars").apply { mkdirs() }
                    val uid = currentUser?.id ?: userGoals?.userId ?: 1L
                    val destFile = File(avatarsDir, "avatar_${uid}_${System.currentTimeMillis()}.jpg")
                    destFile.outputStream().use { out ->
                        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    }
                    val path = destFile.absolutePath
                    selectedAvatar = path
                    onUpdateAvatar(path)
                } catch (_: Exception) {}
                showCropDialog = false
                cropImageUri = null
                showAvatarPicker = false
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out from tabzero.") },
            text = { Text("Your offline progress and session records are preserved locally in Room SQLite. You can log back in at any time.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Log Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Text(
                    text = "Edit Profile Name",
                    style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = OfflineTextPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Your Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = tempName.trim().ifBlank { name }
                        name = trimmed
                        val s = stepsText.toIntOrNull() ?: 8000
                        val m = offlineText.toIntOrNull() ?: 120
                        val c = caloriesText.toIntOrNull() ?: 400
                        val l = screenLimitText.toIntOrNull() ?: 240
                        onUpdateGoals(s, m, c, l, trimmed)
                        showEditNameDialog = false
                    }
                ) {
                    Text("Save", color = OfflineAccentPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", color = OfflineTextSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OfflineBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "preferences & goals",
                    style = Typography.headlineLarge.copy(fontSize = 28.sp, fontWeight = FontWeight.SemiBold),
                    color = OfflineTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Personalize tabzero. goals and device permissions",
                    style = Typography.bodyMedium,
                    color = OfflineTextSecondary
                )
            }

            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(OfflineSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = OfflineTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // User Account Card with Profile Picture Editing
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clickable { showAvatarPicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                            UserAvatarView(
                                avatar = selectedAvatar,
                                size = 54.dp,
                                defaultName = name
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(OfflineDarkButton),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Avatar",
                                    tint = OfflineOnDarkButton,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    tempName = name
                                    showEditNameDialog = true
                                }
                            ) {
                                Text(
                                    text = name,
                                    style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = OfflineTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Name",
                                    tint = OfflineTextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = currentUser?.email ?: "",
                                style = Typography.bodySmall,
                                color = OfflineTextMuted
                            )
                        }
                    }

                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Log out",
                            tint = OfflineTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SoftGreen.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "Encrypted Local Storage (Room SQLite)",
                        style = Typography.labelSmall.copy(fontSize = 11.sp),
                        color = OfflineTextPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Daily Goals Card with Live Wellness Pulse Sync
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
                    Text(
                        text = "Daily Goals",
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = OfflineTextPrimary
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SoftBlue.copy(alpha = 0.7f),
                        modifier = Modifier.clickable {
                            // Sync current daily wellness pulse into goals
                            val liveSteps = if (activitySummary.stepsToday > 0) activitySummary.stepsToday else (userGoals?.dailySteps ?: 8000)
                            stepsText = liveSteps.toString()
                            offlineText = "150"
                            caloriesText = "450"
                            screenLimitText = "180"
                            focusManager.clearFocus()
                            onUpdateGoals(liveSteps, 150, 450, 180, name)
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = OfflineTextPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sync with Pulse",
                                style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = OfflineTextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Wellness Pulse Status banner in Goals
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = OfflineSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Live Steps", style = Typography.labelSmall, color = OfflineTextMuted)
                            Text(
                                text = "${activitySummary.stepsToday}",
                                style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = OfflineTextPrimary
                            )
                        }
                        Column {
                            Text("Screen Time", style = Typography.labelSmall, color = OfflineTextMuted)
                            Text(
                                text = if (dailyUsage?.hasPermission == true) {
                                    if (screenHours > 0) "${screenHours}h ${screenMins}m" else "${screenMins}m"
                                } else "Active",
                                style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = OfflineTextPrimary
                            )
                        }
                        Column {
                            Text("Weather", style = Typography.labelSmall, color = OfflineTextMuted)
                            Text(
                                text = "${weather?.temperature?.toInt() ?: 25}°C",
                                style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = OfflineTextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = stepsText,
                    onValueChange = { stepsText = it },
                    label = { Text("Daily Steps Goal") },
                    placeholder = { Text("8000") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DirectionsWalk,
                            contentDescription = null,
                            tint = OfflineAccentPrimary
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = offlineText,
                    onValueChange = { offlineText = it },
                    label = { Text("Daily Offline Minutes Goal (min)") },
                    placeholder = { Text("120") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = OfflineAccentPrimary
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = caloriesText,
                    onValueChange = { caloriesText = it },
                    label = { Text("Daily Walking Calorie Target (kcal) [steps only]") },
                    placeholder = { Text("400") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFF97316)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = screenLimitText,
                    onValueChange = { screenLimitText = it },
                    label = { Text("Daily Screen Time Limit (min)") },
                    placeholder = { Text("240") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = SoftBlue
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        val s = stepsText.toIntOrNull() ?: 8000
                        val m = offlineText.toIntOrNull() ?: 120
                        val c = caloriesText.toIntOrNull() ?: 400
                        val l = screenLimitText.toIntOrNull() ?: 240
                        onUpdateGoals(s, m, c, l, name)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OfflineDarkButton),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Goals", color = OfflineOnDarkButton)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Physical Activity & Health Connect Card
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
                    Text(
                        text = "Physical Activity Tracking",
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = OfflineTextPrimary
                    )

                    Icon(
                        imageVector = Icons.Default.DirectionsWalk,
                        contentDescription = null,
                        tint = OfflineAccentPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Source: ${activitySummary.sourceDescription}",
                    style = Typography.bodySmall,
                    color = OfflineTextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Today's Steps", style = Typography.labelSmall, color = OfflineTextMuted)
                        Text(
                            text = "${activitySummary.stepsToday}",
                            style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = OfflineTextPrimary
                        )
                    }

                    Column {
                        Text("Distance", style = Typography.labelSmall, color = OfflineTextMuted)
                        Text(
                            text = "%.2f km".format(activitySummary.estimatedDistanceKm),
                            style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = OfflineTextPrimary
                        )
                    }

                    Column {
                        Text("Health Connect", style = Typography.labelSmall, color = OfflineTextMuted)
                        Text(
                            text = when (activitySummary.healthConnectStatus) {
                                HealthConnectStatus.AVAILABLE_INSTALLED -> "Installed"
                                HealthConnectStatus.UPDATE_REQUIRED -> "Update Needed"
                                HealthConnectStatus.NOT_INSTALLED -> "Supported"
                                HealthConnectStatus.UNSUPPORTED_PLATFORM -> "Legacy"
                            },
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (activitySummary.healthConnectStatus == HealthConnectStatus.AVAILABLE_INSTALLED) Color(0xFF2E7D32) else OfflineTextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = {
                        val healthManager = HealthActivityManager(context)
                        context.startActivity(healthManager.createHealthConnectIntent())
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = OfflineTextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (activitySummary.healthConnectStatus == HealthConnectStatus.AVAILABLE_INSTALLED) "Open Health Connect Settings" else "Setup Health Connect",
                        color = OfflineTextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
