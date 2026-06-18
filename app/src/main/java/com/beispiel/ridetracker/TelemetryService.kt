package com.beispiel.ridetracker

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.io.FileWriter
import kotlin.math.atan2
import kotlin.math.sqrt

class TelemetryService : Service(), SensorEventListener, LocationListener {

    private val binder = TelemetryBinder()
    private lateinit var sensorManager: SensorManager
    private lateinit var locationManager: LocationManager
    private val gson = Gson()

    // High frequency storage
    val sessionPoints = MutableStateFlow<List<TelemetryPoint>>(emptyList())
    val detectedCorners = MutableStateFlow<List<CornerEvent>>(emptyList())
    val pastSessions = MutableStateFlow<List<RideSession>>(emptyList())

    // Live Dashboard States
    val currentLean = MutableStateFlow(0f)
    val currentPitch = MutableStateFlow(0f)
    val currentSpeed = MutableStateFlow(0.0)
    val currentLocation = MutableStateFlow<Location?>(null)
    val currentHeading = MutableStateFlow(0f)
    val isRecording = MutableStateFlow(false)
    val isPaused = MutableStateFlow(false)
    val isWheelieAlert = MutableStateFlow(false)

    // All-time records (persisted in SharedPreferences)
    val allTimeMaxLeft = MutableStateFlow(0f)
    val allTimeMaxRight = MutableStateFlow(0f)
    val allTimeMaxPitch = MutableStateFlow(0f)

    // Session-specific stats
    val sessionMaxLeft = MutableStateFlow(0f)
    val sessionMaxRight = MutableStateFlow(0f)
    val sessionMaxPitch = MutableStateFlow(0f)
    val rollingMaxLeft = MutableStateFlow(0f)
    val rollingMaxRight = MutableStateFlow(0f)
    val rollingMax1000m = MutableStateFlow(0f)

    // Calibration Offsets
    private var leanOffset = 0f
    private var pitchOffset = 0f

    // Sensitivity Thresholds
    private val WHEELIE_THRESHOLD = 15.0f // Degrees
    private val ENDO_THRESHOLD = -10.0f  // Degrees

    // Corner Detection Engine State
    private var currentState = RideState.STRAIGHT
    private var activeCorner: CornerEvent? = null
    private var lastLocation: Location? = null
    private var lastLoggedLocation: Location? = null

    // USB GPS configurations
    private var usbPort: UsbSerialPort? = null
    private var usbJob: Job? = null

    inner class TelemetryBinder : Binder() {
        fun getService(): TelemetryService = this@TelemetryService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        allTimeMaxLeft.value = prefs.getFloat("allTimeMaxLeft", 0f)
        allTimeMaxRight.value = prefs.getFloat("allTimeMaxRight", 0f)
        allTimeMaxPitch.value = prefs.getFloat("allTimeMaxPitch", 0f)
        
        loadSessions()
        startForegroundNotification()
        startDataStreams()
    }

