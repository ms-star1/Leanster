package com.bike.leanster

import android.app.Activity as AndroidActivity
import android.content.pm.ActivityInfo
import android.graphics.Paint
import android.graphics.Typeface
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.edit
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import com.bike.leanster.ui.theme.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import java.util.Calendar

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun DashboardPortraitPreview() {
    LeansterTheme {
        Box(modifier = Modifier.fillMaxSize().background(DeepBase)) {
            DashboardViewContentMock()
        }
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 400)
@Composable
fun DashboardLandscapePreview() {
    LeansterTheme {
        Box(modifier = Modifier.fillMaxSize().background(DeepBase)) {
            DashboardViewContentMock()
        }
    }
}

@Composable
fun DashboardViewContentMock() {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Mock states for preview
    val highlightColor = NeonCyan
    val mutedHighlightColor = MutedCyan
    val highlightColorName = "Cyan"
    val isMetric = true
    val isRecording = false
    val isPaused = false
    val startAnimTriggered = true
    val hasPlayedStartupAnimation = true

    if (isLandscape) {
        DashboardLandscapeLayout(
            service = TelemetryService(),
            isMetric = isMetric,
            highlightColor = highlightColor,
            mutedHighlightColor = mutedHighlightColor,
            highlightColorName = highlightColorName,
            onColorChange = {},
            onToggleUnit = {},
            onShowHistory = {},
            onShowSettings = {},
            isRecording = isRecording,
            isPaused = isPaused,
            startAnimTriggered = startAnimTriggered,
            hasPlayedStartupAnimation = hasPlayedStartupAnimation,
            corners = emptyList(),
            allTimeMaxLeft = 0f,
            allTimeMaxRight = 0f,
            sessionMaxPitch = 0f,
            sessionMaxLeft = 0f,
            sessionMaxRight = 0f,
            rollingMaxLeft = 0f,
            rollingMaxRight = 0f,
            rollingMax1000m = 0f,
            rollingDistanceTarget = 1000
        )
    } else {
        DashboardPortraitLayout(
            service = TelemetryService(),
            isMetric = isMetric,
            highlightColor = highlightColor,
            mutedHighlightColor = mutedHighlightColor,
            highlightColorName = highlightColorName,
            onColorChange = {},
            onToggleUnit = {},
            onShowHistory = {},
            onShowSettings = {},
            isRecording = isRecording,
            isPaused = isPaused,
            startAnimTriggered = startAnimTriggered,
            hasPlayedStartupAnimation = hasPlayedStartupAnimation,
            corners = emptyList(),
            allTimeMaxLeft = 0f,
            allTimeMaxRight = 0f,
            sessionMaxPitch = 0f,
            sessionMaxLeft = 0f,
            sessionMaxRight = 0f,
            rollingMaxLeft = 0f,
            rollingMaxRight = 0f,
            rollingMax1000m = 0f,
            rollingDistanceTarget = 1000
        )
    }
}

@Composable
fun DashboardView(
    service: TelemetryService,
    isMetric: Boolean,
    highlightColor: Color,
    mutedHighlightColor: Color,
    highlightColorName: String,
    onColorChange: (String) -> Unit,
    onToggleUnit: () -> Unit,
    onShowHistory: () -> Unit,
    onShowSettings: () -> Unit,
    hasPlayedStartupAnimation: Boolean = false,
    onResetMaxLean: () -> Unit = { service.resetMaxLean1000m() },
    onStartupAnimationFinished: () -> Unit = {}
) {
    val corners by service.detectedCorners.collectAsStateWithLifecycle()
    val allTimeMaxLeft by service.allTimeMaxLeft.collectAsStateWithLifecycle()
    val allTimeMaxRight by service.allTimeMaxRight.collectAsStateWithLifecycle()
    val sessionMaxPitch by service.sessionMaxPitch.collectAsStateWithLifecycle()
    val sessionMaxLeft by service.sessionMaxLeft.collectAsStateWithLifecycle()
    val sessionMaxRight by service.sessionMaxRight.collectAsStateWithLifecycle()
    val rollingMaxLeft by service.rollingMaxLeft.collectAsStateWithLifecycle()
    val rollingMaxRight by service.rollingMaxRight.collectAsStateWithLifecycle()
    val rollingMax1000m by service.rollingMax1000m.collectAsStateWithLifecycle()
    val rollingDistanceTarget by service.rollingDistanceTarget.collectAsStateWithLifecycle()
    val isRecording by service.isRecording.collectAsStateWithLifecycle()
    val isPaused by service.isPaused.collectAsStateWithLifecycle()
    val isArmed by service.isArmed.collectAsStateWithLifecycle()
    val currentSpeed by service.currentSpeed.collectAsStateWithLifecycle()
    val livePbComparison by service.livePbComparison.collectAsStateWithLifecycle()
    val isCalibrated by service.isCalibrated.collectAsStateWithLifecycle()
    val videoModeActive by service.videoModeActive.collectAsStateWithLifecycle()

    // Ride lock: all UI interaction blocked when recording at ≥10 km/h
    val isRideLocked = isRecording && currentSpeed >= 10.0

    val context = LocalContext.current

    var isCalibrationPendingReset by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "flicker")
    val flickerAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(150),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flickerAlpha"
    )

    // Startup Animation State
    var startAnimTriggered by rememberSaveable { mutableStateOf(value = false) }
    LaunchedEffect(hasPlayedStartupAnimation) {
        if (!hasPlayedStartupAnimation) {
            startAnimTriggered = true
            delay(2.seconds)
            onStartupAnimationFinished()
        } else {
            startAnimTriggered = true
        }
    }

    // Keep screen on while recording
    DisposableEffect(isRecording) {
        val window = (context as? AndroidActivity)?.window
        if (isRecording) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val rideLockScope = rememberCoroutineScope()
    var rideLockTapCount by remember { mutableIntStateOf(0) }
    var rideLockResetJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBase)
    ) {
        if (isLandscape) {
            DashboardLandscapeLayout(
                service = service,
                isMetric = isMetric,
                highlightColor = highlightColor,
                mutedHighlightColor = mutedHighlightColor,
                highlightColorName = highlightColorName,
                onColorChange = onColorChange,
                onToggleUnit = onToggleUnit,
                onShowHistory = onShowHistory,
                onShowSettings = onShowSettings,
                isRecording = isRecording,
                isPaused = isPaused,
                isArmed = isArmed,
                startAnimTriggered = startAnimTriggered,
                hasPlayedStartupAnimation = hasPlayedStartupAnimation,
                corners = corners,
                allTimeMaxLeft = allTimeMaxLeft,
                allTimeMaxRight = allTimeMaxRight,
                sessionMaxPitch = sessionMaxPitch,
                sessionMaxLeft = sessionMaxLeft,
                sessionMaxRight = sessionMaxRight,
                rollingMaxLeft = rollingMaxLeft,
                rollingMaxRight = rollingMaxRight,
                rollingMax1000m = rollingMax1000m,
                rollingDistanceTarget = rollingDistanceTarget,
                onResetMaxLean = onResetMaxLean,
                isCalibrationPendingReset = isCalibrationPendingReset,
                flickerAlpha = flickerAlpha,
                isRideLocked = isRideLocked
            )
        } else {
            DashboardPortraitLayout(
                service = service,
                isMetric = isMetric,
                highlightColor = highlightColor,
                mutedHighlightColor = mutedHighlightColor,
                highlightColorName = highlightColorName,
                onColorChange = onColorChange,
                onToggleUnit = onToggleUnit,
                onShowHistory = onShowHistory,
                onShowSettings = onShowSettings,
                isRecording = isRecording,
                isPaused = isPaused,
                isArmed = isArmed,
                startAnimTriggered = startAnimTriggered,
                hasPlayedStartupAnimation = hasPlayedStartupAnimation,
                corners = corners,
                allTimeMaxLeft = allTimeMaxLeft,
                allTimeMaxRight = allTimeMaxRight,
                sessionMaxPitch = sessionMaxPitch,
                sessionMaxLeft = sessionMaxLeft,
                sessionMaxRight = sessionMaxRight,
                rollingMaxLeft = rollingMaxLeft,
                rollingMaxRight = rollingMaxRight,
                rollingMax1000m = rollingMax1000m,
                rollingDistanceTarget = rollingDistanceTarget,
                onResetMaxLean = onResetMaxLean,
                isCalibrationPendingReset = isCalibrationPendingReset,
                flickerAlpha = flickerAlpha,
                isRideLocked = isRideLocked
            )
        }

        // Fullscreen live camera with burned-in telemetry overlay — shown for the whole video ride
        // (from armed, through recording, until the session stops). Rendered above the dashboard but
        // below the ride-lock overlay so the speed-gated lock still governs interaction.
        if (videoModeActive && (isArmed || isRecording)) {
            SessionCameraView(
                service = service,
                highlightColor = highlightColor,
                isMetric = isMetric,
            )
        }

        livePbComparison?.let { pb ->
            LivePbOverlay(
                pb = pb,
                highlightColor = highlightColor,
                onDismiss = { service.livePbComparison.value = null }
            )
        }

        // Ride lock overlay — intercepts all touch events when speed ≥ 10 km/h.
        // Exceptions: 2 taps = pause/resume, 4 taps = discard + restart.
        if (isRideLocked) {
            var tapCount = 0
            var lastTapMs = 0L
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isRideLocked) {
                        while (true) {
                            awaitPointerEventScope {
                                awaitFirstDown(requireUnconsumed = false).consume()
                                var event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                                while (event.changes.any { it.pressed }) {
                                    event = awaitPointerEvent()
                                    event.changes.forEach { it.consume() }
                                }
                            }
                            val now = System.currentTimeMillis()
                            if (now - lastTapMs > 700L) tapCount = 0
                            tapCount++
                            lastTapMs = now

                            rideLockResetJob?.cancel()
                            if (tapCount >= 4) {
                                service.discardAndRestartSession()
                                tapCount = 0
                            } else {
                                rideLockResetJob = rideLockScope.launch {
                                    kotlinx.coroutines.delay(500L)
                                    if (tapCount == 2) {
                                        if (service.isPaused.value) service.startRecording()
                                        else service.pauseRecording()
                                    }
                                    tapCount = 0
                                }
                            }
                        }
                    }
            )
        }
    }
}

@Composable
fun LivePbOverlay(pb: PbComparison, highlightColor: Color, onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(pb) {
        visible = true
        delay(3000)
        visible = false
        onDismiss()
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
        exit = fadeOut(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, start = 16.dp, end = 16.dp)
    ) {
        val cardColor = if (pb.isNewPb) Color(0xFF1B5E20) else Color(0xFF4A3800)
        val label = if (pb.isNewPb) "NEW PB" else "NEAR PB"
        val valueText = if (pb.isNewPb)
            "${pb.achievedLean.toInt()}°"
        else
            "${pb.deltaLean.toInt()}° off best"

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = cardColor.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, if (pb.isNewPb) Color(0xFF4CAF50) else Color(0xFFFFC107)),
            modifier = Modifier.wrapContentWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (pb.isNewPb) Color(0xFF4CAF50) else Color(0xFFFFC107),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Removed manual orientation persistence

/** Native reproduction of the Ko-fi donate button (opens the Ko-fi page in a browser). */
@Composable
fun KofiButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .background(Color(0xFF000000), RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, Color(0xFF2A2A2A)), RoundedCornerShape(8.dp))
            .clickable {
                context.startActivity(
                    android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://ko-fi.com/F1I523CTZA")
                    )
                )
            }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("☕", fontSize = 15.sp)
        Text(
            "Fun? Support me on Ko-Fi", fontSize = 13.sp, color = PureWhite,
            fontFamily = Inter, fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp
        )
    }
}

