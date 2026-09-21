package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.MintGreen
import com.example.ui.theme.NeonCyan
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * PulsingVisualizer provides visual feedback while the assistant is actively listening to the user.
 * It features concentric pulsating radial waves, audio-reactive wave displacement, and particle beacons.
 */
@Composable
fun PulsingVisualizer(
    isListening: Boolean,
    soundLevel: Float,
    modifier: Modifier = Modifier,
    componentSize: Dp = 260.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_waves")

    // Continuous wave phase animations with staggered durations
    val wavePhase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase1"
    )

    val wavePhase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase2"
    )

    val wavePhase3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase3"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAngle"
    )

    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )

    // Smooth sound level dampener
    val animatedSound = remember { Animatable(0f) }
    LaunchedEffect(soundLevel) {
        animatedSound.animateTo(
            targetValue = soundLevel.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing)
        )
    }

    // Dynamic visibility alpha transition
    val activeAlpha = remember { Animatable(0f) }
    LaunchedEffect(isListening) {
        activeAlpha.animateTo(
            targetValue = if (isListening) 1f else 0f,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
        )
    }

    if (activeAlpha.value <= 0.01f) return

    Box(
        modifier = modifier
            .size(componentSize)
            .testTag("pulsing_listening_visualizer"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f
            val baseRadius = maxRadius * 0.40f * breathingScale
            val reactiveBoost = animatedSound.value * 36.dp.toPx()
            val masterAlpha = activeAlpha.value

            // 1. Concentric Expanding Rings that expand outwards & fade away
            val phases = listOf(wavePhase1, wavePhase2, wavePhase3)
            val ringColors = listOf(NeonCyan, ElectricViolet, MintGreen)

            phases.forEachIndexed { index, phase ->
                val ringRadius = baseRadius + (maxRadius - baseRadius) * phase + (reactiveBoost * (1f - phase))
                val ringAlpha = ((1f - phase) * 0.55f * masterAlpha * (0.6f + animatedSound.value * 0.4f)).coerceIn(0f, 1f)
                val strokeWidth = (2.5.dp.toPx() * (1f - phase * 0.5f)).coerceAtLeast(1.dp.toPx())

                drawCircle(
                    color = ringColors[index % ringColors.size].copy(alpha = ringAlpha),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(
                        width = strokeWidth
                    )
                )

                // Soft glow halo on outermost ripple
                if (phase < 0.6f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ringColors[index % ringColors.size].copy(alpha = ringAlpha * 0.4f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = ringRadius + 14.dp.toPx()
                        ),
                        radius = ringRadius + 14.dp.toPx(),
                        center = center
                    )
                }
            }

            // 2. Multi-point undulating audio wave loop around the core
            val waveSegments = 48
            val wavePath = Path()
            val waveRadiusBase = baseRadius * 1.08f + reactiveBoost * 0.6f

            for (i in 0..waveSegments) {
                val angle = (i.toFloat() / waveSegments) * 2f * PI.toFloat() + Math.toRadians(rotationAngle.toDouble()).toFloat()
                // 4-lobe harmonic oscillation amplified by sound level
                val harmonic = sin(angle * 4f + wavePhase1 * 2f * PI.toFloat()) * (4.dp.toPx() + animatedSound.value * 12.dp.toPx())
                val r = waveRadiusBase + harmonic
                val x = center.x + r * cos(angle)
                val y = center.y + r * sin(angle)

                if (i == 0) {
                    wavePath.moveTo(x, y)
                } else {
                    wavePath.lineTo(x, y)
                }
            }
            wavePath.close()

            drawPath(
                path = wavePath,
                brush = Brush.sweepGradient(
                    listOf(
                        NeonCyan.copy(alpha = 0.85f * masterAlpha),
                        ElectricViolet.copy(alpha = 0.9f * masterAlpha),
                        MintGreen.copy(alpha = 0.85f * masterAlpha),
                        NeonCyan.copy(alpha = 0.85f * masterAlpha)
                    ),
                    center = center
                ),
                style = Stroke(
                    width = (3.dp.toPx() + animatedSound.value * 2.dp.toPx()),
                    cap = StrokeCap.Round
                )
            )

            // 3. Orbiting listening beacon dots reflecting speech energy
            val dotCount = 8
            for (i in 0 until dotCount) {
                val dotAngle = (i.toFloat() / dotCount) * 2f * PI.toFloat() - Math.toRadians((rotationAngle * 1.5f).toDouble()).toFloat()
                val dotDist = baseRadius * 1.35f + (sin(dotAngle * 3f + wavePhase2 * 6f) * 6.dp.toPx()) + reactiveBoost * 0.4f
                val dotX = center.x + dotDist * cos(dotAngle)
                val dotY = center.y + dotDist * sin(dotAngle)

                val dotAlpha = (0.4f + 0.6f * animatedSound.value) * masterAlpha
                val dotRadius = (2.5.dp.toPx() + animatedSound.value * 2.5.dp.toPx())

                drawCircle(
                    color = if (i % 2 == 0) NeonCyan.copy(alpha = dotAlpha) else MintGreen.copy(alpha = dotAlpha),
                    radius = dotRadius,
                    center = Offset(dotX, dotY)
                )
            }
        }
    }
}
