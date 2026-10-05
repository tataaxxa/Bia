package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.VoiceProfile
import com.example.data.ChatMessage
import com.example.data.MessageSender
import com.example.ui.theme.Amber400
import com.example.ui.theme.Cyan300
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Cyan500
import com.example.ui.theme.Cyan900
import com.example.ui.theme.Emerald500
import com.example.ui.theme.HudBorderActive
import com.example.ui.theme.HudBorderCyan
import com.example.ui.theme.HudCardBg
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudCyanBright
import com.example.ui.theme.HudCyanLight
import com.example.ui.theme.HudDarkBg
import com.example.ui.theme.HudPanelBg
import com.example.ui.theme.Rose500
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArushiScreen(
    viewModel: ArushiViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var typedText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Permissions handling
    var hasRecordAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasContactsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasRecordAudioPermission = perms[Manifest.permission.RECORD_AUDIO] == true
        hasContactsPermission = perms[Manifest.permission.READ_CONTACTS] == true
    }

    LaunchedEffect(Unit) {
        if (!hasRecordAudioPermission || !hasContactsPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.CALL_PHONE
                )
            )
        }
    }

    // Auto-scroll conversation to bottom
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(HudDarkBg),
        color = HudDarkBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // HUD Top Bar with Date/Time Telemetry & Jarvis Brand
            JarvisHudTopBar(
                languageName = uiState.detectedLanguage,
                voiceName = uiState.selectedVoice.name.substringBefore("(").trim(),
                isLiveActive = uiState.visualState != AssistantVisualState.IDLE,
                onOpenSettings = { viewModel.setSettingsSheetVisible(true) },
                onOpenBridge = { viewModel.setBridgeConsoleVisible(true) },
                onClearHistory = { viewModel.clearHistory() }
            )

            // Action Feedback Banner
            AnimatedVisibility(
                visible = uiState.actionBanner.isVisible,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                ActionBanner(
                    banner = uiState.actionBanner,
                    onDismiss = { viewModel.dismissActionBanner() }
                )
            }

            // Contact Clarification Dialog in Portuguese
            if (uiState.clarificationState.isOpen) {
                ContactClarificationDialog(
                    state = uiState.clarificationState,
                    onSelectContact = { viewModel.selectClarificationContact(it) },
                    onDismiss = { viewModel.dismissClarification() }
                )
            }

            // Arc Reactor Central HUD Core
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    JarvisArcReactor(
                        visualState = uiState.visualState,
                        amplitude = uiState.currentAudioAmplitude,
                        onClick = {
                            if (uiState.isAudioPlaying) {
                                viewModel.interruptArushi()
                            } else {
                                viewModel.toggleMicrophone(hasRecordAudioPermission)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // HUD System Status Badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = HudPanelBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, HudBorderCyan),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (uiState.visualState) {
                                            AssistantVisualState.LISTENING -> Amber400
                                            AssistantVisualState.SPEAKING -> HudCyan
                                            AssistantVisualState.THINKING -> Cyan300
                                            AssistantVisualState.EXECUTING_ACTION -> Emerald500
                                            else -> HudCyanLight
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (uiState.visualState) {
                                    AssistantVisualState.IDLE -> "J.A.R.V.I.S. ONLINE • REATOR 100% • TOQUE PARA FALAR"
                                    AssistantVisualState.LISTENING -> "ESCUTANDO COMANDO EM PORTUGUÊS..."
                                    AssistantVisualState.THINKING -> "PROCESSANDO PROTOCOLO COM IA..."
                                    AssistantVisualState.SPEAKING -> "TRANSMITINDO ÁUDIO J.A.R.V.I.S. • TOQUE PARA PARAR"
                                    AssistantVisualState.EXECUTING_ACTION -> "EXECUTANDO AÇÃO NO SISTEMA ANDROID..."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when (uiState.visualState) {
                                    AssistantVisualState.LISTENING -> Amber400
                                    AssistantVisualState.SPEAKING -> HudCyanBright
                                    AssistantVisualState.THINKING -> Cyan300
                                    else -> HudCyanLight
                                },
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    // Real-time Speech Recognition Feedback
                    if (uiState.partialSpeech.isNotBlank()) {
                        Text(
                            text = "[ RECONHECENDO: \"${uiState.partialSpeech}\" ]",
                            style = MaterialTheme.typography.bodySmall,
                            color = HudCyanBright,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Audio Frequency Visualizer Waves when speaking or listening
                    if (uiState.isAudioPlaying || uiState.isMicrophoneActive) {
                        HudSpectrumVisualizer(
                            amplitude = uiState.currentAudioAmplitude,
                            isSpeaking = uiState.isAudioPlaying
                        )
                    }
                }
            }

            // Quick Voice Command Chips in 100% Portuguese
            PortugueseTestCasesCarousel(
                onExecuteTest = { command ->
                    viewModel.onUserSpoke(command)
                },
                onInterrupt = {
                    viewModel.interruptArushi()
                }
            )

            // Conversation Messages Feed (HUD Console Style)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                items(uiState.messages, key = { it.id }) { msg ->
                    HudMessageCard(
                        message = msg,
                        onReplayAudio = {
                            viewModel.audioPlayer.playGeminiAudio(
                                msg.audioBase64 ?: "",
                                msg.audioMimeType ?: "audio/pcm;rate=24000"
                            )
                        }
                    )
                }
            }

            // Bottom Input Controls Bar
            HudBottomInputBar(
                typedText = typedText,
                onTextChange = { typedText = it },
                onSend = {
                    if (typedText.isNotBlank()) {
                        viewModel.onUserSpoke(typedText)
                        typedText = ""
                    }
                },
                isListening = uiState.isMicrophoneActive,
                isPlaying = uiState.isAudioPlaying,
                onMicClick = {
                    if (!hasRecordAudioPermission) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.READ_CONTACTS,
                                Manifest.permission.CALL_PHONE
                            )
                        )
                    } else {
                        viewModel.toggleMicrophone(true)
                    }
                },
                onStopAudio = {
                    viewModel.interruptArushi()
                }
            )
        }

        // Bridge Console Sheet Modal
        if (uiState.showBridgeConsole) {
            BridgeConsoleSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.setBridgeConsoleVisible(false) }
            )
        }

        // Voice and Language Settings Sheet Modal (in Portuguese)
        if (uiState.showSettingsSheet) {
            JarvisSettingsSheet(
                currentLanguage = uiState.detectedLanguage,
                currentVoice = uiState.selectedVoice,
                onSelectLanguage = { viewModel.selectLanguage(it) },
                onSelectVoice = { viewModel.selectVoice(it) },
                onTestVoice = { viewModel.testVoice(it) },
                onDismiss = { viewModel.setSettingsSheetVisible(false) }
            )
        }
    }
}

