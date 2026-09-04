package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VeloAction
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.MintGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VeloCardBorder
import com.example.ui.theme.VeloSurface

@Composable
fun ActionStatusCard(
    action: VeloAction,
    onReplaySpeech: () -> Unit,
    onRerunAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, tintColor, title) = when (action) {
        is VeloAction.OpenApp -> Triple(
            Icons.Default.Apps,
            NeonCyan,
            "Open App: ${action.appName.replaceFirstChar { it.uppercase() }}"
        )
        is VeloAction.PlayMusic -> Triple(
            Icons.Default.MusicNote,
            Color(0xFFFF4081),
            if (action.query.isNotEmpty()) "Play Music: \"${action.query}\"" else "Play Music"
        )
        is VeloAction.CheckMessages -> Triple(
            Icons.Default.Message,
            MintGreen,
            "Check Messages: ${action.platform.replaceFirstChar { it.uppercase() }}"
        )
        is VeloAction.GetTime -> Triple(
            Icons.Default.AccessTime,
            Color(0xFFFFD54F),
            "Time: ${action.formattedTime}"
        )
        is VeloAction.GeneralChat -> Triple(
            Icons.AutoMirrored.Filled.Chat,
            ElectricViolet,
            "Velo Chat"
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(VeloSurface)
            .border(1.dp, VeloCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("action_status_card")
    ) {
        // Top row with Icon, Title, and Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(tintColor.copy(alpha = 0.16f))
                        .border(1.dp, tintColor.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Action Icon",
                        tint = tintColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = "Query: \"${action.userQuery}\"",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Replay Speech Button
                IconButton(
                    onClick = onReplaySpeech,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("replay_speech_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Replay Speech",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Re-execute Button
                IconButton(
                    onClick = onRerunAction,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("rerun_action_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Re-run Action",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Status banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (action.isSuccess) MintGreen.copy(alpha = 0.12f) else Color(0xFFFF5252).copy(alpha = 0.12f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (action.isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                contentDescription = "Status",
                tint = if (action.isSuccess) MintGreen else Color(0xFFFF5252),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (action.executionStatus.isNotBlank()) action.executionStatus else "Action Triggered",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (action.isSuccess) MintGreen else Color(0xFFFF5252),
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            )
        }
    }
}
