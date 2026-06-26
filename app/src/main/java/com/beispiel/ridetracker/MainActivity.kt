package com.beispiel.ridetracker

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import androidx.core.net.toUri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.preference.PreferenceManager
import androidx.compose.ui.graphics.Color
import com.beispiel.ridetracker.ui.theme.*
import org.maplibre.android.maps.MapView
import java.io.File

class MainActivity : ComponentActivity() {
    private var telemetryService by mutableStateOf<TelemetryService?>(null)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TelemetryService.TelemetryBinder
            telemetryService = binder.getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            telemetryService = null
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            startTelemetryService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        // Disable system auto-rotation by programmatically locking screen orientation based on user default mode setting
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val defaultMode = prefs.getString("default_launch_mode", "Portrait") ?: "Portrait"
        requestedOrientation = if (defaultMode == "Landscape") {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        requestPermissionLauncher.launch(permissions.toTypedArray())

        setContent {
            RideTrackerTheme {
                MainContent()
            }
        }
    }

    @Composable
    fun MainContent() {
        var currentTab by rememberSaveable { mutableIntStateOf(value = 0) }
        var isMetric by rememberSaveable { mutableStateOf(value = true) }
        var highlightColorName by rememberSaveable { mutableStateOf("Cyan") }
        
        val highlightColor = when (highlightColorName) {
            "Yamaha Blue" -> YamahaBlue
            "Ducati Red" -> DucatiRed
            "Kawasaki Green" -> KawasakiGreen
            "Pure White" -> PureWhiteHighlight
            else -> NeonCyan
        }
        
        val mutedHighlightColor = when (highlightColorName) {
            "Yamaha Blue" -> MutedYamahaBlue
            "Ducati Red" -> MutedDucatiRed
            "Kawasaki Green" -> MutedKawasakiGreen
            "Pure White" -> MutedWhiteHighlight
            else -> MutedCyan
        }

        var hasPlayedStartupAnimation by rememberSaveable { mutableStateOf(value = false) }
        val service = telemetryService
        val isRecording by (service?.isRecording?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(value = false) })
        val context = LocalContext.current