@Composable
fun SettingsScreen(
    service: TelemetryService,
    isMetric: Boolean,
    highlightColor: Color,
    highlightColorName: String,
    onColorChange: (String) -> Unit,
    onToggleUnit: () -> Unit,
    onBack: () -> Unit,
    onShowHelp: () -> Unit = {}
) {
    val context = LocalContext.current
    val isDevModeActive by service.isDevModeActive.collectAsStateWithLifecycle()
    val isUsbGpsConnected by service.isUsbGpsConnected.collectAsStateWithLifecycle()
    val gpsUpdateRateHz by service.gpsUpdateRateHz.collectAsStateWithLifecycle()
    val forceFilter by service.forceFilterStandstill.collectAsStateWithLifecycle()
    val autoResumeOnLean by service.autoResumeOnLean.collectAsStateWithLifecycle()
    val showDemoSession by service.showDemoSession.collectAsStateWithLifecycle()
    val minCornerPeak by service.minCornerPeakLean.collectAsStateWithLifecycle()
    val use24HourTime by service.use24HourTime.collectAsStateWithLifecycle()
    val videoPromptRemembered by service.videoPromptRemembered.collectAsStateWithLifecycle()
    val videoRecordByDefault by service.videoRecordByDefault.collectAsStateWithLifecycle()

    var devModeClickCount by remember { mutableIntStateOf(0) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val presets = listOf(
        "Kawasaki" to KawasakiGreen, "Ducati" to DucatiRed, "Yamaha" to YamahaBlue,
        "KTM" to KtmOrange, "Honda" to HondaRed, "BMW" to BmwBlue,
        "Triumph" to TriumphGold, "Husqvarna" to HusqvarnaYellow,
        "Cyan" to NeonCyan, "White" to PureWhiteHighlight
    )

    Box(modifier = Modifier.fillMaxSize().background(DeepCarbon)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

            // ── Header ────────────────────────────────────────────────────
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp).padding(bottom = 6.dp)) {
                Text("LEANSTER", fontSize = 11.sp, letterSpacing = 3.sp, color = MutedGrey, fontFamily = Inter)
                Text(
                    text = "SETTINGS" + if (isDevModeActive) " (DEV)" else "",
                    fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp,
                    color = highlightColor, fontFamily = Rajdhani,
                    modifier = Modifier.clickable {
                        devModeClickCount++
                        if (devModeClickCount >= 5) {
                            service.isDevModeActive.value = !isDevModeActive
                            devModeClickCount = 0
                            android.widget.Toast.makeText(context,
                                if (!isDevModeActive) "Dev Mode On" else "Dev Mode Off",
                                android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            HorizontalDivider(color = BorderDivider)

            // ── SUPPORT ───────────────────────────────────────────────────
            Text("SUPPORT", fontSize = 11.sp, letterSpacing = 2.sp, color = MutedGrey, fontFamily = Inter,
                modifier = Modifier.padding(start = 24.dp, top = 18.dp, bottom = 12.dp))
            KofiButton(Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = BorderDivider)

            // ── BRAND PRESET ──────────────────────────────────────────────
            Text("BRAND PRESET", fontSize = 11.sp, letterSpacing = 2.sp, color = MutedGrey, fontFamily = Inter,
                modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 14.dp))
            presets.forEachIndexed { idx, (name, color) ->
                val isSelected = highlightColorName == name
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onColorChange(name) }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(Modifier.size(18.dp).background(color, RoundedCornerShape(50)))
                    Text(name, fontSize = 15.sp, modifier = Modifier.weight(1f),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) PureWhite else MidGrey)
                    Text(if (isSelected) "✓" else "○", fontSize = 13.sp,
                        color = if (isSelected) highlightColor else Color(0xFF2A3028))
                }
                if (idx < presets.size - 1)
                    HorizontalDivider(color = Color(0xFF111510), modifier = Modifier.padding(horizontal = 24.dp))
            }
            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = BorderDivider)

            // ── UNITS ─────────────────────────────────────────────────────
            Text("UNITS", fontSize = 11.sp, letterSpacing = 2.sp, color = MutedGrey, fontFamily = Inter,
                modifier = Modifier.padding(start = 24.dp, top = 18.dp, bottom = 12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                    .background(Color(0xFF111510), RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, BorderDivider), RoundedCornerShape(8.dp))
            ) {
                Box(modifier = Modifier.weight(1f)
                    .background(if (isMetric) highlightColor else Color.Transparent, RoundedCornerShape(8.dp))
                    .clickable { if (!isMetric) onToggleUnit() }.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) { Text("km/h", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp, color = if (isMetric) DeepCarbon else MutedGrey) }
                Box(modifier = Modifier.weight(1f)
                    .background(if (!isMetric) highlightColor else Color.Transparent, RoundedCornerShape(8.dp))
                    .clickable { if (isMetric) onToggleUnit() }.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) { Text("mph", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp, color = if (!isMetric) DeepCarbon else MutedGrey) }
            }
            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = BorderDivider)

            // ── TIME FORMAT ───────────────────────────────────────────────
            Text("TIME FORMAT", fontSize = 11.sp, letterSpacing = 2.sp, color = MutedGrey, fontFamily = Inter,
                modifier = Modifier.padding(start = 24.dp, top = 18.dp, bottom = 12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                    .background(Color(0xFF111510), RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, BorderDivider), RoundedCornerShape(8.dp))
            ) {
                Box(modifier = Modifier.weight(1f)
                    .background(if (use24HourTime) highlightColor else Color.Transparent, RoundedCornerShape(8.dp))
                    .clickable { if (!use24HourTime) service.setUse24HourTime(true) }.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) { Text("24 h", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp, color = if (use24HourTime) DeepCarbon else MutedGrey) }
                Box(modifier = Modifier.weight(1f)
                    .background(if (!use24HourTime) highlightColor else Color.Transparent, RoundedCornerShape(8.dp))
                    .clickable { if (use24HourTime) service.setUse24HourTime(false) }.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) { Text("12 h", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp, color = if (!use24HourTime) DeepCarbon else MutedGrey) }
            }
            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = BorderDivider)

            // ── GPS SOURCE ────────────────────────────────────────────────
            Text("GPS SOURCE", fontSize = 11.sp, letterSpacing = 2.sp, color = MutedGrey, fontFamily = Inter,
                modifier = Modifier.padding(start = 24.dp, top = 18.dp, bottom = 12.dp))
            // Built-in GPS
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.size(16.dp)
                    .background(if (!isUsbGpsConnected) highlightColor else Color.Transparent, RoundedCornerShape(50))
                    .border(BorderStroke(if (!isUsbGpsConnected) 0.dp else 1.5.dp, Color(0xFF3A4036)), RoundedCornerShape(50)))
                Text("Built-in GPS", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    color = if (!isUsbGpsConnected) PureWhite else MidGrey, modifier = Modifier.weight(1f))
                Text("~1Hz", fontSize = 12.sp, color = MutedGrey, fontFamily = FontFamily.Monospace)
            }
            HorizontalDivider(color = Color(0xFF111510), modifier = Modifier.padding(horizontal = 24.dp))
            // Adafruit USB GPS
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.size(16.dp)
                    .background(if (isUsbGpsConnected) highlightColor else Color.Transparent, RoundedCornerShape(50))
                    .border(BorderStroke(if (isUsbGpsConnected) 0.dp else 1.5.dp, Color(0xFF3A4036)), RoundedCornerShape(50)))
                Text("Adafruit USB GPS", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    color = if (isUsbGpsConnected) PureWhite else MidGrey, modifier = Modifier.weight(1f))
                // Show the real measured rate once connected (green above 4 Hz); static spec otherwise.
                Text(
                    text = if (isUsbGpsConnected) "${"%.0f".format(gpsUpdateRateHz)}Hz" else "10Hz",
                    fontSize = 12.sp,
                    color = if (isUsbGpsConnected && gpsUpdateRateHz > 4f) NeonGreen
                        else if (isUsbGpsConnected) highlightColor else MutedGrey,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = BorderDivider)

            // ── RIDE BEHAVIOUR ────────────────────────────────────────────
            Text("RIDE BEHAVIOUR", fontSize = 11.sp, letterSpacing = 2.sp, color = MutedGrey, fontFamily = Inter,
                modifier = Modifier.padding(start = 24.dp, top = 18.dp, bottom = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                    .clickable { service.setAutoResumeOnLean(!autoResumeOnLean) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = autoResumeOnLean, onCheckedChange = { service.setAutoResumeOnLean(it) },
                    colors = CheckboxDefaults.colors(checkedColor = highlightColor, uncheckedColor = MutedGrey))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Auto-resume on lean ≥ 30°", color = PureWhite, fontSize = 14.sp)
                    Text("Resumes a paused session when the bike leans past 30°", color = MutedGrey, fontSize = 11.sp)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                    .clickable { service.setShowDemoSession(!showDemoSession) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = showDemoSession, onCheckedChange = { service.setShowDemoSession(it) },
                    colors = CheckboxDefaults.colors(checkedColor = highlightColor, uncheckedColor = MutedGrey))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Show demo session", color = PureWhite, fontSize = 14.sp)
                    Text("Display the Kesselberg demo ride in the session list", color = MutedGrey, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = BorderDivider)

            // ── VIDEO ─────────────────────────────────────────────────────
            Text("VIDEO", fontSize = 11.sp, letterSpacing = 2.sp, color = MutedGrey, fontFamily = Inter,
                modifier = Modifier.padding(start = 24.dp, top = 18.dp, bottom = 4.dp))
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (videoPromptRemembered) {
                    Text(
                        if (videoRecordByDefault) "Always recording video" else "Never recording video",
                        fontSize = 14.sp, color = PureWhite
                    )
                    Text("This choice is applied automatically at the start of every ride.", fontSize = 11.sp, color = MutedGrey)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .background(Color(0xFF111510), RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, BorderDivider), RoundedCornerShape(8.dp))
                            .clickable {
                                service.clearVideoPromptChoice()
                                android.widget.Toast.makeText(context, "You'll be asked before each ride.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("RESET — ASK BEFORE EACH RIDE", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp, color = highlightColor, fontFamily = Rajdhani)
                    }
                } else {
                    Text("Ask before each ride", fontSize = 14.sp, color = PureWhite)
                    Text("You'll be asked whether to record video when you tap to ride.", fontSize = 11.sp, color = MutedGrey)
                }
            }
            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = BorderDivider)

            // ── CORNER DETECTION ─────────────────────────────────────────
            Text("CORNER DETECTION", fontSize = 11.sp, letterSpacing = 2.sp, color = MutedGrey, fontFamily = Inter,
                modifier = Modifier.padding(start = 24.dp, top = 18.dp, bottom = 4.dp))
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Minimum peak lean angle", fontSize = 14.sp, color = PureWhite)
                Text("Corners that never reach this lean are ignored", fontSize = 11.sp, color = MutedGrey)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(Color(0xFF111510), RoundedCornerShape(8.dp))
                        .border(BorderStroke(1.dp, BorderDivider), RoundedCornerShape(8.dp))
                ) {
                    listOf(15f, 25f, 35f).forEach { deg ->
                        val selected = minCornerPeak == deg
                        Box(
                            modifier = Modifier.weight(1f)
                                .background(
                                    if (selected) highlightColor else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { service.setMinCornerPeakLean(deg) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${deg.toInt()}°",
                                fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp,
                                fontFamily = Rajdhani,
                                color = if (selected) DeepCarbon else MutedGrey
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = BorderDivider)

            // ── DATA ──────────────────────────────────────────────────────
            Text("DATA", fontSize = 11.sp, letterSpacing = 2.sp, color = MutedGrey, fontFamily = Inter,
                modifier = Modifier.padding(start = 24.dp, top = 18.dp, bottom = 12.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Export all sessions as CSV", fontSize = 14.sp, color = PureWhite, modifier = Modifier.weight(1f))
                Text("›", fontSize = 18.sp, color = MutedGrey)
            }
            HorizontalDivider(color = Color(0xFF111510), modifier = Modifier.padding(horizontal = 24.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 13.dp)
                .clickable { onShowHelp() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Help & Calibration Guide", fontSize = 14.sp, color = PureWhite, modifier = Modifier.weight(1f))
                Text("›", fontSize = 18.sp, color = MutedGrey)
            }
            HorizontalDivider(color = Color(0xFF111510), modifier = Modifier.padding(horizontal = 24.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 13.dp)
                .clickable { showClearConfirm = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Clear all data", fontSize = 14.sp, color = MutedGrey, modifier = Modifier.weight(1f))
                Text("›", fontSize = 18.sp, color = MutedGrey)
            }
            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = BorderDivider)

            // ── DEVELOPER (hidden) ────────────────────────────────────────
            if (isDevModeActive) {
                Text("DEVELOPER", fontSize = 11.sp, letterSpacing = 2.sp, color = MutedGrey, fontFamily = Inter,
                    modifier = Modifier.padding(start = 24.dp, top = 18.dp, bottom = 4.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                    .clickable { service.setForceFilterStandstill(!forceFilter) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = forceFilter, onCheckedChange = { service.setForceFilterStandstill(it) },
                        colors = CheckboxDefaults.colors(checkedColor = highlightColor, uncheckedColor = MutedGrey))
                    Spacer(Modifier.width(8.dp))
                    Text("Force Filter at Standstill", color = PureWhite)
                }
                HorizontalDivider(color = BorderDivider)
            }

            // Space for footer
            Spacer(Modifier.height(80.dp))
        }

        // ── Footer (pinned) ───────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
            .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Leanster 1.0.0", fontSize = 11.sp, letterSpacing = 1.sp,
                color = Color(0xFF2A3028), fontFamily = FontFamily.Monospace)
            Text("Offline · No account", fontSize = 11.sp, letterSpacing = 1.sp,
                color = Color(0xFF2A3028), fontFamily = FontFamily.Monospace)
        }

        // ── Overlays ─────────────────────────────────────────────────────
        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                containerColor = SurfaceCard, titleContentColor = PureWhite, textContentColor = MutedGrey,
                title = { Text("Clear All Data?") },
                text = { Text("Resets all-time max lean records. Session ride files are not deleted.") },
                confirmButton = { TextButton(onClick = { service.resetAllTimeLean(); showClearConfirm = false }) {
                    Text("CLEAR", color = AlertRed, fontWeight = FontWeight.Bold) }
                },
                dismissButton = { TextButton(onClick = { showClearConfirm = false }) {
                    Text("CANCEL", color = PureWhite) }
                }
            )
        }

    }
}


