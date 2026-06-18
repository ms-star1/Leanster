package com.beispiel.ridetracker

import androidx.compose.foundation.Canvas
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
    highlightColor: Color = NeonCyan
) {
    val mutedHighlightColor = when (highlightColor) {
        YamahaBlue -> MutedYamahaBlue
        DucatiRed -> MutedDucatiRed
        KawasakiGreen -> MutedKawasakiGreen
        PureWhiteHighlight -> MutedWhiteHighlight
        else -> MutedCyan
    }

    var sessionToDelete by remember { mutableStateOf<String?>(null) }

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
                "PAST SESSIONS",
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = Inter),
                color = highlightColor
            )
        }

        if (sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No sessions recorded yet.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            if (isLandscape) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(sessions) { session ->
                        SessionHistoryCard(session, onSelectSession, { sessionToDelete = it }, onExportSession, highlightColor, mutedHighlightColor)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(sessions) { session ->
                        SessionHistoryCard(session, onSelectSession, { sessionToDelete = it }, onExportSession, highlightColor, mutedHighlightColor)
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
    mutedHighlightColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectSession(session) },
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val dateFormat = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) }
                val timeFormat = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
                val dateStr = dateFormat.format(java.util.Date(session.startTime))
                val startTimeStr = timeFormat.format(java.util.Date(session.startTime))
                val endTimeStr = if (session.endTime > 0L) timeFormat.format(java.util.Date(session.endTime)) else "--:--"
                
                Text(dateStr, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                Text("$startTimeStr - $endTimeStr", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Rajdhani, fontWeight = FontWeight.Bold), color = highlightColor)
                Text("ID: ${session.id}", style = MaterialTheme.typography.labelSmall, color = MutedGrey)
            }
            
            IconButton(onClick = { onDeleteRequest(session.id) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed.copy(alpha = 0.7f))
            }

            IconButton(onClick = { onExportSession(session) }) {
                Icon(Icons.Default.Share, contentDescription = "Export", tint = highlightColor.copy(alpha = 0.8f))
            }
            
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = mutedHighlightColor)
        }
    }
}

