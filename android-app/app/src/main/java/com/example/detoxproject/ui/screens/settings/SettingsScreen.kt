package com.example.detoxproject.ui.screens.settings

import android.Manifest
import android.app.AlarmManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.detoxproject.data.integrations.enforcement.DetoxAccessibilityService
import com.example.detoxproject.data.integrations.enforcement.DetoxEnforcementService
import com.example.detoxproject.data.integrations.health.HealthActivityManager
import com.example.detoxproject.data.integrations.health.HealthConnectStatus
import com.example.detoxproject.data.integrations.health.PhysicalActivitySummary
import com.example.detoxproject.data.integrations.usage.UsageStatsHelper
import com.example.detoxproject.ui.components.*
import com.example.detoxproject.ui.theme.*

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    activitySummary: PhysicalActivitySummary,
    quickDetoxPackages: List<String>,
    onUpdateQuickDetoxPackages: (List<String>) -> Unit,
    quickDetoxDurationSeconds: Int = 1800,
    onUpdateQuickDetoxDuration: (Int) -> Unit = {},
    onDeleteAccount: () -> Unit = {}
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val scrollState = rememberScrollState()

    val currentTheme = ThemeManager.themeMode

    val lifecycleOwner = LocalLifecycleOwner.current

    var hoursInput by remember(quickDetoxDurationSeconds) {
        mutableStateOf((quickDetoxDurationSeconds / 3600).toString())
    }
    var minsInput by remember(quickDetoxDurationSeconds) {
        mutableStateOf(((quickDetoxDurationSeconds % 3600) / 60).toString())
    }
    var secsInput by remember(quickDetoxDurationSeconds) {
        mutableStateOf((quickDetoxDurationSeconds % 60).toString())
    }

    fun applyDuration(hStr: String, mStr: String, sStr: String) {
        val h = hStr.toIntOrNull() ?: 0
        val m = mStr.toIntOrNull() ?: 0
        val s = sStr.toIntOrNull() ?: 0
        val total = ((h * 3600) + (m * 60) + s).coerceAtLeast(5)
        onUpdateQuickDetoxDuration(total)
    }

    // Live dynamic permission states connected to device APIs
    var hasActivityRecognition by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        )
    }

    var hasUsageAccess by remember {
        mutableStateOf(UsageStatsHelper.hasUsageStatsPermission(context))
    }

    var hasAccessibilityService by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context))
    }

    var hasOverlayPermission by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }

    var canScheduleExactAlarms by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                alarmManager?.canScheduleExactAlarms() ?: true
            } else true
        )
    }

    var isIgnoringBatteryOptimizations by remember {
        val pmService = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        mutableStateOf(pmService?.isIgnoringBatteryOptimizations(context.packageName) ?: false)
    }

    var liveHcStatus by remember {
        mutableStateOf(HealthActivityManager(context).getHealthConnectStatus())
    }

    // Dynamic refresher querying real device state whenever screen is active
    fun refreshPermissions() {
        hasActivityRecognition = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        } else true

        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

        hasUsageAccess = UsageStatsHelper.hasUsageStatsPermission(context)
        hasOverlayPermission = Settings.canDrawOverlays(context)
        hasAccessibilityService = isAccessibilityServiceEnabled(context)
        liveHcStatus = HealthActivityManager(context).getHealthConnectStatus()

        canScheduleExactAlarms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: true
        } else true

        val pmService = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        isIgnoringBatteryOptimizations = pmService?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    // Re-evaluate automatically as soon as the user returns from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Permission Launchers
    val activityRecognitionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasActivityRecognition = granted
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    // Installed apps and Quick Detox selection
    val installedApps = remember { getInstalledApps(context) }
    var showAppPickerDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showSelectedAppsGrid by remember { mutableStateOf(false) }

    val selectedApps = remember(quickDetoxPackages, installedApps) {
        quickDetoxPackages.mapNotNull { pkg ->
            installedApps.find { it.packageName == pkg }
                ?: AppChoice(packageName = pkg, name = pkg.substringAfterLast("."), iconEmoji = "📱")
        }
    }

    if (showAppPickerDialog) {
        InstalledAppPickerSheet(
            installedApps = installedApps,
            selectedPackages = quickDetoxPackages,
            onTogglePackage = { pkg ->
                val updated = quickDetoxPackages.toMutableList()
                if (updated.contains(pkg)) {
                    updated.remove(pkg)
                } else {
                    updated.add(pkg)
                }
                onUpdateQuickDetoxPackages(updated)
            },
            onDismiss = { showAppPickerDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OfflineBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(OfflineSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = OfflineTextPrimary
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "settings",
                    style = Typography.headlineLarge.copy(fontSize = 28.sp, fontWeight = FontWeight.SemiBold),
                    color = OfflineTextPrimary
                )
                Text(
                    text = "Preferences, permissions & quick detox setup",
                    style = Typography.labelSmall,
                    color = OfflineTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 1. Theme Preference Card
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
                    Column {
                        Text(
                            text = "Appearance & Theme",
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = OfflineTextPrimary
                        )
                        Text(
                            text = "Choose your preferred color theme",
                            style = Typography.bodySmall,
                            color = OfflineTextSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = OfflineAccentPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemeOptionCard(
                        title = "System",
                        subtitle = "Auto Day/Night",
                        icon = Icons.Default.BrightnessAuto,
                        isSelected = currentTheme == ThemeMode.SYSTEM,
                        modifier = Modifier.weight(1f),
                        onClick = { ThemeManager.setTheme(ThemeMode.SYSTEM, context) }
                    )
                    ThemeOptionCard(
                        title = "Light",
                        subtitle = "Daylight",
                        icon = Icons.Default.LightMode,
                        isSelected = currentTheme == ThemeMode.LIGHT,
                        modifier = Modifier.weight(1f),
                        onClick = { ThemeManager.setTheme(ThemeMode.LIGHT, context) }
                    )
                    ThemeOptionCard(
                        title = "Dark",
                        subtitle = "Nocturnal",
                        icon = Icons.Default.DarkMode,
                        isSelected = currentTheme == ThemeMode.DARK,
                        modifier = Modifier.weight(1f),
                        onClick = { ThemeManager.setTheme(ThemeMode.DARK, context) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Quick Detox Mode Apps Card
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
                    val currentH = quickDetoxDurationSeconds / 3600
                    val currentM = (quickDetoxDurationSeconds % 3600) / 60
                    val currentS = quickDetoxDurationSeconds % 60
                    val formattedDuration = when {
                        currentH > 0 && currentM > 0 -> "${currentH}h ${currentM}m"
                        currentH > 0 && currentS > 0 -> "${currentH}h ${currentS}s"
                        currentH > 0 -> "${currentH}h"
                        currentM > 0 && currentS > 0 -> "${currentM}m ${currentS}s"
                        currentM > 0 -> "${currentM}m"
                        else -> "${currentS}s"
                    }
                    Column {
                        Text(
                            text = "Quick Detox Mode",
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = OfflineTextPrimary
                        )
                        Text(
                            text = "Apps blocked during $formattedDuration fast detox on Home",
                            style = Typography.bodySmall,
                            color = OfflineTextSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color(0xFFF97316)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Quick Detox Duration Picker: hours[] minutes[] seconds[]
                Text(
                    text = "Quick Detox Duration",
                    style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = OfflineTextPrimary
                )
                Text(
                    text = "Specify timer length for 1-tap quick detox on Home",
                    style = Typography.bodySmall,
                    color = OfflineTextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimeInputBox(
                        label = "hours",
                        value = hoursInput,
                        onValueChange = { newVal ->
                            val clean = newVal.filter { it.isDigit() }.take(2)
                            hoursInput = clean
                            applyDuration(clean, minsInput, secsInput)
                        },
                        onIncrement = {
                            val next = ((hoursInput.toIntOrNull() ?: 0) + 1).coerceAtMost(24)
                            hoursInput = next.toString()
                            applyDuration(hoursInput, minsInput, secsInput)
                        },
                        onDecrement = {
                            val next = ((hoursInput.toIntOrNull() ?: 0) - 1).coerceAtLeast(0)
                            hoursInput = next.toString()
                            applyDuration(hoursInput, minsInput, secsInput)
                        }
                    )

                    Text(
                        text = ":",
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OfflineTextMuted,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    TimeInputBox(
                        label = "minutes",
                        value = minsInput,
                        onValueChange = { newVal ->
                            val clean = newVal.filter { it.isDigit() }.take(2)
                            minsInput = clean
                            applyDuration(hoursInput, clean, secsInput)
                        },
                        onIncrement = {
                            val next = ((minsInput.toIntOrNull() ?: 0) + 1).coerceAtMost(59)
                            minsInput = next.toString()
                            applyDuration(hoursInput, minsInput, secsInput)
                        },
                        onDecrement = {
                            val next = ((minsInput.toIntOrNull() ?: 0) - 1).coerceAtLeast(0)
                            minsInput = next.toString()
                            applyDuration(hoursInput, minsInput, secsInput)
                        }
                    )

                    Text(
                        text = ":",
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OfflineTextMuted,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    TimeInputBox(
                        label = "seconds",
                        value = secsInput,
                        onValueChange = { newVal ->
                            val clean = newVal.filter { it.isDigit() }.take(2)
                            secsInput = clean
                            applyDuration(hoursInput, minsInput, clean)
                        },
                        onIncrement = {
                            val next = ((secsInput.toIntOrNull() ?: 0) + 5).coerceAtMost(59)
                            secsInput = next.toString()
                            applyDuration(hoursInput, minsInput, secsInput)
                        },
                        onDecrement = {
                            val next = ((secsInput.toIntOrNull() ?: 0) - 5).coerceAtLeast(0)
                            secsInput = next.toString()
                            applyDuration(hoursInput, minsInput, secsInput)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(5 to "5m", 15 to "15m", 30 to "30m", 45 to "45m", 60 to "60m")
                    presets.forEach { (mins, label) ->
                        val isSelected = quickDetoxDurationSeconds == mins * 60
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onUpdateQuickDetoxDuration(mins * 60)
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) OfflineAccentPrimary.copy(alpha = 0.25f) else OfflineSurfaceVariant,
                            border = BorderStroke(1.dp, if (isSelected) OfflineAccentPrimary else OfflineBorder.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = label,
                                style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isSelected) OfflineAccentPrimary else OfflineTextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                HorizontalDivider(color = OfflineBorder.copy(alpha = 0.4f))

                Spacer(modifier = Modifier.height(16.dp))

                // Header summary showing count of restricted apps + [view] toggle button
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
                            text = "${quickDetoxPackages.size} apps selected to limit",
                            style = Typography.bodySmall,
                            color = OfflineTextSecondary
                        )
                    }

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

                // When user taps [view], expand and display Add Apps button + selected apps
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
                            text = "Add / Manage Apps",
                            style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = OfflineOnDarkButton
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val allItemsCount = selectedApps.size + 1
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
                                            val updated = quickDetoxPackages.toMutableList().apply { remove(app.packageName) }
                                            onUpdateQuickDetoxPackages(updated)
                                        }
                                    )
                                } else if (itemIndex == selectedApps.size) {
                                    AddAppTile(
                                        modifier = Modifier.weight(1f),
                                        onClick = { showAppPickerDialog = true }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (selectedApps.isEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = OfflineSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No apps restricted yet for Quick Detox. Tap '+ Add App' to configure apps you want blocked.",
                                style = Typography.bodySmall,
                                color = OfflineTextSecondary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. System Permissions & Physical Wellbeing Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = OfflineSurface),
            border = BorderStroke(1.dp, OfflineBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "System Permissions & Integrations",
                    style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = OfflineTextPrimary
                )
                Text(
                    text = "Ensure all required services are granted for uninterrupted focus and health sync.",
                    style = Typography.bodySmall,
                    color = OfflineTextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Permission Item: Physical Activity Tracking
                PermissionItemRow(
                    icon = Icons.Default.DirectionsWalk,
                    title = "Physical Activity Tracking",
                    description = "Required to count daily steps and movement with native hardware sensor.",
                    isGranted = hasActivityRecognition,
                    grantedLabel = "Granted",
                    actionLabel = "Grant Permission",
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            activityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        }
                    }
                )

                HorizontalDivider(color = OfflineBorder.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 12.dp))

                // Permission Item: Google Health Connect
                val isHcInstalled = liveHcStatus == HealthConnectStatus.AVAILABLE_INSTALLED
                PermissionItemRow(
                    icon = Icons.Default.Favorite,
                    title = "Google Health Connect",
                    description = "Authoritative health database syncing steps, workouts, and calories.",
                    isGranted = isHcInstalled,
                    grantedLabel = "Installed",
                    actionLabel = if (liveHcStatus == HealthConnectStatus.UPDATE_REQUIRED) "Update Required" else "Setup Health Connect",
                    onAction = {
                        val healthManager = HealthActivityManager(context)
                        context.startActivity(healthManager.createHealthConnectIntent())
                    }
                )

                HorizontalDivider(color = OfflineBorder.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 12.dp))

                // Permission Item: Usage Access
                PermissionItemRow(
                    icon = Icons.Default.QueryStats,
                    title = "Usage Access",
                    description = "Reads real-time device screen time and monitors distracting app opens.",
                    isGranted = hasUsageAccess,
                    grantedLabel = "Active",
                    actionLabel = "Open Settings",
                    onAction = {
                        context.startActivity(UsageStatsHelper.createUsageAccessIntent())
                    }
                )

                HorizontalDivider(color = OfflineBorder.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 12.dp))

                // Permission Item: Accessibility Blocker
                PermissionItemRow(
                    icon = Icons.Default.Block,
                    title = "App Blocker (Accessibility)",
                    description = "Provides instant 0ms blocking when restricted apps are launched during detox.",
                    isGranted = hasAccessibilityService,
                    grantedLabel = "Active",
                    actionLabel = "Enable Service",
                    onAction = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }
                )

                HorizontalDivider(color = OfflineBorder.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 12.dp))

                // Permission Item: Display Over Other Apps (Overlay)
                PermissionItemRow(
                    icon = Icons.Default.Layers,
                    title = "Display Over Other Apps",
                    description = "Allows tabzero to display the zen intervention screen directly over restricted apps.",
                    isGranted = hasOverlayPermission,
                    grantedLabel = "Allowed",
                    actionLabel = "Allow",
                    onAction = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        ).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }
                )

                HorizontalDivider(color = OfflineBorder.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 12.dp))

                // Permission Item: Notifications
                PermissionItemRow(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    description = "Required for live countdowns in the notification shade and session chime alerts.",
                    isGranted = hasNotificationPermission,
                    grantedLabel = "Allowed",
                    actionLabel = "Allow",
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                    }
                )

                HorizontalDivider(color = OfflineBorder.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 12.dp))

                // Permission Item: Exact Alarms
                PermissionItemRow(
                    icon = Icons.Default.Alarm,
                    title = "Exact Alarms & Reminders",
                    description = "Guarantees punctual detox completion alerts even when phone is sleeping.",
                    isGranted = canScheduleExactAlarms,
                    grantedLabel = "Enabled",
                    actionLabel = "Enable",
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:${context.packageName}")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                    }
                )

                HorizontalDivider(color = OfflineBorder.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 12.dp))

                // Permission Item: Battery Optimization
                PermissionItemRow(
                    icon = Icons.Default.BatteryChargingFull,
                    title = "Battery Unrestricted Mode",
                    description = if (isIgnoringBatteryOptimizations) {
                        "Unrestricted: Android will not suspend step tracking or background monitors."
                    } else {
                        "Restricted by OS (Optimized): Background step tracking and timers may be frozen by Android."
                    },
                    isGranted = isIgnoringBatteryOptimizations,
                    grantedLabel = "Unrestricted",
                    actionLabel = "Allow Unrestricted",
                    onAction = {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            try {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // App Details Settings Button
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = OfflineTextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Android App Settings", color = OfflineTextPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Policy-Compliant Enforcement Card (Moved into Settings & Updated)
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
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SoftGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Policy-Compliant Enforcement",
                                style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = OfflineTextPrimary
                            )
                            Text(
                                text = "Google Play & Android Developer Safety Compliant",
                                style = Typography.labelSmall.copy(fontSize = 11.sp),
                                color = OfflineTextSecondary
                            )
                        }
                    }

                    val isEnforcing = hasAccessibilityService || hasUsageAccess
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isEnforcing) SoftGreen else SoftAmber,
                        border = BorderStroke(1.dp, if (isEnforcing) Color(0xFF2E7D32).copy(alpha = 0.3f) else Color(0xFFB45309).copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = if (isEnforcing) "Active & Safe" else "Standby",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isEnforcing) Color(0xFF2E7D32) else Color(0xFFB45309),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "tabzero. strictly complies with Android and Google Play safety policies. It pairs non-intrusive Accessibility window monitoring, Android UsageStatsManager, and ongoing foreground notifications to gently guide digital calm — without requiring Device Owner provisioning, device administrator locks, or root access.",
                    style = Typography.bodySmall,
                    color = OfflineTextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = OfflineSurfaceVariant,
                        border = BorderStroke(1.dp, OfflineBorder.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "🛡️ Zero Root",
                            style = Typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                            color = OfflineTextPrimary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = OfflineSurfaceVariant,
                        border = BorderStroke(1.dp, OfflineBorder.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "🔒 Privacy First",
                            style = Typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                            color = OfflineTextPrimary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = OfflineSurfaceVariant,
                        border = BorderStroke(1.dp, OfflineBorder.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "✨ Policy Safe",
                            style = Typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                            color = OfflineTextPrimary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5. Delete Account Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = OfflineSurface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
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
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Delete Account",
                                style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Irreversible data wipe",
                                style = Typography.labelSmall.copy(fontSize = 11.sp),
                                color = OfflineTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Permanently delete your account, focus sessions, custom habits, and mindful notes. All local SQLite databases for this profile will be completely wiped from this device.",
                    style = Typography.bodySmall,
                    color = OfflineTextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = { showDeleteAccountDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Delete Account",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Delete Account Permanently?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will erase your account credentials, streaks, custom habits, and focus history from this device. This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountDialog = false
                        onDeleteAccount()
                    }
                ) {
                    Text("Delete Permanently", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ThemeOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) OfflineAccentPrimary.copy(alpha = 0.25f) else OfflineSurfaceVariant,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) OfflineAccentPrimary else OfflineBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) OfflineAccentPrimary else OfflineSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isSelected) Color(0xFF0F172A) else OfflineTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = OfflineTextPrimary
            )
            Text(
                text = subtitle,
                style = Typography.labelSmall.copy(fontSize = 10.sp),
                color = OfflineTextSecondary,
                maxLines = 1
            )
        }
    }
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    if (DetoxAccessibilityService.isServiceActive) return true

    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager
    val enabled = am?.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    if (enabled != null) {
        for (info in enabled) {
            val serviceId = info.id.lowercase()
            if (serviceId.contains(context.packageName.lowercase()) && serviceId.contains("accessibility")) {
                return true
            }
        }
    }

    try {
        val setting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        if (!setting.isNullOrBlank() && setting.contains(context.packageName)) {
            return true
        }
    } catch (_: Exception) {}

    return false
}

@Composable
private fun PermissionItemRow(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    grantedLabel: String = "Active",
    actionLabel: String = "Enable",
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
                    .background(if (isGranted) SoftGreen else SoftAmber),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) Color(0xFF2E7D32) else Color(0xFFB45309),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    text = title,
                    style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = OfflineTextPrimary
                )
                Text(
                    text = description,
                    style = Typography.bodySmall.copy(fontSize = 11.sp),
                    color = OfflineTextSecondary
                )
            }
        }

        if (isGranted) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SoftGreen,
                border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = grantedLabel,
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        } else {
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = OfflineDarkButton),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = actionLabel,
                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = OfflineOnDarkButton
                )
            }
        }
    }
}

@Composable
private fun TimeInputBox(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = label.uppercase(),
            style = Typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
            color = OfflineTextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = onDecrement,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease $label",
                    tint = OfflineTextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(OfflineSurfaceVariant)
                    .border(1.dp, OfflineBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = Typography.titleMedium.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = OfflineTextPrimary
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    cursorBrush = SolidColor(OfflineAccentPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            IconButton(
                onClick = onIncrement,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase $label",
                    tint = OfflineTextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