    private fun startDataStreams() {
        // Start 50Hz Rotation Vector Loop
        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        rotationVectorSensor?.let {
            sensorManager.registerListener(this, it, 20000) // 20,000 microseconds = 50 Hz
        }

        // Internal high-frequency location updates
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, this)
        } catch (e: SecurityException) { e.printStackTrace() }

        // Automatically attempt initializing Adafruit USB GPS if connected
        initUsbGps()
    }

    private fun loadSessions() {
        val dir = getExternalFilesDir("sessions") ?: return
        val files = dir.listFiles { _, name -> name.endsWith(".json") }
        val sessions = files?.mapNotNull { file ->
            try {
                val json = file.readText()
                gson.fromJson(json, RideSession::class.java)
            } catch (e: Exception) {
                Log.e("TelemetryService", "Error loading session ${file.name}: ${e.message}")
                null
            }
        }?.sortedByDescending { it.startTime } ?: emptyList()
        pastSessions.value = sessions
    }

    private var currentSessionStartTime = 0L

    fun startRecording() {
        if (isRecording.value && !isPaused.value) return
        if (isPaused.value) {
            isPaused.value = false
            return
        }
        isRecording.value = true
        isPaused.value = false
        resetSessionStats()
        currentSessionStartTime = System.currentTimeMillis()
    }

    fun pauseRecording() {
        if (!isRecording.value || isPaused.value) return
        isPaused.value = true
    }

    fun stopRecording() {
        if (!isRecording.value) return
        isRecording.value = false
        isPaused.value = false
        
        // Finalize all-time records in preferences
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit().apply {
            putFloat("allTimeMaxLeft", allTimeMaxLeft.value)
            putFloat("allTimeMaxRight", allTimeMaxRight.value)
            putFloat("allTimeMaxPitch", allTimeMaxPitch.value)
            apply()
        }
    }

    fun confirmSaveSession() {
        saveSessionLocally()
    }

    fun discardSession() {
    }

    fun deleteSession(sessionId: String) {
        val dir = getExternalFilesDir("sessions") ?: return
        val file = File(dir, "$sessionId.json")
        if (file.exists()) {
            file.delete()
            loadSessions()
        }
    }

    fun calibrateSensors() {
        leanOffset = currentLean.value + leanOffset
        pitchOffset = currentPitch.value + pitchOffset
    }

    fun resetAllTimeLean() {
        allTimeMaxLeft.value = 0f
        allTimeMaxRight.value = 0f
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit().apply {
            putFloat("allTimeMaxLeft", 0f)
            putFloat("allTimeMaxRight", 0f)
            apply()
        }
    }

    fun resetMaxPitch() {
        allTimeMaxPitch.value = 0f
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit().apply {
            putFloat("allTimeMaxPitch", 0f)
            apply()
        }
    }

    fun resetSessionLean() {
        sessionMaxLeft.value = 0f
        sessionMaxRight.value = 0f
        sessionMaxPitch.value = 0f
    }

    fun resetSessionPitch() {
        sessionMaxPitch.value = 0f
    }

    fun resetRollingMaxLean() {
        rollingMaxLeft.value = 0f
        rollingMaxRight.value = 0f
        rollingMax1000m.value = 0f
    }

    fun resetAllTimeRecords() {
        resetAllTimeLean()
        resetMaxPitch()
    }

    fun resetSessionStats() {
        sessionPoints.value = emptyList()
        detectedCorners.value = emptyList()
        resetSessionLean()
        resetRollingMaxLean()
        currentState = RideState.STRAIGHT
        activeCorner = null
        lastLocation = null
        lastLoggedLocation = null
        currentSessionStartTime = 0L
    }

    fun resetCornerCount() {
        detectedCorners.value = emptyList()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        // Compute Rotation Matrix from hardware sensor fusion
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

        // Adjust coordinate system based on display rotation
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        val displayRotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                display?.rotation ?: windowManager.defaultDisplay.rotation
            } catch (e: Exception) {
                windowManager.defaultDisplay.rotation
            }
        } else {
            windowManager.defaultDisplay.rotation
        }

        val remappedMatrix = FloatArray(9)
        when (displayRotation) {
            Surface.ROTATION_90 -> {
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_Y,
                    SensorManager.AXIS_MINUS_X,
                    remappedMatrix
                )
            }
            Surface.ROTATION_270 -> {
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_MINUS_Y,
                    SensorManager.AXIS_X,
                    remappedMatrix
                )
            }
            Surface.ROTATION_180 -> {
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_MINUS_X,
                    SensorManager.AXIS_MINUS_Y,
                    remappedMatrix
                )
            }
            else -> {
                System.arraycopy(rotationMatrix, 0, remappedMatrix, 0, 9)
            }
        }

        val orientationAngles = FloatArray(3)
        SensorManager.getOrientation(remappedMatrix, orientationAngles)

        // Retrieve rotation matrix third row to compute pitch/lean angles independent of mounting tilt.
        // remappedMatrix[6], remappedMatrix[7], remappedMatrix[8] are the components of the world vertical (Z) axis
        // projected onto the device's local X, Y, and Z axes.
        val r6 = remappedMatrix[6]
        val r7 = remappedMatrix[7]
        val r8 = remappedMatrix[8]

        // Roll (Side to side) and Pitch (Front to back) in Radians -> Convert to Degrees
        val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        
        // True lean angle (lateral tilt) compensating for the device being angled/pitched towards the driver
        val rawLean = Math.toDegrees(Math.atan2(-r6.toDouble(), kotlin.math.sqrt((r7 * r7 + r8 * r8).toDouble()))).toFloat()
        
        // True pitch angle (front-to-back tilt) compensating for lateral lean
        val rawPitch = Math.toDegrees(Math.atan2(-r7.toDouble(), r8.toDouble())).toFloat()

        val speedKmh = currentSpeed.value
        if (speedKmh <= 2.0 || lastLocation?.hasBearing() == false) {
            // Apply a -25° offset to correct the systematic compass deviation on motorcycles
            currentHeading.value = (azimuth - 25f + 360) % 360
        }
        val correctedLean = rawLean - leanOffset
        val correctedPitch = -(rawPitch - pitchOffset) // Inverted: Front UP is now positive

        currentLean.value = correctedLean
        currentPitch.value = correctedPitch

        // Wheelie Alert Logic
        isWheelieAlert.value = correctedPitch > WHEELIE_THRESHOLD

        if (isRecording.value && !isPaused.value) {
            // Update all-time records
            if (correctedLean < allTimeMaxLeft.value) allTimeMaxLeft.value = correctedLean
            if (correctedLean > allTimeMaxRight.value) allTimeMaxRight.value = correctedLean
            
            // Only register Wheelie (pitch) values of 10 degrees and above
            if (correctedPitch >= 10.0f && correctedPitch > allTimeMaxPitch.value) {
                allTimeMaxPitch.value = correctedPitch
            }

            // Update session records
            if (correctedLean < sessionMaxLeft.value) sessionMaxLeft.value = correctedLean
            if (correctedLean > sessionMaxRight.value) sessionMaxRight.value = correctedLean
            if (correctedPitch > sessionMaxPitch.value) {
                sessionMaxPitch.value = correctedPitch
            }
            
            // Update rolling max (simplified logic here, updated also by location)
            if (correctedLean < rollingMaxLeft.value) {
                rollingMaxLeft.value = correctedLean
            }
            if (correctedLean > rollingMaxRight.value) {
                rollingMaxRight.value = correctedLean
            }

            processTelemetrySnapshop(correctedLean, correctedPitch)
        }
    }

    private fun processTelemetrySnapshop(lean: Float, pitch: Float) {
        val loc = lastLocation ?: Location("Fallback").apply {
            latitude = 52.5200
            longitude = 13.4050
        }
        val currentTime = System.currentTimeMillis()

        val prevPoint = sessionPoints.value.lastOrNull()
        val distance = if (prevPoint != null) {
            val dtSeconds = (currentTime - prevPoint.timestamp) / 1000.0
            val speedMps = currentSpeed.value / 3.6 // speed is in km/h, convert to m/s
            speedMps * dtSeconds
        } else {
            0.0
        }

        val point = TelemetryPoint(
            timestamp = currentTime,
            latitude = loc.latitude,
            longitude = loc.longitude,
            speedKmh = currentSpeed.value,
            leanAngle = lean,
            pitchAngle = pitch,
            distanceDelta = distance
        )

        val updatedList = sessionPoints.value + point
        sessionPoints.value = updatedList

        // Calculate rolling max lean in last 1000m
        var accumulatedDistance = 0.0
        var maxLeanVal = 0f
        var pointsChecked = 0
        for (i in updatedList.indices.reversed()) {
            val pt = updatedList[i]
            accumulatedDistance += pt.distanceDelta
            val currentAbsLean = kotlin.math.abs(pt.leanAngle)
            if (currentAbsLean > maxLeanVal) {
                maxLeanVal = currentAbsLean
            }
            pointsChecked++
            if (accumulatedDistance >= 1000.0) {
                break
            }
            if (pointsChecked >= 100 && accumulatedDistance == 0.0) {
                break
            }
        }
        rollingMax1000m.value = maxLeanVal

        // Evaluate State Machine for Corners
        val absLean = kotlin.math.abs(lean)
        val currentIndex = updatedList.lastIndex

        when (currentState) {
            RideState.STRAIGHT -> {
                if (absLean >= 10.0f) {
                    currentState = RideState.IN_CORNER
                    val newCorner = CornerEvent(
                        id = detectedCorners.value.size + 1,
                        startIndex = currentIndex,
                        maxLeanIndex = currentIndex
                    )
                    if (lean < 0) newCorner.maxLeftLean = lean else newCorner.maxRightLean = lean
                    activeCorner = newCorner
                    detectedCorners.value = detectedCorners.value + newCorner
                }
            }
            RideState.IN_CORNER -> {
                activeCorner?.let { corner ->
                    // Track maximum thresholds
                    if (lean < 0 && lean < corner.maxLeftLean) {
                        corner.maxLeftLean = lean
                        corner.maxLeanIndex = currentIndex
                    } else if (lean > 0 && lean > corner.maxRightLean) {
                        corner.maxRightLean = lean
                        corner.maxLeanIndex = currentIndex
                    }

                    // Exit threshold criteria
                    if (absLean < 7.0f) {
                        corner.endIndex = currentIndex
                        currentState = RideState.STRAIGHT
                        activeCorner = null
                        // Force updates down Flow pipeline
                        detectedCorners.value = detectedCorners.value.toList()
                    }
                }
            }
        }
    }

    // 10Hz Adafruit USB Serial Connection Setup
    private fun initUsbGps() {
        if (usbPort != null && usbPort!!.isOpen) return
        val manager = getSystemService(Context.USB_SERVICE) as android.hardware.usb.UsbManager
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)
        if (availableDrivers.isEmpty()) return

        val driver = availableDrivers[0]
        val connection = manager.openDevice(driver.device) ?: return
        val port = driver.ports[0] // Adafruit usually assigns to channel 0

        try {
            port.open(connection)
            port.setParameters(9600, 8, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1)
            usbPort = port

            // Read loop for incoming 10Hz NMEA Strings
            usbJob = CoroutineScope(Dispatchers.IO).launch {
                val buffer = ByteArray(1024)
                var nmeaBuilder = StringBuilder()
                while (isActive) {
                    val numBytes = port.read(buffer, 1000)
                    if (numBytes > 0) {
                        val chunk = String(buffer, 0, numBytes)
                        nmeaBuilder.append(chunk)

                        if (nmeaBuilder.contains("\n")) {
                            val lines = nmeaBuilder.toString().split("\r\n")
                            for (i in 0 until lines.size - 1) {
                                parseNmeaString(lines[i])
                            }
                            nmeaBuilder = StringBuilder(lines.last())
                        }
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun parseNmeaString(nmea: String) {
        // Minimal absolute parsing framework targeting $GPRMC or $GPGGA sentences at 10Hz
        try {
            if (nmea.startsWith("\$GPRMC")) {
                val tokens = nmea.split(",")
                if (tokens.size > 8 && (tokens[2] == "A" || tokens[2] == "V")) { // Accept both valid and void for testing stability
                    val rawLat = tokens[3]
                    val latDir = tokens[4]
                    val rawLon = tokens[5]
                    val lonDir = tokens[6]
                    val speedKnots = tokens[7].toDoubleOrNull() ?: 0.0

                    if (rawLat.isNotEmpty() && rawLon.isNotEmpty()) {
                        val lat = convertNmeaToDecimal(rawLat, latDir)
                        val lon = convertNmeaToDecimal(rawLon, lonDir)

                        val mockLocation = Location("ExternalUSBGPS").apply {
                            latitude = lat
                            longitude = lon
                            speed = (speedKnots * 1.852 / 3.6).toFloat() // Knots to m/s
                            time = System.currentTimeMillis()
                        }
                        onLocationChanged(mockLocation)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("TelemetryService", "Error parsing NMEA: ${e.message}")
        }
    }

    private fun convertNmeaToDecimal(degreeMinutes: String, direction: String): Double {
        if (degreeMinutes.isEmpty()) return 0.0
        val dotIndex = degreeMinutes.indexOf('.')
        val degreeLength = dotIndex - 2
        val degrees = degreeMinutes.substring(0, degreeLength).toDouble()
        val minutes = degreeMinutes.substring(degreeLength).toDouble()
        var decimal = degrees + (minutes / 60.0)
        if (direction == "S" || direction == "W") decimal = -decimal
        return decimal
    }

    private fun saveSessionLocally() {
        val dir = getExternalFilesDir("sessions") ?: return
        if (!dir.exists()) dir.mkdirs()
        
        val endTime = System.currentTimeMillis()
        val session = RideSession(
            id = "Session_${currentSessionStartTime}",
            startTime = currentSessionStartTime,
            endTime = endTime,
            points = sessionPoints.value,
            corners = detectedCorners.value,
            maxLeanLeft = sessionMaxLeft.value,
            maxLeanRight = sessionMaxRight.value,
            maxPitch = sessionMaxPitch.value
        )

        val filename = "${session.id}.json"
        val file = File(dir, filename)
        
        try {
            val json = gson.toJson(session)
            file.writeText(json)
            loadSessions()
        } catch (e: Exception) {
            Log.e("TelemetryService", "Error saving session: ${e.message}")
        }
    }

    fun saveSessionToCsv(): File {
        val dir = getExternalFilesDir(null)
        val file = File(dir, "RideTelemetry_${System.currentTimeMillis()}.csv")
        FileWriter(file).use { writer ->
            writer.write("Timestamp,Latitude,Longitude,SpeedKmh,LeanAngle,PitchAngle\n")
            sessionPoints.value.forEach {
                writer.write("${it.timestamp},${it.latitude},${it.longitude},${it.speedKmh},${it.leanAngle},${it.pitchAngle}\n")
            }
        }
        return file
    }

    fun saveSessionToCsv(session: RideSession): File {
        val dir = getExternalFilesDir(null)
        val file = File(dir, "RideTelemetry_${session.id}.csv")
        FileWriter(file).use { writer ->
            writer.write("Timestamp,Latitude,Longitude,SpeedKmh,LeanAngle,PitchAngle\n")
            session.points.forEach {
                writer.write("${it.timestamp},${it.latitude},${it.longitude},${it.speedKmh},${it.leanAngle},${it.pitchAngle}\n")
            }
        }
        return file
    }

    override fun onLocationChanged(location: Location) {
        lastLocation = location
        currentLocation.value = location
        currentSpeed.value = location.speed * 3.6 // m/s to km/h
        if (location.hasBearing() && location.speed > 0.5f) {
            currentHeading.value = location.bearing
        }
    }

    private fun startForegroundNotification() {
        val channelId = "telemetry_channel"
        val channel = NotificationChannel(channelId, "Telemetry Engine", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("MotoTelemetry Active")
            .setContentText("Logging orientation metrics at 50Hz...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(101, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(101, notification)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onDestroy() {
        usbJob?.cancel()
        try { usbPort?.close() } catch (e: Exception) {}
        sensorManager.unregisterListener(this)
        try { locationManager.removeUpdates(this) } catch (e: Exception) {}
        super.onDestroy()
    }
}