@file:Suppress("DEPRECATION")

package com.beispiel.ridetracker

import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beispiel.ridetracker.ui.theme.*
import kotlin.math.abs
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items

@Composable
fun HistoryMenuScreen(
    sessions: List<RideSession>,
    onBack: () -> Unit,
    onDeleteSession: (String) -> Unit,
    onSelectSession: (RideSession) -> Unit,
    onExportSession: (RideSession) -> Unit,
    highlightColor: Color = NeonCyan,
    isMetric: Boolean = true
) {
    val mutedHighlightColor = when (highlightColor) {
        YamahaBlue -> MutedYamahaBlue
        DucatiRed -> MutedDucatiRed
        KawasakiGreen -> MutedKawasakiGreen
        PureWhiteHighlight -> MutedWhiteHighlight
        else -> MutedCyan
    }

    val demoSession = remember { createDemoSession() }

    var sessionToDelete by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showReportCard by remember { mutableStateOf(false) }

    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            containerColor = SurfaceCard,
            title = { Text("Delete Session?", style = MaterialTheme.typography.titleLarge) },
            text = { Text("Are you sure you want to delete this session? This action cannot be undone.", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSession(sessionToDelete!!)
                    sessionToDelete = null
                }) {
                    Text("DELETE", color = AlertRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("CANCEL", style = MaterialTheme.typography.bodyLarge, color = PureWhite)
                }
            }
        )
    }

    val allDisplaySessions = remember(sessions) { listOf(demoSession) + sessions }

    val filteredSessions = remember(allDisplaySessions, searchQuery) {
        if (searchQuery.isBlank()) {
            allDisplaySessions
        } else {
            allDisplaySessions.filter { session ->
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val dateStr = dateFormat.format(java.util.Date(session.startTime))
                dateStr.contains(searchQuery, ignoreCase = true) ||
                session.id.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepCarbon)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Back", style = MaterialTheme.typography.labelLarge, color = PureWhite)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                "MY RIDES",
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = Inter),
                color = highlightColor
            )
            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = { showReportCard = !showReportCard },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (showReportCard) highlightColor.copy(alpha = 0.15f) else Color.Transparent
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (showReportCard) highlightColor else BorderDivider),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Report", color = if (showReportCard) highlightColor else MutedGrey,
                    style = MaterialTheme.typography.labelSmall)
            }
        }

        if (showReportCard) {
            ReportCardScreen(sessions = sessions, highlightColor = highlightColor, isMetric = isMetric)
            return@Column
        }

        // Search Bar at Header
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            placeholder = { Text("Filter sessions by date (e.g. 2026)...", color = MutedGrey) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = PureWhite,
                unfocusedTextColor = PureWhite,
                focusedContainerColor = SurfaceCard,
                unfocusedContainerColor = SurfaceCard,
                focusedBorderColor = highlightColor,
                unfocusedBorderColor = BorderDivider,
                cursorColor = highlightColor
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        if (filteredSessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (sessions.isEmpty()) "No sessions match search filter." else "No sessions match search filter.",
                    style = MaterialTheme.typography.bodyLarge, color = MutedGrey
                )
            }
        } else {
            if (isLandscape) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredSessions) { session ->
                        val idx = filteredSessions.indexOf(session) + 1
                        SessionHistoryCard(
                            session, onSelectSession,
                            onDeleteRequest = { if (session.id != DEMO_SESSION_ID) sessionToDelete = it },
                            onExportSession, highlightColor, mutedHighlightColor, isMetric,
                            isDemo = session.id == DEMO_SESSION_ID, rideIndex = idx
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredSessions) { session ->
                        val idx = filteredSessions.indexOf(session) + 1
                        SessionHistoryCard(
                            session, onSelectSession,
                            onDeleteRequest = { if (session.id != DEMO_SESSION_ID) sessionToDelete = it },
                            onExportSession, highlightColor, mutedHighlightColor, isMetric,
                            isDemo = session.id == DEMO_SESSION_ID, rideIndex = idx
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SessionHistoryCard(
    session: RideSession,
    onSelectSession: (RideSession) -> Unit,
    onDeleteRequest: (String) -> Unit,
    onExportSession: (RideSession) -> Unit,
    highlightColor: Color,
    mutedHighlightColor: Color,
    isMetric: Boolean = true,
    isDemo: Boolean = false,
    rideIndex: Int = 0
) {
    val cardBg    = Color(0xFF0E130D)
    val borderCol = Color(0xFF1D211B)

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onSelectSession(session) },
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderCol),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 0.dp, top = 0.dp, end = 14.dp, bottom = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Route thumbnail ──
            Box(
                modifier = Modifier
                    .size(width = 76.dp, height = 76.dp)
                    .background(Color(0xFF070908), RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    val pts = session.points
                    if (pts.size >= 2) {
                        val minLat = pts.minOf { it.latitude };  val maxLat = pts.maxOf { it.latitude }
                        val minLng = pts.minOf { it.longitude }; val maxLng = pts.maxOf { it.longitude }
                        val latR = (maxLat - minLat).coerceAtLeast(0.0001)
                        val lngR = (maxLng - minLng).coerceAtLeast(0.0001)
                        val latCorr = kotlin.math.cos(Math.toRadians((minLat + maxLat) / 2)).toFloat()
                        val gpsAsp = (lngR * latCorr / latR).toFloat()
                        val scrAsp = size.width / size.height
                        val scX: Float; val scY: Float; val ox: Float; val oy: Float
                        if (gpsAsp > scrAsp) {
                            scX = size.width; scY = size.width / gpsAsp; ox = 0f; oy = (size.height - scY) / 2
                        } else {
                            scY = size.height; scX = size.height * gpsAsp; oy = 0f; ox = (size.width - scX) / 2
                        }
                        fun px(lng: Double) = (ox + ((lng - minLng) / lngR * scX)).toFloat()
                        fun py(lat: Double) = (oy + ((maxLat - lat) / latR * scY)).toFloat()

                        // Road base
                        val road = Path(); pts.forEachIndexed { i, p -> if (i == 0) road.moveTo(px(p.longitude), py(p.latitude)) else road.lineTo(px(p.longitude), py(p.latitude)) }
                        drawPath(road, Color(0xFF1A2218), style = Stroke(7.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                        // Route line
                        val route = Path(); pts.forEachIndexed { i, p -> if (i == 0) route.moveTo(px(p.longitude), py(p.latitude)) else route.lineTo(px(p.longitude), py(p.latitude)) }
                        drawPath(route, highlightColor.copy(alpha = 0.85f), style = Stroke(1.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                        // Start / end dots
                        pts.firstOrNull()?.let { drawCircle(MutedGrey, 2.5.dp.toPx(), Offset(px(it.longitude), py(it.latitude))) }
                        pts.lastOrNull()?.let { drawCircle(highlightColor, 2.5.dp.toPx(), Offset(px(it.longitude), py(it.latitude))) }
                    } else {
                        drawCircle(Color(0xFF2A3028), 20.dp.toPx(), Offset(size.width / 2, size.height / 2))
                    }
                }
            }

            // ── Info column ──
            Column(modifier = Modifier.weight(1f).padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                val dateFmt = remember { java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault()) }
                val timeFmt = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
                val dateStr  = dateFmt.format(java.util.Date(session.startTime))
                val timeStr  = timeFmt.format(java.util.Date(session.startTime))
                val durMs    = if (session.endTime > session.startTime) session.endTime - session.startTime else 0L
                val durMin   = durMs / 60000L
                val distKm   = session.points.sumOf { it.distanceDelta } / 1000.0
                val distVal  = if (isMetric) "%.1fkm".format(distKm) else "%.1fmi".format(distKm * 0.621371)

                // Header: ride # + date
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (isDemo) {
                            Box(modifier = Modifier
                                .background(highlightColor.copy(alpha = 0.14f), RoundedCornerShape(5.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)) {
                                Text("DEMO", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = highlightColor)
                            }
                        } else {
                            Text("RIDE #$rideIndex",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = Rajdhani, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                color = highlightColor)
                        }
                    }
                    Text("$dateStr  $timeStr",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = Rajdhani),
                        color = MutedGrey)
                }

                // Stats row
                Text("$distVal  ·  ${durMin}min  ·  ${session.corners.size} corners",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = Rajdhani, fontSize = 11.sp),
                    color = MidGrey)

                // Lean row
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("L ${abs(session.maxLeanLeft).toInt()}°",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = Rajdhani, fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = PureWhite.copy(alpha = 0.75f))
                    Text("·",  style = MaterialTheme.typography.labelSmall, color = Color(0xFF3A4036))
                    Text("${session.maxLeanRight.toInt()}° R",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = Rajdhani, fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = PureWhite.copy(alpha = 0.75f))
                }
            }

            // ── Chevron ──
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                tint = mutedHighlightColor.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun SessionSummaryOverlay(
    session: RideSession,
    isMetric: Boolean,
    onClose: () -> Unit,
    highlightColor: Color = NeonCyan,
    mapView: org.maplibre.android.maps.MapView? = null,
    onGhostReplay: ((RideSession, RideSession) -> Unit)? = null,
    allSessions: List<RideSession> = emptyList(),
    onDelete: ((String) -> Unit)? = null
) {
    val dateFormat = remember { java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()) }
    val timeFormat = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
    val dateStr = dateFormat.format(java.util.Date(session.startTime))
    val startTimeStr = timeFormat.format(java.util.Date(session.startTime))
    val endTimeStr = if (session.endTime > 0L) timeFormat.format(java.util.Date(session.endTime)) else "--:--"

    val totalDistanceRaw = session.points.sumOf { it.distanceDelta } / 1000.0
    val totalDistance = if (isMetric) totalDistanceRaw else totalDistanceRaw * 0.621371
    val distUnit = if (isMetric) "km" else "mi"
    val maxSpeedRaw = session.points.maxByOrNull { it.speedKmh }?.speedKmh ?: 0.0
    val maxSpeed = if (isMetric) maxSpeedRaw else maxSpeedRaw * 0.621371
    val speedUnit = if (isMetric) "km/h" else "mph"
    val avgSpeedRaw = if (session.points.isNotEmpty()) session.points.map { it.speedKmh }.average() else 0.0
    val avgSpeed = if (isMetric) avgSpeedRaw else avgSpeedRaw * 0.621371

    val durationMs = if (session.endTime > 0L) session.endTime - session.startTime else 0L
    val durationStr = if (durationMs > 0) {
        val h = durationMs / 3600000; val m = (durationMs % 3600000) / 60000
        if (h > 0) "${h}h ${m}m" else "${m}m"
    } else "--"

    val ghostSession = remember(allSessions, session) {
        val others = allSessions.filter { it.id != session.id }
        others.maxByOrNull { other ->
            session.corners.count { sc ->
                other.corners.any { oc ->
                    kotlin.math.abs(sc.centroidLat - oc.centroidLat) < 0.0003 &&
                    kotlin.math.abs(sc.centroidLng - oc.centroidLng) < 0.0003
                }
            }
        } ?: others.maxByOrNull { it.startTime }
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepCarbon)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                    Text("✕", color = PureWhite, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("SESSION DETAILS",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                        color = highlightColor)
                    Text("$dateStr  ·  $startTimeStr – $endTimeStr",
                        style = MaterialTheme.typography.labelSmall, color = MutedGrey)
                }
                Spacer(Modifier.size(36.dp))
            }

            HorizontalDivider(color = BorderDivider)

            // ── Stats grid — row 1 ──
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OverlayStatCard("MAX LEAN L", "${abs(session.maxLeanLeft).toInt()}°", AlertRed, Modifier.weight(1f))
                OverlayStatCard("MAX LEAN R", "${session.maxLeanRight.toInt()}°", highlightColor, Modifier.weight(1f))
                OverlayStatCard("TOP SPEED", "${maxSpeed.toInt()} $speedUnit", PureWhite, Modifier.weight(1f))
            }

            // ── Stats grid — row 2 ──
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OverlayStatCard("DISTANCE", "${"%.1f".format(totalDistance)} $distUnit", highlightColor, Modifier.weight(1f))
                OverlayStatCard("AVG SPEED", "${avgSpeed.toInt()} $speedUnit", PureWhite, Modifier.weight(1f))
                OverlayStatCard("DURATION", durationStr, PureWhite, Modifier.weight(1f))
            }

            // ── Stats grid — row 3 ──
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OverlayStatCard("CORNERS", session.corners.size.toString(), highlightColor, Modifier.weight(1f))
                OverlayStatCard("DATA PTS", session.points.size.toString(), PureWhite, Modifier.weight(1f))
                Spacer(Modifier.weight(1f))
            }

            // ── Lean symmetry ──
            if (session.maxLeanLeft != 0f || session.maxLeanRight != 0f) {
                LeanSymmetryCard(
                    leftMax = abs(session.maxLeanLeft),
                    rightMax = session.maxLeanRight,
                    highlightColor = highlightColor
                )
            }

            // ── Corner breakdown ──
            if (session.corners.isNotEmpty()) {
                CornerBreakdownCard(
                    corners = session.corners,
                    points = session.points,
                    isMetric = isMetric,
                    highlightColor = highlightColor
                )
            }

            // ── Ghost Replay button ──
            if (onGhostReplay != null && ghostSession != null) {
                Button(
                    onClick = { onGhostReplay(session, ghostSession) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = highlightColor.copy(alpha = 0.15f),
                        contentColor = highlightColor
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, highlightColor),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("GHOST REPLAY", style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold)
                }
            }

            // ── Action row (close + export + delete) ──
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard, contentColor = PureWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("CLOSE", style = MaterialTheme.typography.labelLarge)
                }
                val context = androidx.compose.ui.platform.LocalContext.current
                IconButton(
                    onClick = { shareSessionCard(context, session, highlightColor, isMetric) },
                    modifier = Modifier.size(46.dp).background(SurfaceCard, RoundedCornerShape(12.dp))
                        .border(1.dp, BorderDivider, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = MutedGrey, modifier = Modifier.size(18.dp))
                }
                if (onDelete != null && session.id != DEMO_SESSION_ID) {
                    IconButton(
                        onClick = { onDelete(session.id) },
                        modifier = Modifier.size(46.dp).background(AlertRed.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .border(1.dp, AlertRed.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun OverlayStatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MutedGrey,
                letterSpacing = 0.5.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontFamily = Rajdhani),
                color = color, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun SessionSummaryScreen(
    points: List<TelemetryPoint>,
    corners: List<CornerEvent>,
    isMetric: Boolean,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    highlightColor: Color = NeonCyan,
    mapView: org.maplibre.android.maps.MapView? = null
) {
    var showDiscardDialog by remember { mutableStateOf(false) }

    val totalDistanceRaw = remember(points) { points.sumOf { it.distanceDelta } / 1000.0 }
    val totalDistance = if (isMetric) totalDistanceRaw else totalDistanceRaw * 0.621371
    val distUnit = if (isMetric) "km" else "mi"
    val maxSpeedRaw = remember(points) { points.maxByOrNull { it.speedKmh }?.speedKmh ?: 0.0 }
    val maxSpeed = if (isMetric) maxSpeedRaw else maxSpeedRaw * 0.621371
    val speedUnit = if (isMetric) "km/h" else "mph"
    val maxLeft  = remember(points) { points.minByOrNull { it.leanAngle }?.leanAngle ?: 0f }
    val maxRight = remember(points) { points.maxByOrNull { it.leanAngle }?.leanAngle ?: 0f }
    val avgSpeed = remember(points) { if (points.isNotEmpty()) points.map { it.speedKmh }.average() else 0.0 }

    val startTs  = points.firstOrNull()?.timestamp ?: 0L
    val endTs    = points.lastOrNull()?.timestamp  ?: 0L
    val durMs    = if (endTs > startTs) endTs - startTs else 0L
    val durStr   = if (durMs > 0) { val h = durMs/3600000; val m = (durMs%3600000)/60000; if (h>0) "${h}h ${m}m" else "${m}m" } else "--"
    val dateStr  = remember(startTs) { java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(startTs)) }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            containerColor = SurfaceCard,
            title = { Text("Discard Session?", style = MaterialTheme.typography.titleLarge) },
            text = { Text("This session will be lost forever.", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = { TextButton(onClick = onDiscard) { Text("DISCARD", color = AlertRed, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { showDiscardDialog = false }) { Text("CANCEL", color = PureWhite) } }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepCarbon)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Header ──
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("RIDE COMPLETE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = Inter, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = highlightColor)
                    Text(dateStr,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = Rajdhani),
                        color = MutedGrey)
                }
            }

            HorizontalDivider(color = BorderDivider)

            // ── Quick stats row ──
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickStatCard("${"%.1f".format(totalDistance)}", distUnit, Modifier.weight(1f))
                QuickStatCard(durStr, "DURATION", Modifier.weight(1f))
                QuickStatCard("${maxSpeed.toInt()}", speedUnit, Modifier.weight(1f))
            }

            // ── Route thumbnail ──
            if (points.size >= 2) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF070908)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                        val minLat = points.minOf { it.latitude };  val maxLat = points.maxOf { it.latitude }
                        val minLng = points.minOf { it.longitude }; val maxLng = points.maxOf { it.longitude }
                        val latR = (maxLat - minLat).coerceAtLeast(0.0001)
                        val lngR = (maxLng - minLng).coerceAtLeast(0.0001)
                        val latCorr = kotlin.math.cos(Math.toRadians((minLat + maxLat) / 2)).toFloat()
                        val gpsAsp = (lngR * latCorr / latR).toFloat()
                        val scrAsp = size.width / size.height
                        val scX: Float; val scY: Float; val ox: Float; val oy: Float
                        if (gpsAsp > scrAsp) { scX = size.width; scY = size.width / gpsAsp; ox = 0f; oy = (size.height - scY) / 2 }
                        else { scY = size.height; scX = size.height * gpsAsp; oy = 0f; ox = (size.width - scX) / 2 }
                        fun px(lng: Double) = (ox + ((lng - minLng) / lngR * scX)).toFloat()
                        fun py(lat: Double) = (oy + ((maxLat - lat) / latR * scY)).toFloat()
                        // Grid
                        val gs = 30.dp.toPx()
                        var gx = 0f; while (gx <= size.width)  { drawLine(Color(0xFF111610), Offset(gx, 0f), Offset(gx, size.height), 0.5.dp.toPx()); gx += gs }
                        var gy = 0f; while (gy <= size.height) { drawLine(Color(0xFF111610), Offset(0f, gy), Offset(size.width, gy), 0.5.dp.toPx()); gy += gs }
                        // Road
                        val road = Path()
                        points.forEachIndexed { i, p -> if (i==0) road.moveTo(px(p.longitude), py(p.latitude)) else road.lineTo(px(p.longitude), py(p.latitude)) }
                        drawPath(road, Color(0xFF1A2218), style = Stroke(12.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                        // Route
                        val route = Path()
                        points.forEachIndexed { i, p -> if (i==0) route.moveTo(px(p.longitude), py(p.latitude)) else route.lineTo(px(p.longitude), py(p.latitude)) }
                        drawPath(route, highlightColor.copy(alpha = 0.9f), style = Stroke(2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                        // Dots
                        points.firstOrNull()?.let { drawCircle(MutedGrey, 4.dp.toPx(), Offset(px(it.longitude), py(it.latitude))) }
                        points.lastOrNull()?.let  { drawCircle(highlightColor, 4.dp.toPx(), Offset(px(it.longitude), py(it.latitude))) }
                    }
                }
            }

            // ── MAX LEAN hero section ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E130D)),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("MAX LEAN",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                        color = MutedGrey)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${abs(maxLeft).toInt()}°",
                                style = MaterialTheme.typography.displayMedium.copy(fontFamily = Rajdhani, fontWeight = FontWeight.Bold),
                                color = AlertRed)
                            Text("LEFT", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = MutedGrey)
                        }
                        Box(modifier = Modifier.width(1.dp).height(56.dp).background(BorderDivider))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${maxRight.toInt()}°",
                                style = MaterialTheme.typography.displayMedium.copy(fontFamily = Rajdhani, fontWeight = FontWeight.Bold),
                                color = highlightColor)
                            Text("RIGHT", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = MutedGrey)
                        }
                    }
                }
            }

            // ── Secondary stats ──
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OverlayStatCard("CORNERS", corners.size.toString(), highlightColor, Modifier.weight(1f))
                OverlayStatCard("AVG SPEED", "${(if (isMetric) avgSpeed else avgSpeed * 0.621371).toInt()} $speedUnit", PureWhite, Modifier.weight(1f))
                OverlayStatCard("DATA PTS", points.size.toString(), MidGrey, Modifier.weight(1f))
            }

            // ── Lean symmetry ──
            if (maxLeft != 0f || maxRight != 0f) {
                LeanSymmetryCard(leftMax = abs(maxLeft), rightMax = maxRight, highlightColor = highlightColor)
            }

            // ── Corner breakdown ──
            if (corners.isNotEmpty()) {
                CornerBreakdownCard(corners = corners, points = points, isMetric = isMetric, highlightColor = highlightColor)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Save / Discard ──
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = highlightColor, contentColor = DeepCarbon),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("SAVE SESSION", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = { showDiscardDialog = true },
                modifier = Modifier.fillMaxWidth().height(40.dp)
            ) {
                Text("DISCARD", style = MaterialTheme.typography.labelMedium, color = MutedGrey)
            }
        }
    }
}

