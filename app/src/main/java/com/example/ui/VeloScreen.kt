package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.VoiceState
import com.example.ui.components.ActionStatusCard
import com.example.ui.components.JsonResponseCard
import com.example.ui.components.VoiceOrb
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.MintGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VeloBackground
import com.example.ui.theme.VeloCardBorder
import com.example.ui.theme.VeloSurface
import com.example.ui.theme.VeloSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VeloScreen(
    viewModel: VeloViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val voiceState by viewModel.voiceState.collectAsState()
    val soundLevel by viewModel.soundLevel.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val currentAction by viewModel.currentAction.collectAsState()
    val history by viewModel.history.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isTtsEnabled by viewModel.isTtsEnabled.collectAsState()

    var showHelpDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleVoiceListening()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice commands", Toast.LENGTH_SHORT).show()
        }
    }

    val requestVoiceTrigger = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.toggleVoiceListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(VeloBackground),
        containerColor = VeloBackground,
        topBar = {
            VeloTopBar(
                isTtsEnabled = isTtsEnabled,
                onToggleTts = { viewModel.toggleTts() },
                onShowHelp = { showHelpDialog = true },
                onClearHistory = { viewModel.clearHistory() },
                modifier = Modifier.statusBarsPadding()
            )
        },
        bottomBar = {
            VeloBottomInputBar(
                inputText = inputText,
                onInputChanged = { viewModel.updateInputText(it) },
                onSubmit = { viewModel.submitTextCommand() },
                onVoiceTrigger = requestVoiceTrigger,
                isListening = voiceState == VoiceState.LISTENING,
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))

                // Central Voice Assistant Section
                VeloAssistantHeader(
                    voiceState = voiceState,
                    soundLevel = soundLevel,
                    statusMessage = statusMessage,
                    onOrbClick = requestVoiceTrigger
                )
            }

            // Quick Voice Command Chips
            item {
                QuickCommandSection(
                    onCommandSelected = { command ->
                        viewModel.updateInputText(command)
                        viewModel.processVoiceCommand(command)
                    }
                )
            }

            // Active Execution & JSON Response
            currentAction?.let { action ->
                item {
                    Text(
                        text = "ACTIVE RESPONSE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            letterSpacing = 1.2.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }

                item {
                    ActionStatusCard(
                        action = action,
                        onReplaySpeech = { viewModel.replaySpeech(action) },
                        onRerunAction = { viewModel.rerunAction(action) }
                    )
                }

                item {
                    JsonResponseCard(
                        rawJson = action.rawJson,
                        actionName = action.actionName
                    )
                }
            }

            // Command History section if more than 1 item
            if (history.size > 1) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "COMMAND HISTORY (${history.size})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.2.sp
                            )
                        )
                    }
                }

                items(history.drop(1)) { pastAction ->
                    JsonResponseCard(
                        rawJson = pastAction.rawJson,
                        actionName = pastAction.actionName,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showHelpDialog) {
        VeloHelpDialog(onDismiss = { showHelpDialog = false })
    }
}

