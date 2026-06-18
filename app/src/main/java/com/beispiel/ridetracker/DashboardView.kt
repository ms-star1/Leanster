package com.beispiel.ridetracker

import android.app.Activity as AndroidActivity
import android.content.pm.ActivityInfo
import android.graphics.Paint
import android.graphics.Typeface
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.edit
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
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
import com.beispiel.ridetracker.ui.theme.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun DashboardPortraitPreview() {
    RideTrackerTheme {
        Box(modifier = Modifier.fillMaxSize().background(DeepBase)) {
            DashboardViewContentMock()
        }
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 400)
@Composable
fun DashboardLandscapePreview() {
    RideTrackerTheme {
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
    val activeView = "lean"
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
            mapView = null,
            activeView = activeView,
            onActiveViewChange = {},
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
            rollingMax1000m = 0f
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
            mapView = null,
            activeView = activeView,
            onActiveViewChange = {},
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
            rollingMax1000m = 0f
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
    mapView: org.maplibre.android.maps.MapView,
    hasPlayedStartupAnimation: Boolean = false,
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
    val isRecording by service.isRecording.collectAsStateWithLifecycle()
    val isPaused by service.isPaused.collectAsStateWithLifecycle()

    var activeView by remember { mutableStateOf("lean") } // "lean" or "map"
    val context = LocalContext.current

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBase)
    ) {
        // Map as Background
        if (activeView == "map") {
            MapContent(service, mapView)
        }

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
                mapView = mapView,
                activeView = activeView,
                onActiveViewChange = { activeView = it },
                isRecording = isRecording,
                isPaused = isPaused,
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
                rollingMax1000m = rollingMax1000m
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
                mapView = mapView,
                activeView = activeView,
                onActiveViewChange = { activeView = it },
                isRecording = isRecording,
                isPaused = isPaused,
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
                rollingMax1000m = rollingMax1000m
            )
        }
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
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBase)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SETTINGS",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
                    color = highlightColor
                )
                IconButton(onClick = onBack) {
                    Text("✕", color = PureWhite, style = MaterialTheme.typography.titleLarge)
                }
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 12.dp else 24.dp))

            val scrollState1 = rememberScrollState()
            val scrollState2 = rememberScrollState()
            val scrollStateSingle = rememberScrollState()

            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Left Column: Color & Unit System
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState1),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("HIGHLIGHT COLOR", style = MaterialTheme.typography.titleMedium, color = highlightColor)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Cyan", "Pure White", "Kawasaki Green", "Ducati Red", "Yamaha Blue").forEach { colorName ->
                                val color = when(colorName) {
                                    "Yamaha Blue" -> YamahaBlue
                                    "Ducati Red" -> DucatiRed
                                    "Kawasaki Green" -> KawasakiGreen
                                    "Pure White" -> PureWhiteHighlight
                                    else -> NeonCyan
                                }
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(color, RoundedCornerShape(20.dp))
                                        .clickable { onColorChange(colorName) }
                                        .padding(4.dp)
                                ) {
                                    if (highlightColorName == colorName) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(DeepCarbon, RoundedCornerShape(20.dp))
                                                .padding(4.dp)
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize().background(color, RoundedCornerShape(20.dp)))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("UNIT SYSTEM", style = MaterialTheme.typography.titleMedium, color = highlightColor)
                        Row(
                            modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { if (!isMetric) onToggleUnit() }
                                            .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isMetric,
                                onClick = { if (!isMetric) onToggleUnit() },
                                colors = RadioButtonDefaults.colors(selectedColor = highlightColor, unselectedColor = MutedGrey)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Metric (km/h)", color = PureWhite, style = MaterialTheme.typography.bodyLarge)
                        }
                        Row(
                            modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { if (isMetric) onToggleUnit() }
                                            .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = !isMetric,
                                onClick = { if (isMetric) onToggleUnit() },
                                colors = RadioButtonDefaults.colors(selectedColor = highlightColor, unselectedColor = MutedGrey)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Imperial (mph)", color = PureWhite, style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    // Right Column: Launch Orientation & All-Time Stats
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState2),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("LAUNCH ORIENTATION", style = MaterialTheme.typography.titleMedium, color = highlightColor)
                        val prefs = remember { androidx.preference.PreferenceManager.getDefaultSharedPreferences(context) }
                        var defaultLaunchMode by remember { mutableStateOf(prefs.getString("default_launch_mode", "Portrait") ?: "Portrait") }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (defaultLaunchMode != "Portrait") {
                                        defaultLaunchMode = "Portrait"
                                        prefs.edit { putString("default_launch_mode", "Portrait") }
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = defaultLaunchMode == "Portrait",
                                onClick = {
                                    defaultLaunchMode = "Portrait"
                                    prefs.edit { putString("default_launch_mode", "Portrait") }
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = highlightColor, unselectedColor = MutedGrey)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Portrait Mode", color = PureWhite, style = MaterialTheme.typography.bodyLarge)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (defaultLaunchMode != "Landscape") {
                                        defaultLaunchMode = "Landscape"
                                        prefs.edit { putString("default_launch_mode", "Landscape") }
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = defaultLaunchMode == "Landscape",
                                onClick = {
                                    defaultLaunchMode = "Landscape"
                                    prefs.edit { putString("default_launch_mode", "Landscape") }
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = highlightColor, unselectedColor = MutedGrey)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Horizontal Mode", color = PureWhite, style = MaterialTheme.typography.bodyLarge)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("ALL-TIME STATS", style = MaterialTheme.typography.titleMedium, color = highlightColor)
                        Button(
                            onClick = { service.resetAllTimeLean() },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertRed, contentColor = PureWhite),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("RESET ALL-TIME MAX LEAN", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Portrait Layout (Single Column)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollStateSingle),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("HIGHLIGHT COLOR", style = MaterialTheme.typography.titleMedium, color = highlightColor)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Cyan", "Pure White", "Kawasaki Green", "Ducati Red", "Yamaha Blue").forEach { colorName ->
                            val color = when(colorName) {
                                "Yamaha Blue" -> YamahaBlue
                                "Ducati Red" -> DucatiRed
                                "Kawasaki Green" -> KawasakiGreen
                                "Pure White" -> PureWhiteHighlight
                                else -> NeonCyan
                            }
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(color, RoundedCornerShape(21.dp))
                                    .clickable { onColorChange(colorName) }
                                    .padding(4.dp)
                            ) {
                                if (highlightColorName == colorName) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(DeepCarbon, RoundedCornerShape(21.dp))
                                            .padding(4.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize().background(color, RoundedCornerShape(21.dp)))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("UNIT SYSTEM", style = MaterialTheme.typography.titleMedium, color = highlightColor)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (!isMetric) onToggleUnit() }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isMetric,
                            onClick = { if (!isMetric) onToggleUnit() },
                            colors = RadioButtonDefaults.colors(selectedColor = highlightColor, unselectedColor = MutedGrey)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Metric (km/h)", color = PureWhite, style = MaterialTheme.typography.bodyLarge)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (isMetric) onToggleUnit() }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !isMetric,
                            onClick = { if (isMetric) onToggleUnit() },
                            colors = RadioButtonDefaults.colors(selectedColor = highlightColor, unselectedColor = MutedGrey)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Imperial (mph)", color = PureWhite, style = MaterialTheme.typography.bodyLarge)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("LAUNCH ORIENTATION", style = MaterialTheme.typography.titleMedium, color = highlightColor)
                    val prefs = remember { androidx.preference.PreferenceManager.getDefaultSharedPreferences(context) }
                    var defaultLaunchMode by remember { mutableStateOf(prefs.getString("default_launch_mode", "Portrait") ?: "Portrait") }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (defaultLaunchMode != "Portrait") {
                                    defaultLaunchMode = "Portrait"
                                    prefs.edit { putString("default_launch_mode", "Portrait") }
                                }
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = defaultLaunchMode == "Portrait",
                            onClick = {
                                defaultLaunchMode = "Portrait"
                                prefs.edit { putString("default_launch_mode", "Portrait") }
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = highlightColor, unselectedColor = MutedGrey)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Portrait Mode", color = PureWhite, style = MaterialTheme.typography.bodyLarge)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (defaultLaunchMode != "Landscape") {
                                    defaultLaunchMode = "Landscape"
                                    prefs.edit { putString("default_launch_mode", "Landscape") }
                                }
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = defaultLaunchMode == "Landscape",
                            onClick = {
                                defaultLaunchMode = "Landscape"
                                prefs.edit { putString("default_launch_mode", "Landscape") }
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = highlightColor, unselectedColor = MutedGrey)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Horizontal Mode", color = PureWhite, style = MaterialTheme.typography.bodyLarge)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("ALL-TIME STATS", style = MaterialTheme.typography.titleMedium, color = highlightColor)
                    Button(
                        onClick = { service.resetAllTimeLean() },
                        colors = ButtonDefaults.buttonColors(containerColor = AlertRed, contentColor = PureWhite),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("RESET ALL-TIME MAX LEAN", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = highlightColor, contentColor = DeepCarbon),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("SAVE & CLOSE", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

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
    mapView: org.maplibre.android.maps.MapView?,
    activeView: String,
    onActiveViewChange: (String) -> Unit,
    isRecording: Boolean,
    isPaused: Boolean,
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
    rollingMax1000m: Float
) {
    val currentLean by service.currentLean.collectAsStateWithLifecycle()
    val currentSpeed by service.currentSpeed.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Metric Row
        val absLeanVal = abs(currentLean)
        val leanProgress = ((absLeanVal - 30f) / 10f).coerceIn(0f, 1f)
        val max1000mProgress = ((rollingMax1000m - 30f) / 10f).coerceIn(0f, 1f)

        // Unified Telemetry Top Row (LEAN, SPEED, MAX 1000M)
        Row(
            modifier = Modifier.fillMaxWidth().height(90.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lean Card
            StartupAnimatedElement(order = 0, triggered = startAnimTriggered, skipAnimation = hasPlayedStartupAnimation, modifier = Modifier.weight(1.0f).fillMaxHeight()) {
                MetricCard(
                    title = "LEAN",
                    value = absLeanVal.toInt().toString(),
                    unit = "°",
                    color = PureWhite,
                    containerColor = if (activeView == "map") lerp(SurfaceCard, AlertRed, leanProgress).copy(alpha = 0.8f) else lerp(SurfaceCard, AlertRed, leanProgress),
                    modifier = Modifier.fillMaxHeight(),
                    weight = FontWeight(700 + (leanProgress * 200).toInt())
                )
            }

            // Speedometer Card (Middle)
            StartupAnimatedElement(order = 1, triggered = startAnimTriggered, skipAnimation = hasPlayedStartupAnimation, modifier = Modifier.weight(1.3f).fillMaxHeight()) {
                Card(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clickable { onToggleUnit() },
                    colors = CardDefaults.cardColors(containerColor = if (activeView == "map") SurfaceCard.copy(alpha = 0.8f) else SurfaceCard),
                    border = BorderStroke(1.5.dp, highlightColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SPEED",
                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = MutedGrey
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isMetric) currentSpeed.toInt().toString() else (currentSpeed * 0.621371).toInt().toString(),
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp, color = highlightColor),
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = if (isMetric) "km/h" else "mph",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = MutedGrey),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }
            }

            // Max 1000m Card
            StartupAnimatedElement(order = 2, triggered = startAnimTriggered, skipAnimation = hasPlayedStartupAnimation, modifier = Modifier.weight(1.0f).fillMaxHeight()) {
                MetricCard(
                    title = "MAX 1000m",
                    value = rollingMax1000m.toInt().toString(),
                    unit = "°",
                    color = PureWhite,
                    containerColor = if (activeView == "map") lerp(SurfaceCard, AlertRed, max1000mProgress).copy(alpha = 0.8f) else lerp(SurfaceCard, AlertRed, max1000mProgress),
                    modifier = Modifier.fillMaxHeight(),
                    weight = FontWeight(700 + (max1000mProgress * 200).toInt())
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Toggle Buttons for View
        StartupAnimatedElement(order = 3, triggered = startAnimTriggered, skipAnimation = hasPlayedStartupAnimation) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onActiveViewChange("lean") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeView == "lean") highlightColor else SurfaceCard.copy(alpha = if (activeView == "map") 0.8f else 1.0f),
                        contentColor = if (activeView == "lean") DeepCarbon else PureWhite
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    border = if (activeView == "lean") null else BorderStroke(1.dp, BorderDivider)
                ) {
                    Text("Lean-o-Meter", style = MaterialTheme.typography.labelLarge)
                }
                Button(
                    onClick = { onActiveViewChange("map") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeView == "map") highlightColor else SurfaceCard.copy(alpha = 0.8f),
                        contentColor = if (activeView == "map") DeepCarbon else PureWhite
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    border = if (activeView == "map") null else BorderStroke(1.dp, BorderDivider)
                ) {
                    Text("Map view", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main Display (Horizon or Map Spacer)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (activeView == "lean") {
                StartupAnimatedElement(order = 4, triggered = startAnimTriggered, skipAnimation = hasPlayedStartupAnimation, modifier = Modifier.fillMaxSize()) {
                    LeanHorizonIndicator(
                        lean = currentLean,
                        highlightColor = highlightColor,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (activeView == "lean") {
            Spacer(modifier = Modifier.height(16.dp))

            // History Data Window
            StartupAnimatedElement(order = 5, triggered = startAnimTriggered, skipAnimation = hasPlayedStartupAnimation) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (activeView == "map") SurfaceCard.copy(alpha = 0.8f) else SurfaceCard),
                    border = BorderStroke(1.dp, BorderDivider),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) {
                        Text(
                            text = "SESSION",
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = Inter),
                            color = highlightColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        HistoryRow("Max Lean Session", "L: ${abs(sessionMaxLeft).toInt()}° / R: ${sessionMaxRight.toInt()}°", PureWhite) { service.resetSessionLean() }
                        HistoryRow("Corners Driven", "${corners.size}", PureWhite) { service.resetCornerCount() }
                        HistoryRow("Wheelie Angle", "${sessionMaxPitch.toInt()}°", PureWhite) { service.resetSessionPitch() }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // All Time Data Window
            StartupAnimatedElement(order = 6, triggered = startAnimTriggered, skipAnimation = hasPlayedStartupAnimation) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (activeView == "map") SurfaceCard.copy(alpha = 0.8f) else SurfaceCard),
                    border = BorderStroke(1.dp, BorderDivider),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) {
                        Text(
                            text = "ALL TIME DATA",
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = Inter),
                            color = highlightColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        HistoryRow("Max Lean", "L: ${abs(allTimeMaxLeft).toInt()}° / R: ${allTimeMaxRight.toInt()}°", PureWhite)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Control Buttons
        StartupAnimatedElement(order = 7, triggered = startAnimTriggered, skipAnimation = hasPlayedStartupAnimation) {
            ControlButtonsContentV3(
                isRecording = isRecording,
                isPaused = isPaused,
                highlightColor = highlightColor,
                onStart = { service.startRecording() },
                onPause = { service.pauseRecording() },
                onCalibrate = { service.calibrateSensors() },
                onStop = { service.stopRecording() },
                onShowHistory = onShowHistory,
                onShowSettings = onShowSettings,
                isMapMode = activeView == "map"
            )
        }
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
    mapView: org.maplibre.android.maps.MapView?,
    activeView: String,
    onActiveViewChange: (String) -> Unit,
    isRecording: Boolean,
    isPaused: Boolean,
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
    rollingMax1000m: Float
) {
    val currentLean by service.currentLean.collectAsStateWithLifecycle()
    val currentSpeed by service.currentSpeed.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Column: All buttons and toggles (width reduced by 30%: weight from 1.0f to 0.7f)
            Column(
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxHeight()
                    .zIndex(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // View toggles at top of Left Column
                Row(
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onActiveViewChange("lean") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeView == "lean") highlightColor else SurfaceCard,
                            contentColor = if (activeView == "lean") DeepCarbon else PureWhite
                        ),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Horizon",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                    Button(
                        onClick = { onActiveViewChange("map") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeView == "map") highlightColor else SurfaceCard,
                            contentColor = if (activeView == "map") DeepCarbon else PureWhite
                        ),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Map",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Stop button state and dialog
                var showStopDialog by remember { mutableStateOf(false) }
                if (showStopDialog) {
                    AlertDialog(
                        onDismissRequest = { showStopDialog = false },
                        title = { Text("Stop Recording") },
                        text = { Text("Are you sure you want to stop recording?") },
                        confirmButton = {
                            TextButton(onClick = {
                                showStopDialog = false
                                service.stopRecording()
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

                // Session control button
                Button(
                    onClick = {
                        if (!isRecording || isPaused) service.startRecording()
                        else service.pauseRecording()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = highlightColor,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isRecording && !isPaused) "PAUSE" else if (isPaused) "RESUME" else "START",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = Inter, color = Color.Black)
                        ,
                        maxLines = 2,
                        softWrap = true
                    )
                }

                if (isRecording) {
                    OutlinedButton(
                        onClick = { showStopDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = DeepCarbon,
                            contentColor = highlightColor
                        ),
                        border = BorderStroke(2.dp, highlightColor),
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "STOP\nSESSION",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = Inter,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center
                            ),
                            maxLines = 2,
                            softWrap = true
                        )
                    }
                }

                // Calibrate & History
                Row(
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { service.calibrateSensors() },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderDivider)
                    ) {
                        Text(
                            text = "Calibrate",
                            style = MaterialTheme.typography.labelSmall.copy(color = MutedGrey, fontSize = 9.sp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Button(
                        onClick = onShowHistory,
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderDivider)
                    ) {
                        Text(
                            text = "History",
                            style = MaterialTheme.typography.labelSmall.copy(color = MutedGrey, fontSize = 9.sp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Settings & Rotate at the very bottom
                Row(
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onShowSettings,
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderDivider)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MutedGrey,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    val context = LocalContext.current
                    Button(
                        onClick = {
                            val activity = context as? AndroidActivity
                            val newOrientation = if (activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            }
                            activity?.requestedOrientation = newOrientation
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderDivider)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ScreenRotation,
                            contentDescription = "Rotate Screen",
                            tint = highlightColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Central space where indicator is shown
            if (activeView == "lean") {
                LeanHorizonIndicator(
                    lean = currentLean,
                    highlightColor = highlightColor,
                    modifier = Modifier
                        .weight(1.8f)
                        .fillMaxHeight(),
                    sizeScale = 1.0f,
                    yOffsetPercent = 0.0f
                )
            } else {
                Spacer(modifier = Modifier.weight(1.8f))
            }

            // Right Column: All stats / data (weight remaining 1.0f)
            Column(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxHeight()
                    .zIndex(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Oversized Speedometer Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(105.dp)
                        .clickable { onToggleUnit() },
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.5.dp, highlightColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SPEEDOMETER",
                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold),
                            color = MutedGrey
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isMetric) currentSpeed.toInt().toString() else (currentSpeed * 0.621371).toInt().toString(),
                                style = MaterialTheme.typography.displayMedium.copy(fontSize = 46.sp, color = highlightColor),
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isMetric) "km/h" else "mph",
                                style = MaterialTheme.typography.labelMedium.copy(color = MutedGrey),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }
                }

                // Row for LEAN and 1000m MAX LEAN
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val absLeanVal = abs(currentLean)
                    val leanProgress = ((absLeanVal - 30f) / 10f).coerceIn(0f, 1f)
                    val max1000mProgress = ((rollingMax1000m - 30f) / 10f).coerceIn(0f, 1f)

                    MetricCard(
                        title = "LEAN",
                        value = absLeanVal.toInt().toString(),
                        unit = "°",
                        color = PureWhite,
                        containerColor = lerp(SurfaceCard, AlertRed, leanProgress),
                        modifier = Modifier.weight(1f),
                        weight = FontWeight(700 + (leanProgress * 200).toInt())
                    )
                    
                    MetricCard(
                        title = "1000m MAX",
                        value = rollingMax1000m.toInt().toString(),
                        unit = "°",
                        color = PureWhite,
                        containerColor = lerp(SurfaceCard, AlertRed, max1000mProgress),
                        modifier = Modifier.weight(1f),
                        weight = FontWeight(700 + (max1000mProgress * 200).toInt())
                    )
                }

                // Session details card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, BorderDivider),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.SpaceAround
                    ) {
                        HistoryRow("Session Max", "L: ${abs(sessionMaxLeft).toInt()}° / R: ${sessionMaxRight.toInt()}°", PureWhite) { service.resetSessionLean() }
                        HistoryRow("Curves Driven", "${corners.size}", PureWhite) { service.resetCornerCount() }
                        HistoryRow("Wheelie Angle", "${sessionMaxPitch.toInt()}°", PureWhite) { service.resetSessionPitch() }
                    }
                }
            }
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
    onCalibrate: () -> Unit,
    onStop: () -> Unit,
    onShowHistory: () -> Unit,
    onShowSettings: () -> Unit,
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

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top row: Go/Pause, Stop (Super glove-friendly tall buttons)
        Row(
            modifier = Modifier.fillMaxWidth().height(72.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    if (!isRecording || isPaused) onStart()
                    else onPause()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording && !isPaused) AlertRed.copy(alpha = buttonAlpha) else highlightColor.copy(alpha = buttonAlpha),
                    contentColor = Color.Black
                ),
                modifier = Modifier.fillMaxHeight().weight(1.4f),
                contentPadding = PaddingValues(horizontal = 8.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, Color.Black.copy(alpha = 0.3f))
            ) {
                Text(
                    text = if (isRecording && !isPaused) "PAUSE SESSION" else if (isPaused) "RESUME SESSION" else "START SESSION",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = Inter, color = Color.Black, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp),
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

        // Bottom row: Settings, Calibrate, Past Sessions
        Row(
            modifier = Modifier.fillMaxWidth().height(62.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onShowSettings,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard.copy(alpha = buttonAlpha)),
                modifier = Modifier.fillMaxHeight().width(62.dp),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, BorderDivider)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MutedGrey,
                    modifier = Modifier.size(24.dp)
                )
            }

            val context = LocalContext.current
            Button(
                onClick = {
                    val activity = context as? AndroidActivity
                    val newOrientation = if (activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }
                    activity?.requestedOrientation = newOrientation
                },
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard.copy(alpha = buttonAlpha)),
                modifier = Modifier.fillMaxHeight().width(62.dp),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, BorderDivider)
            ) {
                Icon(
                    imageVector = Icons.Default.ScreenRotation,
                    contentDescription = "Rotate Screen",
                    tint = highlightColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Button(
                onClick = onCalibrate,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard.copy(alpha = buttonAlpha)),
                modifier = Modifier.fillMaxHeight().weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, BorderDivider)
            ) {
                Text(
                    text = "Calibrate",
                    style = MaterialTheme.typography.titleMedium.copy(color = MutedGrey, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    softWrap = false
                )
            }

            Button(
                onClick = onShowHistory,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard.copy(alpha = buttonAlpha)),
                modifier = Modifier.fillMaxHeight().weight(1.2f),
                contentPadding = PaddingValues(horizontal = 8.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, BorderDivider)
            ) {
                Text(
                    text = "Past Sessions",
                    style = MaterialTheme.typography.titleMedium.copy(color = MutedGrey, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun LeanHorizonIndicator(
    lean: Float,
    highlightColor: Color,
    modifier: Modifier = Modifier,
    sizeScale: Float = 1.0f,
    yOffsetPercent: Float = 0.0f
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        val density = LocalDensity.current
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

        // Base measurements in pixels
        val sizeWidthPx = constraints.maxWidth.toFloat()
        val sizeHeightPx = constraints.maxHeight.toFloat()
        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }

        // Use the smaller dimension as the base to fit perfectly in both orientations
        val limitingDimPx = if (sizeWidthPx < sizeHeightPx) sizeWidthPx else sizeHeightPx

        val baseMarkingRadius = (limitingDimPx / 2f) * 0.98f
        val baseTickLength = with(density) { 12.dp.toPx() }
        val baseLabelPadding = with(density) { 10.dp.toPx() }
        val baseLabelRadius = baseMarkingRadius + baseTickLength + baseLabelPadding

        // Center of dial/horizon line
        val calculatedCenterY = if (isLandscape) {
            sizeHeightPx + with(density) { 12.dp.toPx() } - (0.10f * screenWidthPx)
        } else {
            sizeHeightPx - with(density) { 8.dp.toPx() } - (sizeHeightPx * yOffsetPercent)
        }

        // Calculate dynamic optimal scale to use available space and prevent cutting off elements at the top/sides
        val paddingY = with(density) { 24.dp.toPx() }
        val paddingX = with(density) { 16.dp.toPx() }

        val maxSFromHeight = if (calculatedCenterY > paddingY) {
            (calculatedCenterY - paddingY) / baseLabelRadius
        } else {
            1.0f
        }

        val maxSFromWidth = ((sizeWidthPx / 2f) - paddingX) / (baseLabelRadius * 0.7071f)

        // Select the ideal scale based on constraints, allowing scaling up to fill available space
        val optimalScale = minOf(maxSFromHeight, maxSFromWidth).coerceIn(0.6f, 1.8f)
        val finalScale = optimalScale * sizeScale

        // Use optimal scale to size the bike and indicator dial
        val bikeWidth = with(density) { limitingDimPx.toDp() } * 0.85f * finalScale
        val bikeHeight = bikeWidth * (1536f / 2816f)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, calculatedCenterY)
            val markingRadius = baseMarkingRadius * finalScale
            val tickLength = baseTickLength * finalScale

            rotate(degrees = -lean, pivot = center) {
                // Draw Ground / Street line (always 10% of screen width above screen bottom in landscape)
                drawLine(
                    color = MutedCyan.copy(alpha = 0.6f),
                    start = Offset(center.x - 5000f, center.y),
                    end = Offset(center.x + 5000f, center.y),
                    strokeWidth = 3.dp.toPx() * finalScale
                )

                val markingAngles = listOf(0f, 15f, 30f, 45f)
                markingAngles.forEach { angle ->
                    listOf(270f - angle, 270f + angle).distinct().forEach { a ->
                        val rad = Math.toRadians(a.toDouble()).toFloat()
                        val isZero = angle == 0f
                        val currentTickLength = if (isZero) tickLength * 1.5f else tickLength
                        val currentStrokeWidth = if (isZero) 9.dp.toPx() * finalScale else 3.dp.toPx() * finalScale
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
                                    textSize = 18.sp.toPx() * finalScale
                                    textAlign = Paint.Align.CENTER
                                    typeface = Typeface.DEFAULT
                                }
                                val labelRadius = markingRadius + tickLength + (10.dp.toPx() * finalScale)
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
            val dotRadius = markingRadius - (12.dp.toPx() * finalScale)
            drawCircle(
                color = highlightColor,
                radius = 6.dp.toPx() * finalScale,
                center = Offset(center.x + dotRadius * cos(dotRad), center.y + dotRadius * sin(dotRad))
            )
        }

        val offsetDp = remember(density, bikeHeight, sizeHeightPx, isLandscape, screenWidthPx, finalScale) {
            with(density) {
                val offsetY = calculatedCenterY - sizeHeightPx + (40f * finalScale)
                offsetY.toDp()
            }
        }

        Image(
            painter = painterResource(id = R.drawable.bikeindicatorv2),
            contentDescription = "Bike Indicator",
            colorFilter = ColorFilter.tint(PureWhite),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(width = bikeWidth, height = bikeHeight)
                .offset(y = offsetDp)
                .graphicsLayer {
                    val bikeHeightPx = bikeHeight.toPx()
                    val pivotY = if (bikeHeightPx > 0f) (bikeHeightPx - 40f * finalScale) / bikeHeightPx else 1.0f
                    transformOrigin = TransformOrigin(0.5f, pivotY)
                }
        )
    }
}

@Composable
fun HistoryRow(label: String, value: String, color: Color, onReset: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
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
                fontSize = 15.sp
            ),
            color = color,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.weight(0.8f),
            textAlign = TextAlign.End
        )
        if (onReset != null) {
            IconButton(
                onClick = onReset,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = MutedCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(30.dp))
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
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, BorderDivider),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MutedGrey,
                maxLines = 1,
                softWrap = false
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 24.sp,
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
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedGrey,
                        modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                    )
                }
            }
        }
    }
}