@Composable
private fun QuickStatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontFamily = Rajdhani, fontWeight = FontWeight.Bold),
                color = PureWhite, textAlign = TextAlign.Center)
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                color = MutedGrey, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun LeanSymmetryCard(leftMax: Float, rightMax: Float, highlightColor: Color) {
    val total = leftMax + rightMax
    val leftRatio = if (total > 0f) leftMax / total else 0.5f
    val rightRatio = if (total > 0f) rightMax / total else 0.5f
    val imbalancePct = if (total > 0f) (kotlin.math.abs(leftMax - rightMax) / total * 100).toInt() else 0
    val weakSide = when {
        leftMax < rightMax * 0.85f -> "left"
        rightMax < leftMax * 0.85f -> "right"
        else -> null
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("LEAN SYMMETRY", style = MaterialTheme.typography.labelSmall,
                    color = highlightColor, letterSpacing = 1.sp)
                Text("$imbalancePct% imbalance", style = MaterialTheme.typography.labelSmall,
                    color = MutedGrey)
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("L ${leftMax.toInt()}°", style = MaterialTheme.typography.labelMedium,
                    color = AlertRed, modifier = Modifier.width(44.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(7.dp))
                    .background(SurfaceCard)) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(leftRatio).fillMaxHeight()
                            .background(AlertRed.copy(alpha = 0.8f)))
                        Box(modifier = Modifier.weight(rightRatio).fillMaxHeight()
                            .background(highlightColor.copy(alpha = 0.8f)))
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text("${rightMax.toInt()}° R", style = MaterialTheme.typography.labelMedium,
                    color = highlightColor, modifier = Modifier.width(44.dp))
            }
            if (weakSide != null) {
                Spacer(Modifier.height(6.dp))
                Text("Your $weakSide lean is weaker — focus on ${weakSide}-handers",
                    style = MaterialTheme.typography.bodySmall, color = MutedGrey)
            }
        }
    }
}

