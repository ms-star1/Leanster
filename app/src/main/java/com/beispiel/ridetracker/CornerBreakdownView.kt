package com.beispiel.ridetracker

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.view.GestureDetector
import android.view.MotionEvent
import com.beispiel.ridetracker.ui.theme.*
import kotlin.math.abs
import kotlin.math.roundToInt
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.annotations.PolylineOptions

/** One row in the corner breakdown — this run vs. the all-time best for the same corner. */
data class CornerRow(
    val index: Int,
    val distanceKm: Double,
    val thisLean: Float,
    val bestLean: Float?,
    val isPb: Boolean
)

data class Top20CornerItem(
    val corner: CornerEvent,
    val timeRank: Int,
    val lean: Float,
    val direction: String,
    val apexPoint: TelemetryPoint?,
    val midpointPoint: TelemetryPoint?,
    val entrySpeed: Double,
    val exitSpeed: Double,
    val avgSpeed: Double
)

fun computeCornerRows(session: RideSession, allSessions: List<RideSession>): List<CornerRow> {
    val cumKm = DoubleArray(session.points.size)
    var acc = 0.0
    session.points.forEachIndexed { i, p -> acc += p.distanceDelta; cumKm[i] = acc / 1000.0 }
    val others = allSessions.filter { it.id != session.id }
    return session.corners.mapIndexed { i, c ->
        val thisLean = maxOf(abs(c.maxLeftLean), c.maxRightLean)
        val distKm = if (c.maxLeanIndex in session.points.indices) cumKm[c.maxLeanIndex] else 0.0
        val bestLean: Float? = if (c.centroidLat != 0.0) {
            others.flatMap { o ->
                o.corners.filter { oc ->
                    oc.centroidLat != 0.0 &&
                        abs(oc.centroidLat - c.centroidLat) < 0.0003 &&
                        abs(oc.centroidLng - c.centroidLng) < 0.0003
                }.map { oc -> maxOf(abs(oc.maxLeftLean), oc.maxRightLean) }
            }.maxOrNull()
        } else null
        CornerRow(
            index = i + 1, distanceKm = distKm, thisLean = thisLean, bestLean = bestLean,
            isPb = bestLean == null || thisLean >= bestLean
        )
    }
}

fun computeTop20Corners(session: RideSession): List<Top20CornerItem> {
    val top20 = session.corners
        .sortedByDescending { maxOf(abs(it.maxLeftLean), it.maxRightLean) }
        .take(20)
        .sortedBy { it.startIndex }

    return top20.mapIndexed { i, corner ->
        val lean = maxOf(abs(corner.maxLeftLean), corner.maxRightLean)
        val dir = if (abs(corner.maxLeftLean) > corner.maxRightLean) "L" else "R"
        val apexPt = session.points.getOrNull(corner.maxLeanIndex)
        val entryPt = session.points.getOrNull(corner.startIndex)
        val endIdx = if (corner.endIndex >= 0 && corner.endIndex < session.points.size)
            corner.endIndex else corner.maxLeanIndex
        val exitPt = session.points.getOrNull(endIdx)
        val midIdx = (corner.startIndex + endIdx) / 2
        val midPt = session.points.getOrNull(midIdx)
        val cornerPts = if (corner.startIndex >= 0 && endIdx >= corner.startIndex && endIdx < session.points.size)
            session.points.subList(corner.startIndex, endIdx + 1)
        else emptyList()
        val avgSpd = if (cornerPts.isNotEmpty()) cornerPts.map { it.speedKmh }.average()
                     else apexPt?.speedKmh ?: 0.0

        Top20CornerItem(
            corner = corner,
            timeRank = i + 1,
            lean = lean,
            direction = dir,
            apexPoint = apexPt,
            midpointPoint = midPt,
            entrySpeed = entryPt?.speedKmh ?: 0.0,
            exitSpeed = exitPt?.speedKmh ?: 0.0,
            avgSpeed = avgSpd
        )
    }
}

/**
 * Rejects zero-coords and exactly the dev-fallback used in old sessions (52.5200, 13.4050).
 * Epsilon ~1 m — real Berlin GPS will never hit this exact coordinate.
 */
fun isValidGps(lat: Double, lng: Double) =
    lat != 0.0 && lng != 0.0 &&
    !(kotlin.math.abs(lat - 52.5200) < 0.00001 && kotlin.math.abs(lng - 13.4050) < 0.00001)

private const val DEFAULT_CORNER_MAP_ZOOM = 18.0

