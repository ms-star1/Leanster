package com.beispiel.ridetracker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beispiel.ridetracker.ui.theme.*
import kotlin.math.abs

/** One row in the corner breakdown — this run vs. the all-time best for the same corner. */
data class CornerRow(
    val index: Int,          // 1-based T number
    val distanceKm: Double,  // distance from start to the apex
    val thisLean: Float,     // this session's max lean (abs)
    val bestLean: Float?,    // best lean for the same corner across other sessions
    val isPb: Boolean        // true when this run set or matched the best
)

/**
 * Matches each corner in [session] against the same corner (by GPS proximity) across
 * [allSessions] to derive the historical best lean and whether this run is a personal best.
 */
fun computeCornerRows(session: RideSession, allSessions: List<RideSession>): List<CornerRow> {
    // Cumulative distance (km) at every point index.
    val cumKm = DoubleArray(session.points.size)
    var acc = 0.0
    session.points.forEachIndexed { i, p ->
        acc += p.distanceDelta
        cumKm[i] = acc / 1000.0
    }

    val others = allSessions.filter { it.id != session.id }

    return session.corners.mapIndexed { i, c ->
        val thisLean = maxOf(abs(c.maxLeftLean), c.maxRightLean)
        val distKm = if (c.maxLeanIndex in session.points.indices) cumKm[c.maxLeanIndex] else 0.0

        // Best lean for the geographically-matching corner in any other session.
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
            index = i + 1,
            distanceKm = distKm,
            thisLean = thisLean,
            bestLean = bestLean,
            isPb = bestLean == null || thisLean >= bestLean
        )
    }
}

@Composable
fun CornerBreakdownScreen(
    session: RideSession,
    rideNumber: Int,
    highlightColor: Color,
    allSessions: List<RideSession> = emptyList(),
    onClose: () -> Unit
) {
    val rows = remember(session, allSessions) { computeCornerRows(session, allSessions) }
    val pbCount = rows.count { it.isPb && it.bestLean != null }
    val timeStr = remember(session.startTime) {
        java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(session.startTime))
    }

    val leftMax = abs(session.points.minByOrNull { it.leanAngle }?.leanAngle ?: session.maxLeanLeft)
    val rightMax = session.points.maxByOrNull { it.leanAngle }?.leanAngle ?: session.maxLeanRight

    val rowMuted = Color(0xFF5E655D)
    val pbRowBg  = Color(0xFF0B150A)
    val rowDiv   = Color(0xFF111510)

    Box(modifier = Modifier.fillMaxSize().background(DeepCarbon)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Status row ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(timeStr, fontSize = 12.sp, color = rowMuted)
                Text("RIDE #$rideNumber", fontSize = 12.sp, color = rowMuted,
                    fontFamily = FontFamily.Monospace, letterSpacing = 0.6.sp)
            }

            // ── Header ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("←", fontSize = 20.sp, color = rowMuted,
                    modifier = Modifier.clickable(onClick = onClose))
                Text("CORNERS", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = PureWhite)
                Spacer(Modifier.weight(1f))
                Text("${rows.size} detected", fontSize = 11.sp, letterSpacing = 1.sp, color = rowMuted)
            }

            // ── Column headers ──
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", fontSize = 10.sp, letterSpacing = 1.6.sp, color = rowMuted,
                    fontFamily = FontFamily.Monospace, modifier = Modifier.width(34.dp))
                Text("DIST", fontSize = 10.sp, letterSpacing = 1.6.sp, color = rowMuted,
                    fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                Text("THIS", fontSize = 10.sp, letterSpacing = 1.6.sp, color = rowMuted,
                    fontFamily = FontFamily.Monospace, modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
                Text("BEST", fontSize = 10.sp, letterSpacing = 1.6.sp, color = rowMuted,
                    fontFamily = FontFamily.Monospace, modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
                Text("Δ", fontSize = 10.sp, letterSpacing = 1.6.sp, color = rowMuted,
                    fontFamily = FontFamily.Monospace, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
                Spacer(Modifier.width(22.dp))
            }
            HorizontalDivider(color = BorderDivider)

            // ── Corner rows ──
            if (rows.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No corners detected in this ride.", color = rowMuted, fontSize = 14.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(rows) { row ->
                        val pbColor = highlightColor
                        val delta = row.bestLean?.let { row.thisLean - it }
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .background(if (row.isPb && row.bestLean != null) pbRowBg else Color.Transparent)
                                .padding(horizontal = 24.dp, vertical = 15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("T${row.index}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                color = if (row.isPb && row.bestLean != null) pbColor else rowMuted,
                                modifier = Modifier.width(34.dp))
                            Text("%.1f km".format(row.distanceKm), fontSize = 13.sp, color = rowMuted,
                                modifier = Modifier.weight(1f))
                            Text("${row.thisLean.toInt()}°", fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                                color = if (row.isPb && row.bestLean != null) pbColor else PureWhite,
                                modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
                            Text(row.bestLean?.let { "${it.toInt()}°" } ?: "—", fontSize = 14.sp, color = rowMuted,
                                modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
                            Text(
                                delta?.let { (if (it >= 0) "+" else "−") + "${abs(it).toInt()}°" } ?: "—",
                                fontSize = 13.sp,
                                color = if (delta != null && delta >= 0) pbColor else rowMuted,
                                modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
                            Text(if (row.isPb && row.bestLean != null) "★" else "", fontSize = 12.sp,
                                color = pbColor, modifier = Modifier.width(22.dp), textAlign = TextAlign.End)
                        }
                        HorizontalDivider(color = rowDiv)
                    }

                    item {
                        // ── Lean symmetry mini ──
                        LeanSymmetryMini(leftMax = leftMax, rightMax = rightMax, highlightColor = highlightColor)
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }

            if (pbCount > 0) {
                Text("$pbCount new personal best${if (pbCount == 1) "" else "s"} this ride",
                    fontSize = 11.sp, letterSpacing = 0.6.sp, color = highlightColor,
                    modifier = Modifier.padding(start = 24.dp, bottom = 12.dp))
            }
        }
    }
}

@Composable
private fun LeanSymmetryMini(leftMax: Float, rightMax: Float, highlightColor: Color) {
    val gap = abs(leftMax - rightMax).toInt()
    val weakSide = if (leftMax < rightMax) "left" else "right"
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)
            .border(androidx.compose.foundation.BorderStroke(1.dp, BorderDivider), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text("LEAN SYMMETRY", fontSize = 11.sp, letterSpacing = 1.8.sp, color = MutedGrey)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("L ${leftMax.toInt()}°", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MidGrey)
            Box(modifier = Modifier.weight(leftMax.coerceAtLeast(1f)).height(4.dp)
                .background(Color(0xFF3A4036), RoundedCornerShape(2.dp)))
            Box(modifier = Modifier.weight(rightMax.coerceAtLeast(1f)).height(4.dp)
                .background(highlightColor, RoundedCornerShape(2.dp)))
            Text("R ${rightMax.toInt()}°", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PureWhite)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (gap <= 2) "Well balanced — keep it up" else "Work $weakSide side — ${gap}° gap to close",
            fontSize = 11.sp, letterSpacing = 0.4.sp, color = MutedGrey)
    }
}
