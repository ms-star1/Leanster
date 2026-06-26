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
import androidx.compose.ui.text.font.FontFamily
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
    onExportAll: (() -> Unit)? = null,
    highlightColor: Color = NeonCyan,
    isMetric: Boolean = true,
    onShowSettings: () -> Unit = {}
) {
    val mutedHighlightColor = when (highlightColor) {
        YamahaBlue       -> MutedYamahaBlue
        DucatiRed        -> MutedDucatiRed
        KawasakiGreen    -> MutedKawasakiGreen
        KtmOrange        -> MutedKtmOrange
        HondaRed         -> MutedHondaRed
        BmwBlue          -> MutedBmwBlue
        TriumphGold      -> MutedTriumphGold
        HusqvarnaYellow  -> MutedHusqvarnaYellow
        PureWhiteHighlight -> MutedWhiteHighlight
        else -> MutedCyan
    }

    val demoSession = remember { createDemoSession() }
    var sessionToDelete by remember { mutableStateOf<String?>(null) }

    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            containerColor = SurfaceCard,
            title = { Text("Delete Session?", style = MaterialTheme.typography.titleLarge) },
            text = { Text("This action cannot be undone.", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TextButton(onClick = { onDeleteSession(sessionToDelete!!); sessionToDelete = null }) {
                    Text("DELETE", color = AlertRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("CANCEL", color = PureWhite)
                }
            }
        )
    }

    val allDisplaySessions = remember(sessions) { listOf(demoSession) + sessions }

    // Group sessions by month/year
    val monthFmt = remember { java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault()) }
    val todayStr = monthFmt.format(java.util.Date())
    val grouped = remember(allDisplaySessions) {
        allDisplaySessions.groupBy { monthFmt.format(java.util.Date(it.startTime)).uppercase() }
    }
    val sortedMonths = remember(grouped) { grouped.keys.sortedDescending() }

    Box(modifier = Modifier.fillMaxSize().background(DeepCarbon)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SESSIONS", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = PureWhite, fontFamily = Inter)
                Box(
                    modifier = Modifier
                        .border(androidx.compose.foundation.BorderStroke(1.dp, BorderDivider), RoundedCornerShape(6.dp))
                        .clickable { onExportAll?.invoke() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text("EXPORT ALL", fontSize = 12.sp, letterSpacing = 1.sp, color = highlightColor)
                }
            }
            HorizontalDivider(color = BorderDivider)

            // ── Session List ──────────────────────────────────────────────
            if (allDisplaySessions.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No sessions yet. Start riding!", color = MutedGrey, fontSize = 14.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    sortedMonths.forEach { month ->
                        val monthSessions = grouped[month] ?: return@forEach
                        item {
                            Text(
                                text = month,
                                fontSize = 11.sp, letterSpacing = 2.sp, color = MutedGrey, fontFamily = Inter,
                                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp)
                            )
                        }
                        item { HorizontalDivider(color = BorderDivider) }
                        items(monthSessions) { session ->
                            val isDemo = session.id == DEMO_SESSION_ID
                            val globalIdx = allDisplaySessions.indexOf(session) + 1
                            SessionHistoryRow(
                                session = session,
                                rideIndex = globalIdx,
                                isDemo = isDemo,
                                isMetric = isMetric,
                                highlightColor = highlightColor,
                                onClick = { onSelectSession(session) },
                                onDeleteRequest = { if (!isDemo) sessionToDelete = session.id }
                            )
                            HorizontalDivider(color = Color(0xFF111510))
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) } // space for bottom nav
                }
            }
        }

        // ── Bottom Nav ────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).background(DeepCarbon)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 10.dp).padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SessionNavItem("DIAL", false, highlightColor, onBack)
                SessionNavItem("SESSIONS", true, highlightColor) {}
                Text("⚙", fontSize = 18.sp, color = MutedGrey, modifier = Modifier.clickable { onShowSettings() })
            }
        }
    }
}

@Composable
private fun SessionNavItem(label: String, isActive: Boolean, highlightColor: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Text(label, fontSize = 12.sp, letterSpacing = 1.2.sp,
            color = if (isActive) highlightColor else MutedGrey,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            fontFamily = FontFamily.SansSerif)
        if (isActive) {
            Spacer(Modifier.height(3.dp))
            Box(Modifier.width(20.dp).height(1.5.dp).background(highlightColor, RoundedCornerShape(1.dp)))
        }
    }
}

