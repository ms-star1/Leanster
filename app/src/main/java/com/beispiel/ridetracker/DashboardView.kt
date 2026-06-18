package com.beispiel.ridetracker

import android.app.Activity as AndroidActivity
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.ui.platform.LocalConfiguration
import android.content.pm.ActivityInfo
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.filled.Settings
import android.app.Activity
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.shape.RoundedCornerShape
import com.beispiel.ridetracker.ui.theme.*
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import android.graphics.Paint
import android.graphics.Typeface
import java.util.Locale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beispiel.ridetracker.ui.theme.*
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
    highlightColor: androidx.compose.ui.graphics.Color,
    mutedHighlightColor: androidx.compose.ui.graphics.Color,
    highlightColorName: String,
    onColorChange: (String) -> Unit,
    onToggleUnit: () -> Unit,
    onShowHistory: () -> Unit,
    onShowSettings: () -> Unit,
    mapView: org.maplibre.android.maps.MapView,
    hasPlayedStartupAnimation: Boolean = false,
    onStartupAnimationFinished: () -> Unit = {}
) {
    val currentLean by service.currentLean.collectAsStateWithLifecycle()
    val currentSpeed by service.currentSpeed.collectAsStateWithLifecycle()
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
    var showSettingsDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Startup Animation State
    var startAnimTriggered by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(hasPlayedStartupAnimation) {
        if (!hasPlayedStartupAnimation) {
            startAnimTriggered = true
            kotlinx.coroutines.delay(2000)
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
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
                text = {
                    Column(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("Highlight Color", style = MaterialTheme.typography.titleMedium, color = highlightColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Unit System", style = MaterialTheme.typography.titleMedium, color = highlightColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!isMetric) onToggleUnit()
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isMetric,
                                onClick = { if (!isMetric) onToggleUnit() },
                                colors = RadioButtonDefaults.colors(selectedColor = highlightColor, unselectedColor = MutedGrey)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Metric (km/h)", color = PureWhite)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isMetric) onToggleUnit()
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = !isMetric,
                                onClick = { if (isMetric) onToggleUnit() },
                                colors = RadioButtonDefaults.colors(selectedColor = highlightColor, unselectedColor = MutedGrey)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Imperial (mph)", color = PureWhite)
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Launch Orientation", style = MaterialTheme.typography.titleMedium, color = highlightColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        val prefs = remember { androidx.preference.PreferenceManager.getDefaultSharedPreferences(context) }
                        var defaultLaunchMode by remember { mutableStateOf(prefs.getString("default_launch_mode", "Portrait") ?: "Portrait") }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (defaultLaunchMode != "Portrait") {
                                        defaultLaunchMode = "Portrait"
                                        prefs.edit().putString("default_launch_mode", "Portrait").apply()
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = defaultLaunchMode == "Portrait",
                                onClick = {
                                    defaultLaunchMode = "Portrait"
                                    prefs.edit().putString("default_launch_mode", "Portrait").apply()
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = highlightColor, unselectedColor = MutedGrey)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Portrait Mode", color = PureWhite)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (defaultLaunchMode != "Landscape") {
                                        defaultLaunchMode = "Landscape"
                                        prefs.edit().putString("default_launch_mode", "Landscape").apply()
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = defaultLaunchMode == "Landscape",
                                onClick = {
                                    defaultLaunchMode = "Landscape"
                                    prefs.edit().putString("default_launch_mode", "Landscape").apply()
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = highlightColor, unselectedColor = MutedGrey)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Horizontal Mode", color = PureWhite)
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text("All-Time Stats", style = MaterialTheme.typography.titleMedium, color = highlightColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                service.resetAllTimeLean()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AlertRed,
                                contentColor = PureWhite
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ) {
                            Text("RESET ALL-TIME MAX LEAN", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("CLOSE", color = highlightColor, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = SurfaceCard,
                titleContentColor = PureWhite,
                textContentColor = MutedGrey
            )
        }

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
                onShowSettings = { showSettingsDialog = true },
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
                onShowSettings = { showSettingsDialog = true },
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StartupAnimatedElement(order = 0, triggered = startAnimTriggered, skipAnimation = hasPlayedStartupAnimation, modifier = Modifier.weight(1f)) {
                MetricCard(
                    title = "LEAN",
                    value = absLeanVal.toInt().toString(),
                    unit = "°",
                    color = PureWhite,
                    containerColor = if (activeView == "map") lerp(SurfaceCard, AlertRed, leanProgress).copy(alpha = 0.8f) else lerp(SurfaceCard, AlertRed, leanProgress),
                    modifier = Modifier,
                    weight = FontWeight(700 + (leanProgress * 200).toInt())
                )
            }
            StartupAnimatedElement(order = 1, triggered = startAnimTriggered, skipAnimation = hasPlayedStartupAnimation, modifier = Modifier.weight(1f)) {
                MetricCard(
                    title = "SPEED",
                    value = if (isMetric) currentSpeed.toInt().toString() else (currentSpeed * 0.621371).toInt().toString(),
                    unit = if (isMetric) "km/h" else "mph",
                    color = highlightColor,
                    containerColor = if (activeView == "map") SurfaceCard.copy(alpha = 0.8f) else SurfaceCard,
                    modifier = Modifier,
                    onClick = onToggleUnit
                )
            }
            StartupAnimatedElement(order = 2, triggered = startAnimTriggered, skipAnimation = hasPlayedStartupAnimation, modifier = Modifier.weight(1f)) {
                MetricCard(
                    title = "MAX 1000m",
                    value = rollingMax1000m.toInt().toString(),
                    unit = "°",
                    color = PureWhite,
                    containerColor = if (activeView == "map") lerp(SurfaceCard, AlertRed, max1000mProgress).copy(alpha = 0.8f) else lerp(SurfaceCard, AlertRed, max1000mProgress),
                    modifier = Modifier,
                    weight = FontWeight(700 + (max1000mProgress * 200).toInt())
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(16.dp))

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

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Left Column: Main Gauges (Lean or Map)
        Box(
            modifier = Modifier
                .weight(1.5f)
                .fillMaxHeight()
        ) {
            if (activeView == "lean") {
                LeanHorizonIndicator(
                    lean = currentLean,
                    highlightColor = highlightColor,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // View Toggles overlayed at top
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onActiveViewChange("lean") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeView == "lean") highlightColor else SurfaceCard.copy(alpha = 0.8f),
                        contentColor = if (activeView == "lean") DeepCarbon else PureWhite
                    ),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Horizon", style = MaterialTheme.typography.labelMedium)
                }
                Button(
                    onClick = { onActiveViewChange("map") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeView == "map") highlightColor else SurfaceCard.copy(alpha = 0.8f),
                        contentColor = if (activeView == "map") DeepCarbon else PureWhite
                    ),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Map", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Right Column: Stats and Controls
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Stats Row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val absLeanVal = abs(currentLean)
                val leanProgress = ((absLeanVal - 30f) / 10f).coerceIn(0f, 1f)
                
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
                    title = "SPEED",
                    value = if (isMetric) currentSpeed.toInt().toString() else (currentSpeed * 0.621371).toInt().toString(),
                    unit = if (isMetric) "km/h" else "mph",
                    color = highlightColor,
                    containerColor = SurfaceCard,
                    modifier = Modifier.weight(1f),
                    onClick = onToggleUnit
                )
            }

            // More Stats
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, BorderDivider),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    HistoryRow("Session Max", "L: ${abs(sessionMaxLeft).toInt()}° / R: ${sessionMaxRight.toInt()}°", PureWhite)
                    HistoryRow("All-Time Max", "L: ${abs(allTimeMaxLeft).toInt()}° / R: ${allTimeMaxRight.toInt()}°", highlightColor)
                    HistoryRow("1000m Max", "${rollingMax1000m.toInt()}°", PureWhite)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Controls
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
            kotlinx.coroutines.delay(randomDelay.toLong() + duration)
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
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top row: Go/Pause, Stop
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (!isRecording || isPaused) onStart()
                    else onPause()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = highlightColor.copy(alpha = buttonAlpha),
                    contentColor = Color.Black
                ),
                modifier = Modifier.fillMaxHeight().weight(1.4f),
                contentPadding = PaddingValues(horizontal = 4.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (isRecording && !isPaused) "PAUSE SESSION" else if (isPaused) "RESUME SESSION" else "START SESSION",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = Inter, color = Color.Black),
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
                    contentColor = highlightColor
                ),
                border = BorderStroke(2.dp, highlightColor),
                modifier = Modifier.fillMaxHeight().weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Text("STOP", style = MaterialTheme.typography.titleMedium.copy(fontFamily = Inter), maxLines = 1, softWrap = false)
            }
        }

        // Bottom row: Settings, Calibrate, Past Sessions
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onShowSettings,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard.copy(alpha = buttonAlpha)),
                modifier = Modifier.fillMaxHeight().width(56.dp),
                contentPadding = PaddingValues(0.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderDivider)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MutedGrey
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
                modifier = Modifier.fillMaxHeight().width(56.dp),
                contentPadding = PaddingValues(0.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderDivider)
            ) {
                Icon(
                    imageVector = Icons.Default.ScreenRotation,
                    contentDescription = "Rotate Screen",
                    tint = highlightColor
                )
            }

            Button(
                onClick = onCalibrate,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard.copy(alpha = buttonAlpha)),
                modifier = Modifier.fillMaxHeight().weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderDivider)
            ) {
                Text("Calibrate", style = MaterialTheme.typography.labelMedium.copy(color = MutedGrey), maxLines = 1, softWrap = false)
            }

            Button(
                onClick = onShowHistory,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard.copy(alpha = buttonAlpha)),
                modifier = Modifier.fillMaxHeight().weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderDivider)
            ) {
                Text("Past Sessions", style = MaterialTheme.typography.labelMedium.copy(color = MutedGrey), maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
fun LeanHorizonIndicator(lean: Float, highlightColor: Color, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        val bikeWidth = maxWidth * 0.85f
        val bikeHeight = bikeWidth * (1536f / 2816f)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerY = size.height - 8.dp.toPx()
            val center = Offset(size.width / 2f, centerY)
            val markingRadius = (size.width / 2f) * 0.98f
            val tickLength = 12.dp.toPx()

            rotate(degrees = -lean, pivot = center) {
                // Draw Ground / Street line
                drawLine(
                    color = MutedCyan.copy(alpha = 0.6f),
                    start = Offset(center.x - 5000f, center.y),
                    end = Offset(center.x + 5000f, center.y),
                    strokeWidth = 3.dp.toPx()
                )

                val markingAngles = listOf(0f, 15f, 30f, 45f)
                markingAngles.forEach { angle ->
                    listOf(270f - angle, 270f + angle).distinct().forEach { a ->
                        val rad = Math.toRadians(a.toDouble()).toFloat()
                        val isZero = angle == 0f
                        val currentTickLength = if (isZero) tickLength * 1.5f else tickLength
                        val currentStrokeWidth = if (isZero) 9.dp.toPx() else 3.dp.toPx()
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
                                    textSize = 18.sp.toPx()
                                    textAlign = Paint.Align.CENTER
                                    typeface = Typeface.DEFAULT
                                }
                                val labelRadius = markingRadius + tickLength + 10.dp.toPx()
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
            val dotRadius = markingRadius - 12.dp.toPx()
            drawCircle(
                color = highlightColor,
                radius = 6.dp.toPx(),
                center = Offset(center.x + dotRadius * cos(dotRad), center.y + dotRadius * sin(dotRad))
            )
        }

        val density = LocalDensity.current
        val offsetDp = remember(density) {
            with(density) {
                (-8.dp.toPx() + 40f).toDp()
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
                    val pivotY = if (bikeHeightPx > 0f) (bikeHeightPx - 40f) / bikeHeightPx else 1.0f
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
