package com.example.detoxproject.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.detoxproject.R
import com.example.detoxproject.ui.theme.*

private enum class ForgotPasswordStep {
    ENTER_EMAIL,
    ANSWER_QUESTION,
    NEW_PASSWORD,
    SUCCESS
}

@Composable
fun AuthScreen(
    onLogin: (email: String, pass: String) -> Unit,
    onSignUp: (name: String, email: String, pass: String, confirmPass: String) -> Unit,
    onResetPassword: (email: String, newPass: String, onComplete: (Boolean, String) -> Unit) -> Unit = { _, _, _ -> },
    onGetSecurityQuestion: (email: String, onResult: (Boolean, String, String, String?) -> Unit) -> Unit = { _, _ -> },
    onVerifySecurityAnswer: (email: String, questionKey: String, answer: String, onResult: (Boolean, String?) -> Unit) -> Unit = { _, _, _, _ -> }
) {
    val isDark = ThemeManager.isDark()

    var isSignUp by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Terms & Conditions state for Sign Up
    var termsAccepted by remember { mutableStateOf(false) }
    var hasScrolledTermsToEnd by remember { mutableStateOf(false) }
    val termsScrollState = rememberScrollState()

    LaunchedEffect(termsScrollState.value, termsScrollState.maxValue, termsScrollState.canScrollForward) {
        if (termsScrollState.maxValue > 0 && (!termsScrollState.canScrollForward || termsScrollState.value >= (termsScrollState.maxValue - 20))) {
            hasScrolledTermsToEnd = true
        }
    }

    // Redesigned Offline Security-Question Password Recovery
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var recoveryStep by remember { mutableStateOf(ForgotPasswordStep.ENTER_EMAIL) }
    var recoveryEmail by remember { mutableStateOf("") }
    var recoverySecurityKey by remember { mutableStateOf("DAILY_STEPS") }
    var recoveryQuestionText by remember { mutableStateOf("What is your daily step goal?") }
    var recoveryAnswer by remember { mutableStateOf("") }
    var recoveryNewPass by remember { mutableStateOf("") }
    var recoveryConfirmPass by remember { mutableStateOf("") }
    var recoveryPassVisible by remember { mutableStateOf(false) }
    var recoveryError by remember { mutableStateOf<String?>(null) }
    var recoveryLoading by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Unified high-contrast text field styling for dark and light modes
    val authTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = if (isDark) Color(0xFF161B22) else Color(0xFFFFFFFF),
        unfocusedContainerColor = if (isDark) Color(0xFF161B22) else Color(0xFFF8FAFC),
        disabledContainerColor = if (isDark) Color(0xFF12161D) else Color(0xFFF1F5F9),
        focusedTextColor = if (isDark) Color(0xFFF0F6FC) else Color(0xFF111827),
        unfocusedTextColor = if (isDark) Color(0xFFF0F6FC) else Color(0xFF111827),
        focusedLabelColor = if (isDark) OfflineAccentCyan else Color(0xFF111827),
        unfocusedLabelColor = if (isDark) Color(0xFF8B949E) else Color(0xFF6B7280),
        focusedBorderColor = if (isDark) OfflineAccentCyan else Color(0xFF111827),
        unfocusedBorderColor = if (isDark) Color(0xFF30363D) else Color(0xFFE2E8F0),
        cursorColor = if (isDark) OfflineAccentCyan else Color(0xFF111827),
        focusedTrailingIconColor = if (isDark) Color(0xFFF0F6FC) else Color(0xFF111827),
        unfocusedTrailingIconColor = if (isDark) Color(0xFF8B949E) else Color(0xFF6B7280),
        focusedPlaceholderColor = if (isDark) Color(0xFF6E7681) else Color(0xFF9CA3AF),
        unfocusedPlaceholderColor = if (isDark) Color(0xFF6E7681) else Color(0xFF9CA3AF),
        errorBorderColor = MaterialTheme.colorScheme.error,
        errorTextColor = if (isDark) Color(0xFFF0F6FC) else Color(0xFF111827)
    )

    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showForgotPasswordDialog = false
                recoveryStep = ForgotPasswordStep.ENTER_EMAIL
                recoveryError = null
            },
            containerColor = if (isDark) Color(0xFF161B22) else Color.White,
            titleContentColor = if (isDark) Color(0xFFF0F6FC) else Color(0xFF111827),
            textContentColor = if (isDark) Color(0xFFC9D1D9) else Color(0xFF4B5563),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (recoveryStep == ForgotPasswordStep.SUCCESS) Icons.Default.CheckCircle else Icons.Default.LockReset,
                        contentDescription = null,
                        tint = if (recoveryStep == ForgotPasswordStep.SUCCESS) Color(0xFF16A34A) else (if (isDark) OfflineAccentCyan else OfflineAccentPrimary),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (recoveryStep) {
                            ForgotPasswordStep.ENTER_EMAIL -> "Reset Password"
                            ForgotPasswordStep.ANSWER_QUESTION -> "Security Verification"
                            ForgotPasswordStep.NEW_PASSWORD -> "Set New Password"
                            ForgotPasswordStep.SUCCESS -> "Password Updated"
                        },
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isDark) Color(0xFFF0F6FC) else Color(0xFF111827)
                    )
                }
            },
            text = {
                Column {
                    when (recoveryStep) {
                        ForgotPasswordStep.ENTER_EMAIL -> {
                            Text(
                                text = "Enter your registered email to begin account verification.",
                                style = Typography.bodySmall,
                                color = if (isDark) Color(0xFF8B949E) else Color(0xFF6B7280)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedTextField(
                                value = recoveryEmail,
                                onValueChange = {
                                    recoveryEmail = it
                                    recoveryError = null
                                },
                                label = { Text("Account Email") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = authTextFieldColors
                            )
                        }

                        ForgotPasswordStep.ANSWER_QUESTION -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0xFF21262D) else SoftBlueLight.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF30363D) else Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = null,
                                            tint = if (isDark) OfflineAccentCyan else OfflineAccentPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "SECURITY QUESTION",
                                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isDark) OfflineAccentCyan else OfflineAccentPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = recoveryQuestionText,
                                        style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isDark) Color(0xFFF0F6FC) else Color(0xFF111827)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(
                                onClick = {
                                    recoveryError = null
                                    if (recoverySecurityKey == "DAILY_STEPS") {
                                        recoverySecurityKey = "DAILY_OFFLINE"
                                        recoveryQuestionText = "What is your daily offline focus goal (in minutes)?"
                                    } else {
                                        recoverySecurityKey = "DAILY_STEPS"
                                        recoveryQuestionText = "What is your daily step goal?"
                                    }
                                }
                            ) {
                                Text(
                                    text = if (recoverySecurityKey == "DAILY_STEPS") "Switch to Daily Offline Goal question" else "Switch to Daily Step Goal question",
                                    fontSize = 11.sp,
                                    color = if (isDark) OfflineAccentCyan else OfflineAccentPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = recoveryAnswer,
                                onValueChange = {
                                    recoveryAnswer = it
                                    recoveryError = null
                                },
                                label = { Text("Your Answer (number)") },
                                placeholder = { Text(if (recoverySecurityKey == "DAILY_STEPS") "e.g. 8000" else "e.g. 120") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = authTextFieldColors
                            )
                        }

                        ForgotPasswordStep.NEW_PASSWORD -> {
                            Text(
                                text = "Security answer verified! Please choose a new password with at least 6 characters.",
                                style = Typography.bodySmall,
                                color = if (isDark) Color(0xFF8B949E) else Color(0xFF6B7280)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedTextField(
                                value = recoveryNewPass,
                                onValueChange = {
                                    recoveryNewPass = it
                                    recoveryError = null
                                },
                                label = { Text("New Password") },
                                singleLine = true,
                                visualTransformation = if (recoveryPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    val icon = if (recoveryPassVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                                    IconButton(onClick = { recoveryPassVisible = !recoveryPassVisible }) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = "Toggle visibility",
                                            tint = if (isDark) Color(0xFFC9D1D9) else Color(0xFF6B7280)
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = authTextFieldColors
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = recoveryConfirmPass,
                                onValueChange = {
                                    recoveryConfirmPass = it
                                    recoveryError = null
                                },
                                label = { Text("Confirm New Password") },
                                singleLine = true,
                                visualTransformation = if (recoveryPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = authTextFieldColors
                            )
                        }

                        ForgotPasswordStep.SUCCESS -> {
                            Text(
                                text = "Your password has been changed successfully. You can now log into your tabzero. account.",
                                style = Typography.bodyMedium,
                                color = if (isDark) Color(0xFFF0F6FC) else Color(0xFF111827)
                            )
                        }
                    }

                    if (recoveryError != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = recoveryError!!,
                            style = Typography.bodySmall,
                            color = if (isDark) Color(0xFFFF7B72) else MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                val buttonBg = if (isDark) Color(0xFFF0F6FC) else Color(0xFF18181B)
                val buttonContent = if (isDark) Color(0xFF0F172A) else Color.White
                when (recoveryStep) {
                    ForgotPasswordStep.ENTER_EMAIL -> {
                        Button(
                            onClick = {
                                if (recoveryEmail.isBlank() || !recoveryEmail.contains("@")) {
                                    recoveryError = "Please enter a valid email address"
                                    return@Button
                                }
                                recoveryLoading = true
                                onGetSecurityQuestion(recoveryEmail) { success, key, question, err ->
                                    recoveryLoading = false
                                    if (success) {
                                        recoverySecurityKey = key
                                        recoveryQuestionText = question
                                        recoveryStep = ForgotPasswordStep.ANSWER_QUESTION
                                    } else {
                                        recoveryError = err ?: "No registered account found with this email"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonBg,
                                contentColor = buttonContent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Next", color = buttonContent)
                        }
                    }

                    ForgotPasswordStep.ANSWER_QUESTION -> {
                        Button(
                            onClick = {
                                if (recoveryAnswer.isBlank()) {
                                    recoveryError = "Please enter your answer"
                                    return@Button
                                }
                                recoveryLoading = true
                                onVerifySecurityAnswer(recoveryEmail, recoverySecurityKey, recoveryAnswer) { success, err ->
                                    recoveryLoading = false
                                    if (success) {
                                        recoveryStep = ForgotPasswordStep.NEW_PASSWORD
                                    } else {
                                        recoveryError = err ?: "Incorrect answer. Does not match your goal setting."
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonBg,
                                contentColor = buttonContent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Verify Answer", color = buttonContent)
                        }
                    }

                    ForgotPasswordStep.NEW_PASSWORD -> {
                        Button(
                            onClick = {
                                if (recoveryNewPass.length < 6) {
                                    recoveryError = "Password must be at least 6 characters"
                                    return@Button
                                }
                                if (recoveryNewPass != recoveryConfirmPass) {
                                    recoveryError = "Passwords do not match"
                                    return@Button
                                }
                                recoveryLoading = true
                                onResetPassword(recoveryEmail, recoveryNewPass) { success, msg ->
                                    recoveryLoading = false
                                    if (success) {
                                        email = recoveryEmail
                                        password = recoveryNewPass
                                        recoveryStep = ForgotPasswordStep.SUCCESS
                                    } else {
                                        recoveryError = msg
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonBg,
                                contentColor = buttonContent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Change Password", color = buttonContent)
                        }
                    }

                    ForgotPasswordStep.SUCCESS -> {
                        Button(
                            onClick = {
                                showForgotPasswordDialog = false
                                recoveryStep = ForgotPasswordStep.ENTER_EMAIL
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonBg,
                                contentColor = buttonContent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Back to Sign In", color = buttonContent)
                        }
                    }
                }
            },
            dismissButton = {
                if (recoveryStep != ForgotPasswordStep.SUCCESS) {
                    TextButton(onClick = {
                        if (recoveryStep == ForgotPasswordStep.ANSWER_QUESTION) {
                            recoveryStep = ForgotPasswordStep.ENTER_EMAIL
                        } else if (recoveryStep == ForgotPasswordStep.NEW_PASSWORD) {
                            recoveryStep = ForgotPasswordStep.ANSWER_QUESTION
                        } else {
                            showForgotPasswordDialog = false
                        }
                        recoveryError = null
                    }) {
                        Text(
                            text = if (recoveryStep == ForgotPasswordStep.ENTER_EMAIL) "Cancel" else "Back",
                            color = if (isDark) Color(0xFF8B949E) else Color(0xFF6B7280)
                        )
                    }
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) OfflineBackgroundDark else OfflineBackgroundLight)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 28.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Official tabzero. Logo Emblem with subtle dark/light container border
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(
                    BorderStroke(1.5.dp, if (isDark) Color(0xFF30363D) else Color(0xFFE2E8F0)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.tabzero_logo),
                contentDescription = "tabzero. logo",
                modifier = Modifier.size(90.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "tabzero.",
            style = Typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                fontSize = 32.sp
            ),
            color = if (isDark) Color(0xFFF0F6FC) else Color(0xFF111827)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Life happens beyond the screen.",
            style = Typography.bodyMedium,
            color = if (isDark) Color(0xFF8B949E) else Color(0xFF6B7280)
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Tab Selector (Sign In vs Create Account) with high-contrast active pill in both modes
        val tabTrackBg = if (isDark) Color(0xFF161B22) else Color(0xFFF1F5F9)
        val tabTrackBorder = if (isDark) Color(0xFF30363D) else Color(0xFFE2E8F0)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(23.dp))
                .background(tabTrackBg)
                .border(BorderStroke(1.dp, tabTrackBorder), RoundedCornerShape(23.dp))
                .padding(3.dp)
        ) {
            val isSignInActive = !isSignUp
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSignInActive) {
                            if (isDark) Color(0xFFF0F6FC) else Color.White
                        } else {
                            Color.Transparent
                        }
                    )
                    .clickable {
                        isSignUp = false
                        errorMessage = null
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sign In",
                    style = Typography.titleSmall.copy(
                        fontWeight = if (isSignInActive) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSignInActive) {
                        if (isDark) Color(0xFF0F172A) else Color(0xFF111827)
                    } else {
                        if (isDark) Color(0xFF8B949E) else Color(0xFF6B7280)
                    }
                )
            }

            val isCreateActive = isSignUp
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isCreateActive) {
                            if (isDark) Color(0xFFF0F6FC) else Color.White
                        } else {
                            Color.Transparent
                        }
                    )
                    .clickable {
                        isSignUp = true
                        errorMessage = null
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Create Account",
                    style = Typography.titleSmall.copy(
                        fontWeight = if (isCreateActive) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isCreateActive) {
                        if (isDark) Color(0xFF0F172A) else Color(0xFF111827)
                    } else {
                        if (isDark) Color(0xFF8B949E) else Color(0xFF6B7280)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Sign Up Only: Full Name Field
        if (isSignUp) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = authTextFieldColors
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = authTextFieldColors
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Password Field with clear visibility icon contrast
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val icon = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Toggle password visibility",
                        tint = if (isDark) Color(0xFFC9D1D9) else Color(0xFF6B7280)
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = authTextFieldColors
        )

        if (!isSignUp) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        recoveryEmail = email
                        recoveryStep = ForgotPasswordStep.ENTER_EMAIL
                        recoveryError = null
                        showForgotPasswordDialog = true
                    }
                ) {
                    Text(
                        text = "Forgot password?",
                        style = Typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isDark) OfflineAccentCyan else Color(0xFF4B5563)
                    )
                }
            }
        }

        // Sign Up Only: Confirm Password Field & Terms
        if (isSignUp) {
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = authTextFieldColors
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Terms and Conditions Section Header
            Text(
                text = "Terms and Conditions",
                style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isDark) Color(0xFFF0F6FC) else Color(0xFF111827),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Scrollable Terms Container
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF161B22) else Color(0xFFF8FAFC),
                border = BorderStroke(
                    1.dp,
                    if (hasScrolledTermsToEnd) {
                        if (isDark) Color(0xFF30363D) else Color(0xFFE2E8F0)
                    } else {
                        if (isDark) OfflineAccentCyan.copy(alpha = 0.6f) else OfflineAccentPrimary
                    }
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(termsScrollState)
                        .padding(12.dp)
                ) {
                    Text(
                        text = """
                            Welcome to tabzero. By creating an account, you agree to our Terms and Conditions:

                            1. Digital Detox & Mindfulness
                            tabzero. provides focused digital detox sessions, habit tracking, and screen-time discouragement to help you reclaim your time. During active sessions, selected apps will be restricted according to your device permissions.

                            2. Offline Privacy & Local Data Security
                            Your wellness records, step activity, and focus sessions are stored locally on your device in an encrypted Room database. tabzero. does not sell, track, or share your personal health data with third parties.

                            3. User Accountability & Wellness Goals
                            You agree to use this application for personal wellness and productivity. You can pause or stop sessions at any time using your chosen commitment mode.

                            4. Device Permissions
                            To deliver enforcement, notifications, and step counting, tabzero. requests standard Android system permissions including Activity Recognition, Notifications, and Usage Access. You maintain control over these permissions via Settings.

                            5. Changes & Continuous Improvement
                            tabzero. reserves the right to update features, interface designs, and privacy practices to improve your digital wellness experience.
                        """.trimIndent(),
                        style = Typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                        color = if (isDark) Color(0xFFC9D1D9) else Color(0xFF4B5563)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scroll instruction hint if not scrolled to bottom yet
            if (!hasScrolledTermsToEnd) {
                Text(
                    text = "↓ Please scroll to the bottom of the terms to accept",
                    style = Typography.labelSmall.copy(fontSize = 11.sp),
                    color = if (isDark) OfflineAccentCyan else Color(0xFF6B7280),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Checkbox Row (only clickable after user scrolls to the bottom)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = hasScrolledTermsToEnd) {
                        termsAccepted = !termsAccepted
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = termsAccepted,
                    onCheckedChange = { if (hasScrolledTermsToEnd) termsAccepted = it },
                    enabled = hasScrolledTermsToEnd,
                    colors = CheckboxDefaults.colors(
                        checkedColor = if (isDark) OfflineAccentCyan else Color(0xFF18181B),
                        checkmarkColor = if (isDark) Color(0xFF0F172A) else Color.White,
                        uncheckedColor = if (hasScrolledTermsToEnd) {
                            if (isDark) Color(0xFF8B949E) else Color(0xFF6B7280)
                        } else {
                            if (isDark) Color(0xFF30363D) else Color(0xFFCBD5E1)
                        },
                        disabledCheckedColor = (if (isDark) OfflineAccentCyan else Color(0xFF18181B)).copy(alpha = 0.5f),
                        disabledUncheckedColor = if (isDark) Color(0xFF21262D) else Color(0xFFE2E8F0)
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "I have read and agree to the Terms and Conditions",
                    style = Typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = if (hasScrolledTermsToEnd) {
                        if (isDark) Color(0xFFF0F6FC) else Color(0xFF111827)
                    } else {
                        if (isDark) Color(0xFF6E7681) else Color(0xFF94A3B8)
                    }
                )
            }
        }

        // Error message banner
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                color = if (isDark) Color(0xFF381C20) else Color(0xFFFEE2E2),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0xFFF85149).copy(alpha = 0.6f) else Color(0xFFFCA5A5)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage!!,
                    style = Typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = if (isDark) Color(0xFFFF7B72) else Color(0xFFB91C1C),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Main Submit Pill Button with clear enabled/disabled dark/light styling
        val isActionEnabled = !isSignUp || (hasScrolledTermsToEnd && termsAccepted)
        Button(
            onClick = {
                errorMessage = null
                if (isSignUp) {
                    if (name.isBlank()) {
                        errorMessage = "Please enter your full name"
                        return@Button
                    }
                    if (!email.contains("@") || !email.contains(".")) {
                        errorMessage = "Please enter a valid email address"
                        return@Button
                    }
                    if (password.length < 6) {
                        errorMessage = "Password must be at least 6 characters"
                        return@Button
                    }
                    if (password != confirmPassword) {
                        errorMessage = "Passwords do not match"
                        return@Button
                    }
                    if (!hasScrolledTermsToEnd || !termsAccepted) {
                        errorMessage = "Please read and accept the Terms and Conditions to sign up"
                        return@Button
                    }
                    onSignUp(name, email, password, confirmPassword)
                } else {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Please enter both email and password"
                        return@Button
                    }
                    onLogin(email, password)
                }
            },
            enabled = isActionEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDark) Color(0xFFF0F6FC) else Color(0xFF18181B),
                contentColor = if (isDark) Color(0xFF0F172A) else Color.White,
                disabledContainerColor = if (isDark) Color(0xFF21262D) else Color(0xFFE2E8F0),
                disabledContentColor = if (isDark) Color(0xFF6E7681) else Color(0xFF94A3B8)
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = if (isSignUp) "Create Account" else "Sign In",
                style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
