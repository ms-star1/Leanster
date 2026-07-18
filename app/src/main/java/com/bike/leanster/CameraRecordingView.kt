package com.bike.leanster

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Range
import android.util.Rational
import android.view.Surface
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import android.util.Size
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

// Throttled per-frame diagnostics: logs buffer size / crop rect / rotation once per unique combo.
private val diagSeen = HashSet<String>()
private fun logFrameDiag(frame: androidx.camera.effects.Frame, crop: android.graphics.Rect, rot: Int) {
    val size = frame.size
    val key = "${size.width}x${size.height}/${crop.toShortString()}/$rot/${frame.isMirroring}"
    if (diagSeen.add(key)) {
        Log.i(TAG, "FRAME buffer=${size.width}x${size.height} crop=${crop.toShortString()} rot=$rot mirror=${frame.isMirroring}")
    }
}

private fun hasCameraPermission(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

/**
 * Picks the highest-fps AE target range the back camera supports, capped at 60:
 * fixed 60 beats 30–60 beats fixed 30. Null when the query fails (keep CameraX defaults).
 */
private fun bestFpsRange(context: android.content.Context): Range<Int>? = try {
    val cm = context.getSystemService(android.content.Context.CAMERA_SERVICE) as CameraManager
    val chars = cm.cameraIdList.asSequence()
        .map { cm.getCameraCharacteristics(it) }
        .firstOrNull { it.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK }
    chars?.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
        ?.filter { it.upper <= 60 }
        ?.maxWithOrNull(compareBy({ it.upper }, { it.lower }))
} catch (e: Exception) {
    Log.w(TAG, "FPS range query failed", e)
    null
}

/** Requests the best supported AE fps range for the whole capture session (raises preview fps). */
@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
private fun applyFpsRange(builder: Preview.Builder, context: android.content.Context) {
    val range = bestFpsRange(context) ?: return
    Log.i(TAG, "Requesting AE target FPS range $range")
    Camera2Interop.Extender(builder)
        .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, range)
}

/**
 * Fullscreen live camera shown while a video-enabled ride is armed or recording. The telemetry HUD
 * (lean / speed / pitch) is burned into both the on-screen preview and the recorded mp4 via a
 * CameraX [OverlayEffect], so what the rider sees is exactly what ends up in the file.
 *
 * Recording is driven by the session state — it starts automatically once [TelemetryService.isRecording]
 * flips true (after roll-off) and stops when the session stops. There is no manual record button; the
 * whole thing is controlled by TAP TO RIDE on the dashboard.
 *
 * Zoom exposes the lens presets the device actually supports, derived from the bound camera's
 * [androidx.camera.core.ZoomState] min/max ratio.
 *
 * Videos are written to getExternalFilesDir("videos"). Audio is enabled only when RECORD_AUDIO is granted.
 */
