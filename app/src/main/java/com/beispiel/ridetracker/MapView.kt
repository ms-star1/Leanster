@file:Suppress("DEPRECATION")

package com.beispiel.ridetracker

import android.location.Location
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.foundation.shape.CircleShape
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
    LeansterTheme {
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

// ── Data helpers ─────────────────────────────────────────────────────────────

data class CornerStat(val maxLean: Float, val speedAtApex: Double)

private fun computeCornerStats(session: RideSession): List<CornerStat> =
    session.corners.map { c ->
        val lean = maxOf(abs(c.maxLeftLean), c.maxRightLean)
        val speed = if (c.maxLeanIndex in session.points.indices) session.points[c.maxLeanIndex].speedKmh else 0.0
        CornerStat(lean, speed)
    }

private fun interpolateStats(points: List<TelemetryPoint>, offsetMs: Long): Pair<Float, Double>? {
    if (points.isEmpty()) return null
    val t0 = points.first().timestamp
    val target = t0 + offsetMs
    if (target <= t0) return Pair(points.first().leanAngle, points.first().speedKmh)
    if (target >= points.last().timestamp) return Pair(points.last().leanAngle, points.last().speedKmh)
    var lo = 0; var hi = points.lastIndex
    while (lo < hi - 1) { val mid = (lo + hi) / 2; if (points[mid].timestamp <= target) lo = mid else hi = mid }
    val a = points[lo]; val b = points[hi]
    val frac = if (b.timestamp == a.timestamp) 0.0 else (target - a.timestamp).toDouble() / (b.timestamp - a.timestamp)
    return Pair((a.leanAngle + (b.leanAngle - a.leanAngle) * frac).toFloat(), a.speedKmh + (b.speedKmh - a.speedKmh) * frac)
}

private fun matchCornerStats(
    currentCorners: List<CornerEvent>, currentStats: List<CornerStat>,
    ghostCorners: List<CornerEvent>, ghostStats: List<CornerStat>
): List<Pair<CornerStat?, CornerStat?>> = currentCorners.mapIndexed { i, cur ->
    val ghostIdx = if (cur.centroidLat != 0.0 && ghostCorners.isNotEmpty()) {
        ghostCorners.indices.minByOrNull { j ->
            val g = ghostCorners[j]
            val dl = cur.centroidLat - g.centroidLat; val dg = cur.centroidLng - g.centroidLng
            dl * dl + dg * dg
        } ?: i
    } else i
    Pair(currentStats.getOrNull(i), ghostStats.getOrNull(ghostIdx))
}

// ── Ghost Replay Screen ───────────────────────────────────────────────────────

@Composable
fun GhostReplayScreen(
    currentSession: RideSession,
    ghostSession: RideSession,
    highlightColor: Color,
    mapView: MapView,          // retained for API compat, not used in canvas mode
    onClose: () -> Unit
) {
    val curPts  = currentSession.points
    val ghoPts  = ghostSession.points

    var isPlaying  by remember { mutableStateOf(false) }
    val speedMult  by remember { mutableStateOf(1f) }
    var progressMs by remember { mutableStateOf(0L) }

    val curDuration = remember(curPts)  { ((curPts.lastOrNull()?.timestamp  ?: 0L) - (curPts.firstOrNull()?.timestamp  ?: 0L)).coerceAtLeast(1L) }
    val ghoDuration = remember(ghoPts) { ((ghoPts.lastOrNull()?.timestamp ?: 0L) - (ghoPts.firstOrNull()?.timestamp ?: 0L)).coerceAtLeast(1L) }
    val maxDuration = maxOf(curDuration, ghoDuration)

    LaunchedEffect(isPlaying, speedMult) {
        while (isPlaying) {
            delay(16L)
            progressMs = (progressMs + (16L * speedMult).toLong()).coerceAtMost(maxDuration)
            if (progressMs >= maxDuration) isPlaying = false
        }
    }

    val curPos   = remember(progressMs) { interpolatePosition(curPts,  progressMs) }
    val ghoPos   = remember(progressMs) { interpolatePosition(ghoPts, progressMs) }
    val curStats = remember(progressMs) { interpolateStats(curPts,  progressMs) }
    val ghoStats = remember(progressMs) { interpolateStats(ghoPts, progressMs) }

    val curCornerStats = remember(currentSession) { computeCornerStats(currentSession) }
    val ghoCornerStats = remember(ghostSession)   { computeCornerStats(ghostSession) }
    val matched = remember(currentSession.corners, ghostSession.corners) {
        matchCornerStats(currentSession.corners, curCornerStats, ghostSession.corners, ghoCornerStats)
    }

    // GPS projection bounds
    val allPts  = curPts + ghoPts
    val minLat  = allPts.minOfOrNull { it.latitude  } ?: 0.0
    val maxLat  = allPts.maxOfOrNull { it.latitude  } ?: 1.0
    val minLng  = allPts.minOfOrNull { it.longitude } ?: 0.0
    val maxLng  = allPts.maxOfOrNull { it.longitude } ?: 1.0
    val latRange = (maxLat - minLat).coerceAtLeast(0.0001)
    val lngRange = (maxLng - minLng).coerceAtLeast(0.0001)

    val mapBg      = Color(0xFF070908)
    val roadBg     = Color(0xFF1A2218)
    val gridCol    = Color(0xFF111610)
    val textMuted  = MutedGrey
    val textMid    = MidGrey
    val surfCol    = Color(0xFF1D211B)
    val borderCol  = Color(0xFF2A3028)

    fun fmtTime(ms: Long) = "%02d:%02d".format(ms / 60000, (ms % 60000) / 1000)

    // Cumulative distance arrays, for time-delta lookup.
    val curCum = remember(curPts) { cumulativeDistances(curPts) }
    val ghoCum = remember(ghoPts) { cumulativeDistances(ghoPts) }

    // Time delta: at the distance you have covered, how much sooner/later the ghost reached it.
    val curDistNow = remember(progressMs) {
        val tEnd = (curPts.firstOrNull()?.timestamp ?: 0L) + progressMs
        interpDistanceAt(curPts, curCum, tEnd)
    }
    val timeDeltaMs = remember(progressMs) {
        progressMs - timeForDistance(ghoPts, ghoCum, curDistNow)   // negative = you are ahead
    }

    // Most recently passed corner on your run.
    val lastCornerLabel = remember(progressMs) {
        val start = curPts.firstOrNull()?.timestamp ?: 0L
        val passed = currentSession.corners.withIndex().filter { (_, c) ->
            c.maxLeanIndex in curPts.indices && curPts[c.maxLeanIndex].timestamp - start <= progressMs
        }
        passed.lastOrNull()?.let { "AT T${it.index + 1}" } ?: "AT START"
    }

    val dateLabel = remember(currentSession.startTime) {
        java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(java.util.Date(currentSession.startTime)).uppercase()
    }
    val totalDistKm = remember(curPts) { curPts.sumOf { it.distanceDelta } / 1000.0 }

    Box(modifier = Modifier.fillMaxSize().background(mapBg)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ──
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 4.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(44.dp)) {
                    Text("←", color = textMuted, style = MaterialTheme.typography.titleMedium)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("GHOST REPLAY",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = Inter, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
                        color = PureWhite)
                    Text("$dateLabel · ${"%.1f".format(totalDistKm)} KM · ${currentSession.corners.size} CORNERS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, letterSpacing = 0.5.sp),
                        color = textMuted)
                }
            }

            // ── Canvas map ──
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val pad  = 36.dp.toPx()
                    val mapW = size.width  - pad * 2
                    val mapH = size.height - pad * 2

                    // Aspect-correct projection
                    val latCorr  = cos(Math.toRadians((minLat + maxLat) / 2)).toFloat()
                    val gpsAsp   = (lngRange * latCorr / latRange).toFloat()
                    val scrAsp   = mapW / mapH
                    val scX: Float; val scY: Float; val offX: Float; val offY: Float
                    if (gpsAsp > scrAsp) {
                        scX = mapW; scY = mapW / gpsAsp; offX = pad; offY = pad + (mapH - scY) / 2
                    } else {
                        scY = mapH; scX = mapH * gpsAsp; offY = pad; offX = pad + (mapW - scX) / 2
                    }
                    fun px(lng: Double) = (offX + ((lng - minLng) / lngRange * scX)).toFloat()
                    fun py(lat: Double) = (offY + ((maxLat - lat) / latRange * scY)).toFloat()

                    // Grid
                    val gs = 44.dp.toPx()
                    var gx = 0f; while (gx <= size.width)  { drawLine(gridCol, Offset(gx, 0f), Offset(gx, size.height), 0.5.dp.toPx()); gx += gs }
                    var gy = 0f; while (gy <= size.height) { drawLine(gridCol, Offset(0f, gy), Offset(size.width, gy), 0.5.dp.toPx()); gy += gs }

                    // Road base (thick dark stroke — ghost route)
                    if (ghoPts.size >= 2) {
                        val rp = Path(); ghoPts.forEachIndexed { i, p -> if (i == 0) rp.moveTo(px(p.longitude), py(p.latitude)) else rp.lineTo(px(p.longitude), py(p.latitude)) }
                        drawPath(rp, roadBg, style = Stroke(20.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }

                    // Ghost trace (highlightColor)
                    if (ghoPts.size >= 2) {
                        val gp = Path(); ghoPts.forEachIndexed { i, p -> if (i == 0) gp.moveTo(px(p.longitude), py(p.latitude)) else gp.lineTo(px(p.longitude), py(p.latitude)) }
                        drawPath(gp, highlightColor.copy(alpha = 0.9f), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }

                    // Current (you) trace — white, low alpha
                    if (curPts.size >= 2) {
                        val cp = Path(); curPts.forEachIndexed { i, p -> if (i == 0) cp.moveTo(px(p.longitude), py(p.latitude)) else cp.lineTo(px(p.longitude), py(p.latitude)) }
                        drawPath(cp, Color(0xFFEEF2EE).copy(alpha = 0.35f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }

                    // Corner markers (on ghost route)
                    ghostSession.corners.forEachIndexed { i, c ->
                        if (c.centroidLat != 0.0) {
                            val cx = px(c.centroidLng); val cy = py(c.centroidLat)
                            drawCircle(highlightColor.copy(alpha = 0.65f), 5.5.dp.toPx(), Offset(cx, cy), style = Stroke(1.5.dp.toPx()))
                        }
                    }

                    // Start dot
                    ghoPts.firstOrNull()?.let { drawCircle(Color(0xFF5E655D), 4.dp.toPx(), Offset(px(it.longitude), py(it.latitude))) }

                    // Ghost dot (highlightColor, glowing)
                    ghoPos?.let { (lat, lng) ->
                        val cx = px(lng); val cy = py(lat)
                        drawCircle(highlightColor.copy(alpha = 0.12f), 20.dp.toPx(), Offset(cx, cy))
                        drawCircle(highlightColor.copy(alpha = 0.28f), 13.dp.toPx(), Offset(cx, cy))
                        drawCircle(highlightColor,                      8.dp.toPx(),  Offset(cx, cy))
                    }

                    // You dot (white, subtle)
                    curPos?.let { (lat, lng) ->
                        val cx = px(lng); val cy = py(lat)
                        drawCircle(Color(0xFFEEF2EE).copy(alpha = 0.18f), 14.dp.toPx(), Offset(cx, cy))
                        drawCircle(Color(0xFFEEF2EE).copy(alpha = 0.60f),  7.dp.toPx(), Offset(cx, cy))
                    }
                }

                // ── Delta badge ──
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = mapBg.copy(alpha = 0.92f)),
                        border = BorderStroke(1.dp, Color(0xFF2A4020))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            val ahead = timeDeltaMs <= 0
                            val sign  = if (ahead) "−" else "+"
                            val secs  = abs(timeDeltaMs) / 1000.0
                            Text("$sign${"%.1f".format(secs)}s",
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = Rajdhani, fontWeight = FontWeight.Bold),
                                color = if (ahead) highlightColor else AlertRed)
                            Text(lastCornerLabel, style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), color = textMuted)
                        }
                    }
                }

                // ── Live lean + speed overlay (bottom-left of map) ──
                if (curStats != null && ghoStats != null) {
                    Row(
                        modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LiveStatPill("G ${abs(ghoStats.first).toInt()}°  ${ghoStats.second.toInt()}", highlightColor, mapBg)
                        LiveStatPill("Y ${abs(curStats.first).toInt()}°  ${curStats.second.toInt()}", textMid, mapBg)
                    }
                }
            }

            // ── Bottom panel ──
            Column(
                modifier = Modifier.fillMaxWidth().background(mapBg).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Legend
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(20.dp, 2.5.dp).background(highlightColor, RoundedCornerShape(2.dp)))
                        Text("GHOST", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp), color = textMid)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(20.dp, 2.dp).background(Color(0xFFEEF2EE).copy(alpha = 0.35f), RoundedCornerShape(2.dp)))
                        Text("YOU", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp), color = textMuted)
                    }
                }

                // Corner splits (lean + speed per corner)
                if (matched.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(matched.size) { i ->
                            val (cSt, gSt) = matched[i]
                            val better = (cSt?.maxLean ?: 0f) >= (gSt?.maxLean ?: 0f)
                            val chipBg  = if (better) highlightColor.copy(alpha = 0.14f) else surfCol
                            val chipBdr = if (better) highlightColor.copy(alpha = 0.45f) else borderCol
                            Card(shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = chipBg),
                                border = BorderStroke(1.dp, chipBdr)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("T${i + 1}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = Rajdhani, letterSpacing = 0.8.sp),
                                        color = if (better) highlightColor else textMuted)
                                    if (cSt != null && gSt != null) {
                                        Text("${cSt.maxLean.toInt()}° / ${gSt.maxLean.toInt()}°",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = Rajdhani),
                                            color = if (better) PureWhite else textMid)
                                        Text("${cSt.speedAtApex.toInt()} · ${gSt.speedAtApex.toInt()}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = textMuted)
                                    } else {
                                        Text("—", style = MaterialTheme.typography.labelSmall, color = Color(0xFF3A4036))
                                    }
                                }
                            }
                        }
                    }
                }

                // Scrubber
                Slider(
                    value = if (maxDuration > 0) progressMs.toFloat() / maxDuration else 0f,
                    onValueChange = { progressMs = (it * maxDuration).toLong(); isPlaying = false },
                    colors = SliderDefaults.colors(
                        thumbColor = highlightColor,
                        activeTrackColor = highlightColor,
                        inactiveTrackColor = surfCol
                    ),
                    modifier = Modifier.fillMaxWidth().height(20.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(fmtTime(progressMs), style = MaterialTheme.typography.labelSmall.copy(fontFamily = Rajdhani), color = textMuted)
                    Text(fmtTime(maxDuration), style = MaterialTheme.typography.labelSmall.copy(fontFamily = Rajdhani), color = textMuted)
                }

                // Transport controls — skip back / play-pause / skip forward
                Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterHorizontally)
                ) {
                    Text("⟨⟨", style = MaterialTheme.typography.titleLarge, color = textMuted,
                        modifier = Modifier.clickable { progressMs = (progressMs - 5000L).coerceAtLeast(0L) })

                    Box(
                        modifier = Modifier.size(48.dp).background(highlightColor, CircleShape)
                            .clickable {
                                if (progressMs >= maxDuration) progressMs = 0L
                                isPlaying = !isPlaying
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color(0xFF070908),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text("⟩⟩", style = MaterialTheme.typography.titleLarge, color = textMuted,
                        modifier = Modifier.clickable { progressMs = (progressMs + 5000L).coerceAtMost(maxDuration) })
                }
            }
        }
    }
}