enum class MetricType(val title: String) {
    LEAN("Lean"),
    SPEED("Speed"),
    MAX_1000M("Max Lean Distance"),
    SESSION_MAX_LEFT("Session Max Left"),
    SESSION_MAX_RIGHT("Session Max Right"),
    ALL_TIME_LEFT("All-Time Left"),
    ALL_TIME_RIGHT("All-Time Right"),
    ALL_TIME_MAX("All-Time Max Lean"),
    WHEELIE("Wheelie Angle"),
    CORNERS("Corners Driven")
}

@Composable
fun TopRightControls(
    onShowSettings: () -> Unit,
    onShowHistory: () -> Unit,
    onCalibrate: () -> Unit,
    highlightColor: Color
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = highlightColor
                )
            }
            
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(SurfaceCard)
            ) {
                DropdownMenuItem(
                    text = { Text("Settings", color = PureWhite) },
                    onClick = { menuExpanded = false; onShowSettings() }
                )
                DropdownMenuItem(
                    text = { Text("Calibrate 3D", color = PureWhite) },
                    onClick = { 
                        menuExpanded = false
                        onCalibrate() 
                        android.widget.Toast.makeText(context, "3D Sensor Calibrated!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
                DropdownMenuItem(
                    text = { Text("My Rides", color = PureWhite) },
                    onClick = { menuExpanded = false; onShowHistory() }
                )
                DropdownMenuItem(
                    text = { Text("Donate", color = PureWhite) },
                    onClick = { 
                        menuExpanded = false
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://www.paypal.com/paypalme/michasteinauer")
                        )
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun CustomizableMetricCard(
    selectedMetric: MetricType,
    onMetricSelected: (MetricType) -> Unit,
    currentLean: Float,
    currentSpeed: Double,
    rollingMax1000m: Float,
    rollingDistanceTarget: Int,
    isMetric: Boolean,
    sessionMaxLeft: Float,
    sessionMaxRight: Float,
    allTimeMaxLeft: Float,
    allTimeMaxRight: Float,
    sessionMaxPitch: Float,
    cornersCount: Int,
    activeView: String,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 80.dp,
    highlightColor: Color,
    forceWhiteText: Boolean = false,
    onReset1000m: () -> Unit,
    onResetSessionLean: () -> Unit,
    onResetSessionPitch: () -> Unit,
    onResetCorners: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val absLeanVal = abs(currentLean)
    val leanProgress = ((absLeanVal - 30f) / 10f).coerceIn(0f, 1f)
    val max1000mProgress = ((rollingMax1000m - 30f) / 10f).coerceIn(0f, 1f)

    var title = ""
    var valueStr = ""
    var unit = ""
    var progress = 0f
    var onReset: (() -> Unit)? = null
    var color = PureWhite

    when (selectedMetric) {
        MetricType.LEAN -> {
            title = "LEAN"
            valueStr = absLeanVal.toInt().toString()
            unit = "°"
            progress = leanProgress
        }
        MetricType.SPEED -> {
            title = "SPEED"
            valueStr = if (isMetric) currentSpeed.toInt().toString() else (currentSpeed * 0.621371).toInt().toString()
            unit = if (isMetric) "km/h" else "mph"
            color = highlightColor
        }
        MetricType.MAX_1000M -> {
            title = if (rollingDistanceTarget >= 1000) "MAX ${rollingDistanceTarget / 1000}km" else "MAX ${rollingDistanceTarget}m"
            valueStr = rollingMax1000m.toInt().toString()
            unit = "°"
            progress = max1000mProgress
            onReset = onReset1000m
        }
        MetricType.SESSION_MAX_LEFT -> {
            title = "SESS MAX L"
            valueStr = abs(sessionMaxLeft).toInt().toString()
            unit = "°"
            onReset = onResetSessionLean
        }
        MetricType.SESSION_MAX_RIGHT -> {
            title = "SESS MAX R"
            valueStr = sessionMaxRight.toInt().toString()
            unit = "°"
            onReset = onResetSessionLean
        }
        MetricType.ALL_TIME_LEFT -> {
            title = "ALL-TIME L"
            valueStr = abs(allTimeMaxLeft).toInt().toString()
            unit = "°"
        }
        MetricType.ALL_TIME_RIGHT -> {
            title = "ALL-TIME R"
            valueStr = allTimeMaxRight.toInt().toString()
            unit = "°"
        }
        MetricType.ALL_TIME_MAX -> {
            title = "ALL-TIME MAX"
            valueStr = maxOf(abs(allTimeMaxLeft), allTimeMaxRight).toInt().toString()
            unit = "°"
        }
        MetricType.WHEELIE -> {
            title = "WHEELIE"
            valueStr = sessionMaxPitch.toInt().toString()
            unit = "°"
            onReset = onResetSessionPitch
        }
        MetricType.CORNERS -> {
            title = "CORNERS"
            valueStr = cornersCount.toString()
            onReset = onResetCorners
        }
    }

    if (forceWhiteText) {
        color = PureWhite
    }

    Box(modifier = modifier) {
        MetricCard(
            title = title,
            value = valueStr,
            unit = unit,
            color = color,
            containerColor = if (activeView == "map") lerp(SurfaceCard, AlertRed, progress).copy(alpha = 0.8f) else lerp(SurfaceCard, AlertRed, progress),
            modifier = Modifier.fillMaxWidth(),
            weight = FontWeight(700 + (progress * 200).toInt()),
            height = height,
            onClick = { expanded = true }
        )
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(SurfaceCard)
        ) {
            MetricType.values().forEach { metric ->
                DropdownMenuItem(
                    text = { Text(metric.title, color = PureWhite) },
                    onClick = {
                        onMetricSelected(metric)
                        expanded = false
                    }
                )
            }
            if (onReset != null) {
                HorizontalDivider(color = BorderDivider)
                DropdownMenuItem(
                    text = { Text("Reset Value", color = AlertRed) },
                    onClick = {
                        onReset()
                        expanded = false
                    }
                )
            }
        }
    }
}

// ── Calibration Alert Overlay ─────────────────────────────────────────────────
@Composable
fun CalibrationAlertOverlay(
    reason: String,
    highlightColor: Color,
    onOk: () -> Unit,
    onRecalibrate: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF0070908))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .background(Color(0xFF1A0A0A), RoundedCornerShape(18.dp))
                .border(BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f)), RoundedCornerShape(18.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pulsing warning icon
            val infiniteTransition = rememberInfiniteTransition(label = "alertPulse")
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 1f, targetValue = 0.3f,
                animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
                label = "alertAlpha"
            )
            Text("⚠", fontSize = 40.sp, color = AlertRed.copy(alpha = pulseAlpha))
            Text(
                "CALIBRATION LOST",
                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AlertRed,
                letterSpacing = 2.sp, fontFamily = Inter
            )
            Text(
                reason,
                fontSize = 13.sp, color = MidGrey, textAlign = TextAlign.Center, lineHeight = 20.sp
            )
            HorizontalDivider(color = Color(0xFF2A1010))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onOk,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderDivider),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite)
                ) {
                    Text("OK", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                }
                Button(
                    onClick = onRecalibrate,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("RECALIBRATE", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp, color = PureWhite)
                }
            }
        }
    }
}

