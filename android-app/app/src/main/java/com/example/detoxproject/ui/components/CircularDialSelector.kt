package com.example.detoxproject.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.detoxproject.ui.theme.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Unified radial duration selector supporting hours, minutes, and seconds.
 * Provides a clock-face radial dial with Canvas-rendered ticks, numerals (12, 3, 6, 9),
 * colored progress arc, tactile draggable handle, stepper buttons, and direct manual time entry.
 */
@Composable
fun CircularDialSelector(
    selectedMinutes: Int,
    onMinutesChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minMinutes: Int = 1,
    maxMinutes: Int = 720,
    step: Int = 5,
    selectedSeconds: Int = selectedMinutes * 60,
    onTotalSecondsChanged: ((Int) -> Unit)? = null
) {
    var showManualInputDialog by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val currentTotalSeconds = if (onTotalSecondsChanged != null) selectedSeconds else selectedMinutes * 60
    var lastHapticStep by remember { mutableIntStateOf(currentTotalSeconds / 60) }

    val primaryTextColor = OfflineTextPrimary
    val trackBackgroundColor = OfflineSurfaceVariant.copy(alpha = 0.5f)
    val dialArcColor = Color(0xFF7C3AED) // Reference-inspired vibrant purple

    val hours = currentTotalSeconds / 3600
    val mins = (currentTotalSeconds % 3600) / 60
    val secs = currentTotalSeconds % 60

    val maxTotalSeconds = maxMinutes * 60
    val minTotalSeconds = 5 // Allow 5 seconds minimum for quick testing

    fun updateDuration(newTotalSeconds: Int) {
        val clamped = newTotalSeconds.coerceIn(minTotalSeconds, maxTotalSeconds)
        if (onTotalSecondsChanged != null) {
            onTotalSecondsChanged(clamped)
        }
        val minsEquivalent = (clamped / 60).coerceAtLeast(if (clamped > 0) 1 else 0)
        onMinutesChanged(minsEquivalent)
    }

    val currentTotalSecondsState = rememberUpdatedState(currentTotalSeconds)
    val updateDurationState = rememberUpdatedState(::updateDuration)

    if (showManualInputDialog) {
        ManualDurationDialog(
            initialTotalSeconds = currentTotalSeconds,
            minSeconds = minTotalSeconds,
            maxSeconds = maxTotalSeconds,
            onDismiss = { showManualInputDialog = false },
            onConfirm = { newDurationSecs ->
                updateDuration(newDurationSecs)
                showManualInputDialog = false
            }
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .pointerInput(maxTotalSeconds) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val minRadius = 45.dp.toPx()

                        val downDx = down.position.x - centerX
                        val downDy = down.position.y - centerY
                        val downDist = sqrt(downDx * downDx + downDy * downDy)

                        // If user tapped directly inside center numbers, don't intercept (allows "tap to edit")
                        if (downDist < minRadius) return@awaitEachGesture

                        down.consume()

                        fun updateAngle(posX: Float, posY: Float) {
                            val dx = posX - centerX
                            val dy = posY - centerY
                            val dist = sqrt(dx * dx + dy * dy)
                            if (dist >= 30.dp.toPx()) {
                                val rad = atan2(dy.toDouble(), dx.toDouble())
                                var deg = Math.toDegrees(rad) + 90.0
                                if (deg < 0.0) deg += 360.0
                                val rawSecs = (deg / 360.0 * maxTotalSeconds).roundToInt()
                                val stepSecs = 15
                                val snapped = ((rawSecs + stepSecs / 2) / stepSecs * stepSecs).coerceIn(minTotalSeconds, maxTotalSeconds)
                                if (snapped != currentTotalSecondsState.value) {
                                    val currentStep = snapped / 60
                                    if (currentStep != lastHapticStep) {
                                        lastHapticStep = currentStep
                                        try {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } catch (_: Exception) {}
                                    }
                                    updateDurationState.value(snapped)
                                }
                            }
                        }

                        // Immediate update on touch / tap
                        updateAngle(down.position.x, down.position.y)

                        // Seamless continuous tracking while holding and turning around
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: event.changes.firstOrNull()
                                ?: break
                            if (!change.pressed) break
                            change.consume()
                            updateAngle(change.position.x, change.position.y)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Background Canvas: Circular track, clock ticks, 12/3/6/9 numerals, progress arc, handle
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val centerX = canvasWidth / 2f
                val centerY = canvasHeight / 2f

                val strokeWidth = 20.dp.toPx()
                val trackRadius = (size.minDimension / 2f) - (strokeWidth / 2f) - 14.dp.toPx()
                val innerTickRadius = trackRadius - (strokeWidth / 2f) - 6.dp.toPx()

                // 1. Clock face ticks: 12 major hour marks and minor 15-minute marks
                for (tick in 0 until 48) {
                    val tickAngleDeg = -90.0 + (tick * 7.5)
                    val isMajor = tick % 4 == 0 // Every hour
                    val isQuarter = tick % 12 == 0 // 12, 3, 6, 9 positions

                    val tickLen = if (isQuarter) 12.dp.toPx() else if (isMajor) 8.dp.toPx() else 4.dp.toPx()
                    val tickColor = if (isQuarter) {
                        primaryTextColor.copy(alpha = 0.8f)
                    } else if (isMajor) {
                        primaryTextColor.copy(alpha = 0.45f)
                    } else {
                        primaryTextColor.copy(alpha = 0.2f)
                    }
                    val stroke = if (isQuarter) 2.dp.toPx() else if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()

                    val rad = Math.toRadians(tickAngleDeg)
                    val outerX = centerX + innerTickRadius * cos(rad).toFloat()
                    val outerY = centerY + innerTickRadius * sin(rad).toFloat()
                    val innerX = centerX + (innerTickRadius - tickLen) * cos(rad).toFloat()
                    val innerY = centerY + (innerTickRadius - tickLen) * sin(rad).toFloat()

                    drawLine(
                        color = tickColor,
                        start = Offset(innerX, innerY),
                        end = Offset(outerX, outerY),
                        strokeWidth = stroke
                    )
                }

                // 2. Clock Numerals (12, 3, 6, 9)
                val textRadius = innerTickRadius - 20.dp.toPx()
                val textPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 13.sp.toPx()
                    color = primaryTextColor.toArgb()
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                drawContext.canvas.nativeCanvas.drawText("12", centerX, centerY - textRadius + (textPaint.textSize / 3f), textPaint)
                drawContext.canvas.nativeCanvas.drawText("3", centerX + textRadius, centerY + (textPaint.textSize / 3f), textPaint)
                drawContext.canvas.nativeCanvas.drawText("6", centerX, centerY + textRadius + (textPaint.textSize / 3f), textPaint)
                drawContext.canvas.nativeCanvas.drawText("9", centerX - textRadius, centerY + (textPaint.textSize / 3f), textPaint)

                // 3. Base circular track
                drawCircle(
                    color = trackBackgroundColor,
                    radius = trackRadius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = strokeWidth)
                )

                // 4. Colored progress arc
                val sweepAngle = ((currentTotalSeconds.toFloat() / maxTotalSeconds.toFloat()) * 360f).coerceIn(0f, 360f)
                if (sweepAngle > 0f) {
                    drawArc(
                        color = dialArcColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // 5. Draggable Selection Handle (at sweepAngle endpoint)
                val handleAngleRad = Math.toRadians((-90.0 + sweepAngle))
                val handleX = centerX + trackRadius * cos(handleAngleRad).toFloat()
                val handleY = centerY + trackRadius * sin(handleAngleRad).toFloat()

                // Outer handle thumb
                drawCircle(
                    color = Color.White,
                    radius = 15.dp.toPx(),
                    center = Offset(handleX, handleY)
                )
                drawCircle(
                    color = dialArcColor,
                    radius = 15.dp.toPx(),
                    center = Offset(handleX, handleY),
                    style = Stroke(width = 2.5.dp.toPx())
                )
                // Inner emblem
                drawCircle(
                    color = dialArcColor,
                    radius = 6.dp.toPx(),
                    center = Offset(handleX, handleY)
                )
            }

            // Central Duration Display with Hours, Minutes, and Seconds
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { showManualInputDialog = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "%02d".format(hours),
                        style = Typography.headlineLarge.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = primaryTextColor
                    )
                    Text(
                        text = ":",
                        style = Typography.headlineMedium.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = OfflineTextMuted
                        ),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                    Text(
                        text = "%02d".format(mins),
                        style = Typography.headlineLarge.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = primaryTextColor
                    )
                    Text(
                        text = ":",
                        style = Typography.headlineMedium.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = OfflineTextMuted
                        ),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                    Text(
                        text = "%02d".format(secs),
                        style = Typography.headlineLarge.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = primaryTextColor
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.width(160.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Text(
                        text = "HR",
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = OfflineTextSecondary
                    )
                    Text(
                        text = "MIN",
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = OfflineTextSecondary
                    )
                    Text(
                        text = "SEC",
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = OfflineTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = OfflineSurfaceVariant.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "tap to edit",
                        style = Typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                        color = OfflineAccentPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tactile Stepper Row: (-) | Duration Summary Chip | (+)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Stepper Minus Button (Decrements by 15s if < 1m, or 1m if < 5m, or 5m)
            val stepDecrement = if (currentTotalSeconds <= 60) 15 else if (currentTotalSeconds <= 300) 60 else 300
            Surface(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .clickable {
                        val next = (currentTotalSeconds - stepDecrement).coerceAtLeast(minTotalSeconds)
                        if (next != currentTotalSeconds) {
                            try {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } catch (e: Exception) {}
                            updateDuration(next)
                        }
                    },
                shape = CircleShape,
                color = OfflineSurfaceVariant.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, OfflineBorder)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease",
                        tint = primaryTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Duration Summary Chip
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showManualInputDialog = true },
                shape = RoundedCornerShape(16.dp),
                color = OfflineSurfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, OfflineBorder)
            ) {
                val chipText = buildString {
                    if (hours > 0) append("${hours}h ")
                    if (mins > 0) append("${mins}m ")
                    if (secs > 0 || (hours == 0 && mins == 0)) append("${secs}s ")
                    append("focus")
                }
                Text(
                    text = chipText,
                    style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = primaryTextColor,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Stepper Plus Button (Increments by 15s if < 1m, or 1m if < 5m, or 5m)
            val stepIncrement = if (currentTotalSeconds < 60) 15 else if (currentTotalSeconds < 300) 60 else 300
            Surface(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .clickable {
                        val next = (currentTotalSeconds + stepIncrement).coerceAtMost(maxTotalSeconds)
                        if (next != currentTotalSeconds) {
                            try {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } catch (e: Exception) {}
                            updateDuration(next)
                        }
                    },
                shape = CircleShape,
                color = OfflineSurfaceVariant.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, OfflineBorder)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase",
                        tint = primaryTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
