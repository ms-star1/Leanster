import re

with open('app/src/main/java/com/beispiel/ridetracker/DashboardView.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Add Imports
content = content.replace('import androidx.compose.material3.*', 'import androidx.compose.material3.*\\nimport androidx.compose.material.icons.filled.Menu\\nimport android.content.Intent\\nimport android.net.Uri')

# Inject TopRightControls in DashboardView
overlay = '''
        TopRightControls(
            service = service,
            onShowSettings = onShowSettings,
            onShowHistory = onShowHistory,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 16.dp).zIndex(10f)
        )

        if (isLandscape) {'''
content = content.replace('        if (isLandscape) {', overlay, 1)

# Modify DashboardPortraitLayout
portrait_find = '''        // Metric Row
        val absLeanVal = abs(currentLean)
        val leanProgress = ((absLeanVal - 30f) / 10f).coerceIn(0f, 1f)
        val max1000mProgress = ((rollingMax1000m - 30f) / 10f).coerceIn(0f, 1f)

        // Unified Telemetry Top Row (LEAN, SPEED, MAX 1000M)
        Row(
            modifier = Modifier.fillMaxWidth().height(90.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {'''
portrait_repl = '''        var topBox1 by rememberSaveable { mutableStateOf(DashboardMetric.LEAN) }
        var topBox2 by rememberSaveable { mutableStateOf(DashboardMetric.MAX_LEAN_1000M) }
        var bottomBox1 by rememberSaveable { mutableStateOf(DashboardMetric.SPEED) }
        var bottomBox2 by rememberSaveable { mutableStateOf(DashboardMetric.SESSION_MAX_LEAN) }

        // Unified Telemetry Top Row
        Row(
            modifier = Modifier.fillMaxWidth().height(90.dp).padding(end = 60.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StartupAnimatedElement(order = 0, triggered = startAnimTriggered, skipAnimation = hasPlayedStartupAnimation, modifier = Modifier.weight(1.0f).fillMaxHeight()) {
                CustomizableMetricCard(selectedMetric = topBox1, onMetricSelected = { topBox1 = it }, service = service, isMetric = isMetric, highlightColor = highlightColor, activeView = activeView, modifier = Modifier.fillMaxHeight())
            }
            StartupAnimatedElement(order = 1, triggered = startAnimTriggered, skipAnimation = hasPlayedStartupAnimation, modifier = Modifier.weight(1.0f).fillMaxHeight()) {
                CustomizableMetricCard(selectedMetric = topBox2, onMetricSelected = { topBox2 = it }, service = service, isMetric = isMetric, highlightColor = highlightColor, activeView = activeView, modifier = Modifier.fillMaxHeight())
            }
        }
'''
content = re.sub(r'        // Metric Row\n.*?\) \{', portrait_repl, content, flags=re.DOTALL)

# Delete the old top row contents up to the Spacer
content = re.sub(r'            // Lean Card.*?Spacer\(modifier = Modifier.height\(8.dp\)\)', '        Spacer(modifier = Modifier.height(8.dp))', content, flags=re.DOTALL)

# Remove History and All Time windows from Portrait
content = re.sub(r'            // History Data Window.*?// Control Buttons', '            // 2 Bottom Value Boxes\n            Spacer(modifier = Modifier.height(16.dp))\n            Row(\n                modifier = Modifier.fillMaxWidth().height(90.dp),\n                horizontalArrangement = Arrangement.spacedBy(8.dp),\n                verticalAlignment = Alignment.CenterVertically\n            ) {\n                StartupAnimatedElement(order = 5, triggered = startAnimTriggered, skipAnimation = hasPlayedStartupAnimation, modifier = Modifier.weight(1.0f).fillMaxHeight()) {\n                    CustomizableMetricCard(selectedMetric = bottomBox1, onMetricSelected = { bottomBox1 = it }, service = service, isMetric = isMetric, highlightColor = highlightColor, activeView = activeView, modifier = Modifier.fillMaxHeight())\n                }\n                StartupAnimatedElement(order = 6, triggered = startAnimTriggered, skipAnimation = hasPlayedStartupAnimation, modifier = Modifier.weight(1.0f).fillMaxHeight()) {\n                    CustomizableMetricCard(selectedMetric = bottomBox2, onMetricSelected = { bottomBox2 = it }, service = service, isMetric = isMetric, highlightColor = highlightColor, activeView = activeView, modifier = Modifier.fillMaxHeight())\n                }\n            }\n\n            Spacer(modifier = Modifier.height(16.dp))\n\n            // Control Buttons', content, flags=re.DOTALL)

# Replace DashboardLandscapeLayout entirely
landscape_find = r'@Suppress\(\"UNUSED_PARAMETER\"\)\n@Composable\nfun DashboardLandscapeLayout\(.*?\}\n    \}\n}'
landscape_repl = '''@Suppress("UNUSED_PARAMETER")
@Composable
fun DashboardLandscapeLayout(
    service: TelemetryService,
    isMetric: Boolean,
    highlightColor: Color,
    mutedHighlightColor: Color,
    highlightColorName: String,
    onColorChange: (String) -> Unit,
    onToggleUnit: () -> Unit,
    onShowHistory: () -> Unit,
    onShowSettings: () -> Unit,
    mapView: org.maplibre.android.maps.MapView?,
    activeView: String,
    onActiveViewChange: (String) -> Unit,
    isRecording: Boolean,
    isPaused: Boolean,
    startAnimTriggered: Boolean,
    hasPlayedStartupAnimation: Boolean,
    corners: List<CornerEvent>,
    allTimeMaxLeft: Float,
    allTimeMaxRight: Float,
    sessionMaxPitch: Float,
    sessionMaxLeft: Float,
    sessionMaxRight: Float,
    rollingMaxLeft: Float,
    rollingMaxRight: Float,
    rollingMax1000m: Float,
    rollingDistanceTarget: Int,
    onResetMaxLean: () -> Unit = {}
) {
    val currentLean by service.currentLean.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(0.7f).fillMaxHeight().zIndex(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onActiveViewChange("lean") },
                        colors = ButtonDefaults.buttonColors(containerColor = if (activeView == "lean") highlightColor else SurfaceCard, contentColor = if (activeView == "lean") DeepCarbon else PureWhite),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("Horizon", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), textAlign = TextAlign.Center, maxLines = 1) }
                    Button(
                        onClick = { onActiveViewChange("map") },
                        colors = ButtonDefaults.buttonColors(containerColor = if (activeView == "map") highlightColor else SurfaceCard, contentColor = if (activeView == "map") DeepCarbon else PureWhite),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("Map", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), textAlign = TextAlign.Center, maxLines = 1) }
                }

                Spacer(modifier = Modifier.weight(1f))

                var showStopDialog by remember { mutableStateOf(false) }
                if (showStopDialog) {
                    AlertDialog(
                        onDismissRequest = { showStopDialog = false },
                        title = { Text("Stop Recording") },
                        text = { Text("Are you sure you want to stop recording?") },
                        confirmButton = {
                            TextButton(onClick = { showStopDialog = false; service.stopRecording() }) { Text("Stop", color = AlertRed) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showStopDialog = false }) { Text("Cancel", color = PureWhite) }
                        },
                        containerColor = SurfaceCard,
                        titleContentColor = PureWhite,
                        textContentColor = MutedGrey
                    )
                }

                Button(
                    onClick = { if (!isRecording || isPaused) service.startRecording() else service.pauseRecording() },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isRecording && !isPaused) AlertRed else highlightColor, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isRecording && !isPaused) "PAUSE SESSION" else if (isPaused) "RESUME SESSION" else "START SESSION", style = MaterialTheme.typography.titleSmall.copy(fontFamily = Inter, color = Color.Black, fontWeight = FontWeight.ExtraBold), maxLines = 1, softWrap = false)
                }

                if (isRecording) {
                    OutlinedButton(
                        onClick = { showStopDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = DeepCarbon, contentColor = AlertRed),
                        border = BorderStroke(2.dp, AlertRed),
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("STOP", style = MaterialTheme.typography.titleMedium.copy(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp), maxLines = 1, softWrap = false) }
                }
            }

            if (activeView == "lean") {
                LeanHorizonIndicator(lean = currentLean, highlightColor = highlightColor, modifier = Modifier.weight(1.8f).fillMaxHeight(), sizeScale = 1.0f, yOffsetPercent = 0.0f)
            } else {
                Spacer(modifier = Modifier.weight(1.8f))
            }

            var rightOversizedBox by rememberSaveable { mutableStateOf(DashboardMetric.LEAN) }
            Column(
                modifier = Modifier.weight(1.0f).fillMaxHeight().zIndex(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Spacer(modifier = Modifier.height(48.dp))
                CustomizableMetricCard(selectedMetric = rightOversizedBox, onMetricSelected = { rightOversizedBox = it }, service = service, isMetric = isMetric, highlightColor = highlightColor, activeView = activeView, modifier = Modifier.fillMaxWidth(), height = 78.dp, isOversized = true)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        var bottomLandscape1 by rememberSaveable { mutableStateOf(DashboardMetric.SPEED) }
        var bottomLandscape2 by rememberSaveable { mutableStateOf(DashboardMetric.MAX_LEAN_1000M) }
        var bottomLandscape3 by rememberSaveable { mutableStateOf(DashboardMetric.SESSION_MAX_LEAN) }
        var bottomLandscape4 by rememberSaveable { mutableStateOf(DashboardMetric.CORNERS) }

        Row(
            modifier = Modifier.fillMaxWidth().height(70.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CustomizableMetricCard(selectedMetric = bottomLandscape1, onMetricSelected = { bottomLandscape1 = it }, service = service, isMetric = isMetric, highlightColor = highlightColor, activeView = activeView, modifier = Modifier.weight(1f).fillMaxHeight())
            CustomizableMetricCard(selectedMetric = bottomLandscape2, onMetricSelected = { bottomLandscape2 = it }, service = service, isMetric = isMetric, highlightColor = highlightColor, activeView = activeView, modifier = Modifier.weight(1f).fillMaxHeight())
            CustomizableMetricCard(selectedMetric = bottomLandscape3, onMetricSelected = { bottomLandscape3 = it }, service = service, isMetric = isMetric, highlightColor = highlightColor, activeView = activeView, modifier = Modifier.weight(1f).fillMaxHeight())
            CustomizableMetricCard(selectedMetric = bottomLandscape4, onMetricSelected = { bottomLandscape4 = it }, service = service, isMetric = isMetric, highlightColor = highlightColor, activeView = activeView, modifier = Modifier.weight(1f).fillMaxHeight())
        }
    }
}'''
content = re.sub(landscape_find, landscape_repl, content, flags=re.DOTALL)


# Replace ControlButtonsContentV3 entirely
control_btns_find = r'@Composable\nfun ControlButtonsContentV3.*?\}\n    \}\n}'
control_btns_repl = '''@Composable
fun ControlButtonsContentV3(
    isRecording: Boolean,
    isPaused: Boolean,
    highlightColor: Color,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onCalibrate: () -> Unit,
    onStop: () -> Unit,
    onShowHistory: () -> Unit,
    onShowSettings: () -> Unit,
    modifier: Modifier = Modifier,
    isMapMode: Boolean = false
) {
    val buttonAlpha = if (isMapMode) 0.8f else 1.0f
    var showStopDialog by remember { mutableStateOf(false) }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("Stop Recording") },
            text = { Text("Are you sure you want to stop recording?") },
            confirmButton = {
                TextButton(onClick = { showStopDialog = false; onStop() }) { Text("Stop", color = AlertRed) }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) { Text("Cancel", color = PureWhite) }
            },
            containerColor = SurfaceCard,
            titleContentColor = PureWhite,
            textContentColor = MutedGrey
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(72.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { if (!isRecording || isPaused) onStart() else onPause() },
                colors = ButtonDefaults.buttonColors(containerColor = if (isRecording && !isPaused) AlertRed.copy(alpha = buttonAlpha) else highlightColor.copy(alpha = buttonAlpha), contentColor = Color.Black),
                modifier = Modifier.fillMaxHeight().weight(1.4f),
                contentPadding = PaddingValues(horizontal = 8.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, Color.Black.copy(alpha = 0.3f))
            ) {
                Text(if (isRecording && !isPaused) "PAUSE SESSION" else if (isPaused) "RESUME SESSION" else "START SESSION", style = MaterialTheme.typography.titleMedium.copy(fontFamily = Inter, color = Color.Black, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp), maxLines = 1, softWrap = false)
            }

            OutlinedButton(
                onClick = { if (isRecording) showStopDialog = true else onStop() },
                colors = ButtonDefaults.outlinedButtonColors(containerColor = DeepCarbon.copy(alpha = buttonAlpha), contentColor = AlertRed),
                border = BorderStroke(2.5.dp, AlertRed),
                modifier = Modifier.fillMaxHeight().weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("STOP", style = MaterialTheme.typography.titleMedium.copy(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp), maxLines = 1, softWrap = false)
            }
        }
    }
}'''
content = re.sub(control_btns_find, control_btns_repl, content, flags=re.DOTALL)

# Add new components to the end
new_components = '''

enum class DashboardMetric(val title: String) {
    LEAN("LEAN"),
    SPEED("SPEED"),
    MAX_LEAN_1000M("MAX 1000m LEAN"),
    SESSION_MAX_LEAN("SESSION MAX LEAN"),
    ALL_TIME_MAX_LEAN("ALL TIME MAX LEAN"),
    CORNERS("CORNERS DRIVEN"),
    WHEELIE("WHEELIE ANGLE")
}

@Composable
fun CustomizableMetricCard(
    selectedMetric: DashboardMetric,
    onMetricSelected: (DashboardMetric) -> Unit,
    service: TelemetryService,
    isMetric: Boolean,
    highlightColor: Color,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 80.dp,
    isOversized: Boolean = false,
    activeView: String = "lean"
) {
    val currentLean by service.currentLean.collectAsStateWithLifecycle()
    val currentSpeed by service.currentSpeed.collectAsStateWithLifecycle()
    val rollingMax1000m by service.rollingMax1000m.collectAsStateWithLifecycle()
    val rollingDistanceTarget by service.rollingDistanceTarget.collectAsStateWithLifecycle()
    val sessionMaxLeft by service.sessionMaxLeft.collectAsStateWithLifecycle()
    val sessionMaxRight by service.sessionMaxRight.collectAsStateWithLifecycle()
    val allTimeMaxLeft by service.allTimeMaxLeft.collectAsStateWithLifecycle()
    val allTimeMaxRight by service.allTimeMaxRight.collectAsStateWithLifecycle()
    val corners by service.detectedCorners.collectAsStateWithLifecycle()
    val sessionMaxPitch by service.sessionMaxPitch.collectAsStateWithLifecycle()

    var expanded by remember { mutableStateOf(false) }

    val title = when (selectedMetric) {
        DashboardMetric.MAX_LEAN_1000M -> if (rollingDistanceTarget >= 1000) "MAX ${rollingDistanceTarget / 1000}km" else "MAX ${rollingDistanceTarget}m"
        else -> selectedMetric.title
    }

    var valueStr = ""
    var unitStr = ""
    var progress = 0f

    when (selectedMetric) {
        DashboardMetric.LEAN -> {
            val absLeanVal = kotlin.math.abs(currentLean)
            valueStr = absLeanVal.toInt().toString()
            unitStr = "°"
            progress = ((absLeanVal - 30f) / 10f).coerceIn(0f, 1f)
        }
        DashboardMetric.SPEED -> {
            valueStr = if (isMetric) currentSpeed.toInt().toString() else (currentSpeed * 0.621371).toInt().toString()
            unitStr = if (isMetric) "km/h" else "mph"
            progress = 0f
        }
        DashboardMetric.MAX_LEAN_1000M -> {
            valueStr = rollingMax1000m.toInt().toString()
            unitStr = "°"
            progress = ((rollingMax1000m - 30f) / 10f).coerceIn(0f, 1f)
        }
        DashboardMetric.SESSION_MAX_LEAN -> {
            valueStr = "L:${kotlin.math.abs(sessionMaxLeft).toInt()} R:${sessionMaxRight.toInt()}"
            unitStr = "°"
        }
        DashboardMetric.ALL_TIME_MAX_LEAN -> {
            valueStr = "L:${kotlin.math.abs(allTimeMaxLeft).toInt()} R:${allTimeMaxRight.toInt()}"
            unitStr = "°"
        }
        DashboardMetric.CORNERS -> {
            valueStr = corners.size.toString()
            unitStr = ""
        }
        DashboardMetric.WHEELIE -> {
            valueStr = sessionMaxPitch.toInt().toString()
            unitStr = "°"
        }
    }

    val containerColor = if (progress > 0f) androidx.compose.ui.graphics.lerp(SurfaceCard, AlertRed, progress) else SurfaceCard
    val finalContainerColor = if (activeView == "map") containerColor.copy(alpha = 0.8f) else containerColor

    Card(
        modifier = modifier
            .height(height)
            .clickable { expanded = true },
        colors = CardDefaults.cardColors(containerColor = finalContainerColor),
        border = BorderStroke(if (isOversized) 1.5.dp else 1.dp, if (isOversized) highlightColor else BorderDivider),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(SurfaceCard)
            ) {
                DashboardMetric.values().forEach { metric ->
                    DropdownMenuItem(
                        text = { Text(metric.title, color = PureWhite) },
                        onClick = {
                            onMetricSelected(metric)
                            expanded = false
                        }
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = if (height < 70.dp || valueStr.length > 6) 9.sp else 12.sp,
                        letterSpacing = if (isOversized) 1.5.sp else 0.sp
                    ),
                    color = MutedGrey,
                    maxLines = 1,
                    softWrap = false
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = valueStr,
                        style = if (isOversized) MaterialTheme.typography.displayMedium.copy(fontSize = 32.sp, color = highlightColor)
                                else MaterialTheme.typography.displaySmall.copy(
                                    fontSize = if (height < 70.dp) 16.sp else if (valueStr.length > 6) 16.sp else 24.sp,
                                    fontWeight = FontWeight(700 + (progress * 200).toInt()),
                                    fontFamily = Rajdhani
                                ),
                        color = if (isOversized) highlightColor else PureWhite,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (unitStr.isNotEmpty()) {
                        Text(
                            text = unitStr,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = if (height < 70.dp) 8.sp else 9.sp),
                            color = MutedGrey,
                            modifier = Modifier.padding(bottom = if (height < 70.dp) 2.dp else 4.dp, start = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopRightControls(
    service: TelemetryService,
    onShowSettings: () -> Unit,
    onShowHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showCalibrationToast by remember { mutableStateOf(false) }

    if (showCalibrationToast) {
        LaunchedEffect(Unit) {
            android.widget.Toast.makeText(
                context,
                "3D Sensor Matrix Calibrated!",
                android.widget.Toast.LENGTH_LONG
            ).show()
            kotlinx.coroutines.delay(100)
            showCalibrationToast = false
        }
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(onClick = {
            val activity = context as? android.app.Activity
            val newOrientation = if (activity?.requestedOrientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            activity?.requestedOrientation = newOrientation
        }) {
            Icon(imageVector = androidx.compose.material.icons.Icons.Default.ScreenRotation, contentDescription = "Rotate", tint = PureWhite)
        }

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(imageVector = androidx.compose.material.icons.Icons.Default.Menu, contentDescription = "Menu", tint = PureWhite)
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(SurfaceCard)
            ) {
                DropdownMenuItem(
                    text = { Text("Settings", color = PureWhite) },
                    onClick = {
                        menuExpanded = false
                        onShowSettings()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Calibrate 3D", color = PureWhite) },
                    onClick = {
                        menuExpanded = false
                        service.calibrateSensors()
                        showCalibrationToast = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("Past Sessions", color = PureWhite) },
                    onClick = {
                        menuExpanded = false
                        onShowHistory()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Donate to this app", color = NeonCyan) },
                    onClick = {
                        menuExpanded = false
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=michasteinauer@gmail.com&item_name=RideTracker+Donation")
                        )
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}
'''
content += new_components

with open('app/src/main/java/com/beispiel/ridetracker/DashboardView.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("SUCCESS")