@Composable
fun CornerBreakdownCard(
    corners: List<CornerEvent>,
    points: List<TelemetryPoint>,
    isMetric: Boolean,
    highlightColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text("CORNERS", style = MaterialTheme.typography.labelSmall,
                color = highlightColor, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("#", style = MaterialTheme.typography.labelSmall, color = MutedGrey,
                    modifier = Modifier.width(24.dp))
                Text("DIR", style = MaterialTheme.typography.labelSmall, color = MutedGrey,
                    modifier = Modifier.width(36.dp))
                Text("LEAN", style = MaterialTheme.typography.labelSmall, color = MutedGrey,
                    modifier = Modifier.weight(1f))
                Text("SPEED", style = MaterialTheme.typography.labelSmall, color = MutedGrey,
                    modifier = Modifier.width(56.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
            Spacer(Modifier.height(4.dp))
            val display = corners.takeLast(10)
            display.forEachIndexed { i, corner ->
                val maxLean = maxOf(abs(corner.maxLeftLean), corner.maxRightLean)
                val dir = if (abs(corner.maxLeftLean) > corner.maxRightLean) "L" else "R"
                val dirColor = if (dir == "L") AlertRed else highlightColor
                val speedRaw = points.getOrNull(corner.maxLeanIndex)?.speedKmh ?: 0.0
                val speed = if (isMetric) speedRaw else speedRaw * 0.621371
                val speedUnit = if (isMetric) "km/h" else "mph"
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("${corners.size - display.size + i + 1}", style = MaterialTheme.typography.labelSmall,
                        color = MutedGrey, modifier = Modifier.width(24.dp))
                    Text(dir, style = MaterialTheme.typography.labelMedium, color = dirColor,
                        fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
                    Text("${maxLean.toInt()}°", style = MaterialTheme.typography.labelMedium,
                        color = PureWhite, modifier = Modifier.weight(1f))
                    Text("${speed.toInt()} $speedUnit", style = MaterialTheme.typography.labelSmall,
                        color = MutedGrey, modifier = Modifier.width(56.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
            }
        }
    }
}

@Composable
fun SummaryStatCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    color: Color = PureWhite,
    compact: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 8.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp, fontSize = if (compact) 10.sp else 12.sp),
                color = MutedGrey
            )
            Spacer(modifier = Modifier.height(if (compact) 2.dp else 4.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = if (value.length > 8) {
                            if (compact) 16.sp else 20.sp
                        } else {
                            if (compact) 22.sp else 28.sp
                        },
                        fontFamily = Rajdhani,
                        fontWeight = FontWeight.Bold,
                        color = color
                    ),
                    maxLines = 1,
                    softWrap = false
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = if (compact) 10.sp else 12.sp, color = color),
                        modifier = Modifier.padding(bottom = if (compact) 2.dp else 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TelemetryGraph(
    points: List<TelemetryPoint>,
    valueSelector: (TelemetryPoint) -> Float,
    color: Color,
    unit: String,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    var scaleX by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var width by remember { mutableFloatStateOf(0f) }

    val totalDurationMs = if (points.size > 1) (points.last().timestamp - points.first().timestamp).toFloat() else 0f
    val totalDurationSec = totalDurationMs / 1000f

    // Calculate visible time window
    val startFract = if ((scaleX > 1f) && (width > 0f)) (-offsetX / (width * scaleX)).coerceIn(0f, 1f) else 0f
    val endFract = if ((scaleX > 1f) && (width > 0f)) ((-offsetX + width) / (width * scaleX)).coerceIn(0f, 1f) else 1f

    val startSec = startFract * totalDurationSec
    val endSec = endFract * totalDurationSec

    fun formatDuration(seconds: Float): String {
        val totalSec = seconds.toInt().coerceAtLeast(0)
        val hrs = totalSec / 3600
        val mins = (totalSec % 3600) / 60
        val secs = totalSec % 60
        return if (hrs > 0) {
            String.format(java.util.Locale.getDefault(), "%d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(java.util.Locale.getDefault(), "%02d:%02d", mins, secs)
        }
    }

    val values = points.map(valueSelector)
    val minVal = values.minOrNull() ?: 0f
    val maxVal = values.maxOrNull() ?: 1f
    val range = if (maxVal - minVal == 0f) 1f else maxVal - minVal

    Column(modifier = modifier) {
        // Main Graph Canvas
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { size ->
                    width = size.width.toFloat()
                }
                .pointerInput(width) {
                    if (width > 0f) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scaleX = (scaleX * zoom).coerceIn(1f, 15f)
                            val maxOffset = width * (scaleX - 1f)
                            
                            // Adjust offset by pan
                            offsetX = if (scaleX > 1f) {
                                (offsetX + pan.x).coerceIn(-maxOffset, 0f)
                            } else {
                                0f
                            }
                        }
                    }
                }
        ) {
            val drawWidth = size.width
            val drawHeight = size.height

            // 1. Draw horizontal grid lines and value labels
            val gridLinesY = listOf(0.1f, 0.5f, 0.9f)
            gridLinesY.forEach { fract ->
                val y = drawHeight * fract
                val gridVal = maxVal - (fract * range)
                
                // Draw dashed line
                drawLine(
                    color = PureWhite.copy(alpha = 0.1f),
                    start = Offset(0f, y),
                    end = Offset(drawWidth, y),
                    strokeWidth = 1.dp.toPx()
                )

                // Draw label
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        this.color = MutedGrey.toArgb()
                        textSize = 9.sp.toPx()
                        typeface = Typeface.MONOSPACE
                    }
                    val labelText = String.format(java.util.Locale.getDefault(), "%.1f%s", gridVal, unit)
                    canvas.nativeCanvas.drawText(labelText, 8.dp.toPx(), y - 4.dp.toPx(), paint)
                }
            }

            // 2. Draw zero line if it exists
            if (minVal < 0f && maxVal > 0f) {
                val zeroY = drawHeight - ((-minVal) / range) * drawHeight
                drawLine(
                    color = AlertRed.copy(alpha = 0.25f),
                    start = Offset(0f, zeroY),
                    end = Offset(drawWidth, zeroY),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // 3. Draw the telemetry curve
            val path = Path()
            var hasStarted = false
            
            points.forEachIndexed { index, point ->
                val normX = if (points.size > 1) index.toFloat() / (points.size - 1).toFloat() else 0f
                val x = (normX * drawWidth * scaleX) + offsetX
                val y = drawHeight - ((valueSelector(point) - minVal) / range) * drawHeight
                
                // Only draw points that are reasonably near the visible viewport to optimize performance
                if (x >= -50f && x <= drawWidth + 50f) {
                    if (!hasStarted) {
                        path.moveTo(x, y)
                        hasStarted = true
                    } else {
                        path.lineTo(x, y)
                    }
                }
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // 4. Time indicators at the bottom of the graph
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(startSec),
                style = MaterialTheme.typography.labelSmall,
                color = MutedGrey,
                fontSize = 9.sp
            )
            Text(
                text = "Time (scroll/zoom)",
                style = MaterialTheme.typography.labelSmall,
                color = MutedGrey.copy(alpha = 0.6f),
                fontSize = 9.sp
            )
            Text(
                text = formatDuration(endSec),
                style = MaterialTheme.typography.labelSmall,
                color = MutedGrey,
                fontSize = 9.sp
            )
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun SummaryStatsGrid(
    totalDistance: Double,
    maxSpeed: Double,
    maxLeanLeft: Float,
    maxLeanRight: Float,
    maxPitch: Float,
    cornersCount: Int,
    isMetric: Boolean,
    highlightColor: Color,
    isLandscape: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(if (isLandscape) 6.dp else 12.dp)) {
        // Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 6.dp else 12.dp)
        ) {
            SummaryStatCard(
                label = "MAX LEAN (L / R)",
                value = "L ${abs(maxLeanLeft).toInt()}° | R ${maxLeanRight.toInt()}°",
                unit = "",
                modifier = Modifier.weight(1f),
                color = AlertRed,
                compact = isLandscape
            )
            SummaryStatCard(
                label = "MAX SPEED",
                value = maxSpeed.toInt().toString(),
                unit = if (isMetric) "km/h" else "mph",
                modifier = Modifier.weight(1f),
                color = highlightColor,
                compact = isLandscape
            )
        }

        // Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 6.dp else 12.dp)
        ) {
            SummaryStatCard(
                label = "TOTAL CURVES",
                value = cornersCount.toString(),
                unit = "",
                modifier = Modifier.weight(1f),
                color = highlightColor,
                compact = isLandscape
            )
            SummaryStatCard(
                label = "WHEELIE ANGLE",
                value = maxPitch.toInt().toString(),
                unit = "°",
                modifier = Modifier.weight(1f),
                color = PureWhite,
                compact = isLandscape
            )
        }
    }
}

@Composable
fun SummaryGraphs(
    points: List<TelemetryPoint>,
    isMetric: Boolean,
    highlightColor: Color,
    mutedHighlightColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Recorded Lean Angles",
            style = MaterialTheme.typography.labelSmall,
            color = mutedHighlightColor,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        TelemetryGraph(
            points = points,
            valueSelector = { it.leanAngle },
            color = AlertRed,
            unit = "°",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(SurfaceCard)
                .border(1.dp, BorderDivider)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Recorded Speed",
            style = MaterialTheme.typography.labelSmall,
            color = mutedHighlightColor,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        TelemetryGraph(
            points = points,
            valueSelector = {
                val speed = it.speedKmh.toFloat()
                if (isMetric) speed else (speed * 0.621371f)
            },
            color = highlightColor,
            unit = if (isMetric) "km/h" else "mph",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(SurfaceCard)
                .border(1.dp, BorderDivider)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun SessionSummaryPortraitPreview() {
    val mockPoints = List(100) { i ->
        TelemetryPoint(
            timestamp = i.toLong(),
            latitude = 0.0,
            longitude = 0.0,
            speedKmh = 50.0 + i % 20,
            leanAngle = (kotlin.math.sin(i / 10.0) * 30).toFloat(),
            pitchAngle = 0f,
            distanceDelta = 10.0
        )
    }
    RideTrackerTheme {
        Box(modifier = Modifier.fillMaxSize().background(DeepBase)) {
            SessionSummaryScreen(mockPoints, emptyList(), true, {}, {})
        }
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 400)
@Composable
fun SessionSummaryLandscapePreview() {
    val mockPoints = List(100) { i ->
        TelemetryPoint(
            timestamp = i.toLong(),
            latitude = 0.0,
            longitude = 0.0,
            speedKmh = 50.0 + i % 20,
            leanAngle = (kotlin.math.sin(i / 10.0) * 30).toFloat(),
            pitchAngle = 0f,
            distanceDelta = 10.0
        )
    }
    RideTrackerTheme {
        Box(modifier = Modifier.fillMaxSize().background(DeepBase)) {
            SessionSummaryScreen(mockPoints, emptyList(), true, {}, {})
        }
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 400)
@Composable
fun HistoryMenuLandscapePreview() {
    val mockSessions = List(10) { i ->
        RideSession(
            id = "Session_$i",
            startTime = System.currentTimeMillis() - (i * 86400000L),
            endTime = System.currentTimeMillis() - (i * 86400000L) + 3600000L,
            points = emptyList(),
            corners = emptyList(),
            maxLeanLeft = -30f,
            maxLeanRight = 30f,
            maxPitch = 15f
        )
    }
    RideTrackerTheme {
        HistoryMenuScreen(
            sessions = mockSessions,
            onBack = {},
            onDeleteSession = {},
            onSelectSession = {},
            onExportSession = {}
        )
    }
}