@Composable
fun SessionCameraView(
    service: TelemetryService,
    highlightColor: Color,
    isMetric: Boolean,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isArmed by service.isArmed.collectAsStateWithLifecycle()
    val isRecording by service.isRecording.collectAsStateWithLifecycle()
    val isPaused by service.isPaused.collectAsStateWithLifecycle()
    val speed by service.currentSpeed.collectAsStateWithLifecycle()

    if (!hasCameraPermission(context)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text(
                "Camera permission is required to record video.\nEnable it in system settings, then start again.",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(32.dp),
            )
        }
        return
    }

    // Live telemetry pushed into the plain-field HUD holder the (background-thread) draw listener reads.
    val lean by service.currentLean.collectAsStateWithLifecycle()
    val maxLeft by service.sessionMaxLeft.collectAsStateWithLifecycle()
    val maxRight by service.sessionMaxRight.collectAsStateWithLifecycle()
    val currentLocation by service.currentLocation.collectAsStateWithLifecycle()
    val sessionPoints by service.sessionPoints.collectAsStateWithLifecycle()
    val corners by service.detectedCorners.collectAsStateWithLifecycle()
    val pbComparison by service.livePbComparison.collectAsStateWithLifecycle()
    val gpsUpdateRateHz by service.gpsUpdateRateHz.collectAsStateWithLifecycle()
    val isUsbGpsConnected by service.isUsbGpsConnected.collectAsStateWithLifecycle()

    // Recording start clock for the elapsed-time readout (stable while a recording is running).
    val recordingStartMs = remember(isRecording) { if (isRecording) System.currentTimeMillis() else 0L }

    val hud = remember { HudState() }
    hud.lean = lean
    hud.speedKmh = speed.toFloat()
    hud.maxLeft = maxLeft
    hud.maxRight = maxRight
    hud.isMetric = isMetric
    hud.recording = isRecording
    hud.recordingStartMs = recordingStartMs
    hud.distanceKm = (sessionPoints.sumOf { it.distanceDelta } / 1000.0).toFloat()
    hud.cornerCount = corners.size
    hud.gpsActive = currentLocation != null
    hud.gpsHz = gpsUpdateRateHz
    hud.gpsExternal = isUsbGpsConnected
    hud.accentColor = highlightColor.toArgb()
    val pb = pbComparison
    hud.pbActive = pb != null
    if (pb != null) {
        hud.pbDelta = pb.deltaLean
        hud.pbIsNewBest = pb.isNewPb
        hud.pbTurn = corners.size
    }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            // FIT_CENTER letterboxes the full 16:9 stream in the live view (instead of cropping it to
            // fill the screen), so the whole HUD is visible on-screen and the side/top bars give a
            // clear place for the controls.
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }

    val videoCaptureHolder = remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    val recordingHolder = remember { mutableStateOf<Recording?>(null) }
    val cameraHolder = remember { mutableStateOf<Camera?>(null) }
    var zoomPresets by remember { mutableStateOf(listOf(1f)) }
    var currentZoom by remember { mutableStateOf(1f) }

    DisposableEffect(Unit) {
        val overlayEffect = OverlayEffect(
            CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE,
            /* queueDepth = */ 0,
            Handler(Looper.getMainLooper()),
        ) { throwable -> Log.e(TAG, "Overlay effect error", throwable) }

        overlayEffect.setOnDrawListener { frame ->
            val canvas = frame.overlayCanvas
            canvas.drawColor(AndroidColor.TRANSPARENT, PorterDuff.Mode.CLEAR)
            // Only the crop rect (not the whole buffer) becomes the visible output, and rotationDegrees
            // is the clockwise rotation applied to make it upright. Draw into the crop region, pre-rotated
            // by the inverse, so the HUD sits inside the visible frame in upright screen coordinates.
            val crop = frame.cropRect
            val cw = crop.width().toFloat()
            val ch = crop.height().toFloat()
            val rot = frame.rotationDegrees
            logFrameDiag(frame, crop, rot)
            canvas.save()
            canvas.translate(crop.left.toFloat(), crop.top.toFloat())
            when (rot) {
                90 -> { canvas.translate(0f, ch); canvas.rotate(-90f) }
                180 -> { canvas.translate(cw, ch); canvas.rotate(180f) }
                270 -> { canvas.translate(cw, 0f); canvas.rotate(90f) }
            }
            val uprightW = if (rot % 180 == 0) cw else ch
            val uprightH = if (rot % 180 == 0) ch else cw
            drawHud(canvas, uprightW, uprightH, hud)
            canvas.restore()
            true
        }

        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val cameraProvider = providerFuture.get()

            // The OverlayEffect's shared surface follows the preview resolution, so pin the preview to
            // 1080p 16:9 — otherwise the shared stream (and therefore the recording) caps lower.
            val preview = Preview.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                        .setResolutionStrategy(
                            ResolutionStrategy(Size(1920, 1080), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER),
                        )
                        .build(),
                )
                .also { applyFpsRange(it, context) }
                .build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val recorder = Recorder.Builder()
                .setQualitySelector(
                    // 1080p first; prefer going UP (UHD) over dropping to HD if FHD is unavailable.
                    QualitySelector.from(
                        Quality.FHD,
                        FallbackStrategy.higherQualityOrLowerThan(Quality.FHD),
                    ),
                )
                .build()
            val videoCapture = VideoCapture.withOutput(recorder)
            videoCaptureHolder.value = videoCapture

            // Bind after the preview is laid out so its ViewPort is available. The ViewPort makes the
            // preview, the recording and the burned-in overlay share ONE field of view, so the HUD is
            // drawn against exactly what's shown/recorded — instead of the wider sensor buffer, whose
            // edges get cropped out of the 16:9 video (that was the "UI cut off").
            // NOTE: only Preview + VideoCapture go through the effect. A 3rd (ImageAnalysis) stream
            // forced the shared surface down to ~720p, so it was removed to keep recordings at 1080p.
            previewView.post {
                // Strict 16:9 (9:16 in portrait) so the recording is always a standard video aspect,
                // not the phone's screen shape.
                val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
                val landscape = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270
                val aspect = if (landscape) Rational(16, 9) else Rational(9, 16)
                val viewPort = ViewPort.Builder(aspect, rotation).setScaleType(ViewPort.FILL_CENTER).build()
                val useCaseGroup = UseCaseGroup.Builder()
                    .addUseCase(preview)
                    .addUseCase(videoCapture)
                    .addEffect(overlayEffect)
                    .setViewPort(viewPort)
                    .build()
                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, useCaseGroup,
                    )
                    cameraHolder.value = camera
                    val zs = camera.cameraInfo.zoomState.value
                    zoomPresets = buildZoomPresets(zs?.minZoomRatio ?: 1f, zs?.maxZoomRatio ?: 1f)
                    currentZoom = zs?.zoomRatio ?: 1f
                } catch (e: Exception) {
                    Log.e(TAG, "Camera bind failed", e)
                }
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

    // Drive the CameraX recording from the session state — start on roll-off, stop when the session stops.
    LaunchedEffect(isRecording, videoCaptureHolder.value) {
        val vc = videoCaptureHolder.value
        if (isRecording && recordingHolder.value == null && vc != null) {
            startRecording(context, vc, recordingHolder, hud, service)
        } else if (!isRecording && recordingHolder.value != null) {
            stopRecording(recordingHolder)
        }
    }

    var showStopDialog by remember { mutableStateOf(false) }
    val stopEnabled = speed < 10.0
    // While actually riding (recording + moving) every control is greyed out and inert — the ride is
    // speed-locked, so the buttons must not look tappable.
    val riding = isRecording && speed >= 10.0

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // ── Top status chip (armed/paused only; the burned-in HUD owns the REC timer) ──
        if (!isRecording || isPaused) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .background(Color(0xAA000000), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(modifier = Modifier.size(10.dp).background(if (isPaused) Color.Red else highlightColor, CircleShape))
                Text(
                    text = if (isPaused) "PAUSED" else "ARMED · Waiting for roll-off",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // ── Cancel (armed only) — left-centre, clear of the top-left REC readout ──
        if (isArmed && !isRecording) {
            IconButton(
                onClick = { service.disarmSession() },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
                    .background(Color(0x66000000), CircleShape),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel ride", tint = Color.White)
            }
        }

        // ── Zoom lens presets — vertical strip on the right edge (off the burned-in HUD) ──
        if (zoomPresets.size > 1) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .background(Color(0xAA000000), RoundedCornerShape(24.dp))
                    .padding(4.dp)
                    .alpha(if (riding) 0.35f else 1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                zoomPresets.forEach { ratio ->
                    val selected = abs(currentZoom - ratio) < 0.05f
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(if (selected) highlightColor else Color(0x33FFFFFF), CircleShape)
                            .border(1.dp, if (selected) highlightColor else Color.Transparent, CircleShape)
                            .clickable(enabled = !riding) {
                                cameraHolder.value?.cameraControl?.setZoomRatio(ratio)
                                currentZoom = ratio
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            zoomLabel(ratio),
                            color = if (selected) Color(0xFF070908) else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        // ── Stop button (recording only) — left-centre, speed-gated & greyed while moving ──
        if (isRecording) {
            IconButton(
                onClick = { if (stopEnabled) showStopDialog = true },
                enabled = stopEnabled,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
                    .size(64.dp)
                    .background(Color(0x66000000), CircleShape)
                    .alpha(if (stopEnabled) 1f else 0.35f),
            ) {
                Icon(
                    imageVector = Icons.Filled.Stop,
                    contentDescription = "Stop recording",
                    tint = Color.Red,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
    }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("Stop Session?") },
            text = { Text("End and save your ride.") },
            confirmButton = {
                TextButton(onClick = { showStopDialog = false; service.stopRecording() }) {
                    Text("STOP", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) { Text("CANCEL") }
            },
        )
    }
}

/**
 * Small live camera preview used inside the calibration steps of a video ride, so the rider can
 * frame the shot while mounting/positioning the phone. Binds only a Preview use case.
 */
@Composable
fun CalibrationCameraPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    if (!hasCameraPermission(context)) {
        Box(modifier = modifier.background(Color(0xFF0E130D)), contentAlignment = Alignment.Center) {
            Text("Camera unavailable", color = Color(0xFF7A857A), fontSize = 12.sp)
        }
        return
    }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(Unit) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val cameraProvider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
            } catch (e: Exception) {
                Log.e(TAG, "Preview bind failed", e)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            try {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            } catch (e: Exception) {
                Log.e(TAG, "Preview unbind failed", e)
            }
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

/** Builds the reachable lens presets for the device from the bound camera's zoom range. */
private fun buildZoomPresets(minRatio: Float, maxRatio: Float): List<Float> {
    val presets = sortedSetOf<Float>()
    if (minRatio < 0.95f) presets.add(minRatio)      // ultra-wide lens, if present
    presets.add(1f.coerceIn(minRatio, maxRatio))     // main lens
    for (r in listOf(2f, 3f, 5f, 10f)) if (r in minRatio..maxRatio) presets.add(r)
    return presets.toList()
}

private fun zoomLabel(ratio: Float): String = when {
    ratio < 1f -> String.format(Locale.US, "%.1f×", ratio)
    ratio == ratio.toInt().toFloat() -> "${ratio.toInt()}×"
    else -> String.format(Locale.US, "%.1f×", ratio)
}

private fun startRecording(
    context: android.content.Context,
    videoCapture: VideoCapture<Recorder>?,
    recordingHolder: androidx.compose.runtime.MutableState<Recording?>,
    hud: HudState,
    service: TelemetryService,
): Boolean {
    if (videoCapture == null) {
        Log.w(TAG, "VideoCapture not ready yet")
        return false
    }
    val dir = File(context.getExternalFilesDir(null), "videos").apply { mkdirs() }
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val file = File(dir, "Leanster_$stamp.mp4")
    // Link this clip to the ride so the session summary can show its thumbnail / play it back.
    service.attachVideoPath(file.absolutePath)
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
                is VideoRecordEvent.Start -> {
                    hud.recording = true
                    // First frame is being written now — this is the video↔telemetry sync origin.
                    service.attachVideoStartTime(System.currentTimeMillis())
                }
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

/**
 * Plain-field holder read by the draw listener (which runs off the Compose thread).
 * Mirrors the "Leanster Video Overlay" design: rec timer, GPS/distance, lean dial + value,
 * speed, session-max symmetry bars, ghost/off-best readout and the Leanster wordmark.
 */
private class HudState {
    @Volatile var lean: Float = 0f
    @Volatile var speedKmh: Float = 0f
    @Volatile var maxLeft: Float = 0f
    @Volatile var maxRight: Float = 0f
    @Volatile var isMetric: Boolean = true
    @Volatile var recording: Boolean = false
    @Volatile var recordingStartMs: Long = 0L
    @Volatile var distanceKm: Float = 0f
    @Volatile var cornerCount: Int = 0
    @Volatile var gpsActive: Boolean = false
    @Volatile var gpsHz: Float = 0f
    @Volatile var gpsExternal: Boolean = false
    @Volatile var accentColor: Int = AndroidColor.CYAN
    @Volatile var pbActive: Boolean = false
    @Volatile var pbDelta: Float = 0f
    @Volatile var pbIsNewBest: Boolean = false
    @Volatile var pbTurn: Int = 0
}

// ── HUD paint kit (reused every frame; mutated in place) ─────────────────────
private const val REC_RED = 0xFFFF3B30.toInt()
private const val HUD_GREEN = 0xFF6FD000.toInt()

// Text paints: anti-alias + sub-pixel + linear (smoother edges when the overlay is scaled to the
// output). All HUD paints dither to avoid banded/rough edges on the effect surface.
private const val TEXT_FLAGS = Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG
private val numPaint = Paint(TEXT_FLAGS).apply {
    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    isDither = true
}
private val leanWordPaint = Paint(TEXT_FLAGS).apply {
    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC)
    isDither = true
}
private val monoPaint = Paint(TEXT_FLAGS).apply {
    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
    isDither = true
}
private val monoBoldPaint = Paint(TEXT_FLAGS).apply {
    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    isDither = true
}
private val labelPaint = Paint(TEXT_FLAGS).apply {
    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    isDither = true
}
private val hudFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isDither = true }
private val hudStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    isDither = true
    strokeCap = Paint.Cap.ROUND   // round caps/joins read smoother than hard butt ends on thin lines
    strokeJoin = Paint.Join.ROUND
}

private fun white(alpha: Float): Int = AndroidColor.argb((alpha * 255).roundToInt(), 255, 255, 255)

// ── HUD text colors ──────────────────────────────────────────────────────────
// Fixed high-contrast palette: white text + brand accent + a soft dark drop shadow, readable over
// most footage. (An earlier adaptive light/dark scheme needed a 3rd camera stream that capped the
// recording resolution, so it was dropped in favour of 1080p.)
private fun ink(alpha: Float = 1f): Int = AndroidColor.argb((alpha * 255).roundToInt(), 255, 255, 255)

private fun accentInk(accent: Int): Int = accent

private fun shadowColor(): Int = 0xC0000000.toInt()

/** Draws text with a soft dark drop shadow, honoring the paint's alignment. */
private fun shadowText(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint, size: Float, color: Int) {
    paint.textSize = size
    val o = size * 0.035f
    paint.color = shadowColor()
    canvas.drawText(text, x + o, y + o, paint)
    paint.color = color
    canvas.drawText(text, x, y, paint)
}

private fun elapsedString(hud: HudState): String {
    if (!hud.recording || hud.recordingStartMs <= 0L) return "00:00:00"
    val sec = ((System.currentTimeMillis() - hud.recordingStartMs) / 1000L).coerceAtLeast(0L)
    return "%02d:%02d:%02d".format(sec / 3600, (sec % 3600) / 60, sec % 60)
}

/** Rec-dot pulse: opacity 1 → .2 → 1 over ~1.4 s (matches the design keyframes). */
private fun pulseAlpha(): Float {
    val t = (System.currentTimeMillis() % 1400L) / 1400f
    return 0.2f + 0.8f * (0.5f + 0.5f * kotlin.math.cos(t * 2f * Math.PI.toFloat()))
}

private fun distanceLabel(hud: HudState): String {
    val d = if (hud.isMetric) hud.distanceKm else hud.distanceKm * 0.621371f
    return "%.0f %s".format(d, if (hud.isMetric) "km" else "mi")
}

// ── Shared element renderers (absolute coordinates; used by both orientations) ─

// Scrim paints are cached and only rebuilt when the frame geometry changes — allocating
// Paint + LinearGradient every frame at 30–60 fps causes GC churn that janks the preview.
private val topScrimPaint = Paint()
private val botScrimPaint = Paint()
private var scrimKey = 0L

private fun drawScrims(canvas: Canvas, w: Float, h: Float, topH: Float, botH: Float) {
    val key = (w.toLong() shl 42) xor (h.toLong() shl 21) xor topH.toLong()
    if (key != scrimKey) {
        scrimKey = key
        topScrimPaint.shader = android.graphics.LinearGradient(
            0f, 0f, 0f, topH, 0x8C000000.toInt(), 0x00000000, android.graphics.Shader.TileMode.CLAMP,
        )
        botScrimPaint.shader = android.graphics.LinearGradient(
            0f, h - botH, 0f, h, 0x00000000, 0xB8000000.toInt(), android.graphics.Shader.TileMode.CLAMP,
        )
    }
    canvas.drawRect(0f, 0f, w, topH, topScrimPaint)
    canvas.drawRect(0f, h - botH, w, h, botScrimPaint)
}

// Reusable clip path for the dial and wordmark (avoids a Path allocation per frame).
private val reusableClip = android.graphics.Path()

/** Pulsing red dot + mono elapsed timer, left-anchored at [x], vertically centered on [cy]. */
private fun drawRec(canvas: Canvas, x: Float, cy: Float, s: Float, hud: HudState, dotR: Float, timerSize: Float) {
    val a = if (hud.recording) pulseAlpha() else 1f
    hudFill.color = REC_RED
    hudFill.alpha = (a * 85).roundToInt()
    canvas.drawCircle(x + dotR, cy, dotR * 1.9f, hudFill)      // glow
    hudFill.alpha = (a * 255).roundToInt()
    canvas.drawCircle(x + dotR, cy, dotR, hudFill)
    hudFill.alpha = 255
    monoBoldPaint.textAlign = Paint.Align.LEFT
    shadowText(canvas, elapsedString(hud), x + dotR * 2f + 10f * s, cy + timerSize * 0.35f, monoBoldPaint, timerSize, ink())
}

/**
 * GPS status dot + label, right-anchored to [rightX], optionally with a [secondary] readout.
 * The update rate is shown only with an external receiver (built-in GPS shows just "GPS"); when
 * shown it uses the real measured rate and turns green above 4 Hz.
 */
private fun drawGps(canvas: Canvas, rightX: Float, cy: Float, s: Float, hud: HudState, fontSize: Float, secondary: String?) {
    monoPaint.textAlign = Paint.Align.RIGHT
    var rx = rightX
    if (secondary != null) {
        shadowText(canvas, secondary, rx, cy + fontSize * 0.35f, monoPaint, fontSize, ink(0.85f))
        rx -= monoPaint.measureText(secondary) + 14f * s
    }
    val showHz = hud.gpsExternal && hud.gpsActive
    val gps = if (showHz) "GPS ${"%.0f".format(hud.gpsHz)}Hz" else "GPS"
    val gpsColor = if (showHz && hud.gpsHz > 4f) accentInk(HUD_GREEN) else ink(0.85f)
    shadowText(canvas, gps, rx, cy + fontSize * 0.35f, monoPaint, fontSize, gpsColor)
    rx -= monoPaint.measureText(gps) + 9f * s
    hudFill.color = if (hud.gpsActive) accentInk(hud.accentColor) else ink(0.4f)
    canvas.drawCircle(rx - 3f * s, cy, 3.5f * s, hudFill)
}

/** Mini artificial-horizon lean dial centered on ([cx],[cy]) with radius [r]. */
private fun drawDial(canvas: Canvas, cx: Float, cy: Float, r: Float, lean: Float, accent: Int) {
    val acc = accentInk(accent)
    hudFill.color = 0x47000000
    canvas.drawCircle(cx, cy, r, hudFill)
    hudStroke.color = ink(0.3f)
    hudStroke.strokeWidth = r * 0.03f
    canvas.drawCircle(cx, cy, r, hudStroke)

    // Horizon line, tilted by lean, clipped inside the dial.
    canvas.save()
    reusableClip.rewind()
    reusableClip.addCircle(cx, cy, r, android.graphics.Path.Direction.CW)
    canvas.clipPath(reusableClip)
    canvas.rotate(lean, cx, cy)
    hudStroke.color = acc
    hudStroke.strokeWidth = r * 0.06f
    canvas.drawLine(cx - r * 1.3f, cy, cx + r * 1.3f, cy, hudStroke)
    canvas.restore()

    // Reference ticks around the top rim (±45 accent, ±30/±15 neutral).
    fun tick(angle: Float, color: Int, wFrac: Float) {
        canvas.save(); canvas.rotate(angle, cx, cy)
        hudStroke.color = color; hudStroke.strokeWidth = r * wFrac
        canvas.drawLine(cx, cy - r * 0.98f, cx, cy - r * 0.72f, hudStroke)
        canvas.restore()
    }
    tick(45f, acc, 0.055f); tick(-45f, acc, 0.055f)
    tick(30f, ink(0.42f), 0.05f); tick(-30f, ink(0.42f), 0.05f)
    tick(15f, ink(0.42f), 0.05f); tick(-15f, ink(0.42f), 0.05f)

    // Pointer dot riding the rim, moved by lean.
    canvas.save(); canvas.rotate(lean, cx, cy)
    hudFill.color = acc
    canvas.drawCircle(cx, cy - r, r * 0.1f, hudFill)
    canvas.restore()
}

/** Big lean number + degree + "LEAN LEFT/RIGHT" caption, left-anchored at [x]. */
private fun drawLeanValue(
    canvas: Canvas, x: Float, numBaseY: Float, labelBaseY: Float,
    numSize: Float, degSize: Float, labelSize: Float, lean: Float, accent: Int,
) {
    numPaint.textAlign = Paint.Align.LEFT
    val v = abs(lean).roundToInt().toString()
    shadowText(canvas, v, x, numBaseY, numPaint, numSize, ink())
    val vw = numPaint.measureText(v)
    shadowText(canvas, "°", x + vw + numSize * 0.02f, numBaseY, numPaint, degSize, accentInk(accent))
    val caption = if (lean < -0.5f) "LEAN LEFT" else if (lean > 0.5f) "LEAN RIGHT" else "LEAN"
    labelPaint.textAlign = Paint.Align.LEFT
    labelPaint.letterSpacing = 0.28f
    shadowText(canvas, caption, x, labelBaseY, labelPaint, labelSize, accentInk(accent))
    labelPaint.letterSpacing = 0f
}

/** Speed value + unit. [align] LEFT anchors at [x]; CENTER centers on [x]. */
private fun drawSpeed(
    canvas: Canvas, x: Float, baseY: Float, numSize: Float, unitSize: Float, s: Float,
    speedKmh: Float, isMetric: Boolean, align: Paint.Align,
) {
    val v = (if (isMetric) speedKmh else speedKmh * 0.621371f).roundToInt().toString()
    val unit = if (isMetric) "KM/H" else "MPH"
    numPaint.textAlign = Paint.Align.LEFT
    numPaint.textSize = numSize
    val nvw = numPaint.measureText(v)
    labelPaint.textSize = unitSize
    labelPaint.letterSpacing = 0.16f
    val uw = labelPaint.measureText(unit)
    labelPaint.letterSpacing = 0f
    val gap = 8f * s
    val startX = if (align == Paint.Align.CENTER) x - (nvw + gap + uw) / 2f else x
    shadowText(canvas, v, startX, baseY, numPaint, numSize, ink())
    labelPaint.textAlign = Paint.Align.LEFT
    labelPaint.letterSpacing = 0.16f
    shadowText(canvas, unit, startX + nvw + gap, baseY, labelPaint, unitSize, ink(0.72f))
    labelPaint.letterSpacing = 0f
}

/** Rounded track with a proportional fill. */
private fun drawBar(canvas: Canvas, x: Float, y: Float, w: Float, hgt: Float, frac: Float, fillColor: Int) {
    val r = hgt / 2f
    hudFill.color = ink(0.2f)
    canvas.drawRoundRect(x, y, x + w, y + hgt, r, r, hudFill)
    val fw = (w * frac).coerceIn(0f, w)
    if (fw > 0f) {
        hudFill.color = fillColor
        canvas.drawRoundRect(x, y, x + fw, y + hgt, r, r, hudFill)
    }
}

/** Leanster wordmark + ring/lean-line/dot logo, left-anchored at ([leftX],[topY]). */
private fun drawWordmark(canvas: Canvas, leftX: Float, topY: Float, logoSize: Float, textSize: Float, s: Float, accent: Int) {
    val r = logoSize / 2f
    val cx = leftX + r
    val cy = topY + r
    hudStroke.color = ink()
    hudStroke.strokeCap = Paint.Cap.ROUND
    hudStroke.strokeWidth = logoSize * 0.08f
    canvas.drawCircle(cx, cy, r * 0.9f, hudStroke)
    canvas.save()
    reusableClip.rewind()
    reusableClip.addCircle(cx, cy, r * 0.9f, android.graphics.Path.Direction.CW)
    canvas.clipPath(reusableClip)
    canvas.rotate(32f, cx, cy)
    canvas.drawLine(cx - r * 0.95f, cy, cx + r * 0.95f, cy, hudStroke)
    canvas.restore()
    canvas.save(); canvas.rotate(32f, cx, cy)
    hudFill.color = ink()
    canvas.drawCircle(cx, cy - r * 0.78f, logoSize * 0.11f, hudFill)
    canvas.restore()

    val textX = leftX + logoSize + 8f * s
    val baseY = cy + textSize * 0.35f
    leanWordPaint.textAlign = Paint.Align.LEFT
    shadowText(canvas, "Lean", textX, baseY, leanWordPaint, textSize, accentInk(accent))
    val lw = leanWordPaint.measureText("Lean")
    labelPaint.textAlign = Paint.Align.LEFT
    shadowText(canvas, "ster", textX + lw, baseY, labelPaint, textSize, ink())
}

/** off-best / new-best ghost line. Returns the accent+white segments already drawn. */
private fun ghostSegments(hud: HudState, portrait: Boolean): Pair<String, String>? {
    if (!hud.pbActive) return null
    return if (hud.pbIsNewBest) {
        "NEW BEST" to " · Turn ${hud.pbTurn}"
    } else {
        val d = abs(hud.pbDelta).roundToInt()
        "−$d°" to (if (portrait) " off your best · Turn ${hud.pbTurn}" else " off best · Turn ${hud.pbTurn}")
    }
}

private fun drawHud(canvas: Canvas, w: Float, h: Float, hud: HudState) {
    if (w >= h) drawLandscapeHud(canvas, w, h, hud) else drawPortraitHud(canvas, w, h, hud)
}

private fun drawLandscapeHud(canvas: Canvas, w: Float, h: Float, hud: HudState) {
    val s = w / 900f
    val accent = hud.accentColor
    drawScrims(canvas, w, h, 120f * s, 230f * s)

    drawRec(canvas, 24f * s, 30f * s, s, hud, 7f * s, 24f * s)
    drawGps(canvas, w - 24f * s, 30f * s, s, hud, 16f * s, "${distanceLabel(hud)} · ${hud.cornerCount} COR")

    // Watermark (right-anchored under the GPS row).
    run {
        val logo = 26f * s; val ts = 19f * s
        val wmW = logo + 8f * s + leanWordPaint.apply { textSize = ts }.measureText("Lean") +
            labelPaint.apply { textSize = ts; letterSpacing = 0f }.measureText("ster")
        drawWordmark(canvas, w - 22f * s - wmW, 62f * s, logo, ts, s, accent)
    }

    // Lean dial + value (bottom-left).
    val dialR = 46f * s
    val dialCx = 24f * s + dialR
    drawDial(canvas, dialCx, h - 22f * s - dialR, dialR, hud.lean, accent)
    val numX = dialCx + dialR + 18f * s
    val labelBaseY = h - 26f * s
    drawLeanValue(canvas, numX, labelBaseY - 15f * s - 9f * s, labelBaseY, 92f * s, 42f * s, 15f * s, hud.lean, accent)

    // Speed (bottom-center).
    drawSpeed(canvas, w / 2f, h - 30f * s, 72f * s, 17f * s, s, hud.speedKmh, hud.isMetric, Paint.Align.CENTER)

    // Session-max symmetry block (bottom-right).
    val blockRight = w - 24f * s
    val blockW = 250f * s
    val blockLeft = blockRight - blockW
    val blockTop = h - 22f * s - 92f * s
    val absL = abs(hud.maxLeft).roundToInt()
    val absR = hud.maxRight.roundToInt()
    val denom = maxOf(absL, absR, 1).toFloat()

    monoPaint.textAlign = Paint.Align.LEFT
    shadowText(canvas, "SESSION MAX", blockLeft, blockTop + 12f * s, monoPaint, 13f * s, ink(0.7f))
    monoBoldPaint.textAlign = Paint.Align.RIGHT
    shadowText(canvas, "${maxOf(absL, absR)}°", blockRight, blockTop + 12f * s, monoBoldPaint, 16f * s, accentInk(accent))

    val barH = 5f * s
    val barX = blockLeft + 16f * s
    val barW = blockW - 16f * s - 30f * s
    // L row
    labelPaint.textAlign = Paint.Align.LEFT
    shadowText(canvas, "L", blockLeft, blockTop + 30f * s, labelPaint, 13f * s, ink(0.62f))
    drawBar(canvas, barX, blockTop + 24f * s, barW, barH, absL / denom, ink(0.78f))
    monoBoldPaint.textAlign = Paint.Align.RIGHT
    shadowText(canvas, "$absL", blockRight, blockTop + 31f * s, monoBoldPaint, 15f * s, ink())
    // R row
    labelPaint.textAlign = Paint.Align.LEFT
    shadowText(canvas, "R", blockLeft, blockTop + 52f * s, labelPaint, 13f * s, ink(0.62f))
    drawBar(canvas, barX, blockTop + 46f * s, barW, barH, absR / denom, accentInk(accent))
    monoBoldPaint.textAlign = Paint.Align.RIGHT
    shadowText(canvas, "$absR", blockRight, blockTop + 53f * s, monoBoldPaint, 15f * s, accentInk(accent))

    // Ghost / off-best line.
    ghostSegments(hud, portrait = false)?.let { (seg1, seg2) ->
        val size = 15f * s
        numPaint.textAlign = Paint.Align.LEFT
        numPaint.textSize = size
        labelPaint.textAlign = Paint.Align.LEFT
        labelPaint.textSize = size
        labelPaint.letterSpacing = 0f
        val total = numPaint.measureText(seg1) + labelPaint.measureText(seg2)
        val startX = blockRight - total
        shadowText(canvas, seg1, startX, blockTop + 84f * s, numPaint, size, accentInk(accent))
        shadowText(canvas, seg2, startX + numPaint.measureText(seg1), blockTop + 84f * s, labelPaint, size, ink(0.85f))
    }
}

private fun drawPortraitHud(canvas: Canvas, w: Float, h: Float, hud: HudState) {
    val s = w / 412f
    val accent = hud.accentColor
    drawScrims(canvas, w, h, 130f * s, 320f * s)

    drawRec(canvas, 20f * s, 29f * s, s, hud, 6f * s, 21f * s)
    drawGps(canvas, w - 20f * s, 29f * s, s, hud, 15f * s, null)

    drawWordmark(canvas, 20f * s, 54f * s, 21f * s, 17f * s, s, accent)

    // Lean dial + value.
    val dialR = 48f * s
    val dialCx = 20f * s + dialR
    drawDial(canvas, dialCx, h - 150f * s - dialR, dialR, hud.lean, accent)
    val numX = dialCx + dialR + 16f * s
    val labelBaseY = h - 154f * s
    drawLeanValue(canvas, numX, labelBaseY - 14f * s - 7f * s, labelBaseY, 88f * s, 38f * s, 14f * s, hud.lean, accent)

    // Speed (left) + session max (right).
    val rowBase = h - 96f * s
    drawSpeed(canvas, 20f * s, rowBase, 58f * s, 15f * s, s, hud.speedKmh, hud.isMetric, Paint.Align.LEFT)
    val absL = abs(hud.maxLeft).roundToInt()
    val absR = hud.maxRight.roundToInt()
    monoPaint.textAlign = Paint.Align.RIGHT
    shadowText(canvas, "SESSION MAX", w - 20f * s, rowBase - 38f * s, monoPaint, 13f * s, ink(0.7f))
    numPaint.textAlign = Paint.Align.RIGHT
    shadowText(canvas, "${maxOf(absL, absR)}°", w - 20f * s, rowBase, numPaint, 34f * s, accentInk(accent))

    // Symmetry row (mirrored) + centered ghost line.
    val denom = maxOf(absL, absR, 1).toFloat()
    val fullLeft = 20f * s
    val fullRight = w - 20f * s
    val cxCenter = w / 2f
    val barY = h - 22f * s - 36f * s
    val barH = 5f * s
    labelPaint.textAlign = Paint.Align.LEFT
    shadowText(canvas, "L", fullLeft, barY + barH + 5f * s, labelPaint, 13f * s, ink(0.62f))
    labelPaint.textAlign = Paint.Align.RIGHT
    shadowText(canvas, "R", fullRight, barY + barH + 5f * s, labelPaint, 13f * s, ink(0.62f))
    monoBoldPaint.textAlign = Paint.Align.RIGHT
    shadowText(canvas, "$absL", cxCenter - 9f * s, barY + barH + 5f * s, monoBoldPaint, 15f * s, ink())
    monoBoldPaint.textAlign = Paint.Align.LEFT
    shadowText(canvas, "$absR", cxCenter + 9f * s, barY + barH + 5f * s, monoBoldPaint, 15f * s, accentInk(accent))
    val lValW = monoBoldPaint.measureText("$absL")
    val rValW = monoBoldPaint.measureText("$absR")
    val lBarX = fullLeft + 16f * s
    val lBarW = (cxCenter - 9f * s - lValW - 7f * s) - lBarX
    drawBar(canvas, lBarX, barY, lBarW, barH, absL / denom, ink(0.78f))
    val rBarX = cxCenter + 9f * s + rValW + 7f * s
    val rBarW = (fullRight - 16f * s) - rBarX
    drawBar(canvas, rBarX, barY, rBarW, barH, absR / denom, accentInk(accent))

    ghostSegments(hud, portrait = true)?.let { (seg1, seg2) ->
        val size = 15f * s
        numPaint.textAlign = Paint.Align.LEFT
        numPaint.textSize = size
        labelPaint.textAlign = Paint.Align.LEFT
        labelPaint.textSize = size
        labelPaint.letterSpacing = 0f
        val total = numPaint.measureText(seg1) + labelPaint.measureText(seg2)
        val startX = cxCenter - total / 2f
        shadowText(canvas, seg1, startX, h - 24f * s, numPaint, size, accentInk(accent))
        shadowText(canvas, seg2, startX + numPaint.measureText(seg1), h - 24f * s, labelPaint, size, ink(0.85f))
    }
}
