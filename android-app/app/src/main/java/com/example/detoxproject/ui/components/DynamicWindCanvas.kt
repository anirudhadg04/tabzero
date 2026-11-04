package com.example.detoxproject.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.sin

data class WindParticle(
    val initialX: Float,
    val initialY: Float,
    val speed: Float,
    val amplitude: Float,
    val wavelength: Float,
    val size: Float,
    val isLeaf: Boolean
)

@Composable
fun DynamicWindCanvas(
    modifier: Modifier = Modifier,
    isNight: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wind_animation")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wind_progress"
    )

    // Generate fixed particle properties once
    val particles = remember {
        List(24) { i ->
            WindParticle(
                initialX = (i * 47f) % 1000f,
                initialY = 120f + ((i * 89f) % 1800f),
                speed = 200f + ((i * 37f) % 350f),
                amplitude = 15f + ((i * 13f) % 25f),
                wavelength = 0.015f + ((i * 7f) % 20f) * 0.001f,
                size = if (i % 3 == 0) 6f else 3.5f,
                isLeaf = (i % 2 == 0)
            )
        }
    }

    val windColor = if (isNight) {
        Color(0xFFE2E8F0).copy(alpha = 0.22f)
    } else {
        Color(0xFFFDE68A).copy(alpha = 0.35f)
    }

    val leafColor = if (isNight) {
        Color(0xFF94A3B8).copy(alpha = 0.40f)
    } else {
        Color(0xFF84CC16).copy(alpha = 0.55f)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        particles.forEach { particle ->
            val totalDistance = width + 200f
            val currentX = ((particle.initialX + progress * totalDistance) % totalDistance) - 100f
            val currentY = particle.initialY + (sin(currentX * particle.wavelength) * particle.amplitude)

            if (currentY in 0f..height && currentX in -50f..width + 50f) {
                if (particle.isLeaf) {
                    // Draw drifting leaf petal
                    val path = Path().apply {
                        moveTo(currentX, currentY)
                        quadraticBezierTo(
                            currentX + particle.size * 2,
                            currentY - particle.size * 1.5f,
                            currentX + particle.size * 3,
                            currentY
                        )
                        quadraticBezierTo(
                            currentX + particle.size * 2,
                            currentY + particle.size * 1.5f,
                            currentX,
                            currentY
                        )
                    }
                    drawPath(path = path, color = leafColor)
                } else {
                    // Draw flowing wind trail line
                    val trailLength = particle.size * 10f
                    drawLine(
                        color = windColor,
                        start = Offset(currentX, currentY),
                        end = Offset(currentX + trailLength, currentY + sin((currentX + trailLength) * particle.wavelength) * 5f),
                        strokeWidth = 1.5f
                    )
                }
            }
        }
    }
}
