package com.beispiel.ridetracker

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.effects.OverlayEffect
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TAG = "CameraRecording"

/**
 * Live camera capture that burns the telemetry HUD (lean / speed / pitch) into the recorded
 * mp4 via CameraX [OverlayEffect]. The same effect targets both PREVIEW and VIDEO_CAPTURE, so
 * what the rider sees on screen is exactly what ends up in the saved file.
 *
 * Videos are written to getExternalFilesDir("videos"). Audio is intentionally off for now — see
 * the TODO in [startRecording] (would need RECORD_AUDIO + withAudioEnabled).
 */
@Composable
fun CameraRecordingView(
    service: TelemetryService,
    highlightColor: Color,
    isMetric: Boolean,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Live telemetry — collected on the Compose side, pushed into the plain-field HUD holder that
    // the (background-thread) draw listener reads.
    val lean by service.currentLean.collectAsStateWithLifecycle()
    val pitch by service.currentPitch.collectAsStateWithLifecycle()
    val speed by service.currentSpeed.collectAsStateWithLifecycle()
    val maxLeft by service.sessionMaxLeft.collectAsStateWithLifecycle()
    val maxRight by service.sessionMaxRight.collectAsStateWithLifecycle()

    val hud = remember { HudState() }
    hud.lean = lean
    hud.pitch = pitch
    hud.speedKmh = speed.toFloat()
    hud.maxLeft = maxLeft
    hud.maxRight = maxRight
    hud.isMetric = isMetric
    hud.accentColor = highlightColor.toArgb()

    var isRecording by remember { mutableStateOf(false) }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    // Kept in composition scope so the record button can drive the VideoCapture use case.
    val videoCaptureHolder = remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    val recordingHolder = remember { mutableStateOf<Recording?>(null) }

    DisposableEffect(Unit) {
        val overlayEffect = OverlayEffect(
            CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE,
            /* queueDepth = */ 0,
            Handler(Looper.getMainLooper()),
        ) { throwable -> Log.e(TAG, "Overlay effect error", throwable) }

        overlayEffect.setOnDrawListener { frame ->
            val canvas = frame.overlayCanvas
            canvas.drawColor(AndroidColor.TRANSPARENT, PorterDuff.Mode.CLEAR)
            // Buffer coordinates map 1:1 to encoded pixels. Recording orientation is locked while
            // riding, so a horizontally-drawn HUD stays upright in the output.
            drawHud(canvas, frame.size.width.toFloat(), frame.size.height.toFloat(), hud)
            true
        }

        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val cameraProvider = providerFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val recorder = Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(
                        Quality.FHD,
                        FallbackStrategy.lowerQualityOrHigherThan(Quality.HD),
                    ),
                )
                .build()
            val videoCapture = VideoCapture.withOutput(recorder)
            videoCaptureHolder.value = videoCapture

            val useCaseGroup = UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(videoCapture)
                .addEffect(overlayEffect)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    useCaseGroup,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed", e)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            recordingHolder.value?.stop()
            recordingHolder.value = null
            try {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            } catch (e: Exception) {
                Log.e(TAG, "Unbind failed", e)
            }
            overlayEffect.close()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        // Record / stop button
        IconButton(
            onClick = {
                if (isRecording) {
                    stopRecording(recordingHolder)
                    isRecording = false
                } else {
                    val started = startRecording(context, videoCaptureHolder.value, recordingHolder, hud)
                    if (started) isRecording = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .size(72.dp),
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.FiberManualRecord,
                contentDescription = if (isRecording) "Stop recording" else "Start recording",
                tint = Color.Red,
                modifier = Modifier.size(64.dp),
            )
        }

        if (isRecording) {
            Row(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(modifier = Modifier.size(10.dp).background(Color.Red, CircleShape))
                Text("REC", color = Color.White)
            }
        }
    }
}