// ── Calibration Guide Overlay ─────────────────────────────────────────────────
@Composable
fun CalibrationGuideOverlay(
    service: TelemetryService,
    highlightColor: Color,
    onDismiss: () -> Unit,
    onCalibrationSuccess: (() -> Unit)? = null,
    showCameraPreview: Boolean = false
) {
    val currentLean by service.currentLean.collectAsStateWithLifecycle()
    val currentPitch by service.currentPitch.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scope = rememberCoroutineScope()

    // 0=mount, 1=upright, 2=calibrate, 3=success, 4=fail
    var step by remember { mutableIntStateOf(0) }
    var mountMode by remember { mutableStateOf("") }      // "Landscape" or "Portrait"
    var failReason by remember { mutableStateOf("") }
    var countdown by remember { mutableIntStateOf(3) }
    var isCountingDown by remember { mutableStateOf(false) }
    var countdownMessage by remember { mutableStateOf("") }

    // Hold-to-confirm state for step 0 (mount): fills 0→1 over 1s, then runs a movement check.
    val holdProgress = remember { Animatable(0f) }
    var mountChecking by remember { mutableStateOf(false) }

    val showPreview = showCameraPreview && step in 0..2

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        // On a video ride the live camera fills the whole background; a scrim keeps the centered
        // panel readable. Without a preview, a solid dim stands in.
        if (showPreview) {
            CalibrationCameraPreview(modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color(0x99070908)))
        } else {
            Box(Modifier.fillMaxSize().background(Color(0xE5070908)))
        }

        // Step content, layout-independent — hosted by the centered panel below.
        val stepContent: @Composable ColumnScope.() -> Unit = {
            when (step) {

                // ── Step 1: Mount device ──────────────────────────────────
                0 -> {
                    Text("STEP 1 / 3", fontSize = 11.sp, letterSpacing = 2.sp, color = MutedGrey)
                    Text("Mount Your Device", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = PureWhite, textAlign = TextAlign.Center)
                    Text(
                        "Attach your phone to the motorcycle mount securely.\nThen press and hold to confirm.",
                        fontSize = 13.sp, color = MidGrey, textAlign = TextAlign.Center, lineHeight = 20.sp
                    )

                    // Press-and-hold (1 s) confirm. A fill sweeps across the button over the second;
                    // completing the hold triggers the same steadiness check the swipe used to.
                    val p = holdProgress.value
                    val active = p > 0.01f || mountChecking
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0E130D))
                            .border(BorderStroke(1.dp, if (active) highlightColor.copy(0.6f) else BorderDivider), RoundedCornerShape(12.dp))
                            .then(
                                if (mountChecking) Modifier
                                else Modifier.pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            val holdJob = scope.launch {
                                                holdProgress.snapTo(0f)
                                                holdProgress.animateTo(1f, tween(1000, easing = LinearEasing))
                                                // Held the full second → run the steadiness check.
                                                mountChecking = true
                                                val leanAtStart = currentLean
                                                val pitchAtStart = currentPitch
                                                delay(1500L)
                                                if (abs(currentLean - leanAtStart) > 6f || abs(currentPitch - pitchAtStart) > 6f) {
                                                    failReason = "Phone detected as moving.\nEnsure device is firmly mounted and try again."
                                                    mountChecking = false
                                                    holdProgress.snapTo(0f)
                                                    step = 4
                                                } else {
                                                    mountMode = if (isLandscape) "Landscape" else "Portrait"
                                                    step = 1
                                                }
                                            }
                                            tryAwaitRelease()
                                            // Released before the hold completed → cancel & rewind.
                                            if (holdProgress.value < 1f && !mountChecking) {
                                                holdJob.cancel()
                                                scope.launch { holdProgress.animateTo(0f, tween(150)) }
                                            }
                                        }
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight()
                                .fillMaxWidth(p)
                                .background(highlightColor.copy(alpha = 0.18f))
                        )
                        Text(
                            when {
                                mountChecking -> "Checking mount…"
                                p > 0.02f -> "Keep holding…"
                                else -> "Hold to confirm"
                            },
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = if (active) highlightColor else MutedGrey
                        )
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MutedGrey, fontSize = 13.sp)
                    }
                }

                // ── Step 2: Upright + mount confirmation ──────────────────
                1 -> {
                    Text("STEP 2 / 3", fontSize = 11.sp, letterSpacing = 2.sp, color = MutedGrey)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(highlightColor.copy(0.08f), RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Canvas(Modifier.size(8.dp)) { drawCircle(highlightColor) }
                            Text("Mounted in $mountMode mode — confirmed", fontSize = 13.sp, color = highlightColor, fontWeight = FontWeight.Medium)
                        }
                    }
                    Text("Straighten your Bike", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = PureWhite, textAlign = TextAlign.Center)
                    Text(
                        "Place your bike in a fully upright position.\nBest done on a level surface.",
                        fontSize = 13.sp, color = MidGrey, textAlign = TextAlign.Center, lineHeight = 20.sp
                    )
                    Button(
                        onClick = { step = 2 },
                        colors = ButtonDefaults.buttonColors(containerColor = highlightColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("NEXT →", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF070908), letterSpacing = 1.sp)
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MutedGrey, fontSize = 13.sp)
                    }
                }

                // ── Step 3: Calibrate ─────────────────────────────────────
                2 -> {
                    Text("STEP 3 / 3", fontSize = 11.sp, letterSpacing = 2.sp, color = MutedGrey)
                    Text("3D Calibrate Now", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = PureWhite, textAlign = TextAlign.Center)

                    if (isCountingDown) {
                        // Countdown display
                        Text(
                            text = if (countdown > 0) countdown.toString() else "✓",
                            fontSize = 64.sp, fontWeight = FontWeight.Bold,
                            color = if (countdown > 0) highlightColor else Color(0xFF6FD000),
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            countdownMessage,
                            fontSize = 13.sp, color = MidGrey, textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            "Keep your bike perfectly upright and steady.\nTap below to begin the 3-second calibration.",
                            fontSize = 13.sp, color = MidGrey, textAlign = TextAlign.Center, lineHeight = 20.sp
                        )
                        Button(
                            onClick = {
                                isCountingDown = true
                                countdownMessage = "Keep your bike upright and steady"
                                val leanAtStart = currentLean
                                val pitchAtStart = currentPitch
                                scope.launch {
                                    for (i in 3 downTo 1) {
                                        countdown = i
                                        val leanNow = currentLean
                                        val pitchNow = currentPitch
                                        if (abs(leanNow - leanAtStart) > 3f || abs(pitchNow - pitchAtStart) > 3f) {
                                            countdownMessage = "Keep your bike steady!"
                                            delay(600L)
                                            failReason = "Erratic movement detected during calibration.\nKeep your bike steady and try again."
                                            isCountingDown = false
                                            step = 4
                                            return@launch
                                        }
                                        delay(1000L)
                                    }
                                    countdown = 0
                                    service.calibrateSensors()
                                    delay(200L) // let sensor callback fire
                                    step = 3
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = highlightColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("3D CALIBRATE NOW", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF070908), letterSpacing = 1.sp)
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MutedGrey, fontSize = 13.sp)
                    }
                }

                // ── Step 4: Success ───────────────────────────────────────
                3 -> {
                    Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawCircle(Color(0xFF6FD000).copy(0.15f))
                            drawCircle(Color(0xFF6FD000), style = Stroke(2.dp.toPx()))
                        }
                        Text("✓", fontSize = 32.sp, color = Color(0xFF6FD000))
                    }
                    Text("Calibration Locked", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = PureWhite, textAlign = TextAlign.Center)
                    Text(
                        if (onCalibrationSuccess != null)
                            "Sensor calibrated in $mountMode mode.\nStarting your ride now…"
                        else
                            "Sensor calibrated in $mountMode mode.\nYou're ready to ride.",
                        fontSize = 13.sp, color = MidGrey, textAlign = TextAlign.Center, lineHeight = 20.sp
                    )
                    Button(
                        // Success path: when a success handler exists (ride-start flow), invoke only it —
                        // onDismiss doubles as "cancel" there and would clear the pending video mode.
                        onClick = { if (onCalibrationSuccess != null) onCalibrationSuccess() else onDismiss() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6FD000)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(
                            if (onCalibrationSuccess != null) "START RIDING" else "DONE",
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF070908), letterSpacing = 1.sp
                        )
                    }
                }

                // ── Step 5: Fail ──────────────────────────────────────────
                4 -> {
                    Text("✕", fontSize = 32.sp, color = AlertRed)
                    Text("Calibration Failed", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = PureWhite, textAlign = TextAlign.Center)
                    Text(failReason, fontSize = 13.sp, color = MidGrey, textAlign = TextAlign.Center, lineHeight = 20.sp)
                    Button(
                        onClick = { step = 0; mountChecking = false; scope.launch { holdProgress.snapTo(0f) }; countdown = 3; isCountingDown = false },
                        colors = ButtonDefaults.buttonColors(containerColor = BorderDivider),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("TRY AGAIN", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PureWhite, letterSpacing = 1.sp)
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MutedGrey, fontSize = 13.sp)
                    }
                }
            }
        }

        // Centered panel holding the step's text and controls, over the fullscreen preview or dim.
        Column(
            modifier = Modifier
                .fillMaxWidth(if (isLandscape) 0.6f else 0.86f)
                // Cap to the viewport and scroll so controls stay reachable on short screens.
                .heightIn(max = (configuration.screenHeightDp * 0.92f).dp)
                .background(Color(0xF01A211A), RoundedCornerShape(18.dp))
                .border(BorderStroke(1.dp, BorderDivider), RoundedCornerShape(18.dp))
                .verticalScroll(rememberScrollState())
                .padding(if (isLandscape) 20.dp else 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (isLandscape) 12.dp else 20.dp)
        ) {
            stepContent()
        }
    }
}

// ── Ride-start flow (video prompt → mount reminder → calibration) ─────────────
/**
 * Orchestrates everything that happens after TAP TO RIDE: an optional "record video?" prompt (with a
 * remember-my-choice option), a mount-safety reminder for video rides, and the calibration guide
 * (which shows a live camera preview during a video ride). Ends by calling [onArm] to start the session.
 *
 * Bump [trigger] (e.g. `trigger++`) to launch the flow. When the user has a remembered choice the
 * prompt is skipped automatically.
 */
@Composable
fun RideStartOverlays(
    service: TelemetryService,
    highlightColor: Color,
    trigger: Int,
    onArm: () -> Unit,
) {
    var stage by remember { mutableStateOf("") }        // "", "video", "mount", "calib"
    var pendingVideo by remember { mutableStateOf(false) }

    fun cancel() {
        stage = ""
        service.setVideoMode(false)
    }

    fun proceedToArmOrCalibrate() {
        service.setVideoMode(pendingVideo)
        if (service.isCalibrated.value) {
            stage = ""
            onArm()
        } else {
            stage = "calib"
        }
    }

    fun onVideoDecision(recordVideo: Boolean, rememberChoice: Boolean) {
        pendingVideo = recordVideo
        if (rememberChoice) service.setVideoPromptChoice(true, recordVideo)
        if (recordVideo) stage = "mount" else proceedToArmOrCalibrate()
    }

    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        if (service.videoPromptRemembered.value) {
            pendingVideo = service.videoRecordByDefault.value
            if (pendingVideo) stage = "mount" else proceedToArmOrCalibrate()
        } else {
            stage = "video"
        }
    }

    when (stage) {
        "video" -> VideoRecordPromptDialog(
            highlightColor = highlightColor,
            onChoice = { record, rememberChoice -> onVideoDecision(record, rememberChoice) },
            onCancel = { cancel() },
        )
        "mount" -> MountReminderDialog(
            highlightColor = highlightColor,
            onOk = { proceedToArmOrCalibrate() },
            onCancel = { cancel() },
        )
        "calib" -> CalibrationGuideOverlay(
            service = service,
            highlightColor = highlightColor,
            showCameraPreview = pendingVideo,
            onDismiss = { cancel() },
            onCalibrationSuccess = { stage = ""; onArm() },
        )
    }
}

@Composable
private fun RideDialogCard(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE5070908))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .background(Color(0xFF131813), RoundedCornerShape(18.dp))
                .border(BorderStroke(1.dp, BorderDivider), RoundedCornerShape(18.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            content = content,
        )
    }
}

@Composable
fun VideoRecordPromptDialog(
    highlightColor: Color,
    onChoice: (record: Boolean, rememberChoice: Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    var rememberChoice by remember { mutableStateOf(false) }
    RideDialogCard {
        Text("Record This Ride?", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = PureWhite, textAlign = TextAlign.Center)
        Text(
            "Capture the ride from your phone camera with the live telemetry overlay burned into the video.",
            fontSize = 13.sp, color = MidGrey, textAlign = TextAlign.Center, lineHeight = 20.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth().clickable { rememberChoice = !rememberChoice },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = rememberChoice,
                onCheckedChange = { rememberChoice = it },
                colors = CheckboxDefaults.colors(checkedColor = highlightColor, uncheckedColor = MutedGrey)
            )
            Spacer(Modifier.width(4.dp))
            Text("Always do this — don't ask again", color = PureWhite, fontSize = 13.sp)
        }
        Button(
            onClick = { onChoice(true, rememberChoice) },
            colors = ButtonDefaults.buttonColors(containerColor = highlightColor),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("YES, RECORD VIDEO", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF070908), letterSpacing = 1.sp)
        }
        Button(
            onClick = { onChoice(false, rememberChoice) },
            colors = ButtonDefaults.buttonColors(containerColor = BorderDivider),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("NO, TELEMETRY ONLY", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PureWhite, letterSpacing = 1.sp)
        }
        TextButton(onClick = onCancel) {
            Text("Cancel", color = MutedGrey, fontSize = 13.sp)
        }
    }
}