        val mapView = remember {
            MapView(context).apply {
                onCreate(null)
                getMapAsync { maplibreMap ->
                    try {
                        val styleJson = context.assets.open("style.json").bufferedReader().use { it.readText() }
                        maplibreMap.setStyle(org.maplibre.android.maps.Style.Builder().fromJson(styleJson))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    maplibreMap.moveCamera(
                        org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                            org.maplibre.android.geometry.LatLng(52.5200, 13.4050),
                            15.0,
                        ),
                    )
                }
            }
        }

        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner, mapView) {
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                when (event) {
                    androidx.lifecycle.Lifecycle.Event.ON_START -> mapView.onStart()
                    androidx.lifecycle.Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    androidx.lifecycle.Lifecycle.Event.ON_STOP -> mapView.onStop()
                    androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                if (service != null) {
                    when (currentTab) {
                        0 -> DashboardView(
                            service = service,
                            isMetric = isMetric,
                            highlightColor = highlightColor,
                            mutedHighlightColor = mutedHighlightColor,
                            highlightColorName = highlightColorName,
                            onColorChange = { highlightColorName = it },
                            onToggleUnit = { isMetric = !isMetric },
                            onShowHistory = { currentTab = 3 },
                            onShowSettings = { currentTab = 1 },
                            mapView = mapView,
                            hasPlayedStartupAnimation = hasPlayedStartupAnimation,
                            onResetMaxLean = { service.resetMaxLean1000m() }
                        ) {
                            hasPlayedStartupAnimation = true
                        }
                        1 -> {
                            BackHandler(enabled = true) {
                                currentTab = 0
                            }
                            SettingsScreen(
                                service = service,
                                isMetric = isMetric,
                                highlightColor = highlightColor,
                                highlightColorName = highlightColorName,
                                onColorChange = { highlightColorName = it },
                                onToggleUnit = { isMetric = !isMetric },
                                onBack = { currentTab = 0 }
                            )
                        }
                        2 -> {
                            val points by service.sessionPoints.collectAsStateWithLifecycle()
                            val corners by service.detectedCorners.collectAsStateWithLifecycle()
                            
                            SessionSummaryScreen(
                                points = points,
                                corners = corners,
                                isMetric = isMetric,
                                highlightColor = highlightColor,
                                onSave = {
                                    service.confirmSaveSession()
                                    currentTab = 0
                                },
                                onDiscard = {
                                    service.discardSession()
                                    currentTab = 0
                                },
                                mapView = mapView
                            )
                        }
                        3 -> {
                            val sessions by service.pastSessions.collectAsStateWithLifecycle()
                            val demoSession = remember { createDemoSession() }
                            val allSessionsWithDemo = remember(sessions) { listOf(demoSession) + sessions }
                            var selectedSession by remember { mutableStateOf<RideSession?>(null) }
                            var ghostPair by remember { mutableStateOf<Pair<RideSession, RideSession>?>(null) }

                            BackHandler(enabled = true) {
                                when {
                                    ghostPair != null -> ghostPair = null
                                    selectedSession != null -> selectedSession = null
                                    else -> currentTab = 0
                                }
                            }

                            Box(modifier = Modifier.fillMaxSize()) {
                                HistoryMenuScreen(
                                    sessions = sessions,
                                    highlightColor = highlightColor,
                                    isMetric = isMetric,
                                    onBack = { currentTab = 0 },
                                    onDeleteSession = { sessionId ->
                                        service.deleteSession(sessionId)
                                    },
                                    onSelectSession = { session ->
                                        selectedSession = session
                                    },
                                    onExportSession = { session ->
                                        val csvFile = service.saveSessionToCsv(session)
                                        shareCsvFile(context, csvFile)
                                    }
                                )

                                if (selectedSession != null) {
                                    SessionSummaryOverlay(
                                        session = selectedSession!!,
                                        isMetric = isMetric,
                                        highlightColor = highlightColor,
                                        onClose = { selectedSession = null },
                                        mapView = mapView,
                                        onGhostReplay = { current, ghost ->
                                            ghostPair = Pair(current, ghost)
                                        },
                                        allSessions = allSessionsWithDemo,
                                        onDelete = { sessionId ->
                                            service.deleteSession(sessionId)
                                            selectedSession = null
                                        }
                                    )
                                }

                                ghostPair?.let { (current, ghost) ->
                                    GhostReplayScreen(
                                        currentSession = current,
                                        ghostSession = ghost,
                                        highlightColor = highlightColor,
                                        mapView = mapView,
                                        onClose = { ghostPair = null }
                                    )
                                }
                            }
                        }
                    }
                    
                    LaunchedEffect(isRecording) {
                        if (((!isRecording) && ((currentTab == 0) || (currentTab == 1))) && (service.sessionPoints.value.isNotEmpty())) {
                            currentTab = 2
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Connecting to Telemetry Engine...", color = PureWhite)
                    }
                }
            }
        }
    }

    @Suppress("unused")
    @Composable
    fun AnimatedSplashScreen(onAnimationComplete: () -> Unit) {
        val context = LocalContext.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepCarbon),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx: Context ->
                    VideoView(ctx).apply {
                        val videoPath = "android.resource://${context.packageName}/${R.raw.splashv3}"
                        setVideoURI(videoPath.toUri())
                        setOnCompletionListener {
                            onAnimationComplete()
                        }
                        start()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    private fun shareCsvFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Telemetry CSV"))
    }

    private fun startTelemetryService() {
        val intent = Intent(this, TelemetryService::class.java)
        startForegroundService(intent)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (telemetryService != null) {
            unbindService(serviceConnection)
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun MapContent(
    service: TelemetryService,
    mapView: MapView,
    modifier: Modifier = Modifier,
    highlightColor: Color = NeonCyan
) {
    val currentLocation by service.currentLocation.collectAsStateWithLifecycle()
    val currentHeading by service.currentHeading.collectAsStateWithLifecycle()

    val hasFix = (currentLocation != null) && ((currentLocation?.accuracy ?: 100f) < 30f)
    
    Box(modifier = modifier.fillMaxSize()) {
        if (currentLocation != null) {
            MinimalistCockpitMap(
                currentLocation = currentLocation!!,
                currentBearing = currentHeading
            )
        }

        // Waiting overlay
        AnimatedVisibility(
            visible = !hasFix,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepCarbon.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = highlightColor)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Waiting for location...",
                        color = PureWhite,
                        style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
                    )
                }
            }
        }

        // Red arrow marker (triangle)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(30.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path().apply {
                    moveTo(size.width / 2f, 0f)
                    lineTo(size.width, size.height)
                    lineTo(size.width / 2f, size.height * 0.8f)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(path, color = PureWhite)
            }
        }
    }
}