@Composable
private fun LiveStatPill(text: String, color: Color, bg: Color) {
    Box(modifier = Modifier
        .background(bg.copy(alpha = 0.88f), RoundedCornerShape(6.dp))
        .padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(text, style = MaterialTheme.typography.labelSmall.copy(fontFamily = Rajdhani, fontWeight = FontWeight.Bold), color = color)
    }
}

/** Cumulative travelled distance (metres) at every point index. */
private fun cumulativeDistances(points: List<TelemetryPoint>): DoubleArray {
    val out = DoubleArray(points.size)
    var acc = 0.0
    points.forEachIndexed { i, p -> acc += p.distanceDelta; out[i] = acc }
    return out
}

/** Distance (metres) travelled by the given absolute timestamp, interpolated. */
private fun interpDistanceAt(points: List<TelemetryPoint>, cum: DoubleArray, targetTs: Long): Double {
    if (points.isEmpty()) return 0.0
    if (targetTs <= points.first().timestamp) return 0.0
    if (targetTs >= points.last().timestamp) return cum.last()
    var lo = 0; var hi = points.lastIndex
    while (lo < hi - 1) { val mid = (lo + hi) / 2; if (points[mid].timestamp <= targetTs) lo = mid else hi = mid }
    val frac = if (points[hi].timestamp == points[lo].timestamp) 0.0
        else (targetTs - points[lo].timestamp).toDouble() / (points[hi].timestamp - points[lo].timestamp)
    return cum[lo] + (cum[hi] - cum[lo]) * frac
}

/** Elapsed time (ms from start) when cumulative distance first reaches [targetDist], interpolated. */
private fun timeForDistance(points: List<TelemetryPoint>, cum: DoubleArray, targetDist: Double): Long {
    if (points.isEmpty()) return 0L
    val start = points.first().timestamp
    if (targetDist <= 0.0) return 0L
    if (targetDist >= cum.last()) return points.last().timestamp - start
    val idx = cum.indexOfFirst { it >= targetDist }
    if (idx <= 0) return 0L
    val d0 = cum[idx - 1]; val d1 = cum[idx]
    val frac = if (d1 == d0) 0.0 else (targetDist - d0) / (d1 - d0)
    val t = points[idx - 1].timestamp + (points[idx].timestamp - points[idx - 1].timestamp) * frac
    return (t - start).toLong()
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
