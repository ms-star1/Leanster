package com.beispiel.ridetracker

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
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

fun shareSessionCard(context: Context, session: RideSession, highlightColor: Color, isMetric: Boolean) {
    val bitmap = Bitmap.createBitmap(1200, 400, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(DeepCarbon.toArgb())

    val composeView = ComposeView(context).apply {
        setContent {
            ShareableSessionCard(
                session = session,
                highlightColor = highlightColor,
                isMetric = isMetric,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    val parent = FrameLayout(context)
    parent.addView(composeView, FrameLayout.LayoutParams(1200, 400))
    parent.measure(
        View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY)
    )
    parent.layout(0, 0, 1200, 400)
    parent.draw(canvas)

    val shareDir = File(context.getExternalFilesDir(null), "share").also { it.mkdirs() }
    val file = File(shareDir, "session_${session.id}.png")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 95, it) }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share session card"))
}
