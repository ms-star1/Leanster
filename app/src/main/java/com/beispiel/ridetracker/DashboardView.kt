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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.PathEffect
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
    mapView: org.maplibre.android.maps.MapView,
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
                rollingMax1000m = rollingMax1000m,
                rollingDistanceTarget = rollingDistanceTarget,
                onResetMaxLean = onResetMaxLean
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
                rollingMax1000m = rollingMax1000m,
                rollingDistanceTarget = rollingDistanceTarget,
                onResetMaxLean = onResetMaxLean
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
    val vibrationFiltering by service.vibrationFiltering.collectAsStateWithLifecycle()
    val forceFilter by service.forceFilterStandstill.collectAsStateWithLifecycle()
    val isDevModeActive by service.isDevModeActive.collectAsStateWithLifecycle()

    var devModeClickCount by remember { mutableIntStateOf(0) }

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
                    text = "SETTINGS" + if (isDevModeActive) " (DEV MODE)" else "",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
                    color = highlightColor,
                    modifier = Modifier.clickable {
                        devModeClickCount++
                        if (devModeClickCount >= 5) {
                            service.isDevModeActive.value = !isDevModeActive
                            devModeClickCount = 0
                            android.widget.Toast.makeText(
                                context,
                                if (isDevModeActive) "Developer Mode Activated!" else "Developer Mode Deactivated!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
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

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("VIBRATION FILTERING", style = MaterialTheme.typography.titleMedium, color = highlightColor)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Very Low", "Low", "Standard", "High", "Very High").forEach { level ->
                                Button(
                                    onClick = { service.setVibrationFiltering(level) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (vibrationFiltering == level) highlightColor else SurfaceCard,
                                        contentColor = if (vibrationFiltering == level) DeepCarbon else PureWhite
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(level, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("MAX LEAN DISTANCE WINDOW", style = MaterialTheme.typography.titleMedium, color = highlightColor)
                        val rollingDistanceTarget by service.rollingDistanceTarget.collectAsStateWithLifecycle()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(100, 500, 1000, 2000, 5000).forEach { dist ->
                                Button(
                                    onClick = { service.setRollingDistanceTarget(dist) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (rollingDistanceTarget == dist) highlightColor else SurfaceCard,
                                        contentColor = if (rollingDistanceTarget == dist) DeepCarbon else PureWhite
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (dist >= 1000) "${dist / 1000}km" else "${dist}m", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }

                        if (isDevModeActive) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("DEVELOPER OPTIONS", style = MaterialTheme.typography.titleMedium, color = highlightColor)
                            
                            Text("HIGHLIGHT COLOR", style = MaterialTheme.typography.titleSmall, color = MutedGrey)
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { service.setForceFilterStandstill(!forceFilter) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = forceFilter,
                                    onCheckedChange = { service.setForceFilterStandstill(it) },
                                    colors = CheckboxDefaults.colors(checkedColor = highlightColor, uncheckedColor = MutedGrey)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Force Filter at Standstill", color = PureWhite, style = MaterialTheme.typography.bodyLarge)
                            }
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

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("VIBRATION FILTERING", style = MaterialTheme.typography.titleMedium, color = highlightColor)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Very Low", "Low", "Standard", "High", "Very High").forEach { level ->
                            Button(
                                onClick = { service.setVibrationFiltering(level) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (vibrationFiltering == level) highlightColor else SurfaceCard,
                                    contentColor = if (vibrationFiltering == level) DeepCarbon else PureWhite
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(level, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
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

                    if (isDevModeActive) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("DEVELOPER OPTIONS", style = MaterialTheme.typography.titleMedium, color = highlightColor)
                        
                        Text("HIGHLIGHT COLOR", style = MaterialTheme.typography.titleSmall, color = MutedGrey)
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { service.setForceFilterStandstill(!forceFilter) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = forceFilter,
                                onCheckedChange = { service.setForceFilterStandstill(it) },
                                colors = CheckboxDefaults.colors(checkedColor = highlightColor, uncheckedColor = MutedGrey)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Force Filter at Standstill", color = PureWhite, style = MaterialTheme.typography.bodyLarge)
                        }
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
        IconButton(onClick = {
            val activity = context as? AndroidActivity
            val newOrientation = if (activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            activity?.requestedOrientation = newOrientation
        }) {
            Icon(
                imageVector = Icons.Default.ScreenRotation,
                contentDescription = "Rotate Screen",
                tint = highlightColor
            )
        }
        
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
                    text = { Text("Past Sessions", color = PureWhite) },
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
                androidx.compose.material3.Divider(color = BorderDivider)
                DropdownMenuItem(
                    text = { Text("Reset Value", color = AlertRed) },
                    onClick = {
                        onReset!!()
                        expanded = false
                    }
                )
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
    rollingMax1000m: Float,
    rollingDistanceTarget: Int,
    onResetMaxLean: () -> Unit = {}
) {
    val currentLean by service.currentLean.collectAsStateWithLifecycle()
    val currentSpeed by service.currentSpeed.collectAsStateWithLifecycle()

    var box1Metric by rememberSaveable { mutableStateOf(MetricType.LEAN) }
    var box2Metric by rememberSaveable { mutableStateOf(MetricType.MAX_1000M) }
    var box3Metric by rememberSaveable { mutableStateOf(MetricType.SPEED) }
    var box4Metric by rememberSaveable { mutableStateOf(MetricType.CORNERS) }
    var box5Metric by rememberSaveable { mutableStateOf(MetricType.ALL_TIME_MAX) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RideTracker",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = Rajdhani, fontWeight = FontWeight.Bold),
                color = PureWhite
            )
            TopRightControls(
                onShowSettings = onShowSettings,
                onShowHistory = onShowHistory,
                onCalibrate = { service.calibrateSensors() },
                highlightColor = highlightColor
            )
        }

        // Unified Telemetry Top Row (2 Customizable Boxes)
        Row(
            modifier = Modifier.fillMaxWidth().height(90.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StartupAnimatedElement(order = 0, triggered = startAnimTriggered, skipAnimation = hasPlayedStartupAnimation, modifier = Modifier.weight(1.0f).fillMaxHeight()) {
                CustomizableMetricCard(
                    selectedMetric = box1Metric,
                    onMetricSelected = { box1Metric = it },
                    currentLean = currentLean, currentSpeed = currentSpeed, rollingMax1000m = rollingMax1000m,
                    rollingDistanceTarget = rollingDistanceTarget, isMetric = isMetric, sessionMaxLeft = sessionMaxLeft,
                    sessionMaxRight = sessionMaxRight, allTimeMaxLeft = allTimeMaxLeft, allTimeMaxRight = allTimeMaxRight,
                    sessionMaxPitch = sessionMaxPitch, cornersCount = corners.size, activeView = activeView,
                    highlightColor = highlightColor, modifier = Modifier.fillMaxHeight(),
                    onReset1000m = onResetMaxLean, onResetSessionLean = { service.resetSessionLean() },
                    onResetSessionPitch = { service.resetSessionPitch() }, onResetCorners = { service.resetCornerCount() }
                )
            }

            StartupAnimatedElement(order = 1, triggered = startAnimTriggered, skipAnimation = hasPlayedStartupAnimation, modifier = Modifier.weight(1.0f).fillMaxHeight()) {
                CustomizableMetricCard(
                    selectedMetric = box2Metric,
                    onMetricSelected = { box2Metric = it },
                    currentLean = currentLean, currentSpeed = currentSpeed, rollingMax1000m = rollingMax1000m,
                    rollingDistanceTarget = rollingDistanceTarget, isMetric = isMetric, sessionMaxLeft = sessionMaxLeft,
                    sessionMaxRight = sessionMaxRight, allTimeMaxLeft = allTimeMaxLeft, allTimeMaxRight = allTimeMaxRight,
                    sessionMaxPitch = sessionMaxPitch, cornersCount = corners.size, activeView = activeView,
                    highlightColor = highlightColor, modifier = Modifier.fillMaxHeight(),
                    onReset1000m = onResetMaxLean, onResetSessionLean = { service.resetSessionLean() },
                    onResetSessionPitch = { service.resetSessionPitch() }, onResetCorners = { service.resetCornerCount() }
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
                        speed = currentSpeed,
                        highlightColor = highlightColor,
                        modifier = Modifier.fillMaxSize(),
                        sizeScale = 1.3f,
                        yOffsetPercent = 0.10f
                    )
                }
            }
        }

        if (activeView == "lean") {
            Spacer(modifier = Modifier.height(12.dp))

            StartupAnimatedElement(order = 5, triggered = startAnimTriggered, skipAnimation = hasPlayedStartupAnimation) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomizableMetricCard(
                        selectedMetric = box3Metric,
                        onMetricSelected = { box3Metric = it },
                        currentLean = currentLean, currentSpeed = currentSpeed, rollingMax1000m = rollingMax1000m,
                        rollingDistanceTarget = rollingDistanceTarget, isMetric = isMetric, sessionMaxLeft = sessionMaxLeft,
                        sessionMaxRight = sessionMaxRight, allTimeMaxLeft = allTimeMaxLeft, allTimeMaxRight = allTimeMaxRight,
                        sessionMaxPitch = sessionMaxPitch, cornersCount = corners.size, activeView = activeView,
                        highlightColor = highlightColor, modifier = Modifier.weight(1f).fillMaxHeight(),
                        forceWhiteText = true,
                        onReset1000m = onResetMaxLean, onResetSessionLean = { service.resetSessionLean() },
                        onResetSessionPitch = { service.resetSessionPitch() }, onResetCorners = { service.resetCornerCount() }
                    )

                    CustomizableMetricCard(
                        selectedMetric = box4Metric,
                        onMetricSelected = { box4Metric = it },
                        currentLean = currentLean, currentSpeed = currentSpeed, rollingMax1000m = rollingMax1000m,
                        rollingDistanceTarget = rollingDistanceTarget, isMetric = isMetric, sessionMaxLeft = sessionMaxLeft,
                        sessionMaxRight = sessionMaxRight, allTimeMaxLeft = allTimeMaxLeft, allTimeMaxRight = allTimeMaxRight,
                        sessionMaxPitch = sessionMaxPitch, cornersCount = corners.size, activeView = activeView,
                        highlightColor = highlightColor, modifier = Modifier.weight(1f).fillMaxHeight(),
                        forceWhiteText = true,
                        onReset1000m = onResetMaxLean, onResetSessionLean = { service.resetSessionLean() },
                        onResetSessionPitch = { service.resetSessionPitch() }, onResetCorners = { service.resetCornerCount() }
                    )

                    CustomizableMetricCard(
                        selectedMetric = box5Metric,
                        onMetricSelected = { box5Metric = it },
                        currentLean = currentLean, currentSpeed = currentSpeed, rollingMax1000m = rollingMax1000m,
                        rollingDistanceTarget = rollingDistanceTarget, isMetric = isMetric, sessionMaxLeft = sessionMaxLeft,
                        sessionMaxRight = sessionMaxRight, allTimeMaxLeft = allTimeMaxLeft, allTimeMaxRight = allTimeMaxRight,
                        sessionMaxPitch = sessionMaxPitch, cornersCount = corners.size, activeView = activeView,
                        highlightColor = highlightColor, modifier = Modifier.weight(1f).fillMaxHeight(),
                        forceWhiteText = true,
                        onReset1000m = onResetMaxLean, onResetSessionLean = { service.resetSessionLean() },
                        onResetSessionPitch = { service.resetSessionPitch() }, onResetCorners = { service.resetCornerCount() }
                    )
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
                onStop = { service.stopRecording() },
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
    rollingMax1000m: Float,
    rollingDistanceTarget: Int,
    onResetMaxLean: () -> Unit = {}
) {
    val currentLean by service.currentLean.collectAsStateWithLifecycle()
    val currentSpeed by service.currentSpeed.collectAsStateWithLifecycle()

    var mainMetric by rememberSaveable { mutableStateOf(MetricType.LEAN) }
    var box1Metric by rememberSaveable { mutableStateOf(MetricType.SPEED) }
    var box2Metric by rememberSaveable { mutableStateOf(MetricType.MAX_1000M) }
    var box3Metric by rememberSaveable { mutableStateOf(MetricType.SESSION_MAX_LEFT) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Column: All buttons and toggles
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

                    if (activeView == "map") {
                        ControlButtonsContentV3(
                            isRecording = isRecording,
                            isPaused = isPaused,
                            highlightColor = highlightColor,
                            onStart = { service.startRecording() },
                            onPause = { service.pauseRecording() },
                            onStop = { service.stopRecording() },
                            isMapMode = true
                        )
                    }
                }

                // Central space where indicator is shown
                if (activeView == "lean") {
                    LeanHorizonIndicator(
                        lean = currentLean,
                        speed = currentSpeed,
                        highlightColor = highlightColor,
                        modifier = Modifier
                            .weight(1.8f)
                            .fillMaxHeight(),
                        sizeScale = 1.3f,
                        yOffsetPercent = 0.0f
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1.8f))
                }

                // Right Column: All stats / data
                Column(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxHeight()
                        .zIndex(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    TopRightControls(
                        onShowSettings = onShowSettings,
                        onShowHistory = onShowHistory,
                        onCalibrate = { service.calibrateSensors() },
                        highlightColor = highlightColor
                    )

                    CustomizableMetricCard(
                        selectedMetric = mainMetric,
                        onMetricSelected = { mainMetric = it },
                        currentLean = currentLean, currentSpeed = currentSpeed, rollingMax1000m = rollingMax1000m,
                        rollingDistanceTarget = rollingDistanceTarget, isMetric = isMetric, sessionMaxLeft = sessionMaxLeft,
                        sessionMaxRight = sessionMaxRight, allTimeMaxLeft = allTimeMaxLeft, allTimeMaxRight = allTimeMaxRight,
                        sessionMaxPitch = sessionMaxPitch, cornersCount = corners.size, activeView = activeView,
                        highlightColor = highlightColor, modifier = Modifier.fillMaxWidth(), height = 90.dp,
                        onReset1000m = onResetMaxLean, onResetSessionLean = { service.resetSessionLean() },
                        onResetSessionPitch = { service.resetSessionPitch() }, onResetCorners = { service.resetCornerCount() }
                    )
                }
            }
            
            if (activeView == "lean") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Start and Stop buttons on the bottom left
                    Column(
                        modifier = Modifier.weight(1.5f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (!isRecording || isPaused) service.startRecording()
                                else service.pauseRecording()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecording && !isPaused) AlertRed else highlightColor,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, Color.Black.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = if (isRecording && !isPaused) "PAUSE SESSION" else if (isPaused) "RESUME SESSION" else "START SESSION",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = Inter, color = Color.Black, fontWeight = FontWeight.ExtraBold),
                                maxLines = 1,
                                softWrap = false
                            )
                        }

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

                        OutlinedButton(
                            onClick = {
                                if (isRecording) {
                                    showStopDialog = true
                                } else {
                                    service.stopRecording()
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = DeepCarbon,
                                contentColor = AlertRed
                            ),
                            border = BorderStroke(2.5.dp, AlertRed),
                            modifier = Modifier.fillMaxWidth().height(60.dp),
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

                    // Remaining 3 Value Boxes
                    Row(
                        modifier = Modifier.weight(3f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CustomizableMetricCard(
                            selectedMetric = box1Metric,
                            onMetricSelected = { box1Metric = it },
                            currentLean = currentLean, currentSpeed = currentSpeed, rollingMax1000m = rollingMax1000m,
                            rollingDistanceTarget = rollingDistanceTarget, isMetric = isMetric, sessionMaxLeft = sessionMaxLeft,
                            sessionMaxRight = sessionMaxRight, allTimeMaxLeft = allTimeMaxLeft, allTimeMaxRight = allTimeMaxRight,
                            sessionMaxPitch = sessionMaxPitch, cornersCount = corners.size, activeView = activeView,
                            highlightColor = highlightColor, modifier = Modifier.weight(1f), height = 60.dp,
                            onReset1000m = onResetMaxLean, onResetSessionLean = { service.resetSessionLean() },
                            onResetSessionPitch = { service.resetSessionPitch() }, onResetCorners = { service.resetCornerCount() }
                        )

                        CustomizableMetricCard(
                            selectedMetric = box2Metric,
                            onMetricSelected = { box2Metric = it },
                            currentLean = currentLean, currentSpeed = currentSpeed, rollingMax1000m = rollingMax1000m,
                            rollingDistanceTarget = rollingDistanceTarget, isMetric = isMetric, sessionMaxLeft = sessionMaxLeft,
                            sessionMaxRight = sessionMaxRight, allTimeMaxLeft = allTimeMaxLeft, allTimeMaxRight = allTimeMaxRight,
                            sessionMaxPitch = sessionMaxPitch, cornersCount = corners.size, activeView = activeView,
                            highlightColor = highlightColor, modifier = Modifier.weight(1f), height = 60.dp,
                            onReset1000m = onResetMaxLean, onResetSessionLean = { service.resetSessionLean() },
                            onResetSessionPitch = { service.resetSessionPitch() }, onResetCorners = { service.resetCornerCount() }
                        )

                        CustomizableMetricCard(
                            selectedMetric = box3Metric,
                            onMetricSelected = { box3Metric = it },
                            currentLean = currentLean, currentSpeed = currentSpeed, rollingMax1000m = rollingMax1000m,
                            rollingDistanceTarget = rollingDistanceTarget, isMetric = isMetric, sessionMaxLeft = sessionMaxLeft,
                            sessionMaxRight = sessionMaxRight, allTimeMaxLeft = allTimeMaxLeft, allTimeMaxRight = allTimeMaxRight,
                            sessionMaxPitch = sessionMaxPitch, cornersCount = corners.size, activeView = activeView,
                            highlightColor = highlightColor, modifier = Modifier.weight(1f), height = 60.dp,
                            onReset1000m = onResetMaxLean, onResetSessionLean = { service.resetSessionLean() },
                            onResetSessionPitch = { service.resetSessionPitch() }, onResetCorners = { service.resetCornerCount() }
                        )
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
