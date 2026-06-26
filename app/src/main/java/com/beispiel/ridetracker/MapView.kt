@file:Suppress("DEPRECATION")

package com.beispiel.ridetracker

import android.location.Location
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.beispiel.ridetracker.ui.theme.*
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.annotations.PolylineOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun AnalyticsMapPreview() {
    val mockPoints = listOf(
        TelemetryPoint(0, 52.5200, 13.4050, 50.0, 0f, 0f, 0.0),
        TelemetryPoint(1000, 52.5210, 13.4060, 55.0, 15f, 0f, 10.0)
    )
    val mockCorners = listOf(
        CornerEvent(1, 0, 1, -5f, 15f, 1)
    )
    RideTrackerTheme {
        AnalyticsMapContent(
            points = mockPoints,
            corners = mockCorners,
            activeCornerIndex = 0,
            onActiveCornerChange = {},
            onJumpToApex = {},
            onCenterOnCorner = {},
            isPreview = true
        )
    }
}

@Composable
fun MinimalistCockpitMap(
    currentLocation: Location,
    currentBearing: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var activeMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var currentZoomState by remember { mutableStateOf(17f) }

    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
            getMapAsync { maplibreMap ->
                activeMap = maplibreMap
                try {
                    val styleJson = context.assets.open("style.json").bufferedReader().use { it.readText() }
                    maplibreMap.setStyle(Style.Builder().fromJson(styleJson))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                maplibreMap.uiSettings.isLogoEnabled = false
                maplibreMap.uiSettings.isAttributionEnabled = false
                maplibreMap.uiSettings.isZoomGesturesEnabled = false
                maplibreMap.uiSettings.isScrollGesturesEnabled = false
                maplibreMap.uiSettings.isRotateGesturesEnabled = false
                maplibreMap.uiSettings.isTiltGesturesEnabled = false
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { mv ->
                mv.getMapAsync { maplibreMap ->
                    maplibreMap.setPadding(0, 0, 0, 0)
                    
                    val target = LatLng(currentLocation.latitude, currentLocation.longitude)
                    
                    maplibreMap.cameraPosition = CameraPosition.Builder()
                        .target(target)
                        .zoom(currentZoomState.toDouble())
                        .bearing(currentBearing.toDouble())
                        .build()
                }
            }
        )

        // Floating Zoom Buttons
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FilledIconButton(
                onClick = {
                    currentZoomState = (currentZoomState + 1f).coerceIn(1f, 20f)
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = SurfaceCard.copy(alpha = 0.8f),
                    contentColor = NeonCyan
                ),
                modifier = Modifier.size(56.dp)
            ) {
                Text("+", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
            }
            
            FilledIconButton(
                onClick = {
                    currentZoomState = (currentZoomState - 1f).coerceIn(1f, 20f)
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = SurfaceCard.copy(alpha = 0.8f),
                    contentColor = NeonCyan
                ),
                modifier = Modifier.size(56.dp)
            ) {
                Text("-", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
            }
        }
    }
}

@Composable
fun AnalyticsMapScreen(
    points: List<TelemetryPoint>,
    corners: List<CornerEvent>,
    mapView: MapView
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var activeCornerIndex by remember { mutableStateOf(-1) }
    var hasCenteredOnFirstPoint by remember { mutableStateOf(false) }

    // Auto-center on first load
    LaunchedEffect(points) {
        if (points.isNotEmpty() && !hasCenteredOnFirstPoint) {
            val lastPoint = points.last()
            mapView.getMapAsync { maplibreMap ->
                maplibreMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(lastPoint.latitude, lastPoint.longitude),
                        17.0
                    )
                )
            }
            hasCenteredOnFirstPoint = true
        }
    }

    AnalyticsMapContent(
        points = points,
        corners = corners,
        activeCornerIndex = activeCornerIndex,
        onActiveCornerChange = { index ->
            activeCornerIndex = index
        },
        onJumpToApex = { corner ->
            val apexPoint = points.getOrNull(corner.maxLeanIndex)
            apexPoint?.let {
                mapView.getMapAsync { maplibreMap ->
                    maplibreMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(it.latitude, it.longitude),
                            19.0
                        )
                    )
                }
            }
        },
        onCenterOnCorner = { corner ->
            val cornerStartPoint = points.getOrNull(corner.startIndex)
            cornerStartPoint?.let {
                mapView.getMapAsync { maplibreMap ->
                    maplibreMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(it.latitude, it.longitude),
                            17.0
                        )
                    )
                }
            }
        },
        mapViewProvider = { mapView }
    )
}

