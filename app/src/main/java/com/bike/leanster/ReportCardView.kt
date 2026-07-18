package com.bike.leanster

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bike.leanster.ui.theme.*
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.sqrt

data class WeeklyReport(
    val weekLabel: String,
    val sessionCount: Int,
    val totalDistanceKm: Double,
    val avgMaxLean: Float,
    val smoothnessScore: Float,
    val symmetryScore: Float
)

@Composable
fun ReportCardScreen(
    sessions: List<RideSession>,
    highlightColor: Color,
    isMetric: Boolean
) {
    val reports = remember(sessions) { computeWeeklyReports(sessions) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepCarbon)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "REPORT CARD",
            style = MaterialTheme.typography.titleSmall.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
            color = highlightColor,
            letterSpacing = 1.sp
        )

        if (reports.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center) {
                Text("Ride more sessions to see your progress",
                    style = MaterialTheme.typography.bodyMedium, color = MutedGrey)
            }
            return@Column
        }

        val latest = reports.last()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScoreCard("SMOOTH", latest.smoothnessScore, highlightColor, Modifier.weight(1f))
            ScoreCard("SYMM", latest.symmetryScore, highlightColor, Modifier.weight(1f))
            ScoreCard("AVG LEAN", null, highlightColor, Modifier.weight(1f),
                label = "${latest.avgMaxLean.toInt()}°")
        }

        if (reports.size >= 2) {
            Spacer(Modifier.height(4.dp))
            Text("TREND — last ${reports.size} weeks",
                style = MaterialTheme.typography.labelSmall, color = MutedGrey, letterSpacing = 1.sp)
            WeeklyTrendChart(reports, highlightColor)
        }

        reports.reversed().forEach { report ->
            WeekRow(report, highlightColor, isMetric)
        }
    }
}

@Composable
private fun ScoreCard(title: String, score: Float?, highlightColor: Color,
                      modifier: Modifier = Modifier, label: String? = null) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
        shape = RoundedCornerShape(10.dp)) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MutedGrey, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            val display = label ?: score?.let { "${it.toInt()}" } ?: "--"
            Text(display, style = MaterialTheme.typography.titleLarge.copy(fontFamily = Rajdhani),
                color = highlightColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WeeklyTrendChart(reports: List<WeeklyReport>, highlightColor: Color) {
    val smoothValues = reports.map { it.smoothnessScore }
    val symmValues = reports.map { it.symmetryScore }

    Canvas(modifier = Modifier.fillMaxWidth().height(100.dp).padding(vertical = 4.dp)) {
        val w = size.width; val h = size.height
        val maxVal = 100f; val minVal = 0f
        val stepX = if (reports.size > 1) w / (reports.size - 1) else w

        fun yFor(v: Float) = h - (v - minVal) / (maxVal - minVal) * h

        listOf(smoothValues to highlightColor, symmValues to AlertRed.copy(alpha = 0.7f)).forEach { (vals, color) ->
            if (vals.size >= 2) {
                val path = Path()
                vals.forEachIndexed { i, v ->
                    val x = i * stepX; val y = yFor(v)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
                vals.forEachIndexed { i, v ->
                    drawCircle(color, radius = 3.dp.toPx(), center = Offset(i * stepX, yFor(v)))
                }
            }
        }

        drawLine(BorderDivider, Offset(0f, h / 2), Offset(w, h / 2), strokeWidth = 0.5.dp.toPx())
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.size(8.dp).background(highlightColor, RoundedCornerShape(4.dp)))
            Text("Smoothness", style = MaterialTheme.typography.labelSmall, color = MutedGrey)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.size(8.dp).background(AlertRed.copy(alpha = 0.7f), RoundedCornerShape(4.dp)))
            Text("Symmetry", style = MaterialTheme.typography.labelSmall, color = MutedGrey)
        }
    }
}

@Composable
private fun WeekRow(report: WeeklyReport, highlightColor: Color, isMetric: Boolean) {
    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
        shape = RoundedCornerShape(10.dp)) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(report.weekLabel, style = MaterialTheme.typography.labelMedium, color = PureWhite)
                val dist = if (isMetric) "%.1f km".format(report.totalDistanceKm)
                           else "%.1f mi".format(report.totalDistanceKm * 0.621371)
                Text("${report.sessionCount} rides · $dist",
                    style = MaterialTheme.typography.labelSmall, color = MutedGrey)
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("${report.avgMaxLean.toInt()}° avg",
                    style = MaterialTheme.typography.labelMedium, color = highlightColor)
                Text("S${report.smoothnessScore.toInt()} / Y${report.symmetryScore.toInt()}",
                    style = MaterialTheme.typography.labelSmall, color = MutedGrey)
            }
        }
    }
}

private fun computeWeeklyReports(sessions: List<RideSession>): List<WeeklyReport> {
    if (sessions.isEmpty()) return emptyList()
    val cal = Calendar.getInstance()
    val grouped = sessions.groupBy { session ->
        cal.timeInMillis = session.startTime
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        "$year-W$week"
    }
    return grouped.entries.sortedBy { it.key }.takeLast(8).map { (weekKey, weekSessions) ->
        val allPoints = weekSessions.flatMap { it.points }
        val totalDist = allPoints.sumOf { it.distanceDelta } / 1000.0
        val allLeftMax = weekSessions.map { s ->
            abs(s.points.minByOrNull { it.leanAngle }?.leanAngle ?: 0f) }
        val allRightMax = weekSessions.map { s ->
            s.points.maxByOrNull { it.leanAngle }?.leanAngle ?: 0f }
        val avgMaxLean = (allLeftMax + allRightMax).average().toFloat()
        val smoothness = computeSmoothness(allPoints)
        val symmetry = computeSymmetry(allLeftMax.average().toFloat(), allRightMax.average().toFloat())
        val label = weekKey.replace("-W", " W")
        WeeklyReport(label, weekSessions.size, totalDist, avgMaxLean, smoothness, symmetry)
    }
}

private fun computeSmoothness(points: List<TelemetryPoint>): Float {
    if (points.size < 2) return 50f
    val deltas = mutableListOf<Float>()
    for (i in 1 until points.size) {
        val dt = (points[i].timestamp - points[i - 1].timestamp).coerceAtLeast(1L) / 1000f
        deltas.add(abs(points[i].leanAngle - points[i - 1].leanAngle) / dt)
    }
    val mean = deltas.average()
    val variance = deltas.map { (it - mean) * (it - mean) }.average()
    val stddev = sqrt(variance).toFloat()
    return (100f / (1f + stddev / 5f)).coerceIn(0f, 100f)
}

private fun computeSymmetry(leftAvg: Float, rightAvg: Float): Float {
    val total = leftAvg + rightAvg
    if (total == 0f) return 50f
    return (100f * minOf(leftAvg, rightAvg) / maxOf(leftAvg, rightAvg)).coerceIn(0f, 100f)
}