@Composable
private fun SessionHistoryRow(
    session: RideSession,
    rideIndex: Int,
    isDemo: Boolean,
    isMetric: Boolean,
    highlightColor: Color,
    onClick: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val dateFmt = remember { java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()) }
    val todayFmt = remember { java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()) }
    val today = todayFmt.format(java.util.Date())
    val dateStr = dateFmt.format(java.util.Date(session.startTime))
    val isToday = dateStr == today

    val durMs = if (session.endTime > session.startTime) session.endTime - session.startTime else 0L
    val h = durMs / 3600000; val m = (durMs % 3600000) / 60000; val s = (durMs % 60000) / 1000
    val durStr = if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    val distKm = session.points.sumOf { it.distanceDelta } / 1000.0
    val distStr = if (isMetric) "%.1f km".format(distKm) else "%.1f mi".format(distKm * 0.621371)

    Row(
        modifier = Modifier.fillMaxWidth().background(if (isToday || isDemo) Color(0xFF0B150A) else Color.Transparent).clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Mini route thumbnail
        Canvas(modifier = Modifier.size(width = 44.dp, height = 56.dp)) {
            val pts = session.points
            if (pts.size >= 2) {
                val minLat = pts.minOf { it.latitude }; val maxLat = pts.maxOf { it.latitude }
                val minLng = pts.minOf { it.longitude }; val maxLng = pts.maxOf { it.longitude }
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
                val path = androidx.compose.ui.graphics.Path()
                pts.forEachIndexed { i, p -> if (i == 0) path.moveTo(px(p.longitude), py(p.latitude)) else path.lineTo(px(p.longitude), py(p.latitude)) }
                drawPath(path, if (isToday || isDemo) highlightColor else Color(0xFF3A4036), style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            } else {
                drawCircle(Color(0xFF2A3028), 10.dp.toPx(), Offset(size.width / 2, size.height / 2))
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isDemo) "DEMO" else "#$rideIndex",
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = if (isToday || isDemo) highlightColor else PureWhite
                )
                Text(
                    text = if (isToday) "Today" else dateStr,
                    fontSize = 12.sp,
                    color = if (isToday) highlightColor else MutedGrey,
                    letterSpacing = 0.4.sp
                )
            }
            Text(
                text = "$durStr · $distStr · ${session.corners.size} corners",
                fontSize = 12.sp, color = MutedGrey, fontFamily = FontFamily.Monospace
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${session.maxLeanRight.toInt()}°", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (isToday || isDemo) highlightColor else PureWhite)
                Text("R", fontSize = 10.sp, color = if (isToday || isDemo) highlightColor else MutedGrey)
                Text("·", fontSize = 12.sp, color = Color(0xFF3A4036))
                Text("${abs(session.maxLeanLeft).toInt()}°", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (isToday || isDemo) PureWhite else PureWhite.copy(0.7f))
                Text("L", fontSize = 10.sp, color = MutedGrey)
            }
        }

        Text("›", fontSize = 18.sp, color = MutedGrey)
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
    onDelete: ((String) -> Unit)? = null,
    rideNumber: Int = 0,
    onShowCorners: ((RideSession) -> Unit)? = null,
    onExportCsv: ((RideSession) -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dateFormat = remember { java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()) }
    val timeFormat = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
    val dateStr = dateFormat.format(java.util.Date(session.startTime))
    val startTimeStr = timeFormat.format(java.util.Date(session.startTime))

    val totalDistanceRaw = session.points.sumOf { it.distanceDelta } / 1000.0
    val totalDistance = if (isMetric) totalDistanceRaw else totalDistanceRaw * 0.621371
    val distUnit = if (isMetric) "km" else "mi"
    val maxSpeedRaw = session.points.maxByOrNull { it.speedKmh }?.speedKmh ?: 0.0
    val maxSpeed = if (isMetric) maxSpeedRaw else maxSpeedRaw * 0.621371
    val speedUnit = if (isMetric) "km/h" else "mph"

    val maxLeft = abs(session.points.minByOrNull { it.leanAngle }?.leanAngle ?: session.maxLeanLeft)
    val maxRight = session.points.maxByOrNull { it.leanAngle }?.leanAngle ?: session.maxLeanRight

    val durationMs = if (session.endTime > 0L) session.endTime - session.startTime
        else ((session.points.lastOrNull()?.timestamp ?: 0L) - (session.points.firstOrNull()?.timestamp ?: 0L))
    val durationStr = if (durationMs > 0) {
        val totalSec = durationMs / 1000
        "%02d:%02d".format(totalSec / 60, totalSec % 60)
    } else "--:--"

    val smoothness = remember(session) { rideSmoothness(session.points) }
    val consistency = remember(session) { rideConsistency(session.points) }
    val symmetry = remember(session) { rideSymmetry(maxLeft, maxRight) }

    val cornerRows = remember(session, allSessions) { computeCornerRows(session, allSessions) }
    val pbRows = remember(cornerRows) {
        cornerRows.filter { it.isPb && it.bestLean != null && it.thisLean > it.bestLean }
    }

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
                .padding(bottom = 24.dp)
        ) {
            // ── Status row ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(startTimeStr, fontSize = 12.sp, color = MutedGrey)
                Spacer(Modifier.weight(1f))
                if (rideNumber > 0) {
                    Text("RIDE #$rideNumber", fontSize = 12.sp, color = MutedGrey,
                        fontFamily = FontFamily.Monospace, letterSpacing = 0.6.sp)
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Text("✕", color = MutedGrey, fontSize = 18.sp)
                }
            }

            // ── Title ──
            Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 6.dp)) {
                Text("RIDE DETAILS", fontSize = 11.sp, letterSpacing = 4.sp, color = MutedGrey, fontFamily = Inter)
                Text(dateStr, fontSize = 32.sp, fontWeight = FontWeight.SemiBold, color = PureWhite,
                    letterSpacing = (-0.2).sp, modifier = Modifier.padding(top = 4.dp))
            }

            // ── Top stats row ──
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 20.dp)
                    .height(IntrinsicSize.Min)
            ) {
                TopStat("DISTANCE", "%.1f".format(totalDistance), distUnit, Modifier.weight(1f))
                Box(Modifier.width(1.dp).fillMaxHeight().background(BorderDivider).padding(vertical = 2.dp))
                TopStat("DURATION", durationStr, null, Modifier.weight(1f), mono = true)
                Box(Modifier.width(1.dp).fillMaxHeight().background(BorderDivider))
                TopStat("TOP SPEED", "${maxSpeed.toInt()}", speedUnit, Modifier.weight(1f))
            }
            HorizontalDivider(color = BorderDivider)

            // ── Max lean ──
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                Text("MAX LEAN", fontSize = 11.sp, letterSpacing = 2.sp, color = MutedGrey)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LeanHero(maxLeft.toInt(), "LEFT", MidGrey, MutedGrey)
                    Text("|", fontSize = 22.sp, color = Color(0xFF3A4036))
                    LeanHero(maxRight.toInt(), "RIGHT", PureWhite, highlightColor)
                }
            }
            HorizontalDivider(color = BorderDivider)

            // ── Rider report card ──
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp)) {
                Text("RIDER REPORT CARD", fontSize = 11.sp, letterSpacing = 2.sp, color = MutedGrey,
                    modifier = Modifier.padding(bottom = 14.dp))
                ReportBar("SMOOTHNESS", smoothness, highlightColor)
                Spacer(Modifier.height(13.dp))
                ReportBar("CONSISTENCY", consistency, highlightColor)
                Spacer(Modifier.height(13.dp))
                ReportBar("SYMMETRY", symmetry, highlightColor)
            }
            HorizontalDivider(color = BorderDivider)

            // ── New personal bests ──
            if (pbRows.isNotEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Text("${pbRows.size} NEW PERSONAL BEST${if (pbRows.size == 1) "" else "S"}",
                        fontSize = 11.sp, letterSpacing = 2.sp, color = MutedGrey,
                        modifier = Modifier.padding(bottom = 12.dp))
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pbRows.forEach { row ->
                            val delta = (row.thisLean - (row.bestLean ?: row.thisLean)).toInt()
                            PbChip("T${row.index}", "${row.thisLean.toInt()}°", "+$delta°", highlightColor)
                        }
                    }
                }
            }

            // ── Action buttons ──
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { shareSessionCard(context, session, highlightColor, isMetric) },
                        colors = ButtonDefaults.buttonColors(containerColor = highlightColor, contentColor = DeepCarbon),
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("SHARE SESSION", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                    }
                    if (onExportCsv != null && session.id != DEMO_SESSION_ID) {
                        OutlinedButton(
                            onClick = { onExportCsv(session) },
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = MutedGrey),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
                            modifier = Modifier.height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("CSV", fontSize = 13.sp, letterSpacing = 0.8.sp)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (onGhostReplay != null && ghostSession != null) {
                        OutlinedButton(
                            onClick = { onGhostReplay(session, ghostSession) },
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = highlightColor.copy(alpha = 0.12f), contentColor = highlightColor),
                            border = androidx.compose.foundation.BorderStroke(1.dp, highlightColor.copy(alpha = 0.6f)),
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("VIEW GHOST", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
                        }
                    }
                    if (onShowCorners != null && session.corners.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { onShowCorners(session) },
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceCard, contentColor = PureWhite),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("CORNERS · ${session.corners.size}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
                        }
                    }
                }
                if (onDelete != null && session.id != DEMO_SESSION_ID) {
                    TextButton(
                        onClick = { onDelete(session.id) },
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Text("DELETE RIDE", fontSize = 12.sp, color = AlertRed, letterSpacing = 0.8.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopStat(label: String, value: String, unit: String?, modifier: Modifier = Modifier, mono: Boolean = false) {
    Column(modifier = modifier) {
        Text(label, fontSize = 11.sp, letterSpacing = 1.6.sp, color = MutedGrey)
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = PureWhite,
                fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default)
            if (unit != null) {
                Text(" $unit", fontSize = 14.sp, color = MutedGrey, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
    }
}

@Composable
private fun LeanHero(angle: Int, label: String, valueColor: Color, labelColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Top) {
            Text("$angle", fontSize = 64.sp, fontWeight = FontWeight.SemiBold, color = valueColor,
                letterSpacing = (-1).sp, lineHeight = 56.sp)
            Text("°", fontSize = 26.sp, color = labelColor)
        }
        Text(label, fontSize = 11.sp, letterSpacing = 2.4.sp, color = labelColor,
            modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun ReportBar(label: String, score: Int, highlightColor: Color) {
    val barColor = if (score >= 75) highlightColor else MidGrey
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, fontSize = 11.sp, letterSpacing = 1.sp, color = MutedGrey, modifier = Modifier.width(92.dp))
        Box(modifier = Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(BorderDivider)) {
            Box(modifier = Modifier.fillMaxWidth(score / 100f).fillMaxHeight().background(barColor))
        }
        Text("$score", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            color = if (score >= 75) PureWhite else MidGrey,
            modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun PbChip(corner: String, lean: String, delta: String, highlightColor: Color) {
    Row(
        modifier = Modifier
            .border(androidx.compose.foundation.BorderStroke(1.dp, highlightColor.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
            .background(highlightColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$corner ", fontSize = 12.sp, color = MutedGrey, fontFamily = FontFamily.Monospace)
        Text(lean, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = highlightColor)
        Text(" $delta", fontSize = 11.sp, color = highlightColor.copy(alpha = 0.8f))
    }
}

/** Smoothness 0–100 from the stability of lean angular rate (lower jerk = smoother). */
private fun rideSmoothness(points: List<TelemetryPoint>): Int {
    if (points.size < 2) return 50
    val rates = ArrayList<Float>(points.size)
    for (i in 1 until points.size) {
        val dt = (points[i].timestamp - points[i - 1].timestamp).coerceAtLeast(1L) / 1000f
        rates.add(abs(points[i].leanAngle - points[i - 1].leanAngle) / dt)
    }
    val mean = rates.average()
    val sd = kotlin.math.sqrt(rates.map { (it - mean) * (it - mean) }.average()).toFloat()
    return (100f / (1f + sd / 5f)).coerceIn(0f, 100f).toInt()
}

/** Consistency 0–100 from how steady speed is held (low relative variation = consistent). */
private fun rideConsistency(points: List<TelemetryPoint>): Int {
    val moving = points.filter { it.speedKmh > 5.0 }
    if (moving.size < 2) return 50
    val mean = moving.map { it.speedKmh }.average()
    if (mean <= 0.0) return 50
    val sd = kotlin.math.sqrt(moving.map { (it.speedKmh - mean) * (it.speedKmh - mean) }.average())
    val cv = (sd / mean).toFloat()
    return (100f * (1f - cv).coerceIn(0f, 1f)).toInt()
}

/** Symmetry 0–100 from how balanced left vs. right lean is. */
private fun rideSymmetry(leftMax: Float, rightMax: Float): Int {
    val hi = maxOf(leftMax, rightMax)
    if (hi <= 0f) return 50
    return (100f * minOf(leftMax, rightMax) / hi).coerceIn(0f, 100f).toInt()
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