@Composable
fun MountReminderDialog(
    highlightColor: Color,
    onOk: () -> Unit,
    onCancel: () -> Unit,
) {
    RideDialogCard {
        Text("Mount Safely First", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = PureWhite, textAlign = TextAlign.Center)
        Text(
            "Make sure your phone is firmly mounted and aimed where you want to film before you set off.\n\nNever adjust the device while riding.",
            fontSize = 13.sp, color = MidGrey, textAlign = TextAlign.Center, lineHeight = 20.sp
        )
        Button(
            onClick = onOk,
            colors = ButtonDefaults.buttonColors(containerColor = highlightColor),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("OK", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF070908), letterSpacing = 1.sp)
        }
        TextButton(onClick = onCancel) {
            Text("Cancel", color = MutedGrey, fontSize = 13.sp)
        }
    }
}

// ── New Design: Dashboard Dial ────────────────────────────────────────────────
@Composable
private fun DashboardDial(lean: Float, highlightColor: Color, modifier: Modifier = Modifier) {
    val animatedLean by animateFloatAsState(
        targetValue = lean,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "leanDial"
    )

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val dialSize = minOf(constraints.maxWidth, constraints.maxHeight).toFloat()

        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = minOf(size.width, size.height) * 0.44f
            val leanRad = Math.toRadians(animatedLean.toDouble())

            // Outer circle
            drawCircle(Color(0xFF1D211B), radius = r, center = Offset(cx, cy), style = Stroke(1.5.dp.toPx()))

            // Tick marks: ±15, ±30, ±45 degrees from top (clockwise = right)
            val ticks = listOf(-45f, -30f, -15f, 0f, 15f, 30f, 45f)
            ticks.forEach { t ->
                val isGreen = abs(t) >= 45f
                val isCenter = t == 0f
                val tickLen = if (isGreen || isCenter) 16.dp.toPx() else 10.dp.toPx()
                val strokeW = if (isGreen) 2.5.dp.toPx() else 2f.dp.toPx()
                val tRad = Math.toRadians(t.toDouble())
                val ox = cx + r * sin(tRad).toFloat()
                val oy = cy - r * cos(tRad).toFloat()
                val ix = cx + (r - tickLen) * sin(tRad).toFloat()
                val iy = cy - (r - tickLen) * cos(tRad).toFloat()
                val col = when {
                    isGreen -> highlightColor
                    isCenter -> Color(0xFF2A3028)
                    else -> Color(0xFF3A4036)
                }
                drawLine(col, Offset(ix, iy), Offset(ox, oy), strokeW)
            }

            // Horizon line (endpoints exactly on circle boundary)
            val hx = r * cos(leanRad).toFloat()
            val hy = r * sin(leanRad).toFloat()
            drawLine(highlightColor, Offset(cx - hx, cy - hy), Offset(cx + hx, cy + hy), 2.2.dp.toPx())

            // Dot at current lean position on circle edge
            val dotX = cx + r * sin(leanRad).toFloat()
            val dotY = cy - r * cos(leanRad).toFloat()
            drawCircle(highlightColor, radius = 5.5.dp.toPx(), center = Offset(dotX, dotY))

            // Degree labels outside circle
            val labelColor45 = android.graphics.Color.argb(255, 74, 96, 64)
            val labelColor = android.graphics.Color.argb(255, 58, 64, 54)
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    textSize = 11.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT
                    isAntiAlias = true
                }
                listOf(-45f to "45", -30f to "30", -15f to "15", 15f to "15", 30f to "30", 45f to "45").forEach { (angle, label) ->
                    val aRad = Math.toRadians(angle.toDouble())
                    val lr = r + 18.dp.toPx()
                    val lx = (cx + lr * sin(aRad)).toFloat()
                    val ly = (cy - lr * cos(aRad)).toFloat() + 4.dp.toPx()
                    paint.color = if (abs(angle) >= 45f) labelColor45 else labelColor
                    canvas.nativeCanvas.drawText(label, lx, ly, paint)
                }
            }
        }

        // Center text overlay
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = (-8).dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = abs(lean).toInt().toString(),
                    fontSize = 96.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PureWhite,
                    fontFamily = Rajdhani,
                    lineHeight = 80.sp,
                    letterSpacing = (-2).sp
                )
                Text(
                    text = "°",
                    fontSize = 38.sp,
                    color = highlightColor,
                    fontFamily = Rajdhani,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (abs(lean) > 1f) (if (lean > 0f) "RIGHT" else "LEFT") else "UPRIGHT",
                fontSize = 11.sp,
                letterSpacing = 3.6.sp,
                color = highlightColor,
                fontFamily = Inter
            )
        }
    }
}