@Composable
private fun VeloTopBar(
    isTtsEnabled: Boolean,
    onToggleTts: () -> Unit,
    onShowHelp: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(VeloBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // App Identity
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(NeonCyan, ElectricViolet))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Velo Logo",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "Velo AI",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "Voice Assistant • JSON Engine",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = NeonCyan,
                        fontSize = 11.sp
                    )
                )
            }
        }

        // Action Buttons
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onToggleTts,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("toggle_tts_button")
            ) {
                Icon(
                    imageVector = if (isTtsEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = if (isTtsEnabled) "Mute Voice" else "Unmute Voice",
                    tint = if (isTtsEnabled) NeonCyan else TextSecondary
                )
            }

            IconButton(
                onClick = onClearHistory,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("clear_history_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear History",
                    tint = TextSecondary
                )
            }

            IconButton(
                onClick = onShowHelp,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("help_button")
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = "Commands Help",
                    tint = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun VeloAssistantHeader(
    voiceState: VoiceState,
    soundLevel: Float,
    statusMessage: String,
    onOrbClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glowing Voice Orb
        VoiceOrb(
            voiceState = voiceState,
            soundLevel = soundLevel,
            onClick = onOrbClick
        )

        Spacer(modifier = Modifier.height(14.dp))

        // State indicator badge
        val stateText = when (voiceState) {
            VoiceState.IDLE -> "Tap orb to speak"
            VoiceState.LISTENING -> "Listening..."
            VoiceState.PROCESSING -> "Analyzing with Velo AI..."
            VoiceState.SPEAKING -> "Velo speaking..."
            VoiceState.ERROR -> "Attention required"
        }

        val stateColor = when (voiceState) {
            VoiceState.IDLE -> NeonCyan
            VoiceState.LISTENING -> NeonCyan
            VoiceState.PROCESSING -> ElectricViolet
            VoiceState.SPEAKING -> MintGreen
            VoiceState.ERROR -> Color(0xFFFF5252)
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(stateColor.copy(alpha = 0.15f))
                .border(1.dp, stateColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Text(
                text = stateText,
                color = stateColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Status description
        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TextSecondary,
                fontSize = 13.sp
            ),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun QuickCommandSection(
    onCommandSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickCommands = listOf(
        "Open YouTube",
        "WhatsApp check karo",
        "Gaana bajao",
        "क्या समय हुआ है?",
        "Insta open karo",
        "Play Kesariya",
        "Kaise ho Velo?"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "TRY VOICE COMMANDS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (command in quickCommands) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(VeloSurface)
                        .border(1.dp, VeloCardBorder, RoundedCornerShape(20.dp))
                        .clickable { onCommandSelected(command) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("quick_command_$command")
                ) {
                    Text(
                        text = command,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun VeloBottomInputBar(
    inputText: String,
    onInputChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onVoiceTrigger: () -> Unit,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(VeloBackground)
            .border(1.dp, VeloCardBorder)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Text Input Field
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChanged,
                placeholder = {
                    Text(
                        text = "Voice command or type here...",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = VeloSurface,
                    unfocusedContainerColor = VeloSurface,
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = VeloCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = NeonCyan
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("command_input_field")
            )

            // Voice Mic Button
            IconButton(
                onClick = onVoiceTrigger,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (isListening) NeonCyan else VeloSurfaceVariant)
                    .border(1.dp, if (isListening) NeonCyan else VeloCardBorder, CircleShape)
                    .testTag("bottom_mic_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Record",
                    tint = if (isListening) VeloBackground else NeonCyan,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Send Button
            AnimatedVisibility(
                visible = inputText.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    onClick = onSubmit,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(NeonCyan)
                        .testTag("submit_command_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Submit Command",
                        tint = VeloBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VeloHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VeloSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text("Velo AI Voice Commands", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Velo converts voice/text commands into structured JSON and executes device actions:",
                    fontSize = 13.sp
                )

                HelpCategory(
                    title = "1. Open App",
                    example = "open youtube / insta open karo",
                    jsonFormat = "{\"action\": \"open_app\", \"app_name\": \"youtube\"}"
                )

                HelpCategory(
                    title = "2. Play Music",
                    example = "play song / gaana bajao / play kesariya",
                    jsonFormat = "{\"action\": \"play_music\", \"query\": \"kesariya\"}"
                )

                HelpCategory(
                    title = "3. Check Messages",
                    example = "whatsapp check karo / eska message aya kya",
                    jsonFormat = "{\"action\": \"check_messages\", \"platform\": \"whatsapp\"}"
                )

                HelpCategory(
                    title = "4. Check Time",
                    example = "time hora wo sb / क्या समय हुआ है",
                    jsonFormat = "{\"action\": \"get_time\"}"
                )

                HelpCategory(
                    title = "5. General Chat",
                    example = "kaise ho / tum kaun ho",
                    jsonFormat = "{\"action\": \"general_chat\", \"reply\": \"...\"}"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got It", color = NeonCyan, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun HelpCategory(
    title: String,
    example: String,
    jsonFormat: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = VeloSurfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, VeloCardBorder),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(title, color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("Example: \"$example\"", color = TextPrimary, fontSize = 11.sp)
            Text("JSON: $jsonFormat", color = Color(0xFFA7F3D0), fontSize = 10.sp)
        }
    }
}