@Composable
fun SessionSummaryOverlay(
    session: RideSession,
    isMetric: Boolean,
    onClose: () -> Unit,
    highlightColor: Color = NeonCyan
) {
    val mutedHighlightColor = when (highlightColor) {
        YamahaBlue -> MutedYamahaBlue
        DucatiRed -> MutedDucatiRed
        KawasakiGreen -> MutedKawasakiGreen
        PureWhiteHighlight -> MutedWhiteHighlight
        else -> MutedCyan
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepCarbon)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                        Text("✕", color = PureWhite, style = MaterialTheme.typography.titleLarge)
                    }
                    Text(
                        text = "SESSION DETAILS",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = highlightColor
                    )
                }

                val dateFormat = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) }
                val timeFormat = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
                val dateStr = dateFormat.format(java.util.Date(session.startTime))
                val startTimeStr = timeFormat.format(java.util.Date(session.startTime))
                val endTimeStr = if (session.endTime > 0L) timeFormat.format(java.util.Date(session.endTime)) else "--:--"

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = "$dateStr",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = PureWhite
                    )
                    Text(
                        text = "$startTimeStr - $endTimeStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedGrey
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val totalDistanceRaw = session.points.sumOf { it.distanceDelta } / 1000.0 // km
            val totalDistance = if (isMetric) totalDistanceRaw else totalDistanceRaw * 0.621371
            val maxSpeedRaw = session.points.maxByOrNull { it.speedKmh }?.speedKmh ?: 0.0
            val maxSpeed = if (isMetric) maxSpeedRaw else maxSpeedRaw * 0.621371

            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SummaryStatsGrid(
                            totalDistance = totalDistance,
                            maxSpeed = maxSpeed,
                            maxLeanLeft = session.maxLeanLeft,
                            maxLeanRight = session.maxLeanRight,
                            maxPitch = session.maxPitch,
                            cornersCount = session.corners.size,
                            isMetric = isMetric,
                            highlightColor = highlightColor,
                            isLandscape = true
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1.5f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SummaryGraphs(session.points, isMetric, highlightColor, mutedHighlightColor)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    SummaryStatsGrid(
                        totalDistance = totalDistance,
                        maxSpeed = maxSpeed,
                        maxLeanLeft = session.maxLeanLeft,
                        maxLeanRight = session.maxLeanRight,
                        maxPitch = session.maxPitch,
                        cornersCount = session.corners.size,
                        isMetric = isMetric,
                        highlightColor = highlightColor,
                        isLandscape = false
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SummaryGraphs(session.points, isMetric, highlightColor, mutedHighlightColor)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = highlightColor, contentColor = DeepCarbon),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("CLOSE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
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
    highlightColor: Color = NeonCyan
) {
    val mutedHighlightColor = when (highlightColor) {
        YamahaBlue -> MutedYamahaBlue
        DucatiRed -> MutedDucatiRed
        KawasakiGreen -> MutedKawasakiGreen
        PureWhiteHighlight -> MutedWhiteHighlight
        else -> MutedCyan
    }

    var showDiscardDialog by remember { mutableStateOf(value = false) }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            containerColor = SurfaceCard,
            title = { Text("Discard Session?", style = MaterialTheme.typography.titleLarge) },
            text = { Text("Are you sure you want to discard this data? It will be lost forever.", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TextButton(onClick = onDiscard) {
                    Text("DISCARD", color = AlertRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("CANCEL", style = MaterialTheme.typography.bodyLarge, color = PureWhite)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepCarbon)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SESSION SUMMARY",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = highlightColor
                )

                val startTime = points.firstOrNull()?.timestamp ?: 0L
                val endTime = points.lastOrNull()?.timestamp ?: 0L
                
                val summaryDateFormat = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) }
                val summaryTimeFormat = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
                
                val summaryDateStr = if (startTime > 0L) summaryDateFormat.format(java.util.Date(startTime)) else ""
                val summaryStartTimeStr = if (startTime > 0L) summaryTimeFormat.format(java.util.Date(startTime)) else "--:--"
                val summaryEndTimeStr = if (endTime > 0L) summaryTimeFormat.format(java.util.Date(endTime)) else "--:--"
                
                if (summaryDateStr.isNotEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = "$summaryDateStr",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = PureWhite
                        )
                        Text(
                            text = "$summaryStartTimeStr - $summaryEndTimeStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedGrey
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val totalDistanceRaw = points.sumOf { it.distanceDelta } / 1000.0 // km
            val totalDistance = if (isMetric) totalDistanceRaw else totalDistanceRaw * 0.621371
            val maxSpeedRaw = points.maxByOrNull { it.speedKmh }?.speedKmh ?: 0.0
            val maxSpeed = if (isMetric) maxSpeedRaw else maxSpeedRaw * 0.621371
            val maxLeft = points.minByOrNull { it.leanAngle }?.leanAngle ?: 0f
            val maxRight = points.maxByOrNull { it.leanAngle }?.leanAngle ?: 0f
            val maxPitch = points.maxByOrNull { it.pitchAngle }?.pitchAngle ?: 0f

            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryStatsGrid(
                            totalDistance = totalDistance,
                            maxSpeed = maxSpeed,
                            maxLeanLeft = maxLeft,
                            maxLeanRight = maxRight,
                            maxPitch = maxPitch,
                            cornersCount = corners.size,
                            isMetric = isMetric,
                            highlightColor = highlightColor
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1.5f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SummaryGraphs(points, isMetric, highlightColor, mutedHighlightColor)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    SummaryStatsGrid(
                        totalDistance = totalDistance,
                        maxSpeed = maxSpeed,
                        maxLeanLeft = maxLeft,
                        maxLeanRight = maxRight,
                        maxPitch = maxPitch,
                        cornersCount = corners.size,
                        isMetric = isMetric,
                        highlightColor = highlightColor
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SummaryGraphs(points, isMetric, highlightColor, mutedHighlightColor)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showDiscardDialog = false; onDiscard() },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider)
                ) {
                    Text("DISCARD", style = MaterialTheme.typography.labelLarge, color = PureWhite)
                }

                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(containerColor = highlightColor, contentColor = DeepCarbon),
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("SAVE SESSION", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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
        shape = RoundedCornerShape(if (compact) 8.dp else 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 6.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                style = if (compact) MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp) else MaterialTheme.typography.labelSmall,
                color = MutedGrey
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = (if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge).copy(
                        fontFamily = Rajdhani,
                        fontWeight = FontWeight.Bold
                    ),
                    color = color
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = if (compact) 9.sp else 11.sp),
                    color = color,
                    modifier = Modifier.padding(bottom = if (compact) 1.dp else 3.dp, start = 1.dp)
                )
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
    isLandscape: Boolean = false
) {
    val distanceStr = remember(totalDistance) { String.format(java.util.Locale.getDefault(), "%.2f", totalDistance) }

    Column(verticalArrangement = Arrangement.spacedBy(if (isLandscape) 4.dp else 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 4.dp else 8.dp)
        ) {
            SummaryStatCard(
                label = "DISTANCE",
                value = distanceStr,
                unit = if (isMetric) "km" else "mi",
                modifier = Modifier.weight(1f),
                color = PureWhite,
                compact = isLandscape
            )
            SummaryStatCard(
                label = "TOP SPEED",
                value = maxSpeed.toInt().toString(),
                unit = if (isMetric) "km/h" else "mph",
                modifier = Modifier.weight(1f),
                color = highlightColor,
                compact = isLandscape
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 4.dp else 8.dp)
        ) {
            SummaryStatCard(
                label = "MAX LEAN L",
                value = abs(maxLeanLeft).toInt().toString(),
                unit = "°",
                modifier = Modifier.weight(1f),
                color = AlertRed,
                compact = isLandscape
            )
            SummaryStatCard(
                label = "MAX LEAN R",
                value = maxLeanRight.toInt().toString(),
                unit = "°",
                modifier = Modifier.weight(1f),
                color = AlertRed,
                compact = isLandscape
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 4.dp else 8.dp)
        ) {
            SummaryStatCard(
                label = "WHEELIE ANGLE",
                value = maxPitch.toInt().toString(),
                unit = "°",
                modifier = Modifier.weight(1f),
                color = PureWhite,
                compact = isLandscape
            )
            SummaryStatCard(
                label = "CORNERS DRIVEN",
                value = cornersCount.toString(),
                unit = "",
                modifier = Modifier.weight(1f),
                color = highlightColor,
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
    mutedHighlightColor: Color
) {
    Text(
        text = "Recorded Lean Angles",
        style = MaterialTheme.typography.labelSmall,
        color = mutedHighlightColor,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    TelemetryGraph(
        points = points,
        valueSelector = { it.leanAngle },
        color = AlertRed,
        unit = "°",
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .background(SurfaceCard)
            .border(1.dp, BorderDivider)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Recorded Speed",
        style = MaterialTheme.typography.labelSmall,
        color = mutedHighlightColor,
        modifier = Modifier.padding(bottom = 4.dp)
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
            .height(115.dp)
            .background(SurfaceCard)
            .border(1.dp, BorderDivider)
    )
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
