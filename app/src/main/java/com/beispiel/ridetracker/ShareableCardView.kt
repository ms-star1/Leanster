package com.beispiel.ridetracker

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.beispiel.ridetracker.ui.theme.*
import java.io.File
import kotlin.math.abs

@Composable
fun ShareableSessionCard(
    session: RideSession,
    highlightColor: Color,
    isMetric: Boolean,
    modifier: Modifier = Modifier
) {
    val points = session.points
    val maxLeftRaw = points.minByOrNull { it.leanAngle }?.leanAngle ?: 0f
    val maxRightRaw = points.maxByOrNull { it.leanAngle }?.leanAngle ?: 0f
    val maxSpeedRaw = points.maxByOrNull { it.speedKmh }?.speedKmh ?: 0.0
    val distRaw = points.sumOf { it.distanceDelta } / 1000.0

    val maxLeft = abs(maxLeftRaw)
    val maxRight = maxRightRaw
    val maxSpeed = if (isMetric) maxSpeedRaw else maxSpeedRaw * 0.621371
    val dist = if (isMetric) distRaw else distRaw * 0.621371
    val speedUnit = if (isMetric) "km/h" else "mph"
    val distUnit = if (isMetric) "km" else "mi"

    val dateStr = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        .format(java.util.Date(session.startTime))

    Box(
        modifier = modifier
            .background(DeepCarbon)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("RIDETRACKER", style = MaterialTheme.typography.titleMedium.copy(fontFamily = Rajdhani),
                    color = highlightColor, letterSpacing = 2.sp)
                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MutedGrey)
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = highlightColor.copy(alpha = 0.4f), thickness = 1.dp)
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBlock("MAX LEFT", "${maxLeft.toInt()}°", AlertRed, Modifier.weight(1f))
                StatBlock("MAX RIGHT", "${maxRight.toInt()}°", highlightColor, Modifier.weight(1f))
                StatBlock("TOP SPEED", "${maxSpeed.toInt()} $speedUnit", PureWhite, Modifier.weight(1f))
                StatBlock("DISTANCE", "%.1f $distUnit".format(dist), PureWhite, Modifier.weight(1f))
            }

            Spacer(Modifier.height(12.dp))

            val leftRatio = if (maxLeft + maxRight > 0f) maxLeft / (maxLeft + maxRight) else 0.5f
            val rightRatio = 1f - leftRatio
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("L", style = MaterialTheme.typography.labelSmall, color = AlertRed,
                    modifier = Modifier.width(16.dp))
                Row(modifier = Modifier.weight(1f).height(8.dp).padding(horizontal = 4.dp)) {
                    Box(modifier = Modifier.weight(leftRatio).fillMaxHeight()
                        .background(AlertRed.copy(alpha = 0.7f), RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)))
                    Box(modifier = Modifier.weight(rightRatio).fillMaxHeight()
                        .background(highlightColor.copy(alpha = 0.7f), RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)))
                }
                Text("R", style = MaterialTheme.typography.labelSmall, color = highlightColor,
                    modifier = Modifier.width(16.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }

            Spacer(Modifier.height(8.dp))
            Text("${session.corners.size} corners · ${session.points.size} data points",
                style = MaterialTheme.typography.labelSmall, color = MutedGrey)
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MutedGrey, letterSpacing = 0.5.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontFamily = Rajdhani),
            color = color, fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

/**
 * Renders a shareable session card to a PNG and fires an ACTION_SEND chooser.
 *
 * Drawn directly with android.graphics so the bitmap is always produced (offscreen
 * Compose rendering needs a window/lifecycle and silently produced blank images).
 */
