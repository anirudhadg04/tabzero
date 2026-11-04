package com.example.detoxproject.ui.screens.intervention

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.detoxproject.MainActivity
import com.example.detoxproject.R
import com.example.detoxproject.data.integrations.enforcement.DetoxAccessibilityService
import com.example.detoxproject.data.integrations.enforcement.DetoxEnforcementService
import com.example.detoxproject.data.integrations.notifications.ActiveDetoxNotificationManager
import com.example.detoxproject.data.integrations.notifications.SessionAlarmManager
import com.example.detoxproject.data.local.OfflineDatabase
import com.example.detoxproject.ui.components.DynamicWindCanvas
import com.example.detoxproject.ui.theme.OfflineTheme
import com.example.detoxproject.ui.theme.ThemeManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ZenInterventionActivity : ComponentActivity() {

    companion object {
        var isActivityActive: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.init(this)

        // Prevent pressing Back to bypass intervention and return into the restricted app
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val mainIntent = Intent(this@ZenInterventionActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(mainIntent)
                finish()
            }
        })

        updateFromIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        isActivityActive = true
        val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
        notificationManager?.cancel(DetoxEnforcementService.INTERVENTION_NOTIFICATION_ID)
    }

    override fun onPause() {
        super.onPause()
        isActivityActive = false
    }

    override fun onDestroy() {
        super.onDestroy()
        isActivityActive = false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateFromIntent(intent)
    }

    private fun updateFromIntent(intent: Intent?) {
        val targetMinutes = intent?.getIntExtra("EXTRA_TARGET_MINUTES", 25) ?: 25
        val startTimeMs = intent?.getLongExtra("EXTRA_START_TIME", System.currentTimeMillis()) ?: System.currentTimeMillis()
        val sessionTitle = intent?.getStringExtra("EXTRA_SESSION_TITLE") ?: "Focus Session"
        val restrictedApp = intent?.getStringExtra("EXTRA_RESTRICTED_APP") ?: "Restricted App"

        setContent {
            OfflineTheme {
                ZenInterventionScreen(
                    sessionTitle = sessionTitle,
                    restrictedApp = restrictedApp,
                    targetMinutes = targetMinutes,
                    startTimeMs = startTimeMs,
                    onReturnToZen = {
                        val mainIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(mainIntent)
                        finish()
                    },
                    onAdmitDefeat = {
                        // 1. Stop background enforcement immediately
                        DetoxEnforcementService.stop(this)
                        DetoxAccessibilityService.stopDetox()

                        // 2. Stop notification and dismiss alarm
                        ActiveDetoxNotificationManager.cancelNotification(this)
                        SessionAlarmManager.dismissAlarm(this)
                        SessionAlarmManager.resetToIdle()

                        // 3. Cancel active session in Room Database immediately (using non-cancelling scope)
                        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val db = OfflineDatabase.getInstance(applicationContext)
                                val active = db.detoxDao().getActiveSessionDirect(1L)
                                if (active != null) {
                                    db.detoxDao().updateSession(
                                        active.copy(
                                            status = "CANCELLED",
                                            endTime = System.currentTimeMillis()
                                        )
                                    )
                                }
                            } catch (_: Exception) {}
                        }

                        // 4. Return to MainActivity with EXTRA_GIVE_UP to stop session progress UI
                        val mainIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            putExtra("EXTRA_GIVE_UP", true)
                        }
                        startActivity(mainIntent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun ZenInterventionScreen(
    sessionTitle: String,
    restrictedApp: String,
    targetMinutes: Int,
    startTimeMs: Long,
    onReturnToZen: () -> Unit,
    onAdmitDefeat: () -> Unit
) {
    val isNight = ThemeManager.isDark()
    var currentTimeMs by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTimeMs = System.currentTimeMillis()
        }
    }

    val elapsedMs = (currentTimeMs - startTimeMs).coerceAtLeast(0L)
    val totalTargetMs = targetMinutes * 60_000L
    val remainingMs = (totalTargetMs - elapsedMs).coerceAtLeast(0L)

    val remainingMins = (remainingMs / 1000L) / 60L
    val remainingSecs = (remainingMs / 1000L) % 60L
    val timerString = "%02d:%02d".format(remainingMins, remainingSecs)

    var showDefeatDialog by remember { mutableStateOf(false) }

    if (showDefeatDialog) {
        AlertDialog(
            onDismissRequest = { showDefeatDialog = false },
            title = { Text("Admit Defeat?", fontWeight = FontWeight.Bold) },
            text = { Text("Ending your digital detox early will pause your focus streak. Are you sure you want to unlock screens?") },
            confirmButton = {
                TextButton(onClick = {
                    showDefeatDialog = false
                    onAdmitDefeat()
                }) {
                    Text("Unlock & Give Up", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDefeatDialog = false }) {
                    Text("Stay in Zen")
                }
            }
        )
    }

    val bgDrawable = if (isNight) R.drawable.zen_night else R.drawable.zen_day
    val textColor = if (isNight) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val subTextColor = if (isNight) Color(0xFFCBD5E1) else Color(0xFF334155)
    val cardBg = if (isNight) Color(0xFF161B22).copy(alpha = 0.82f) else Color(0xFFFFFFFF).copy(alpha = 0.90f)
    val cardBorder = if (isNight) Color(0xFF30363D).copy(alpha = 0.7f) else Color(0xFFE2E8F0)

    Box(modifier = Modifier.fillMaxSize()) {
        // High-res Background Image (Day or Night depending on theme)
        Image(
            painter = painterResource(id = bgDrawable),
            contentDescription = "Zen Landscape",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Subtle scrim to ensure typography is crisp while artwork remains clearly visible
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isNight) {
                        Color(0xFF090D16).copy(alpha = 0.40f)
                    } else {
                        Color(0xFFFFFFFF).copy(alpha = 0.22f)
                    }
                )
        )

        // Dynamic Wind and Drifting Leaf Animation
        DynamicWindCanvas(
            modifier = Modifier.fillMaxSize(),
            isNight = isNight
        )

        // Gradient scrim to ensure readable typography
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isNight) {
                            listOf(
                                Color(0xFF090D16).copy(alpha = 0.60f),
                                Color.Transparent,
                                Color(0xFF090D16).copy(alpha = 0.75f)
                            )
                        } else {
                            listOf(
                                Color(0xFFFFFFFF).copy(alpha = 0.50f),
                                Color.Transparent,
                                Color(0xFFF8FAFC).copy(alpha = 0.70f)
                            )
                        }
                    )
                )
        )

        // Zen Content Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Pill + Breathing Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                // Top Pill Tag
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isNight) Color(0xFF1E293B).copy(alpha = 0.8f) else Color(0xFFFFFFFF).copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Spa,
                            contentDescription = null,
                            tint = if (isNight) Color(0xFF67E8F9) else Color(0xFF0D9488),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "A Moment of Stillness • Digital Detox Active",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Take a deep breath.",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-0.5).sp,
                    color = textColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "You chose to take a step back from screens.\nLife is happening out here — gentle, unhurried, and real.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = subTextColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Middle Section: Session Countdown Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(24.dp)),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SESSION REMAINING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                        color = subTextColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = timerString,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = (-1).sp,
                        color = textColor
                    )
                }
            }

            // Bottom Section: Recommendation Card + Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Offline Habit Recommendation Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RECOMMENDED WHILE YOU'RE OFFLINE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = if (isNight) Color(0xFFA5B4FC) else Color(0xFF6366F1)
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isNight) Color(0xFF064E3B) else Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    text = "• MOVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isNight) Color(0xFF6EE7B7) else Color(0xFF166534),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Outdoor Mindful Walk",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                        Text(
                            text = "Capture nature and details of the day with your own eyes instead of a screen.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = subTextColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons: Return to Zen & Admit Defeat
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onReturnToZen,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isNight) Color(0xFFF1F5F9) else Color(0xFF1E293B),
                            contentColor = if (isNight) Color(0xFF0F172A) else Color(0xFFFFFFFF)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Return to Zen",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    OutlinedButton(
                        onClick = { showDefeatDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        border = BorderStroke(1.dp, cardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = cardBg,
                            contentColor = textColor
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = subTextColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Admit Defeat",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "tabzero. — Life happens beyond the screen.",
                    fontSize = 11.sp,
                    color = subTextColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}
