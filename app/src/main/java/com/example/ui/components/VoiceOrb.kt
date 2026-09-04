package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.data.model.VoiceState
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.MintGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple

@Composable
fun VoiceOrb(
    voiceState: VoiceState,
    soundLevel: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

    val baseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "baseScale"
    )

    val waveRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveRotation"
    )

    val processingGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "processingGlow"
    )

    val dynamicSoundBoost = if (voiceState == VoiceState.LISTENING) soundLevel * 0.35f else 0f
    val currentScale = when (voiceState) {
        VoiceState.IDLE -> baseScale
        VoiceState.LISTENING -> (1.05f + dynamicSoundBoost).coerceIn(1.0f, 1.4f)
        VoiceState.PROCESSING -> baseScale * 1.1f
        VoiceState.SPEAKING -> (1.0f + processingGlow * 0.15f)
        VoiceState.ERROR -> 1.0f
    }

    Box(
        modifier = modifier
            .size(190.dp)
            .testTag("voice_orb_button")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 95.dp),
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = (size.minDimension / 2.6f) * currentScale

            // Outermost glowing aura
            val auraColor = when (voiceState) {
                VoiceState.LISTENING -> NeonCyan.copy(alpha = 0.25f + soundLevel * 0.3f)
                VoiceState.PROCESSING -> ElectricViolet.copy(alpha = 0.35f * processingGlow)
                VoiceState.SPEAKING -> MintGreen.copy(alpha = 0.28f)
                VoiceState.ERROR -> Color(0xFFFF5252).copy(alpha = 0.3f)
                VoiceState.IDLE -> NeonCyan.copy(alpha = 0.12f)
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(auraColor, Color.Transparent),
                    center = center,
                    radius = baseRadius * 1.45f
                ),
                radius = baseRadius * 1.45f,
                center = center
            )

            // Dynamic outer ripples for listening/speaking
            if (voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING) {
                drawCircle(
                    color = NeonCyan.copy(alpha = 0.45f),
                    radius = baseRadius * 1.15f,
                    center = center,
                    style = Stroke(width = 2.5.dp.toPx())
                )
                drawCircle(
                    color = ElectricViolet.copy(alpha = 0.35f),
                    radius = baseRadius * 1.28f,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Core Orb Gradient
            val orbColors = when (voiceState) {
                VoiceState.LISTENING -> listOf(NeonCyan, Color(0xFF00B0FF), ElectricViolet)
                VoiceState.PROCESSING -> listOf(NeonPurple, ElectricViolet, Color(0xFF4A148C))
                VoiceState.SPEAKING -> listOf(MintGreen, Color(0xFF00B0FF), ElectricViolet)
                VoiceState.ERROR -> listOf(Color(0xFFFF5252), Color(0xFFD50000), Color(0xFF260A0A))
                VoiceState.IDLE -> listOf(NeonCyan.copy(alpha = 0.85f), Color(0xFF1E88E5), Color(0xFF131722))
            }

            drawCircle(
                brush = Brush.linearGradient(
                    colors = orbColors,
                    start = Offset(center.x - baseRadius, center.y - baseRadius),
                    end = Offset(center.x + baseRadius, center.y + baseRadius)
                ),
                radius = baseRadius,
                center = center
            )

            // Inner specular highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.5f), Color.Transparent),
                    center = Offset(center.x - baseRadius * 0.35f, center.y - baseRadius * 0.35f),
                    radius = baseRadius * 0.6f
                ),
                radius = baseRadius * 0.55f,
                center = Offset(center.x - baseRadius * 0.25f, center.y - baseRadius * 0.25f)
            )
        }

        // Center Icon Indicator
        val icon = when (voiceState) {
            VoiceState.LISTENING -> Icons.Default.Mic
            VoiceState.PROCESSING -> Icons.Default.GraphicEq
            VoiceState.SPEAKING -> Icons.Default.VolumeUp
            VoiceState.ERROR -> Icons.Default.Stop
            VoiceState.IDLE -> Icons.Default.Mic
        }

        Icon(
            imageVector = icon,
            contentDescription = "Velo Voice Trigger",
            tint = Color.White,
            modifier = Modifier
                .size(42.dp)
                .padding(4.dp)
        )
    }
}