// ── Bottom Navigation Bar ─────────────────────────────────────────────────────
@Composable
private fun DashboardBottomNav(
    activeTab: String,
    highlightColor: Color,
    onDial: () -> Unit,
    onSessions: () -> Unit,
    onSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepCarbon)
            .padding(horizontal = 30.dp, vertical = 10.dp)
            .padding(bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem("DIAL", activeTab == "dial", highlightColor, onDial)
            BottomNavItem("SESSIONS", activeTab == "sessions", highlightColor, onSessions)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onSettings)) {
                Text(
                    text = "⚙",
                    fontSize = 18.sp,
                    color = if (activeTab == "settings") highlightColor else MutedGrey
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(label: String, isActive: Boolean, highlightColor: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Text(
            text = label,
            fontSize = 12.sp,
            letterSpacing = 1.2.sp,
            color = if (isActive) highlightColor else MutedGrey,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            fontFamily = Inter
        )
        if (isActive) {
            Spacer(Modifier.height(3.dp))
            Box(Modifier.width(20.dp).height(1.5.dp).background(highlightColor, RoundedCornerShape(1.dp)))
        }
    }
}

/**
 * GPS status label + color. The update-rate reading is shown only when an external GPS receiver is
 * connected (built-in phone GPS shows just "GPS"); when shown it uses the real measured rate and
 * turns green above 4 Hz.
 */
private fun gpsStatus(gpsActive: Boolean, isExternal: Boolean, hz: Float): Pair<String, Color> = when {
    !gpsActive -> "GPS –" to MutedGrey
    isExternal -> "GPS ${"%.0f".format(hz)}Hz" to (if (hz > 4f) NeonGreen else MutedGrey)
    else -> "GPS" to MutedGrey
}

// ── Dashboard Portrait Layout (Design v3) ─────────────────────────────────────
@Suppress("UNUSED_PARAMETER")
@Composable
fun DashboardPortraitLayout(
    service: TelemetryService,
    isMetric: Boolean,
    highlightColor: Color,
    mutedHighlightColor: Color,
    highlightColorName: String,
    onColorChange: (String) -> Unit,
    onToggleUnit: () -> Unit,
    onShowHistory: () -> Unit,
    onShowSettings: () -> Unit,
    isRecording: Boolean,
    isPaused: Boolean,
    isArmed: Boolean = false,
    startAnimTriggered: Boolean,
    hasPlayedStartupAnimation: Boolean,
    corners: List<CornerEvent>,
    allTimeMaxLeft: Float,
    allTimeMaxRight: Float,
    sessionMaxPitch: Float,
    sessionMaxLeft: Float,
    sessionMaxRight: Float,
    rollingMaxLeft: Float,
    rollingMaxRight: Float,
    rollingMax1000m: Float,
    rollingDistanceTarget: Int,
    onResetMaxLean: () -> Unit = {},
    isCalibrationPendingReset: Boolean = false,
    flickerAlpha: Float = 1f,
    isRideLocked: Boolean = false
) {
    val currentLean by service.currentLean.collectAsStateWithLifecycle()
    val currentSpeed by service.currentSpeed.collectAsStateWithLifecycle()
    val currentLocation by service.currentLocation.collectAsStateWithLifecycle()
    val pastSessions by service.pastSessions.collectAsStateWithLifecycle()

    // Session timer
    var elapsedSecs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isRecording) { if (!isRecording) elapsedSecs = 0L }
    LaunchedEffect(isRecording, isPaused) {
        if (isRecording && !isPaused) {
            while (true) { delay(1000L); if (isRecording && !isPaused) elapsedSecs++ }
        }
    }
    val timerText = remember(elapsedSecs) {
        val h = elapsedSecs / 3600; val m = (elapsedSecs % 3600) / 60; val s = elapsedSecs % 60
        if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    // Clock
    var clockText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val c = Calendar.getInstance()
            clockText = "%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
            delay(30_000L)
        }
    }
    if (clockText.isEmpty()) {
        val c = Calendar.getInstance()
        clockText = "%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
    }

    val isCalibrated by service.isCalibrated.collectAsStateWithLifecycle()
    val gpsUpdateRateHz by service.gpsUpdateRateHz.collectAsStateWithLifecycle()
    val isUsbGpsConnected by service.isUsbGpsConnected.collectAsStateWithLifecycle()
    val gpsAccuracy = currentLocation?.accuracy ?: Float.MAX_VALUE
    val gpsActive = currentLocation != null

    // Reverse-geocode current location to city name
    val geocoderContext = LocalContext.current
    var cityName by remember { mutableStateOf("") }
    LaunchedEffect(currentLocation) {
        val loc = currentLocation ?: return@LaunchedEffect
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val geocoder = android.location.Geocoder(geocoderContext, java.util.Locale.getDefault())
                val results: List<android.location.Address>? =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        kotlin.coroutines.suspendCoroutine { cont: kotlin.coroutines.Continuation<List<android.location.Address>> ->
                            geocoder.getFromLocation(loc.latitude, loc.longitude, 1,
                                object : android.location.Geocoder.GeocodeListener {
                                    override fun onGeocode(addresses: List<android.location.Address>) {
                                        cont.resumeWith(Result.success(addresses))
                                    }
                                    override fun onError(errorMessage: String?) {
                                        cont.resumeWith(Result.success(emptyList()))
                                    }
                                })
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                    }
                val addr = results?.firstOrNull()
                val city = addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea ?: ""
                if (city.isNotEmpty()) kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { cityName = city }
            } catch (_: Exception) {}
        }
    }

    val absMaxLeft = abs(sessionMaxLeft)
    val absMaxRight = sessionMaxRight
    val maxForBar = maxOf(absMaxLeft, absMaxRight, 1f)
    val sessMax = maxOf(abs(sessionMaxLeft), sessionMaxRight)
    val allTimeBest = maxOf(abs(allTimeMaxLeft), allTimeMaxRight)

    // Pulsing dot animation
    val infiniteTransition = rememberInfiniteTransition(label = "recPulse")
    val recAlpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.12f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "recAlpha"
    )

    val context = LocalContext.current
    val activity = context as? AndroidActivity
    var showStopDialog by remember { mutableStateOf(false) }
    var showCalibrationGuide by remember { mutableStateOf(false) }   // pure calibrate (subtitle button)
    var rideStartTrigger by remember { mutableIntStateOf(0) }          // bump to launch the ride-start flow
    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            containerColor = SurfaceCard, titleContentColor = PureWhite, textContentColor = MutedGrey,
            title = { Text("Stop Session?") },
            text = { Text("End and save your ride.") },
            confirmButton = { TextButton(onClick = { showStopDialog = false; service.stopRecording() }) { Text("STOP", color = AlertRed, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { showStopDialog = false }) { Text("CANCEL", color = PureWhite) } }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepCarbon)) {

        Column(modifier = Modifier.fillMaxSize()) {

                // ── Status Bar ────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Canvas(Modifier.size(8.dp)) { drawCircle(if (gpsActive) highlightColor else MutedGrey) }
                        val (gpsLabel, gpsColor) = gpsStatus(gpsActive, isUsbGpsConnected, gpsUpdateRateHz)
                        Text(
                            text = gpsLabel,
                            fontSize = 12.sp, color = gpsColor, fontFamily = Inter
                        )
                    }
                }

                // ── Ko-fi support (after the user's 2nd recorded ride) ─────
                if (pastSessions.size >= 2) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 8.dp)) {
                        KofiButton()
                    }
                }

                // ── Recording Row ─────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .background(if (!isRecording && !isArmed) highlightColor.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(8.dp))
                            .border(if (!isRecording && !isArmed) BorderStroke(1.dp, highlightColor) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .clickable {
                                when {
                                    !isRecording && !isArmed -> rideStartTrigger++
                                    isPaused -> service.startRecording()
                                }
                            }
                    ) {
                        Canvas(Modifier.size(8.dp)) {
                            drawCircle(when {
                                isRecording && !isPaused -> highlightColor.copy(alpha = recAlpha)
                                isRecording -> highlightColor.copy(alpha = 0.4f)
                                isArmed -> highlightColor.copy(alpha = recAlpha)
                                else -> highlightColor
                            })
                        }
                        Text(
                            text = when {
                                isRecording -> timerText
                                isArmed -> "ARMED · Waiting for roll-off"
                                else -> "TAP TO RIDE"
                            },
                            fontSize = if (isArmed) 13.sp else 15.sp,
                            color = if (isRecording) MidGrey else highlightColor,
                            fontFamily = Rajdhani, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (cityName.isNotEmpty()) {
                            Text(cityName, fontSize = 12.sp, color = MutedGrey, letterSpacing = 0.5.sp, fontFamily = Inter)
                        }
                        if (isArmed) {
                            // Cancel armed state — discard before session even starts
                            Box(
                                modifier = Modifier.size(28.dp)
                                    .background(AlertRed.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                    .border(BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f)), RoundedCornerShape(4.dp))
                                    .clickable { service.disarmSession() },
                                contentAlignment = Alignment.Center
                            ) { Text("■", fontSize = 10.sp, color = AlertRed) }
                        } else if (isRecording) {
                            Box(
                                modifier = Modifier.clickable {
                                    android.widget.Toast.makeText(activity, "Screen rotation is locked while a session is recording.", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ScreenRotation,
                                    contentDescription = "Rotation Locked",
                                    tint = Color(0xFF3A4036),
                                    modifier = Modifier.size(16.dp)
                                )
                                Canvas(Modifier.size(16.dp)) {
                                    drawLine(Color(0xFF3A4036), Offset(0f, size.height), Offset(size.width, 0f), strokeWidth = 2.dp.toPx())
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            // Stop button: only active when speed < 10 km/h
                            val stopEnabled = !isRideLocked
                            Box(
                                modifier = Modifier.size(28.dp)
                                    .background(AlertRed.copy(alpha = if (stopEnabled) 0.12f else 0.04f), RoundedCornerShape(4.dp))
                                    .border(BorderStroke(1.dp, AlertRed.copy(alpha = if (stopEnabled) 0.5f else 0.15f)), RoundedCornerShape(4.dp))
                                    .clickable(enabled = stopEnabled) { showStopDialog = true },
                                contentAlignment = Alignment.Center
                            ) { Text("■", fontSize = 10.sp, color = AlertRed.copy(alpha = if (stopEnabled) 1f else 0.25f)) }
                        }
                    }
                }

                // ── Main Dial ─────────────────────────────────────────────
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    DashboardDial(lean = currentLean, highlightColor = highlightColor, modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp))
                }

                // ── Subtitle ──────────────────────────────────────────────
                val accuracyLabel = if (gpsAccuracy < 30f) "GPS Accuracy ±${gpsAccuracy.toInt()}m" else "GPS No fix"
                if (isCalibrated) {
                    Text(
                        text = "CALIBRATION LOCKED",
                        fontSize = 11.sp, letterSpacing = 2.sp,
                        color = MidGrey.copy(alpha = if (isCalibrationPendingReset) flickerAlpha else 1f),
                        fontFamily = Inter,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)
                    )
                    Text(
                        text = accuracyLabel,
                        fontSize = 10.sp, letterSpacing = 1.2.sp,
                        color = MutedGrey,
                        fontFamily = Inter,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 2.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "NOT CALIBRATED",
                            fontSize = 11.sp, letterSpacing = 2.sp,
                            color = AlertRed, fontFamily = Inter
                        )
                        Text(
                            text = "CALIBRATE ›",
                            fontSize = 10.sp, letterSpacing = 1.5.sp,
                            color = AlertRed.copy(alpha = 0.8f), fontFamily = Inter,
                            modifier = Modifier
                                .border(BorderStroke(1.dp, AlertRed.copy(alpha = 0.35f)), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .clickable { showCalibrationGuide = true }
                        )
                    }
                }

                // ── Speed ─────────────────────────────────────────────────
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = if (isMetric) currentSpeed.toInt().toString() else (currentSpeed * 0.621371).toInt().toString(),
                        fontSize = 52.sp, fontWeight = FontWeight.SemiBold, color = PureWhite,
                        fontFamily = Rajdhani, letterSpacing = (-1).sp
                    )
                    Text(
                        text = " ${if (isMetric) "KM/H" else "MPH"}",
                        fontSize = 14.sp, color = MutedGrey, letterSpacing = 1.sp, fontFamily = Inter,
                        modifier = Modifier.padding(bottom = 8.dp, start = 5.dp)
                    )
                }

                // ── Stats Row ─────────────────────────────────────────────
                Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    HorizontalDivider(color = BorderDivider, modifier = Modifier.align(Alignment.TopCenter))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f).padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(corners.size.toString(), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = PureWhite, fontFamily = Rajdhani)
                            Text("CORNERS", fontSize = 10.sp, letterSpacing = 1.6.sp, color = MutedGrey, fontFamily = Inter, modifier = Modifier.padding(top = 3.dp))
                        }
                        Box(modifier = Modifier.width(1.dp).height(50.dp).background(BorderDivider).align(Alignment.CenterVertically))
                        Column(modifier = Modifier.weight(1f).padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(sessMax.toInt().toString(), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = PureWhite, fontFamily = Rajdhani)
                                Text("°", fontSize = 14.sp, color = highlightColor, fontFamily = Rajdhani)
                            }
                            Text("SESS MAX", fontSize = 10.sp, letterSpacing = 1.6.sp, color = MutedGrey, fontFamily = Inter, modifier = Modifier.padding(top = 3.dp))
                        }
                        Box(modifier = Modifier.width(1.dp).height(50.dp).background(BorderDivider).align(Alignment.CenterVertically))
                        Column(modifier = Modifier.weight(1f).padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(allTimeBest.toInt().toString(), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = highlightColor, fontFamily = Rajdhani)
                                Text("°", fontSize = 14.sp, color = MutedGrey, fontFamily = Rajdhani)
                            }
                            Text("ALL-TIME", fontSize = 10.sp, letterSpacing = 1.6.sp, color = MutedGrey, fontFamily = Inter, modifier = Modifier.padding(top = 3.dp))
                        }
                    }
                }

                // ── Lean Symmetry Bar ─────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("L", fontSize = 11.sp, color = MutedGrey, fontFamily = Inter, modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f).height(3.dp).background(BorderDivider, RoundedCornerShape(2.dp))) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth((absMaxLeft / maxForBar).coerceIn(0f, 1f)).align(Alignment.CenterEnd).background(MidGrey, RoundedCornerShape(2.dp)))
                    }
                    Text("${absMaxLeft.toInt()}°", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MidGrey, fontFamily = Rajdhani, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
                    Text("·", fontSize = 14.sp, color = Color(0xFF2A3028))
                    Text("${absMaxRight.toInt()}°", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PureWhite, fontFamily = Rajdhani, modifier = Modifier.width(32.dp))
                    Box(modifier = Modifier.weight(1f).height(3.dp).background(BorderDivider, RoundedCornerShape(2.dp))) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth((absMaxRight / maxForBar).coerceIn(0f, 1f)).background(highlightColor, RoundedCornerShape(2.dp)))
                    }
                    Text("R", fontSize = 11.sp, color = MutedGrey, fontFamily = Inter, modifier = Modifier.width(10.dp), textAlign = TextAlign.End)
                }

                Spacer(Modifier.weight(0.01f))
            }
        // ── Bottom Nav ────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)) {
            DashboardBottomNav(
                activeTab = "dial",
                highlightColor = highlightColor,
                onDial = {},
                onSessions = onShowHistory,
                onSettings = onShowSettings
            )
        }

        // Calibration guide overlay (pure calibrate, triggered from the subtitle button)
        if (showCalibrationGuide) {
            CalibrationGuideOverlay(
                service = service,
                highlightColor = highlightColor,
                onDismiss = { showCalibrationGuide = false }
            )
        }

        // Ride-start flow: video prompt → mount reminder → calibration → arm
        RideStartOverlays(
            service = service,
            highlightColor = highlightColor,
            trigger = rideStartTrigger,
            onArm = { service.armSession() }
        )
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun DashboardLandscapeLayout(
    service: TelemetryService,
    isMetric: Boolean,
    highlightColor: Color,
    mutedHighlightColor: Color,
    highlightColorName: String,
    onColorChange: (String) -> Unit,
    onToggleUnit: () -> Unit,
    onShowHistory: () -> Unit,
    onShowSettings: () -> Unit,
    isRecording: Boolean,
    isPaused: Boolean,
    isArmed: Boolean = false,
    startAnimTriggered: Boolean,
    hasPlayedStartupAnimation: Boolean,
    corners: List<CornerEvent>,
    allTimeMaxLeft: Float,
    allTimeMaxRight: Float,
    sessionMaxPitch: Float,
    sessionMaxLeft: Float,
    sessionMaxRight: Float,
    rollingMaxLeft: Float,
    rollingMaxRight: Float,
    rollingMax1000m: Float,
    rollingDistanceTarget: Int,
    onResetMaxLean: () -> Unit = {},
    isCalibrationPendingReset: Boolean = false,
    flickerAlpha: Float = 1f,
    isRideLocked: Boolean = false
) {
    val currentLean by service.currentLean.collectAsStateWithLifecycle()
    val currentSpeed by service.currentSpeed.collectAsStateWithLifecycle()

    val currentLocation by service.currentLocation.collectAsStateWithLifecycle()
    val pastSessions by service.pastSessions.collectAsStateWithLifecycle()
    val isCalibrated by service.isCalibrated.collectAsStateWithLifecycle()
    val gpsUpdateRateHz by service.gpsUpdateRateHz.collectAsStateWithLifecycle()
    val isUsbGpsConnected by service.isUsbGpsConnected.collectAsStateWithLifecycle()
    val gpsAccuracy = currentLocation?.accuracy ?: Float.MAX_VALUE
    val gpsActive = currentLocation != null
    val rideNumber = pastSessions.size + 1

    val absMaxLeft = abs(sessionMaxLeft)
    val absMaxRight = sessionMaxRight
    val maxForBar = maxOf(absMaxLeft, absMaxRight, 1f)
    val sessMax = maxOf(abs(sessionMaxLeft), sessionMaxRight)
    val allTimeBest = maxOf(abs(allTimeMaxLeft), allTimeMaxRight)

    // Session timer
    var elapsedSecs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isRecording) { if (!isRecording) elapsedSecs = 0L }
    LaunchedEffect(isRecording, isPaused) {
        if (isRecording && !isPaused) {
            while (true) { delay(1000L); if (isRecording && !isPaused) elapsedSecs++ }
        }
    }
    val timerText = remember(elapsedSecs) {
        val h = elapsedSecs / 3600; val m = (elapsedSecs % 3600) / 60; val s = elapsedSecs % 60
        if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    // Clock
    var clockText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val c = Calendar.getInstance()
            clockText = "%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
            delay(30_000L)
        }
    }
    if (clockText.isEmpty()) {
        val c = Calendar.getInstance()
        clockText = "%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
    }

    val infiniteTransition = rememberInfiniteTransition(label = "recPulseL")
    val recAlpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.12f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "recAlphaL"
    )

    val gpsLabel = if (gpsAccuracy < 30f) "GPS ±${gpsAccuracy.toInt()}m" else "IMPROVING SIGNAL"
    val calibLabel = if (isCalibrated) "CALIBRATION LOCKED" else "NOT CALIBRATED"

    val context = LocalContext.current
    val activity = context as? AndroidActivity
    var showStopDialogL by remember { mutableStateOf(false) }
    var showCalibrationGuide by remember { mutableStateOf(false) }   // pure calibrate (subtitle button)
    var rideStartTrigger by remember { mutableIntStateOf(0) }          // bump to launch the ride-start flow

    if (showStopDialogL) {
        AlertDialog(
            onDismissRequest = { showStopDialogL = false },
            containerColor = SurfaceCard, titleContentColor = PureWhite, textContentColor = MutedGrey,
            title = { Text("Stop Session?") },
            confirmButton = { TextButton(onClick = { showStopDialogL = false; service.stopRecording() }) {
                Text("STOP", color = AlertRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showStopDialogL = false }) {
                Text("CANCEL", color = PureWhite) }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Row(modifier = Modifier.fillMaxSize().background(DeepCarbon)) {

        // ── LEFT: Dial half ────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Status bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Canvas(Modifier.size(7.dp)) {
                            drawCircle(if (isRecording && !isPaused) highlightColor.copy(alpha = recAlpha) else if (isRecording) highlightColor.copy(alpha = 0.4f) else MutedGrey)
                        }
                        Text(
                            text = timerText,
                            fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = MidGrey,
                            fontWeight = FontWeight.Medium, letterSpacing = 0.02.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Canvas(Modifier.size(7.dp)) { drawCircle(if (gpsActive) highlightColor else MutedGrey) }
                        val (gpsLabel, gpsColor) = gpsStatus(gpsActive, isUsbGpsConnected, gpsUpdateRateHz)
                        Text(gpsLabel, fontSize = 11.sp, color = gpsColor, fontFamily = Inter)
                        if (isRecording) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier.clickable {
                                    android.widget.Toast.makeText(activity, "Screen rotation is locked while a session is recording.", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ScreenRotation,
                                    contentDescription = "Rotation Locked",
                                    tint = Color(0xFF3A4036),
                                    modifier = Modifier.size(15.dp)
                                )
                                Canvas(Modifier.size(15.dp)) {
                                    drawLine(Color(0xFF3A4036), Offset(0f, size.height), Offset(size.width, 0f), strokeWidth = 2.dp.toPx())
                                }
                            }
                        }
                    }
                }

                // Main dial
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    DashboardDial(lean = currentLean, highlightColor = highlightColor, modifier = Modifier.fillMaxSize())
                }
            }

            // Calibration + GPS label pinned to bottom of left half
            if (isCalibrated) {
                Text(
                    text = "$calibLabel  ·  $gpsLabel",
                    fontSize = 10.sp, letterSpacing = 1.8.sp,
                    color = (if (gpsAccuracy < 30f) MidGrey else MutedGrey).copy(alpha = if (isCalibrationPendingReset && isCalibrated) flickerAlpha else 1f),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                )
            } else {
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "NOT CALIBRATED",
                        fontSize = 10.sp, letterSpacing = 1.8.sp,
                        color = AlertRed, fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "CALIBRATE ›",
                        fontSize = 10.sp, letterSpacing = 1.5.sp,
                        color = AlertRed.copy(alpha = 0.8f), fontFamily = Inter,
                        modifier = Modifier
                            .border(BorderStroke(1.dp, AlertRed.copy(alpha = 0.35f)), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .clickable { showCalibrationGuide = true }
                    )
                }
            }
        }

        // Vertical divider
        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(BorderDivider))

        // ── RIGHT: Data half ───────────────────────────────────────────────
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp)
        ) {
            // Tap-to-start indicator (same style as portrait) — above the speed value
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .background(if (!isRecording) highlightColor.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(8.dp))
                        .border(if (!isRecording) BorderStroke(1.dp, highlightColor) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clickable {
                            when {
                                !isRecording && !isArmed -> rideStartTrigger++
                                isPaused -> service.startRecording()
                            }
                        }
                ) {
                    Canvas(Modifier.size(8.dp)) {
                        drawCircle(when {
                            isRecording && !isPaused -> highlightColor.copy(alpha = recAlpha)
                            isRecording -> highlightColor.copy(alpha = 0.4f)
                            isArmed -> highlightColor.copy(alpha = recAlpha)
                            else -> highlightColor
                        })
                    }
                    Text(
                        text = when {
                            isRecording -> timerText
                            isArmed -> "ARMED · Waiting for roll-off"
                            else -> "TAP TO RIDE"
                        },
                        fontSize = if (isArmed) 13.sp else 15.sp,
                        color = if (isRecording) MidGrey else highlightColor,
                        fontFamily = Rajdhani, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp
                    )
                }

                if (isArmed) {
                    // Cancel armed state before session begins
                    Box(
                        modifier = Modifier.size(34.dp)
                            .background(AlertRed.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                            .clickable { service.disarmSession() },
                        contentAlignment = Alignment.Center
                    ) { Text("■", fontSize = 11.sp, color = AlertRed) }
                } else if (isRecording || isPaused) {
                    val stopEnabled = !isRideLocked
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Pause button: hidden while locked (the 2-tap gesture replaces it)
                        if (!isRideLocked) {
                            OutlinedButton(
                                onClick = { if (isPaused) service.startRecording() else service.pauseRecording() },
                                border = BorderStroke(1.dp, if (isRecording && !isPaused) highlightColor.copy(alpha = 0.5f) else BorderDivider),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isRecording && !isPaused) highlightColor else MutedGrey),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = if (isRecording && !isPaused) "⏸  PAUSE" else "▶  RESUME",
                                    fontSize = 11.sp, letterSpacing = 0.8.sp, fontFamily = Inter
                                )
                            }
                        }
                        Box(
                            modifier = Modifier.size(34.dp)
                                .background(AlertRed.copy(alpha = if (stopEnabled) 0.1f else 0.03f), RoundedCornerShape(8.dp))
                                .border(BorderStroke(1.dp, AlertRed.copy(alpha = if (stopEnabled) 0.5f else 0.15f)), RoundedCornerShape(8.dp))
                                .clickable(enabled = stopEnabled) { showStopDialogL = true },
                            contentAlignment = Alignment.Center
                        ) { Text("■", fontSize = 11.sp, color = AlertRed.copy(alpha = if (stopEnabled) 1f else 0.25f)) }
                    }
                }
            }

            // Speed + Ride #
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isMetric) currentSpeed.toInt().toString() else (currentSpeed * 0.621371).toInt().toString(),
                        fontSize = 64.sp, fontWeight = FontWeight.SemiBold, lineHeight = 54.sp,
                        letterSpacing = (-1.2).sp, color = PureWhite, fontFamily = Rajdhani
                    )
                    Text(
                        text = if (isMetric) "KM/H" else "MPH",
                        fontSize = 12.sp, letterSpacing = 1.2.sp, color = MutedGrey, fontFamily = Inter,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Text(
                    text = "RIDE #$rideNumber",
                    fontSize = 11.sp, letterSpacing = 0.8.sp, color = MutedGrey, fontFamily = Inter,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            HorizontalDivider(color = BorderDivider)

            // 3-stat row
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("CORNERS", fontSize = 10.sp, letterSpacing = 1.6.sp, color = MutedGrey, fontFamily = Inter, modifier = Modifier.padding(bottom = 3.dp))
                    Text(corners.size.toString(), fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = PureWhite, fontFamily = Rajdhani, lineHeight = 28.sp)
                }
                Box(Modifier.width(1.dp).height(44.dp).background(BorderDivider).align(Alignment.CenterVertically))
                Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text("SESS MAX", fontSize = 10.sp, letterSpacing = 1.6.sp, color = MutedGrey, fontFamily = Inter, modifier = Modifier.padding(bottom = 3.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(sessMax.toInt().toString(), fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = PureWhite, fontFamily = Rajdhani, lineHeight = 28.sp)
                        Text("°", fontSize = 14.sp, color = highlightColor, fontFamily = Rajdhani)
                    }
                }
                Box(Modifier.width(1.dp).height(44.dp).background(BorderDivider).align(Alignment.CenterVertically))
                Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                    Text("ALL-TIME", fontSize = 10.sp, letterSpacing = 1.6.sp, color = MutedGrey, fontFamily = Inter, modifier = Modifier.padding(bottom = 3.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(allTimeBest.toInt().toString(), fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = highlightColor, fontFamily = Rajdhani, lineHeight = 28.sp)
                        Text("°", fontSize = 14.sp, color = MutedGrey, fontFamily = Rajdhani)
                    }
                }
            }
            HorizontalDivider(color = BorderDivider)

            // L/R symmetry bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("L", fontSize = 11.sp, color = MutedGrey, fontFamily = Inter, modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f).height(3.dp).background(BorderDivider, RoundedCornerShape(2.dp))) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth((absMaxLeft / maxForBar).coerceIn(0f, 1f)).align(Alignment.CenterEnd).background(MidGrey, RoundedCornerShape(2.dp)))
                }
                Text("${absMaxLeft.toInt()}°", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MidGrey, fontFamily = Rajdhani, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
                Text("·", fontSize = 14.sp, color = Color(0xFF2A3028))
                Text("${absMaxRight.toInt()}°", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PureWhite, fontFamily = Rajdhani, modifier = Modifier.width(32.dp))
                Box(modifier = Modifier.weight(1f).height(3.dp).background(BorderDivider, RoundedCornerShape(2.dp))) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth((absMaxRight / maxForBar).coerceIn(0f, 1f)).background(highlightColor, RoundedCornerShape(2.dp)))
                }
                Text("R", fontSize = 11.sp, color = MutedGrey, fontFamily = Inter, modifier = Modifier.width(10.dp), textAlign = TextAlign.End)
            }
            HorizontalDivider(color = BorderDivider)

            // Push nav to bottom
            Spacer(modifier = Modifier.weight(1f))

            // Bottom nav
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // DIAL — active
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DIAL", fontSize = 11.sp, letterSpacing = 1.2.sp, color = highlightColor, fontFamily = Inter, modifier = Modifier.padding(bottom = 2.dp))
                    Box(Modifier.height(1.5.dp).width(28.dp).background(highlightColor))
                }
                // SESSIONS
                Text("SESSIONS", fontSize = 11.sp, letterSpacing = 1.2.sp, color = MutedGrey, fontFamily = Inter,
                    modifier = Modifier.clickable { onShowHistory() })
                // Settings
                Text("⚙", fontSize = 15.sp, color = MutedGrey, modifier = Modifier.clickable { onShowSettings() })
            }
        }
    }

        // Calibration guide overlay (pure calibrate, triggered from the NOT CALIBRATED button)
        if (showCalibrationGuide) {
            CalibrationGuideOverlay(
                service = service,
                highlightColor = highlightColor,
                onDismiss = { showCalibrationGuide = false }
            )
        }

        // Ride-start flow: video prompt → mount reminder → calibration → arm
        RideStartOverlays(
            service = service,
            highlightColor = highlightColor,
            trigger = rideStartTrigger,
            onArm = { service.armSession() }
        )

        // Ko-fi support (after the user's 2nd recorded ride), pinned to the top-right corner
        if (pastSessions.size >= 2) {
            KofiButton(Modifier.align(Alignment.TopEnd).padding(12.dp))
        }
    }
}