fun shareSessionCard(context: Context, session: RideSession, highlightColor: Color, isMetric: Boolean) {
    val w = 1080
    val h = 1350
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(DeepCarbon.toArgb())

    val accent = highlightColor.toArgb()
    val white = PureWhite.toArgb()
    val muted = MutedGrey.toArgb()
    val mid = MidGrey.toArgb()
    val border = BorderDivider.toArgb()

    val sans = Typeface.create("sans-serif", Typeface.NORMAL)
    val sansBold = Typeface.create("sans-serif", Typeface.BOLD)
    val mono = Typeface.MONOSPACE

    val points = session.points
    val maxLeft = abs(points.minByOrNull { it.leanAngle }?.leanAngle ?: session.maxLeanLeft)
    val maxRight = points.maxByOrNull { it.leanAngle }?.leanAngle ?: session.maxLeanRight
    val maxSpeedRaw = points.maxByOrNull { it.speedKmh }?.speedKmh ?: 0.0
    val maxSpeed = (if (isMetric) maxSpeedRaw else maxSpeedRaw * 0.621371).toInt()
    val distRaw = points.sumOf { it.distanceDelta } / 1000.0
    val dist = if (isMetric) distRaw else distRaw * 0.621371
    val speedUnit = if (isMetric) "km/h" else "mph"
    val distUnit = if (isMetric) "km" else "mi"
    val dateStr = java.text.SimpleDateFormat("MMM d yyyy", java.util.Locale.getDefault())
        .format(java.util.Date(session.startTime)).uppercase()

    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    fun text(s: String, x: Float, y: Float, size: Float, color: Int, tf: Typeface,
             align: Paint.Align = Paint.Align.LEFT, spacing: Float = 0f) {
        p.typeface = tf; p.textSize = size; p.color = color; p.textAlign = align; p.letterSpacing = spacing
        canvas.drawText(s, x, y, p)
    }

    val pad = 72f

    // ── Header ──
    text("RideTracker", pad, 150f, 64f, white, sansBold, spacing = 0.02f)
    text("$dateStr", pad, 200f, 30f, muted, mono, spacing = 0.12f)

    // ── Route map ──
    val mapTop = 250f
    val mapBottom = 760f
    val mapLeft = pad
    val mapRight = w - pad
    p.style = Paint.Style.FILL; p.color = Color(0xFF0C0F0B).toArgb()
    canvas.drawRoundRect(mapLeft, mapTop, mapRight, mapBottom, 28f, 28f, p)

    if (points.size >= 2) {
        val minLat = points.minOf { it.latitude }; val maxLat = points.maxOf { it.latitude }
        val minLng = points.minOf { it.longitude }; val maxLng = points.maxOf { it.longitude }
        val latR = (maxLat - minLat).coerceAtLeast(0.0001)
        val lngR = (maxLng - minLng).coerceAtLeast(0.0001)
        val latCorr = kotlin.math.cos(Math.toRadians((minLat + maxLat) / 2))
        val gpsAsp = (lngR * latCorr / latR).toFloat()
        val innerPad = 60f
        val boxW = (mapRight - mapLeft) - innerPad * 2
        val boxH = (mapBottom - mapTop) - innerPad * 2
        val scrAsp = boxW / boxH
        val scX: Float; val scY: Float; val ox: Float; val oy: Float
        if (gpsAsp > scrAsp) { scX = boxW; scY = boxW / gpsAsp; ox = mapLeft + innerPad; oy = mapTop + innerPad + (boxH - scY) / 2 }
        else { scY = boxH; scX = boxH * gpsAsp; oy = mapTop + innerPad; ox = mapLeft + innerPad + (boxW - scX) / 2 }
        fun px(lng: Double) = (ox + ((lng - minLng) / lngR * scX)).toFloat()
        fun py(lat: Double) = (oy + ((maxLat - lat) / latR * scY)).toFloat()

        val path = AndroidPath()
        points.forEachIndexed { i, pt -> if (i == 0) path.moveTo(px(pt.longitude), py(pt.latitude)) else path.lineTo(px(pt.longitude), py(pt.latitude)) }
        p.style = Paint.Style.STROKE; p.strokeWidth = 12f; p.strokeCap = Paint.Cap.ROUND; p.strokeJoin = Paint.Join.ROUND
        p.color = accent; canvas.drawPath(path, p)
        p.style = Paint.Style.FILL
        points.firstOrNull()?.let { canvas.drawCircle(px(it.longitude), py(it.latitude), 12f, p.apply { color = muted }) }
        points.lastOrNull()?.let { canvas.drawCircle(px(it.longitude), py(it.latitude), 12f, p.apply { color = white }) }
    }
    text("%.1f $distUnit".format(dist).uppercase(), mapRight - 24f, mapBottom - 28f, 26f, Color(0xFF5E655D).toArgb(), mono, Paint.Align.RIGHT, 0.08f)

    // ── MAX LEAN ──
    p.letterSpacing = 0f
    text("MAX LEAN", pad, 870f, 28f, muted, sans, spacing = 0.2f)
    text("${maxLeft.toInt()}°L", pad, 980f, 110f, mid, sansBold)
    val leftW = run { p.typeface = sansBold; p.textSize = 110f; p.measureText("${maxLeft.toInt()}°L") }
    text("/", pad + leftW + 30f, 980f, 70f, border, sans)
    text("${maxRight.toInt()}°R", pad + leftW + 110f, 980f, 120f, white, sansBold)

    // ── Stats row ──
    p.style = Paint.Style.STROKE; p.strokeWidth = 2f; p.color = border
    canvas.drawLine(pad, 1040f, w - pad, 1040f, p)
    p.style = Paint.Style.FILL
    val colW = (w - pad * 2) / 3f
    fun stat(i: Int, label: String, value: String, valColor: Int) {
        val cx = pad + colW * i
        text(label, cx, 1100f, 24f, muted, sans, spacing = 0.14f)
        text(value, cx, 1160f, 46f, valColor, sansBold)
    }
    stat(0, "SPEED", "$maxSpeed", white)
    stat(1, "CORNERS", "${session.corners.size}", white)
    stat(2, "TOP LEAN", "${maxOf(maxLeft, maxRight).toInt()}°", accent)
    p.letterSpacing = 0f

    // ── Footer ──
    text("ridetracker.app", pad, 1280f, 26f, Color(0xFF2A4020).toArgb(), mono, spacing = 0.1f)
    text("No cloud · No account", w - pad, 1280f, 26f, Color(0xFF2A4020).toArgb(), mono, Paint.Align.RIGHT, 0.04f)

    val shareDir = File(context.getExternalFilesDir(null), "share").also { it.mkdirs() }
    val file = File(shareDir, "session_${session.id}.png")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 95, it) }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share session card"))
}