@Composable
fun AnalyticsMapContent(
    points: List<TelemetryPoint>,
    corners: List<CornerEvent>,
    activeCornerIndex: Int,
    onActiveCornerChange: (Int) -> Unit,
    onJumpToApex: (CornerEvent) -> Unit,
    onCenterOnCorner: (CornerEvent) -> Unit,
    modifier: Modifier = Modifier,
    isPreview: Boolean = false,
    mapViewProvider: (() -> MapView)? = null
) {
    Box(modifier = modifier.fillMaxSize().background(DeepCarbon)) {
        if (isPreview) {
            // Placeholder for Preview
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(DeepCarbon, Color(0xFF1A1A1A))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("Map Preview Placeholder", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            AndroidView(
                factory = { mapViewProvider!!() },
                modifier = Modifier.fillMaxSize(),
                update = { mv ->
                    mv.getMapAsync { maplibreMap ->
                        maplibreMap.clear()
                        if (points.isNotEmpty()) {
                            val latLngs = points.map { LatLng(it.latitude, it.longitude) }
                            maplibreMap.addPolyline(
                                PolylineOptions()
                                    .addAll(latLngs)
                                    .color(AlertRed.toArgb())
                                    .width(5f)
                            )
                        }
                    }
                }
            )
        }

        // --- MAP CONTROLLERS AND SEQUENCING ENGINE ---
        if (corners.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (activeCornerIndex != -1) {
                    val currentTrackedCorner = corners[activeCornerIndex]
                    Button(
                        onClick = { onJumpToApex(currentTrackedCorner) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = DeepCarbon
                        ),
                        modifier = Modifier.padding(bottom = 8.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ) {
                        val maxLeft = abs(currentTrackedCorner.maxLeftLean)
                        val maxRight = currentTrackedCorner.maxRightLean
                        val printValue = if (maxLeft > maxRight) 
                            "L: ${maxLeft.toInt()}°" 
                            else "R: ${maxRight.toInt()}°"
                        Text(
                            text = "Jump to Max Lean Apex ($printValue)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val newIndex = if (activeCornerIndex > 0) activeCornerIndex - 1 else corners.lastIndex
                            onActiveCornerChange(newIndex)
                            onCenterOnCorner(corners[newIndex])
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderDivider)
                    ) {
                        Text("⏮️ Prev", color = PureWhite)
                    }

                    Card(
                        modifier = Modifier.weight(1.2f),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.8f)),
                        border = BorderStroke(1.dp, BorderDivider),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Corner: ${if (activeCornerIndex == -1) "None" else "${activeCornerIndex + 1} / ${corners.size}"}",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium.copy(color = PureWhite, fontFamily = Rajdhani, fontWeight = FontWeight.Bold)
                        )
                    }

                    Button(
                        onClick = {
                            val newIndex = if (activeCornerIndex < corners.lastIndex) activeCornerIndex + 1 else 0
                            onActiveCornerChange(newIndex)
                            onCenterOnCorner(corners[newIndex])
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderDivider)
                    ) {
                        Text("Next ⏭️", color = PureWhite)
                    }
                }
            }
        }
    }
}