private fun startRecording(
    context: android.content.Context,
    videoCapture: VideoCapture<Recorder>?,
    recordingHolder: androidx.compose.runtime.MutableState<Recording?>,
    hud: HudState,
): Boolean {
    if (videoCapture == null) {
        Log.w(TAG, "VideoCapture not ready yet")
        return false
    }
    val dir = File(context.getExternalFilesDir(null), "videos").apply { mkdirs() }
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val file = File(dir, "Leanster_$stamp.mp4")
    val outputOptions = FileOutputOptions.Builder(file).build()

    val pendingRecording = videoCapture.output.prepareRecording(context, outputOptions)
    // Enable the mic only when granted — withAudioEnabled() throws a SecurityException otherwise.
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        == PackageManager.PERMISSION_GRANTED
    ) {
        pendingRecording.withAudioEnabled()
    } else {
        Log.w(TAG, "RECORD_AUDIO not granted — recording video without audio")
    }
    val recording = pendingRecording
        .start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> hud.recording = true
                is VideoRecordEvent.Finalize -> {
                    hud.recording = false
                    if (event.hasError()) {
                        Log.e(TAG, "Recording finalized with error: ${event.error}")
                    } else {
                        Log.i(TAG, "Video saved: ${file.absolutePath}")
                    }
                }
            }
        }
    recordingHolder.value = recording
    return true
}

private fun stopRecording(recordingHolder: androidx.compose.runtime.MutableState<Recording?>) {
    recordingHolder.value?.stop()
    recordingHolder.value = null
}

/** Plain-field holder read by the draw listener (which runs off the Compose thread). */
private class HudState {
    @Volatile var lean: Float = 0f
    @Volatile var pitch: Float = 0f
    @Volatile var speedKmh: Float = 0f
    @Volatile var maxLeft: Float = 0f
    @Volatile var maxRight: Float = 0f
    @Volatile var isMetric: Boolean = true
    @Volatile var recording: Boolean = false
    @Volatile var accentColor: Int = AndroidColor.CYAN
}

private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
}
private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = AndroidColor.WHITE
    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
}
private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = 0x99000000.toInt()
}

private fun drawHud(canvas: Canvas, w: Float, h: Float, hud: HudState) {
    val unit = h * 0.01f // 1% of height as a scale unit
    val margin = unit * 4f
    val bigText = unit * 9f
    val labelText = unit * 3.2f

    // ---- Bottom-left: LEAN ----
    val leanAbs = abs(hud.lean).roundToInt()
    val side = if (hud.lean < -0.5f) "L" else if (hud.lean > 0.5f) "R" else ""
    valuePaint.color = hud.accentColor
    valuePaint.textSize = bigText
    labelPaint.textSize = labelText

    val leanBaseY = h - margin
    canvas.drawText("LEAN", margin, leanBaseY - bigText, labelPaint)
    val leanStr = "$leanAbs° $side".trim()
    drawTextShadowed(canvas, leanStr, margin, leanBaseY, valuePaint, bigText)

    // ---- Bottom-right: SPEED ----
    val speedVal = if (hud.isMetric) hud.speedKmh else hud.speedKmh * 0.621371f
    val speedUnit = if (hud.isMetric) "km/h" else "mph"
    valuePaint.color = AndroidColor.WHITE
    valuePaint.textSize = bigText
    val speedStr = speedVal.roundToInt().toString()
    val speedW = valuePaint.measureText(speedStr)
    drawTextShadowed(canvas, speedStr, w - margin - speedW, leanBaseY, valuePaint, bigText)
    canvas.drawText(speedUnit, w - margin - valuePaint.measureText(speedUnit), leanBaseY - bigText, labelPaint)

    // ---- Top-right: PITCH ----
    valuePaint.textSize = labelText * 1.4f
    val pitchStr = "PITCH ${hud.pitch.roundToInt()}°"
    drawTextShadowed(canvas, pitchStr, w - margin - valuePaint.measureText(pitchStr), margin + labelText, valuePaint, labelText * 1.4f)

    // ---- Lean bar (bottom, centered) ----
    val barW = w * 0.5f
    val barH = unit * 1.2f
    val barLeft = (w - barW) / 2f
    val barTop = h - margin - barH
    shadowPaint.color = 0x66000000
    canvas.drawRect(barLeft, barTop, barLeft + barW, barTop + barH, shadowPaint)
    val maxRange = 60f
    val frac = (hud.lean / maxRange).coerceIn(-1f, 1f)
    val center = barLeft + barW / 2f
    valuePaint.color = hud.accentColor
    if (frac >= 0f) {
        canvas.drawRect(center, barTop, center + (barW / 2f) * frac, barTop + barH, valuePaint)
    } else {
        canvas.drawRect(center + (barW / 2f) * frac, barTop, center, barTop + barH, valuePaint)
    }
}

private fun drawTextShadowed(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint, size: Float) {
    val prevColor = paint.color
    paint.color = 0xCC000000.toInt()
    canvas.drawText(text, x + size * 0.03f, y + size * 0.03f, paint)
    paint.color = prevColor
    canvas.drawText(text, x, y, paint)
}