private val SAT_STYLE = """
{
  "version": 8,
  "sources": {
    "sat": {
      "type": "raster",
      "tiles": ["https://server.arcgisonline.com/arcgis/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"],
      "tileSize": 256,
      "maxzoom": 19
    },
    "labels": {
      "type": "raster",
      "tiles": [
        "https://a.basemaps.cartocdn.com/dark_only_labels/{z}/{x}/{y}.png",
        "https://b.basemaps.cartocdn.com/dark_only_labels/{z}/{x}/{y}.png",
        "https://c.basemaps.cartocdn.com/dark_only_labels/{z}/{x}/{y}.png",
        "https://d.basemaps.cartocdn.com/dark_only_labels/{z}/{x}/{y}.png"
      ],
      "tileSize": 256,
      "maxzoom": 18
    }
  },
  "layers": [
    {
      "id": "sat",
      "type": "raster",
      "source": "sat"
    },
    {
      "id": "labels",
      "type": "raster",
      "source": "labels"
    }
  ]
}
""".trimIndent()

@Composable
fun CornerMapScreen(
    session: RideSession,
    highlightColor: Color,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current

    val top20 = remember(session) { computeTop20Corners(session) }

    // Explicit state objects so MapLibre callbacks (main thread) can update them safely
    val activeMapState = remember { mutableStateOf<MapLibreMap?>(null) }
    val cameraTickState = remember { mutableStateOf(0) }
    val selectedIdxState = remember { mutableStateOf(0) }
    val isExpandedState = remember { mutableStateOf(false) }
    // Zoom: null means "at default". Set by user gestures; reset to null on close.
    val currentZoomState = remember { mutableStateOf(DEFAULT_CORNER_MAP_ZOOM) }

    val selectedIdx = selectedIdxState.value
    val isExpanded = isExpandedState.value
    val currentZoom = currentZoomState.value
    val showZoomReset = abs(currentZoom - DEFAULT_CORNER_MAP_ZOOM) > 0.3

    // Screen-space positions of corner markers, recomputed whenever camera moves
    val cornerPositions: List<Offset?> = remember(cameraTickState.value, activeMapState.value) {
        val map = activeMapState.value ?: return@remember List<Offset?>(top20.size) { null }
        top20.map { item ->
            val lat = item.midpointPoint?.latitude ?: item.apexPoint?.latitude
            val lng = item.midpointPoint?.longitude ?: item.apexPoint?.longitude
            if (lat != null && lng != null && isValidGps(lat, lng)) {
                try {
                    val sp = map.projection.toScreenLocation(LatLng(lat, lng))
                    Offset(sp.x, sp.y)
                } catch (e: Exception) { null }
            } else null
        }
    }

    // Stable reference so the MapView GestureDetector (Android touch system) can
    // always read the latest screen positions without capturing a recomposed value.
    val latestCornerPositions = remember { mutableStateOf<List<Offset?>>(emptyList()) }
    SideEffect { latestCornerPositions.value = cornerPositions }
    val hitRadiusPx = with(density) { 28.dp.toPx() }

    // Text paint objects (remembered to avoid per-frame allocation)
    val labelTextSizePx = with(density) { 9.sp.toPx() }
    val labelPaint = remember(labelTextSizePx) {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = labelTextSizePx
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
    }
    val labelShadowPaint = remember(labelTextSizePx) {
        Paint().apply {
            color = android.graphics.Color.argb(200, 0, 0, 0)
            textSize = labelTextSizePx
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    // Dedicated satellite MapView for this screen
    val mapView = remember {
        var cameraMoveFromGesture = false
        // GestureDetector runs in Android's touch system alongside MapLibre,
        // so pan/pinch-zoom flow through normally — we only intercept confirmed taps.
        val tapDetector = GestureDetector(context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    val positions = latestCornerPositions.value
                    val hitIdx = positions.indexOfFirst { cp ->
                        cp != null && run {
                            val dx = e.x - cp.x
                            val dy = e.y - cp.y
                            kotlin.math.sqrt(dx * dx + dy * dy) <= hitRadiusPx
                        }
                    }
                    if (hitIdx >= 0) {
                        selectedIdxState.value = hitIdx
                        isExpandedState.value = false
                        return true
                    }
                    return false
                }
            }
        )
        MapView(context).apply {
            setOnTouchListener { _, event ->
                tapDetector.onTouchEvent(event)
                false  // never consume — MapLibre handles all pan/zoom/drag
            }
            onCreate(null)
            getMapAsync { map ->
                activeMapState.value = map
                map.uiSettings.isLogoEnabled = false
                map.uiSettings.isAttributionEnabled = false
                map.setStyle(Style.Builder().fromJson(SAT_STYLE)) {
                    // Route polyline
                    val routePoints = session.points
                        .filter { isValidGps(it.latitude, it.longitude) }
                        .map { LatLng(it.latitude, it.longitude) }
                    if (routePoints.size >= 2) {
                        map.addPolyline(
                            PolylineOptions()
                                .addAll(routePoints)
                                .color(PureWhite.copy(alpha = 0.55f).toArgb())
                                .width(2.5f)
                        )
                    }
                    // Center camera on first (time-sorted) top-20 corner
                    val first = top20.firstOrNull()
                    if (first != null) {
                        val lat = first.midpointPoint?.latitude ?: first.apexPoint?.latitude
                        val lng = first.midpointPoint?.longitude ?: first.apexPoint?.longitude
                        if (lat != null && lat != 0.0 && lng != null && lng != 0.0) {
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), DEFAULT_CORNER_MAP_ZOOM))
                        }
                    } else {
                        val mid = session.points
                            .filter { isValidGps(it.latitude, it.longitude) }
                            .getOrNull(session.points.size / 2)
                        if (mid != null) {
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(mid.latitude, mid.longitude), 15.0))
                        }
                    }
                    cameraTickState.value++
                }
                map.addOnCameraMoveStartedListener { reason ->
                    cameraMoveFromGesture = (reason == 1)
                    if (reason == 1) isExpandedState.value = false
                }
                // Continuous tick keeps marker overlay in sync while panning
                map.addOnCameraMoveListener { cameraTickState.value++ }
                // Capture zoom only after a user gesture finishes
                map.addOnCameraIdleListener {
                    if (cameraMoveFromGesture) {
                        currentZoomState.value = map.cameraPosition.zoom
                        cameraMoveFromGesture = false
                    }
                }
            }
        }
    }

    // Animate camera to selected corner, preserving whatever zoom the user has set
    LaunchedEffect(selectedIdx, activeMapState.value) {
        val map = activeMapState.value ?: return@LaunchedEffect
        val item = top20.getOrNull(selectedIdx) ?: return@LaunchedEffect
        val lat = item.midpointPoint?.latitude ?: item.apexPoint?.latitude ?: return@LaunchedEffect
        val lng = item.midpointPoint?.longitude ?: item.apexPoint?.longitude ?: return@LaunchedEffect
        if (lat != 0.0 && lng != 0.0) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), currentZoomState.value))
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START  -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE  -> mapView.onPause()
                Lifecycle.Event.ON_STOP   -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (top20.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(DeepCarbon), contentAlignment = Alignment.Center) {
            Text("No corners detected in this session.", color = MutedGrey, fontSize = 14.sp)
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Satellite map background ──
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // ── Corner marker overlay — pure drawing, no pointer input ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            cornerPositions.forEachIndexed { i, pos ->
                pos ?: return@forEachIndexed
                val item = top20.getOrNull(i) ?: return@forEachIndexed
                val isSelected = i == selectedIdx
                val markerColor = if (item.direction == "R") highlightColor else AlertRed
                val radius = if (isSelected) 20.dp.toPx() else 13.dp.toPx()
                val strokeW = if (isSelected) 2.5.dp.toPx() else 1.5.dp.toPx()

                // Glow ring for selected marker
                if (isSelected) {
                    drawCircle(markerColor.copy(alpha = 0.20f), radius + 9.dp.toPx(), pos)
                    drawCircle(markerColor.copy(alpha = 0.10f), radius + 16.dp.toPx(), pos)
                }

                // Fill
                drawCircle(
                    color = if (isSelected) markerColor.copy(alpha = 0.32f) else DeepCarbon.copy(alpha = 0.82f),
                    radius = radius,
                    center = pos
                )
                // Border
                drawCircle(
                    color = markerColor.copy(alpha = if (isSelected) 1f else 0.65f),
                    radius = radius,
                    center = pos,
                    style = Stroke(strokeW)
                )

                // Label (T1, T2, …)
                val label = "T${i + 1}"
                val textY = pos.y + 3.5.dp.toPx()
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(label, pos.x + 0.6f, textY + 0.6f, labelShadowPaint)
                    canvas.nativeCanvas.drawText(label, pos.x, textY, labelPaint)
                }
            }
        }

        // ── Header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .background(DeepCarbon.copy(alpha = 0.90f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "←", fontSize = 20.sp, color = MutedGrey,
                modifier = Modifier.clickable(onClick = onClose)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "TOP ${top20.size} CORNERS",
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = PureWhite, fontFamily = Inter
                )
                Text(
                    "RANKED BY LEAN · SORTED BY TIME",
                    fontSize = 10.sp, letterSpacing = 0.6.sp,
                    color = MutedGrey, fontFamily = FontFamily.Monospace
                )
            }
        }

        // ── Zoom reset chip (shown when user has changed zoom away from default) ──
        if (showZoomReset) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 68.dp, end = 12.dp)
                    .background(DeepCarbon.copy(alpha = 0.92f), RoundedCornerShape(8.dp))
                    .border(
                        BorderStroke(1.dp, highlightColor.copy(alpha = 0.35f)),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        currentZoomState.value = DEFAULT_CORNER_MAP_ZOOM
                        activeMapState.value?.animateCamera(
                            CameraUpdateFactory.zoomTo(DEFAULT_CORNER_MAP_ZOOM)
                        )
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        "Z${currentZoom.roundToInt()}",
                        fontSize = 12.sp, fontFamily = Rajdhani, fontWeight = FontWeight.Bold,
                        color = highlightColor
                    )
                    Text("↺", fontSize = 12.sp, color = MutedGrey)
                }
            }
        }

        // ── Bottom panel ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            val selectedItem = top20.getOrNull(selectedIdx)
            if (selectedItem != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DeepCarbon.copy(alpha = 0.97f),
                    shadowElevation = 12.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {

                        // Collapsed row — always visible
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpandedState.value = !isExpanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "T${selectedIdx + 1}",
                                    fontSize = 12.sp, letterSpacing = 1.sp,
                                    color = MutedGrey, fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    "${selectedItem.lean.toInt()}°",
                                    fontSize = 38.sp, fontWeight = FontWeight.Bold,
                                    color = PureWhite, fontFamily = Rajdhani,
                                    lineHeight = 38.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (selectedItem.direction == "R") highlightColor.copy(alpha = 0.14f)
                                            else AlertRed.copy(alpha = 0.14f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .border(
                                            BorderStroke(
                                                1.dp,
                                                if (selectedItem.direction == "R") highlightColor.copy(alpha = 0.45f)
                                                else AlertRed.copy(alpha = 0.45f)
                                            ),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        selectedItem.direction,
                                        fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                        color = if (selectedItem.direction == "R") highlightColor else AlertRed
                                    )
                                }
                                Text(
                                    "${(selectedItem.apexPoint?.speedKmh ?: 0.0).toInt()} km/h",
                                    fontSize = 14.sp, color = MidGrey, fontFamily = Rajdhani
                                )
                            }
                            Text(
                                if (isExpanded) "∧" else "∨",
                                fontSize = 16.sp, color = MutedGrey,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }

                        // Expanded details
                        if (isExpanded) {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = BorderDivider)
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CornerSpeedStat("ENTRY", selectedItem.entrySpeed, highlightColor)
                                Box(Modifier.width(1.dp).height(40.dp).background(BorderDivider))
                                CornerSpeedStat("AVG", selectedItem.avgSpeed, MidGrey)
                                Box(Modifier.width(1.dp).height(40.dp).background(BorderDivider))
                                CornerSpeedStat("EXIT", selectedItem.exitSpeed, highlightColor)
                            }
                        }
                    }
                }
            }

            // ── Navigation row ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF07090D))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (selectedIdxState.value > 0) {
                                selectedIdxState.value--
                                isExpandedState.value = false
                            }
                        },
                        enabled = selectedIdx > 0,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                        border = BorderStroke(1.dp, BorderDivider)
                    ) {
                        Text("‹ PREV", fontSize = 12.sp, letterSpacing = 0.6.sp)
                    }

                    Text(
                        "T${selectedIdx + 1} / ${top20.size}",
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp, fontFamily = Rajdhani, fontWeight = FontWeight.Bold,
                        color = highlightColor, textAlign = TextAlign.Center
                    )

                    OutlinedButton(
                        onClick = {
                            if (selectedIdxState.value < top20.size - 1) {
                                selectedIdxState.value++
                                isExpandedState.value = false
                            }
                        },
                        enabled = selectedIdx < top20.size - 1,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                        border = BorderStroke(1.dp, BorderDivider)
                    ) {
                        Text("NEXT ›", fontSize = 12.sp, letterSpacing = 0.6.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CornerSpeedStat(label: String, speedKmh: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label, fontSize = 9.sp, letterSpacing = 1.2.sp,
            color = MutedGrey, fontFamily = Inter
        )
        Spacer(Modifier.height(3.dp))
        Text(
            "${speedKmh.toInt()} km/h",
            fontSize = 17.sp, fontWeight = FontWeight.Bold,
            color = color, fontFamily = Rajdhani
        )
    }
}