@Composable
fun GhostReplayScreen(
    currentSession: RideSession,
    ghostSession: RideSession,
    highlightColor: Color,
    mapView: MapView,
    onClose: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var speedMultiplier by remember { mutableStateOf(1f) }
    var progressMs by remember { mutableStateOf(0L) }

    val currentPoints = currentSession.points
    val ghostPoints = ghostSession.points
    val currentDuration = (currentPoints.lastOrNull()?.timestamp ?: 0L) -
            (currentPoints.firstOrNull()?.timestamp ?: 0L)
    val ghostDuration = (ghostPoints.lastOrNull()?.timestamp ?: 0L) -
            (ghostPoints.firstOrNull()?.timestamp ?: 0L)
    val maxDuration = maxOf(currentDuration, ghostDuration).coerceAtLeast(1L)

    var mapLibreRef by remember { mutableStateOf<MapLibreMap?>(null) }
    val context = LocalContext.current

    LaunchedEffect(isPlaying, speedMultiplier) {
        while (isPlaying) {
            delay(16L)
            progressMs = (progressMs + (16L * speedMultiplier).toLong()).coerceAtMost(maxDuration)
            if (progressMs >= maxDuration) isPlaying = false
        }
    }

    val currentPos = remember(progressMs) {
        interpolatePosition(currentPoints, progressMs)
    }
    val ghostPos = remember(progressMs) {
        interpolatePosition(ghostPoints, progressMs)
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    Box(modifier = Modifier.fillMaxSize().background(DeepCarbon)) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { mv ->
                mv.getMapAsync { map ->
                    mapLibreRef = map
                    if (map.style == null) {
                        try {
                            val styleJson = context.assets.open("style.json").bufferedReader().use { it.readText() }
                            map.setStyle(Style.Builder().fromJson(styleJson))
                        } catch (_: Exception) {}
                    }
                    map.clear()
                    if (currentPoints.size >= 2) {
                        map.addPolyline(PolylineOptions()
                            .addAll(currentPoints.map { LatLng(it.latitude, it.longitude) })
                            .color(highlightColor.toArgb()).width(5f))
                    }
                    if (ghostPoints.size >= 2) {
                        map.addPolyline(PolylineOptions()
                            .addAll(ghostPoints.map { LatLng(it.latitude, it.longitude) })
                            .color(AlertRed.copy(alpha = 0.6f).toArgb()).width(4f))
                    }
                    currentPoints.firstOrNull()?.let { p ->
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(p.latitude, p.longitude), 14.0))
                    }
                }
            }
        )

        LaunchedEffect(currentPos, ghostPos) {
            mapLibreRef?.let { map ->
                currentPos?.let { map.animateCamera(CameraUpdateFactory.newLatLng(LatLng(it.first, it.second))) }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(DeepCarbon.copy(alpha = 0.9f))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(12.dp).background(highlightColor, RoundedCornerShape(6.dp)))
                Text("You", style = MaterialTheme.typography.labelSmall, color = PureWhite)
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.size(12.dp).background(AlertRed.copy(alpha = 0.8f), RoundedCornerShape(6.dp)))
                Text("Ghost", style = MaterialTheme.typography.labelSmall, color = PureWhite)
            }
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.Slider(
                value = progressMs.toFloat() / maxDuration,
                onValueChange = { progressMs = (it * maxDuration).toLong() },
                colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = highlightColor, activeTrackColor = highlightColor),
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { isPlaying = !isPlaying }) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null, tint = highlightColor)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1f, 2f, 5f).forEach { mult ->
                        OutlinedButton(
                            onClick = { speedMultiplier = mult },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (speedMultiplier == mult) highlightColor.copy(alpha = 0.2f) else Color.Transparent
                            ),
                            border = BorderStroke(1.dp, if (speedMultiplier == mult) highlightColor else BorderDivider),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("${mult.toInt()}x", color = if (speedMultiplier == mult) highlightColor else MutedGrey,
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                TextButton(onClick = onClose) {
                    Text("Close", color = MutedGrey, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

private fun interpolatePosition(points: List<TelemetryPoint>, offsetMs: Long): Pair<Double, Double>? {
    if (points.isEmpty()) return null
    val t0 = points.first().timestamp
    val target = t0 + offsetMs
    if (target <= t0) return Pair(points.first().latitude, points.first().longitude)
    if (target >= points.last().timestamp) return Pair(points.last().latitude, points.last().longitude)
    var lo = 0; var hi = points.lastIndex
    while (lo < hi - 1) { val mid = (lo + hi) / 2; if (points[mid].timestamp <= target) lo = mid else hi = mid }
    val a = points[lo]; val b = points[hi]
    val frac = if (b.timestamp == a.timestamp) 0.0 else (target - a.timestamp).toDouble() / (b.timestamp - a.timestamp)
    return Pair(a.latitude + (b.latitude - a.latitude) * frac, a.longitude + (b.longitude - a.longitude) * frac)
}