@Composable
fun StartupAnimatedElement(
    order: Int,
    triggered: Boolean,
    modifier: Modifier = Modifier,
    skipAnimation: Boolean = false,
    content: @Composable () -> Unit
) {
    if (skipAnimation) {
        Box(modifier = modifier) { content() }
        return
    }

    val duration = 1400
    val randomDelay = remember { (order * 150) + (0..300).random() }
    
    val alphaAnim = animateFloatAsState(
        targetValue = if (triggered) 1f else 0f,
        animationSpec = tween(
            durationMillis = 200,
            delayMillis = randomDelay,
            easing = LinearEasing
        ),
        label = "alpha"
    )

    val scaleAnim = animateFloatAsState(
        targetValue = if (triggered) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    // Flickering effect during startup
    val infiniteTransition = rememberInfiniteTransition(label = "flicker")
    val flickerAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (50..150).random(), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flickerAlpha"
    )

    val currentTime = remember { System.currentTimeMillis() }
    var isFlickeringActive by remember { mutableStateOf(true) }
    
    LaunchedEffect(triggered) {
        if (triggered) {
            delay((randomDelay + duration).milliseconds)
            isFlickeringActive = false
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = if (isFlickeringActive && triggered) {
                    if (System.currentTimeMillis() - currentTime < randomDelay) 0f
                    else flickerAlpha * alphaAnim.value
                } else {
                    alphaAnim.value
                }
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
            }
    ) {
        content()
    }
}