/**
 * Top HUD Bar reproducing the futuristic Iron Man / JARVIS UI telemetry
 */
@Composable
private fun JarvisHudTopBar(
    languageName: String,
    voiceName: String,
    isLiveActive: Boolean,
    onOpenSettings: () -> Unit,
    onOpenBridge: () -> Unit,
    onClearHistory: () -> Unit
) {
    val currentTime = remember {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date())
    }
    val currentDate = remember {
        val sdf = SimpleDateFormat("dd MMM", Locale("pt", "BR"))
        sdf.format(Date()).uppercase()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left telemetry: Time & Reactor Gauge
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Mini Circular HUD dial
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(HudPanelBg)
                    .border(1.dp, HudBorderActive, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentTime,
                    color = HudCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "J.A.R.V.I.S.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = HudCyanBright,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isLiveActive) HudCyan else Emerald500)
                    )
                }
                Text(
                    text = "$currentDate // ENERGIA 100%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Language & Voice Badges
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = HudPanelBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, HudBorderCyan),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onOpenSettings() }
                    .testTag("language_badge")
            ) {
                Text(
                    text = "🇧🇷 PT-BR",
                    color = HudCyan,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = HudPanelBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, Amber400.copy(alpha = 0.6f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onOpenSettings() }
                    .testTag("voice_badge")
            ) {
                Text(
                    text = "🎙️ $voiceName",
                    color = Amber400,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }

        // Right side action buttons
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Configurações de Voz",
                    tint = HudCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onOpenBridge,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("bridge_inspector_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = "Console Bridge Android",
                    tint = HudCyanLight,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onClearHistory,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("clear_history_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Limpar Histórico",
                    tint = Slate400,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Authentic Iron Man / J.A.R.V.I.S. Arc Reactor HUD animation based directly on the uploaded image.
 * Features:
 * - Outer notched graduation ring with degree ticks (0°, 45°, 90°, 180°, 270°, etc.) and telemetry markings
 * - Middle rotating bright dashed cyan ring that spins clockwise
 * - Counter-rotating inner segmented reactor cooling blocks
 * - Central Palladium/Vibranium glowing energy core pulsating with audio amplitude
 * - Center holographic indicator
 */
@Composable
private fun JarvisArcReactor(
    visualState: AssistantVisualState,
    amplitude: Float,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "reactor_anim")

    // Continuous clockwise rotation for middle dashed track
    val dashedRingRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dashed_rotation"
    )

    // Counter-clockwise rotation for inner segmented gear
    val innerGearRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gear_rotation"
    )

    // Breathing pulse for core glow
    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    val animatedAmp by animateFloatAsState(
        targetValue = amplitude.coerceIn(0f, 1f),
        animationSpec = tween(80),
        label = "amp"
    )

    Box(
        modifier = Modifier
            .size(180.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag("voice_orb"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(size.width / 2, size.height / 2)
            val outerRadius = (size.minDimension / 2) * 0.92f

            // 1. Outer Calibration Graduation Ring with Tick Marks
            drawCircle(
                color = HudBorderCyan.copy(alpha = 0.8f),
                radius = outerRadius,
                center = centerOffset,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Draw 36 tick marks around outer circle (every 10 degrees)
            for (deg in 0 until 360 step 10) {
                val rad = Math.toRadians(deg.toDouble())
                val isMajor = deg % 30 == 0
                val tickLen = if (isMajor) 7.dp.toPx() else 3.5.dp.toPx()
                val tickColor = if (isMajor) HudCyan else HudBorderCyan

                val startX = centerOffset.x + ((outerRadius - tickLen) * cos(rad)).toFloat()
                val startY = centerOffset.y + ((outerRadius - tickLen) * sin(rad)).toFloat()
                val endX = centerOffset.x + (outerRadius * cos(rad)).toFloat()
                val endY = centerOffset.y + (outerRadius * sin(rad)).toFloat()

                drawLine(
                    color = tickColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()
                )
            }

            // 2. Middle Rotating Dashed Cyan Ring (The signature neon blue dashed ring from the image)
            val dashedRadius = outerRadius * 0.80f
            val dashInterval = floatArrayOf(8.dp.toPx(), 6.dp.toPx())
            val pathEffect = PathEffect.dashPathEffect(dashInterval, dashedRingRotation * 1.5f)

            drawCircle(
                color = HudCyan,
                radius = dashedRadius,
                center = centerOffset,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    pathEffect = pathEffect
                )
            )

            // 3. Counter-Rotating Inner Segmented Reactor Cooling Blocks
            val segmentRadius = outerRadius * 0.62f
            val numSegments = 12
            for (i in 0 until numSegments) {
                val startAngle = (i * (360f / numSegments)) + innerGearRotation
                drawArc(
                    color = if (i % 2 == 0) HudBorderActive.copy(alpha = 0.7f) else HudBorderCyan.copy(alpha = 0.5f),
                    startAngle = startAngle,
                    sweepAngle = (360f / numSegments) * 0.65f,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - segmentRadius, centerOffset.y - segmentRadius),
                    size = Size(segmentRadius * 2, segmentRadius * 2),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 4. Central Arc Reactor Palladium Core
            val dynamicCoreScale = when (visualState) {
                AssistantVisualState.SPEAKING -> 1.0f + (animatedAmp * 0.45f)
                AssistantVisualState.LISTENING -> 1.0f + (animatedAmp * 0.35f)
                AssistantVisualState.THINKING -> corePulse * 1.08f
                else -> corePulse
            }
            val coreRadius = outerRadius * 0.42f * dynamicCoreScale

            // Outer Radiant Neon Glow
            val glowColor = when (visualState) {
                AssistantVisualState.LISTENING -> Amber400
                AssistantVisualState.THINKING -> Cyan300
                AssistantVisualState.EXECUTING_ACTION -> Emerald500
                else -> HudCyan
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.75f),
                        glowColor.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = centerOffset,
                    radius = coreRadius * 1.6f
                ),
                radius = coreRadius * 1.6f,
                center = centerOffset
            )

            // Palladium Core Solid Body
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        HudCyanBright,
                        HudCyan,
                        HudPanelBg
                    ),
                    center = centerOffset,
                    radius = coreRadius
                ),
                radius = coreRadius,
                center = centerOffset
            )

            // Inner Core Steel Ring
            drawCircle(
                color = HudDarkBg,
                radius = coreRadius * 0.55f,
                center = centerOffset
            )

            // Center Arc Reactor Node with Radiant Light
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, HudCyanBright, HudCyan),
                    center = centerOffset,
                    radius = coreRadius * 0.40f
                ),
                radius = coreRadius * 0.40f,
                center = centerOffset
            )

            // Energy rays when actively speaking
            if (visualState == AssistantVisualState.SPEAKING) {
                val numRays = 12
                for (r in 0 until numRays) {
                    val angle = (r * 360f / numRays) + (dashedRingRotation * 0.5f)
                    val rad = Math.toRadians(angle.toDouble())
                    val rayLen = (6.dp.toPx() + (animatedAmp * 18.dp.toPx()))

                    val startX = centerOffset.x + ((coreRadius * 0.9f) * cos(rad)).toFloat()
                    val startY = centerOffset.y + ((coreRadius * 0.9f) * sin(rad)).toFloat()
                    val endX = centerOffset.x + ((coreRadius * 0.9f + rayLen) * cos(rad)).toFloat()
                    val endY = centerOffset.y + ((coreRadius * 0.9f + rayLen) * sin(rad)).toFloat()

                    drawLine(
                        color = HudCyanBright,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

/**
 * Animated cyan neon audio spectrum equalizer
 */
@Composable
private fun HudSpectrumVisualizer(
    amplitude: Float,
    isSpeaking: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spectrum")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(horizontal = 48.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val barsCount = 20
        for (i in 0 until barsCount) {
            val barAmp = ((sin(wavePhase + i * 0.5) + 1f) / 2f).toFloat() * (0.3f + amplitude * 0.7f)
            val heightDp = (4 + barAmp * 20).dp
            val barColor = if (isSpeaking) HudCyan else Amber400

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(heightDp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}

/**
 * Quick Command Chips in 100% Brazilian Portuguese
 */
@Composable
private fun PortugueseTestCasesCarousel(
    onExecuteTest: (String) -> Unit,
    onInterrupt: () -> Unit
) {
    val scrollState = rememberScrollState()

    val testItems = listOf(
        "Olá Jarvis",
        "Abrir WhatsApp",
        "Abrir YouTube",
        "Abrir Prisma 3D",
        "Abrir Godot",
        "Status do Sistema",
        "Abrir Configurações",
        "Ligar para Mãe",
        "Ligar para 9876543210",
        "Em que ano deu os 3D para jogos?",
        "Como criar jogos no Godot com 3D?",
        "Abrir Chrome"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "COMANDOS DE VOZ // PROTOCOLOS",
                style = MaterialTheme.typography.labelSmall,
                color = Slate400,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "TOQUE PARA EXECUTAR",
                style = MaterialTheme.typography.labelSmall,
                color = HudCyan,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Interruption action chip
            AssistChip(
                onClick = onInterrupt,
                label = { Text("PARAR ÁUDIO", color = Rose500, fontWeight = FontWeight.Black, fontSize = 11.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Parar Áudio",
                        tint = Rose500,
                        modifier = Modifier.size(13.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(containerColor = HudPanelBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, Rose500.copy(alpha = 0.7f)),
                modifier = Modifier.testTag("test_chip_interrupt")
            )

            testItems.forEach { testCommand ->
                AssistChip(
                    onClick = { onExecuteTest(testCommand) },
                    label = { Text(testCommand, color = Slate200, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = HudCardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HudBorderCyan),
                    modifier = Modifier.testTag("test_chip_${testCommand.replace(" ", "_").lowercase()}")
                )
            }
        }
    }
}

/**
 * Message Card with futuristic HUD styling (brackets, telemetry header, cyan accents)
 */
@Composable
private fun HudMessageCard(
    message: ChatMessage,
    onReplayAudio: () -> Unit
) {
    when (message.sender) {
        MessageSender.USER -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Card(
                    shape = RoundedCornerShape(topStart = 10.dp, topEnd = 2.dp, bottomStart = 10.dp, bottomEnd = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = HudCardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HudBorderCyan),
                    modifier = Modifier.widthIn(max = 300.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "[ USUÁRIO // COMANDO ]",
                                color = HudCyanLight,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }

        MessageSender.ARUSHI -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Card(
                    shape = RoundedCornerShape(topStart = 2.dp, topEnd = 10.dp, bottomStart = 10.dp, bottomEnd = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = HudPanelBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HudBorderActive.copy(alpha = 0.7f)),
                    modifier = Modifier.widthIn(max = 320.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(HudCyan)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "[ J.A.R.V.I.S. // RESPOSTA ]",
                                    color = HudCyan,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (!message.audioBase64.isNullOrBlank()) {
                                IconButton(
                                    onClick = onReplayAudio,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Ouvir novamente",
                                        tint = HudCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate200,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        MessageSender.SYSTEM_ACTION -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = HudDarkBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Emerald500.copy(alpha = 0.6f)),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Sucesso",
                            tint = Emerald500,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.labelSmall,
                            color = Emerald500,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bottom Input Bar with HUD Styling
 */
@Composable
private fun HudBottomInputBar(
    typedText: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isListening: Boolean,
    isPlaying: Boolean,
    onMicClick: () -> Unit,
    onStopAudio: () -> Unit
) {
    Surface(
        color = HudDarkBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, HudBorderCyan),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Microphone HUD Trigger Button
            FloatingActionButton(
                onClick = onMicClick,
                shape = CircleShape,
                containerColor = if (isListening) Amber400 else HudCyan,
                contentColor = HudDarkBg,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("mic_button"),
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (isListening) "Parar microfone" else "Falar com Jarvis",
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Text input field with cybernetic placeholder
            OutlinedTextField(
                value = typedText,
                onValueChange = onTextChange,
                placeholder = {
                    Text(
                        text = "Diga ou digite: 'Abrir WhatsApp'...",
                        color = Slate400,
                        fontSize = 12.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("text_input_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HudCyan,
                    unfocusedBorderColor = HudBorderCyan,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Slate200,
                    focusedContainerColor = HudPanelBg,
                    unfocusedContainerColor = HudPanelBg
                ),
                shape = RoundedCornerShape(6.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() })
            )

            Spacer(modifier = Modifier.width(6.dp))

            if (isPlaying) {
                IconButton(
                    onClick = onStopAudio,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("stop_audio_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Interromper Áudio",
                        tint = Rose500,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onSend,
                    enabled = typedText.isNotBlank(),
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        tint = if (typedText.isNotBlank()) HudCyan else Slate700,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Action Banner for execution feedback
 */
@Composable
private fun ActionBanner(
    banner: ActionBannerState,
    onDismiss: () -> Unit
) {
    Surface(
        color = if (banner.success) Emerald500.copy(alpha = 0.2f) else Rose500.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (banner.success) Emerald500 else Rose500
        ),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (banner.success) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (banner.success) Emerald500 else Rose500,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "PROTOCOLO: ${banner.actionName.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (banner.success) Emerald500 else Rose500,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                    Text(
                        text = banner.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate200,
                        fontSize = 11.sp
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fechar",
                    tint = Slate400,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Contact Clarification Dialog in Portuguese
 */
@Composable
private fun ContactClarificationDialog(
    state: ContactClarificationState,
    onSelectContact: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Clarificação de Contato",
                color = HudCyanBright,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column {
                Text(
                    text = state.question,
                    color = Slate200,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                state.contacts.forEach { contact ->
                    Button(
                        onClick = { onSelectContact(contact) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HudPanelBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HudBorderCyan),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = null,
                                tint = HudCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = contact,
                                color = HudCyanBright,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Slate400)
            }
        },
        containerColor = HudDarkBg
    )
}

/**
 * J.A.R.V.I.S. Voice & Language Settings Sheet (100% in Portuguese)
 */
@Composable
private fun JarvisSettingsSheet(
    currentLanguage: String,
    currentVoice: VoiceProfile,
    onSelectLanguage: (String) -> Unit,
    onSelectVoice: (VoiceProfile) -> Unit,
    onTestVoice: (VoiceProfile) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = HudCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Configurações J.A.R.V.I.S.",
                    color = HudCyanBright,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "IDIOMA PRINCIPAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = HudCyan,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = HudPanelBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, HudBorderActive),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🇧🇷 Português do Brasil (pt-BR) - Ativo",
                                color = HudCyanBright,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "PERFIS DE VOZ DO ASSISTENTE",
                        style = MaterialTheme.typography.labelSmall,
                        color = HudCyan,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }

                items(VoiceProfile.ALL_VOICES) { voice ->
                    val isSelected = voice.id == currentVoice.id
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) HudCardBg else HudPanelBg,
                        border = androidx.compose.foundation.BorderStroke(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) HudBorderActive else HudBorderCyan
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectVoice(voice) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = voice.name,
                                    color = if (isSelected) HudCyanBright else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = voice.description,
                                    color = Slate400,
                                    fontSize = 11.sp
                                )
                            }

                            Button(
                                onClick = { onTestVoice(voice) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) HudCyan else HudBorderCyan
                                ),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Testar",
                                        tint = if (isSelected) HudDarkBg else Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Testar",
                                        color = if (isSelected) HudDarkBg else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = HudCyan),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("Confirmar", color = HudDarkBg, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = HudDarkBg
    )
}
