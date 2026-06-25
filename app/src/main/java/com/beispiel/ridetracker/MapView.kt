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
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderDivider)
                    ) {
                        Text("Next ⏭️", color = PureWhite)
                    }
                }
            }
        }
    }
}