@Composable
fun ControlButtonsContentV3(
    isRecording: Boolean,
    isPaused: Boolean,
    highlightColor: Color,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    isMapMode: Boolean = false
) {
    val buttonAlpha = if (isMapMode) 0.8f else 1.0f

    var showStopDialog by remember { mutableStateOf(false) }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("Stop Recording") },
            text = { Text("Are you sure you want to stop recording?") },
            confirmButton = {
                TextButton(onClick = {
                    showStopDialog = false
                    onStop()
                }) {
                    Text("Stop", color = AlertRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text("Cancel", color = PureWhite)
                }
            },
            containerColor = SurfaceCard,
            titleContentColor = PureWhite,
            textContentColor = MutedGrey
        )
    }

    Row(
        modifier = modifier.fillMaxWidth().height(72.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = {
                if (!isRecording || isPaused) onStart()
                else onPause()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording && !isPaused) AlertRed.copy(alpha = buttonAlpha) else Color.Black,
                contentColor = if (isRecording && !isPaused) Color.Black else highlightColor
            ),
            modifier = Modifier.fillMaxHeight().weight(1.4f),
            contentPadding = PaddingValues(horizontal = 8.dp),
            shape = RoundedCornerShape(16.dp),
            border = if (isRecording && !isPaused) BorderStroke(1.5.dp, Color.Black.copy(alpha = 0.3f)) else BorderStroke(2.5.dp, highlightColor)
        ) {
            Text(
                text = if (isRecording && !isPaused) "PAUSE SESSION" else if (isPaused) "RESUME SESSION" else "START SESSION",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = Inter,
                    color = if (isRecording && !isPaused) Color.Black else highlightColor,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                ),
                maxLines = 1,
                softWrap = false
            )
        }

        OutlinedButton(
            onClick = {
                if (isRecording) {
                    showStopDialog = true
                } else {
                    onStop()
                }
            },
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = DeepCarbon.copy(alpha = buttonAlpha),
                contentColor = AlertRed
            ),
            border = BorderStroke(2.5.dp, AlertRed),
            modifier = Modifier.fillMaxHeight().weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "STOP",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun LeanHorizonIndicator(
    lean: Float,
    speed: Double,
    highlightColor: Color,
    modifier: Modifier = Modifier,
    sizeScale: Float = 1.0f,
    yOffsetPercent: Float = 0.0f
) {
    val currentSpeedState = rememberUpdatedState(speed)
    var pathOffset by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(Unit) {
        var lastTime = withFrameMillis { it }
        while (true) {
            val currentTime = withFrameMillis { it }
            val dt = (currentTime - lastTime) / 1000f
            lastTime = currentTime
            
            val s = currentSpeedState.value.toFloat()
            if (s > 0.5f) {
                pathOffset += (s * 15f * dt)
                if (pathOffset > 100000f) pathOffset -= 100000f
            }
        }
    }

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        val density = LocalDensity.current
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

        // Base measurements in pixels
        val sizeWidthPx = constraints.maxWidth.toFloat()
        val sizeHeightPx = constraints.maxHeight.toFloat()
        // In landscape, height is always the limiting dimension (determines visible arc height).
        // In portrait, use the shorter side as usual.
        val limitingDimPx = if (isLandscape) sizeHeightPx
                            else if (sizeWidthPx < sizeHeightPx) sizeWidthPx else sizeHeightPx

        val baseMarkingRadius = (limitingDimPx / 2f) * 0.98f
        val baseTickLength = with(density) { 12.dp.toPx() }
        val baseLabelPadding = with(density) { 10.dp.toPx() }
        val baseLabelRadius = baseMarkingRadius + baseTickLength + baseLabelPadding

        // Center of dial/horizon line
        val calculatedCenterY = if (isLandscape) {
            sizeHeightPx + (sizeHeightPx * 0.10f) + with(density) { 12.dp.toPx() } - with(density) { 80.dp.toPx() }
        } else {
            sizeHeightPx * 0.99f - (sizeHeightPx * yOffsetPercent)
        }

        val backgroundCenterY = if (isLandscape) {
            calculatedCenterY + (sizeHeightPx * 0.20f)
        } else {
            calculatedCenterY
        }

        // Calculate dynamic optimal scale to use available space and prevent cutting off elements at the top/sides
        val paddingY = with(density) { 24.dp.toPx() }
        val paddingX = with(density) { 16.dp.toPx() }

        val maxSFromHeight = if (calculatedCenterY > paddingY) {
            (calculatedCenterY - paddingY) / baseLabelRadius
        } else {
            1.0f
        }

        val maxSFromWidth = ((limitingDimPx / 2f) - paddingX) / (baseLabelRadius * 0.7071f)

        // Select the ideal scale based on constraints, allowing scaling up to fill available space
        val optimalScale = minOf(maxSFromHeight, maxSFromWidth).coerceIn(1.0f, 3.0f)
        val finalScale = optimalScale * sizeScale
        val backgroundScale = if (isLandscape) finalScale * 1.3225f else finalScale * 0.92f

        // Use optimal scale to size the bike and indicator dial
        val bikeScale = if (isLandscape) finalScale else finalScale * 0.7f
        val bikeWidth = with(density) { limitingDimPx.toDp() } * 1.25f * bikeScale
        val bikeHeight = bikeWidth * (1536f / 2816f)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, backgroundCenterY)
            val markingRadius = baseMarkingRadius * backgroundScale
            val tickLength = baseTickLength * backgroundScale

            rotate(degrees = -lean, pivot = center) {
                // Ground
                drawRect(
                    color = Color(0xFF181818), // Dark grey ground
                    topLeft = Offset(center.x - 5000f, center.y),
                    size = androidx.compose.ui.geometry.Size(10000f, 5000f)
                )

                // Street background and cyan horizon line removed as per request

                val markingAngles = listOf(0f, 15f, 30f, 45f)
                markingAngles.forEach { angle ->
                    listOf(270f - angle, 270f + angle).distinct().forEach { a ->
                        val rad = Math.toRadians(a.toDouble()).toFloat()
                        val isZero = angle == 0f
                        val currentTickLength = if (isZero) tickLength * 1.5f else tickLength
                        val currentStrokeWidth = if (isZero) 9.dp.toPx() * backgroundScale else 3.dp.toPx() * backgroundScale
                        val currentAlpha = 1f

                        drawLine(
                            color = PureWhite.copy(alpha = currentAlpha),
                            start = Offset(center.x + (markingRadius - currentTickLength / 2) * cos(rad), center.y + (markingRadius - currentTickLength / 2) * sin(rad)),
                            end = Offset(center.x + (markingRadius + currentTickLength / 2) * cos(rad), center.y + (markingRadius + currentTickLength / 2) * sin(rad)),
                            strokeWidth = currentStrokeWidth
                        )

                        if (angle == 45f || angle == 30f || angle == 15f) {
                            drawIntoCanvas { canvas ->
                                val paint = Paint().apply {
                                    color = PureWhite.toArgb()
                                    textSize = 18.sp.toPx() * backgroundScale
                                    textAlign = Paint.Align.CENTER
                                    typeface = Typeface.DEFAULT
                                }
                                val labelRadius = markingRadius + tickLength + (10.dp.toPx() * backgroundScale)
                                val lx = center.x + labelRadius * cos(rad)
                                val ly = center.y + labelRadius * sin(rad) + (paint.textSize / 3)
                                val labelText = "${angle.toInt()}°"

                                canvas.nativeCanvas.save()
                                canvas.nativeCanvas.rotate(a - 270f, lx, ly)
                                canvas.nativeCanvas.drawText(labelText, lx, ly, paint)
                                canvas.nativeCanvas.restore()
                            }
                        }
                    }
                }
            }

            // Draw highlightColor dot for current lean angle on the dial (placed below the indicator circle line)
            val dotRad = Math.toRadians(270.0).toFloat()
            val dotRadius = markingRadius - (12.dp.toPx() * backgroundScale)
            drawCircle(
                color = highlightColor,
                radius = 6.dp.toPx() * backgroundScale,
                center = Offset(center.x + dotRadius * cos(dotRad), center.y + dotRadius * sin(dotRad))
            )

            // Draw a visible cyan point at the center of the lean rotation
            drawCircle(
                color = Color.Cyan,
                radius = 6.dp.toPx() * backgroundScale,
                center = center
            )
        }

        val offsetDp = remember(density, bikeHeight, sizeHeightPx, calculatedCenterY, isLandscape) {
            with(density) {
                val shiftY = if (isLandscape) 0f else (sizeHeightPx * 0.20f)
                val offsetY = calculatedCenterY - sizeHeightPx + (bikeHeight.toPx() / 2f) - shiftY
                offsetY.toDp()
            }
        }

        Image(
            painter = painterResource(id = R.drawable.bikeindicatorv2),
            contentDescription = "Bike Indicator",
            colorFilter = ColorFilter.tint(PureWhite),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .requiredSize(width = bikeWidth, height = bikeHeight)
                .offset(y = offsetDp)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
        )
    }
}

@Composable
fun HistoryRow(
    label: String,
    value: String,
    color: Color,
    compact: Boolean = false,
    onReset: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (compact) MaterialTheme.typography.bodyLarge.copy(fontSize = 11.sp) else MaterialTheme.typography.bodyLarge,
            color = MutedGrey,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = Rajdhani,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 11.sp else 15.sp
            ),
            color = color,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.weight(if (compact) 1.8f else 0.8f),
            textAlign = TextAlign.End
        )
        if (onReset != null) {
            IconButton(
                onClick = onReset,
                modifier = Modifier.size(if (compact) 22.dp else 30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = MutedCyan,
                    modifier = Modifier.size(if (compact) 12.dp else 16.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(if (compact) 22.dp else 30.dp))
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String = "",
    color: Color = PureWhite,
    containerColor: Color = SurfaceCard,
    weight: FontWeight = FontWeight.Bold,
    height: androidx.compose.ui.unit.Dp = 80.dp,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .height(height)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, BorderDivider),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (onClick != null) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select Metric",
                    tint = MutedGrey.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(if (height < 70.dp) 16.dp else 24.dp)
                )
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = if (height < 70.dp) 10.sp else 12.sp),
                    color = MutedGrey,
                    maxLines = 1,
                    softWrap = false
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = if (height < 70.dp) 18.sp else 24.sp,
                            fontWeight = weight,
                            fontFamily = Rajdhani
                        ),
                        color = color,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (unit.isNotEmpty()) {
                        Text(
                            text = unit,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = if (height < 70.dp) 8.sp else 9.sp),
                            color = MutedGrey,
                            modifier = Modifier.padding(bottom = if (height < 70.dp) 2.dp else 4.dp, start = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
