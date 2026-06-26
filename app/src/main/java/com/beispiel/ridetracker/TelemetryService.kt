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
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import kotlin.math.abs as absF
import androidx.core.app.NotificationCompat
import com.beispiel.ridetracker.database.AppDatabase
import com.beispiel.ridetracker.database.entities.SessionCornerEntity
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
    val isDevModeActive = MutableStateFlow(false)

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
    val rollingDistanceTarget = MutableStateFlow(1000)

    // 3D Mounting Calibration Matrix
    private var rDeviceToBike = FloatArray(9) { if (it % 4 == 0) 1f else 0f }
    private var isCalibrating = false
    val isCalibrated = MutableStateFlow(false)

    // Calibration Watch Engine
    val calibrationAlert = MutableStateFlow<String?>(null)
    private var calibrationToneGen: ToneGenerator? = null
    private var calibrationWarmupUntil = 0L

    // Complementary Filter & Gyro variables
    private var lastTimestamp = 0L
    private var leanAngle = 0f // In degrees
    private var pitchAngle = 0f // In degrees
    private var lastTargetLean = 0f
    private var lastTargetPitch = 0f
    private var hasAbsoluteTarget = false
    private val alpha = 0.95f // Complementary filter coefficient
    private var currentGyroRollRate = 0f
    private var currentGyroYawRate = 0f
    val vibrationFiltering = MutableStateFlow("Standard")
    val forceFilterStandstill = MutableStateFlow(false)

    // Sensitivity Thresholds
    private val WHEELIE_THRESHOLD = 15.0f // Degrees
    private val ENDO_THRESHOLD = -10.0f  // Degrees

    // Corner Detection Engine State
    private var currentState = RideState.STRAIGHT
    private var activeCorner: CornerEvent? = null
    private var lastLocation: Location? = null
    private var lastLoggedLocation: Location? = null

    // Per-corner PB system
    val livePbComparison = MutableStateFlow<PbComparison?>(null)
    private lateinit var cornerMatchingEngine: CornerMatchingEngine
    private lateinit var voiceCoach: VoiceCoach
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // USB GPS configurations
    private var usbPort: UsbSerialPort? = null
    private var usbJob: Job? = null
    val isUsbGpsConnected = MutableStateFlow(false)

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
        for (i in 0..8) {
            rDeviceToBike[i] = prefs.getFloat("rDeviceToBike_$i", if (i % 4 == 0) 1f else 0f)
        }
        isCalibrated.value = false
        // Give the filter time to settle before the watch engine begins checking deltas.
        // Without this, watchPrevLean/Pitch start at 0f and the first real reading fires a false alert.
        if (isCalibrated.value) calibrationWarmupUntil = System.currentTimeMillis() + 7000L
        
        // Load default vibration filter preset
        vibrationFiltering.value = prefs.getString("vibration_filtering", "Standard") ?: "Standard"
        forceFilterStandstill.value = prefs.getBoolean("force_filter_standstill", false)
        rollingDistanceTarget.value = prefs.getInt("rolling_distance_target", 1000)
        
        val dao = AppDatabase.getInstance(this).cornerDao()
        cornerMatchingEngine = CornerMatchingEngine(dao)
        voiceCoach = VoiceCoach(this)

        loadSessions()
        startCalibrationWatchEngine()
        startForegroundNotification()
        startDataStreams()
    }

    fun setVibrationFiltering(level: String) {
        vibrationFiltering.value = level
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit().putString("vibration_filtering", level).apply()
    }

    fun setForceFilterStandstill(enabled: Boolean) {
        forceFilterStandstill.value = enabled
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit().putBoolean("force_filter_standstill", enabled).apply()
    }

    fun setRollingDistanceTarget(targetMeters: Int) {
        rollingDistanceTarget.value = targetMeters
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit().putInt("rolling_distance_target", targetMeters).apply()
    }

    private fun startDataStreams() {
        // Start Rotation Vector Loop with SENSOR_DELAY_GAME
        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        rotationVectorSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        // Start Gyroscope Loop with SENSOR_DELAY_GAME
        val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        gyroSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
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
        livePbComparison.value = null
        resetSessionStats()
        currentSessionStartTime = System.currentTimeMillis()
        voiceCoach.trigger(VoiceCoach.CoachEvent.SESSION_START)
    }

    fun pauseRecording() {
        if (!isRecording.value || isPaused.value) return
        isPaused.value = true
    }

    fun stopRecording() {
        if (!isRecording.value) return
        isRecording.value = false
        isPaused.value = false
        voiceCoach.trigger(VoiceCoach.CoachEvent.SESSION_END)
        
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
        isCalibrating = true
    }

    fun dismissCalibrationAlert() {
        calibrationAlert.value = null
        calibrationToneGen?.stopTone()
        calibrationToneGen?.release()
        calibrationToneGen = null
    }

    private fun triggerCalibrationAlertSound() {
        try {
            calibrationToneGen?.release()
            calibrationToneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            calibrationToneGen?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 10000)
        } catch (e: Exception) {
            Log.e("CalibWatch", "Tone failed: ${e.message}")
        }
    }

    private fun startCalibrationWatchEngine() {
        serviceScope.launch {
            // New monitoring loop: Check calibration status every 5 seconds if recording
            launch {
                while (isActive) {
                    if (isRecording.value && !isCalibrated.value) {
                        stopRecording()
                        Log.d("TelemetryService", "Calibration lost during recording, stopping session.")
                    }
                    delay(5000L)
                }
            }

            var watchPrevLean = 0f
            var watchPrevPitch = 0f
            var sustainedHighLeanCount = 0
            delay(3000L) // let sensors settle on startup
            while (true) {
                delay(120L) // ~8 Hz
                val lean = currentLean.value
                val pitch = currentPitch.value

                if (!isCalibrated.value || calibrationAlert.value != null) {
                    watchPrevLean = lean; watchPrevPitch = pitch
                    sustainedHighLeanCount = 0
                    continue
                }
                if (System.currentTimeMillis() < calibrationWarmupUntil) {
                    watchPrevLean = lean; watchPrevPitch = pitch
                    sustainedHighLeanCount = 0
                    continue
                }

                val leanDelta = absF(lean - watchPrevLean)
                val pitchDelta = absF(pitch - watchPrevPitch)

                // Count consecutive samples where lean stays above 55° (impossible sustained on a real bike)
                if (absF(lean) > 55f) sustainedHighLeanCount++ else sustainedHighLeanCount = 0

                val reason = when {
                    absF(lean) > 70f ->
                        "Extreme lean angle detected: ${lean.toInt()}°\nPhone may have slipped off the mount."
                    absF(pitch) > 75f ->
                        "Extreme pitch angle detected: ${pitch.toInt()}°\nPhone may have rotated on the mount."
                    leanDelta > 35f ->
                        "Sudden lean shift of ${leanDelta.toInt()}° detected.\nPhone may have moved on the mount."
                    pitchDelta > 35f ->
                        "Sudden pitch shift of ${pitchDelta.toInt()}° detected.\nPhone may have rotated on the mount."
                    sustainedHighLeanCount > 41 ->
                        "Lean held at ${lean.toInt()}° for 5+ seconds.\nPhone may have shifted on the mount."
                    else -> null
                }

                if (reason != null) {
                    isCalibrated.value = false
                    androidx.preference.PreferenceManager
                        .getDefaultSharedPreferences(this@TelemetryService)
                        .edit().putBoolean("isCalibrated", false).apply()
                    calibrationAlert.value = reason
                    triggerCalibrationAlertSound()
                    sustainedHighLeanCount = 0
                }

                watchPrevLean = lean
                watchPrevPitch = pitch
            }
        }
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

    fun resetMaxLean1000m() {
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



    private fun multiply3x3(a: FloatArray, b: FloatArray): FloatArray {
        val result = FloatArray(9)
        for (i in 0..2) {
            for (j in 0..2) {
                result[i * 3 + j] = a[i * 3 + 0] * b[0 * 3 + j] +
                                    a[i * 3 + 1] * b[1 * 3 + j] +
                                    a[i * 3 + 2] * b[2 * 3 + j]
            }
        }
        return result
    }

    private fun transpose3x3(a: FloatArray): FloatArray {
        val result = FloatArray(9)
        for (i in 0..2) {
            for (j in 0..2) {
                result[j * 3 + i] = a[i * 3 + j]
            }
        }
        return result
    }

    private fun multiplyMatrixVector(matrix: FloatArray, vector: FloatArray): FloatArray {
        return floatArrayOf(
            matrix[0] * vector[0] + matrix[1] * vector[1] + matrix[2] * vector[2],
            matrix[3] * vector[0] + matrix[4] * vector[1] + matrix[5] * vector[2],
            matrix[6] * vector[0] + matrix[7] * vector[1] + matrix[8] * vector[2]
        )
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

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

        val speedKmh = currentSpeed.value
        val gpsSpeedMPS = (speedKmh / 3.6).toFloat()

        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            if (gpsSpeedMPS <= 0.5f && !forceFilterStandstill.value) {
                return
            }

            val timestamp = event.timestamp
            if (lastTimestamp == 0L) {
                lastTimestamp = timestamp
                return
            }
            val dt = (timestamp - lastTimestamp) / 1000000000f
            lastTimestamp = timestamp

            if (dt <= 0f || dt > 0.1f) return

            // Map gyroscope from physical device frame directly to bike frame
            val omegaBike = multiplyMatrixVector(rDeviceToBike, event.values)
            // omegaBike[0] is pitch rate (Bike X-axis)
            // omegaBike[1] is roll rate (Bike Y-axis)
            // omegaBike[2] is yaw rate (Bike Z-axis)
            
            // Standard right hand rule:
            // Thumb Right (Bike X) -> Pitch Up is positive.
            // Thumb Forward (Bike Y) -> Roll Right is positive.
            val gyroPitchRateDeg = Math.toDegrees(omegaBike[0].toDouble()).toFloat()
            val gyroRollRateDeg = Math.toDegrees(omegaBike[1].toDouble()).toFloat()

            // 1. High frequency gyroscope integration
            val integratedLean = leanAngle + (gyroRollRateDeg * dt)
            val integratedPitch = pitchAngle + (gyroPitchRateDeg * dt)

            // 2. Langsame GPS-Referenz (GPS speed and Yaw Rate centripetal calculation)
            val gravity = 9.81f
            // Left turn = positive omegaBike[2], causes negative gpsLeanRad (Left Lean)
            val gpsLeanRad = kotlin.math.atan((gpsSpeedMPS * -omegaBike[2]) / gravity)
            val gpsLeanDeg = Math.toDegrees(gpsLeanRad.toDouble()).toFloat()

            // 3. Fusion (Complementary Filter)
            if (hasAbsoluteTarget) {
                val level = vibrationFiltering.value
                val alphaFast: Float
                val alphaSlow: Float
                when (level) {
                    "Very Low" -> {
                        // Very low filtering, prioritizes hardware reference more heavily. Fast drift correction.
                        alphaFast = 0.992f
                        alphaSlow = 0.985f
                    }
                    "Low" -> {
                        alphaFast = 0.995f
                        alphaSlow = 0.990f
                    }
                    "High" -> {
                        alphaFast = 0.999f
                        alphaSlow = 0.998f
                    }
                    "Very High" -> {
                        // Very high filtering, strongly relies on gyroscope integration. High immunity to vibration, slow drift correction.
                        alphaFast = 0.9998f
                        alphaSlow = 0.999f
                    }
                    else -> { // Standard
                        alphaFast = 0.998f
                        alphaSlow = 0.995f
                    }
                }

                val isMovingFast = kotlin.math.abs(gyroRollRateDeg) > 5.0f
                val dynamicAlpha = if (isMovingFast) alphaFast else alphaSlow
                
                // Blend gravity and centripetal lean to prevent linear acceleration from causing drift via yaw bias
                val yawRateAbs = kotlin.math.abs(omegaBike[2])
                val turnFactor = ((yawRateAbs - 0.02f) * 16.66f).coerceIn(0f, 1f)
                val referenceLean = (turnFactor * gpsLeanDeg) + ((1f - turnFactor) * lastTargetLean)
                
                leanAngle = (dynamicAlpha * integratedLean) + ((1f - dynamicAlpha) * referenceLean)
                pitchAngle = (dynamicAlpha * integratedPitch) + ((1f - dynamicAlpha) * lastTargetPitch)
            } else {
                leanAngle = integratedLean
                pitchAngle = integratedPitch
            }

            // 4. Begrenzung auf physikalische Maximalwerte
            val finalLean = leanAngle.coerceIn(-65f, 65f)
            val finalPitch = pitchAngle.coerceIn(-20f, 90f)

            // 5. Update live dashboard flows instantly
            currentLean.value = finalLean
            currentPitch.value = finalPitch
            isWheelieAlert.value = finalPitch > WHEELIE_THRESHOLD

            // 6. Update recording metrics and stats
            if (isRecording.value && !isPaused.value) {
                if (finalLean < allTimeMaxLeft.value) allTimeMaxLeft.value = finalLean
                if (finalLean > allTimeMaxRight.value) allTimeMaxRight.value = finalLean
                
                if (finalPitch >= 10.0f && finalPitch > allTimeMaxPitch.value) {
                    allTimeMaxPitch.value = finalPitch
                }

                if (finalLean < sessionMaxLeft.value) sessionMaxLeft.value = finalLean
                if (finalLean > sessionMaxRight.value) sessionMaxRight.value = finalLean
                if (finalPitch > sessionMaxPitch.value) {
                    sessionMaxPitch.value = finalPitch
                }
                
                if (finalLean < rollingMaxLeft.value) {
                    rollingMaxLeft.value = finalLean
                }
                if (finalLean > rollingMaxRight.value) {
                    rollingMaxRight.value = finalLean
                }

                processTelemetrySnapshop(finalLean, finalPitch)
            }
            return
        }

        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        val rDeviceToWorld = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rDeviceToWorld, event.values)

        if (isCalibrating) {
            // Find Display Y vector in Device Frame
            val vDispY = when (displayRotation) {
                Surface.ROTATION_90 -> floatArrayOf(1f, 0f, 0f)
                Surface.ROTATION_180 -> floatArrayOf(0f, -1f, 0f)
                Surface.ROTATION_270 -> floatArrayOf(-1f, 0f, 0f)
                else -> floatArrayOf(0f, 1f, 0f)
            }
            // Find Device -Z vector (back of phone)
            val vMinusZ = floatArrayOf(0f, 0f, -1f)
            
            // Map both to World Frame
            val wDispY = multiplyMatrixVector(rDeviceToWorld, vDispY)
            val wMinusZ = multiplyMatrixVector(rDeviceToWorld, vMinusZ)
            
            // Project combined "Forward" intent to the horizontal plane (Z=0)
            val fx = wDispY[0] + wMinusZ[0]
            val fy = wDispY[1] + wMinusZ[1]
            
            val len = kotlin.math.sqrt((fx * fx + fy * fy).toDouble()).toFloat()
            val byW_X = if (len > 0.001f) fx / len else 0f
            val byW_Y = if (len > 0.001f) fy / len else 1f
            
            // Construct R_bike_to_world matrix
            // Bike X (Right) = (byW_Y, -byW_X, 0)
            // Bike Y (Forward) = (byW_X, byW_Y, 0)
            // Bike Z (Up) = (0, 0, 1)
            val rBikeToWorld = floatArrayOf(
                byW_Y,  byW_X, 0f,
               -byW_X,  byW_Y, 0f,
                0f,     0f,    1f
            )
            
            // R_device_to_bike = R_bike_to_world^T * R_device_to_world
            val rWorldToBike = transpose3x3(rBikeToWorld)
            rDeviceToBike = multiply3x3(rWorldToBike, rDeviceToWorld)
            isCalibrating = false

            val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
            val editor = prefs.edit()
            for (i in 0..8) {
                editor.putFloat("rDeviceToBike_$i", rDeviceToBike[i])
            }
            editor.putBoolean("isCalibrated", true)
            editor.putLong("calibration_timestamp", System.currentTimeMillis())
            editor.apply()
            isCalibrated.value = true
            calibrationWarmupUntil = System.currentTimeMillis() + 2500L
        }

        // R_bike_to_world = R_device_to_world * R_device_to_bike^T
        val rBikeToWorld = multiply3x3(rDeviceToWorld, transpose3x3(rDeviceToBike))

        // Extract pitch and roll from the R_bike_to_world matrix
        // Row 3 (indices 6, 7, 8) represents the Up vector in Bike frame
        val upX = rBikeToWorld[6]
        val upY = rBikeToWorld[7]
        val upZ = rBikeToWorld[8]

        // Roll (Lean): positive when leaning right -> Up vector moves to left (-X in bike frame)
        // atan2(-upX, upZ) is mathematically positive for Right Lean
        val rawLean = Math.toDegrees(Math.atan2(-upX.toDouble(), upZ.toDouble())).toFloat()
        
        // Pitch: positive when pitching up (wheelie) -> Up vector moves backward (-Y in bike frame)
        // asin(-upY) is mathematically positive for Wheelie
        val rawPitch = Math.toDegrees(kotlin.math.asin(-upY.toDouble().coerceIn(-1.0, 1.0))).toFloat()

        // Compass Heading
        val orientationAngles = FloatArray(3)
        SensorManager.getOrientation(rBikeToWorld, orientationAngles)
        val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()

        if (speedKmh <= 2.0 || lastLocation?.hasBearing() == false) {
            currentHeading.value = (azimuth + 360) % 360
        }

        val correctedLean = rawLean
        val correctedPitch = rawPitch

        // Store the target values from rotation vector
        lastTargetLean = correctedLean
        lastTargetPitch = correctedPitch
        hasAbsoluteTarget = true

        // Im Stand oder bei Hand-Tests: Werte direkt und OHNE Verzögerung aktualisieren! (außer erzwungen im Dev-Mode)
        if (gpsSpeedMPS <= 0.5f && !forceFilterStandstill.value) {
            val finalLean = correctedLean.coerceIn(-65f, 65f)
            val finalPitch = correctedPitch.coerceIn(-20f, 90f)

            leanAngle = finalLean
            pitchAngle = finalPitch

            // Update live dashboard flows instantly
            currentLean.value = finalLean
            currentPitch.value = finalPitch
            isWheelieAlert.value = finalPitch > WHEELIE_THRESHOLD

            // Update recording metrics and stats if active
            if (isRecording.value && !isPaused.value) {
                if (finalLean < allTimeMaxLeft.value) allTimeMaxLeft.value = finalLean
                if (finalLean > allTimeMaxRight.value) allTimeMaxRight.value = finalLean
                
                if (finalPitch >= 10.0f && finalPitch > allTimeMaxPitch.value) {
                    allTimeMaxPitch.value = finalPitch
                }

                if (finalLean < sessionMaxLeft.value) sessionMaxLeft.value = finalLean
                if (finalLean > sessionMaxRight.value) sessionMaxRight.value = finalLean
                if (finalPitch > sessionMaxPitch.value) {
                    sessionMaxPitch.value = finalPitch
                }
                
                if (finalLean < rollingMaxLeft.value) {
                    rollingMaxLeft.value = finalLean
                }
                if (finalLean > rollingMaxRight.value) {
                    rollingMaxRight.value = finalLean
                }

                processTelemetrySnapshop(finalLean, finalPitch)
            }
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

        // Calculate rolling max lean in the target distance
        var accumulatedDistance = 0.0
        var maxLeanVal = 0f
        val targetDist = rollingDistanceTarget.value.toDouble()
        for (i in updatedList.indices.reversed()) {
            val pt = updatedList[i]
            accumulatedDistance += pt.distanceDelta
            val currentAbsLean = kotlin.math.abs(pt.leanAngle)
            if (currentAbsLean > maxLeanVal) {
                maxLeanVal = currentAbsLean
            }
            if (accumulatedDistance >= targetDist) {
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
                        detectedCorners.value = detectedCorners.value.toList()

                        if (isRecording.value && !isPaused.value) {
                            val closedCorner = corner
                            val pointsSnapshot = updatedList.toList()
                            val sessionId = "Session_${currentSessionStartTime}"
                            serviceScope.launch {
                                val result = cornerMatchingEngine.matchOrCreateCorner(
                                    closedCorner, pointsSnapshot, sessionId
                                ) ?: return@launch

                                AppDatabase.getInstance(this@TelemetryService).cornerDao()
                                    .insertSessionCorner(SessionCornerEntity(
                                        sessionId = sessionId,
                                        cornerId = result.corner.id,
                                        leanAchieved = result.leanAchieved,
                                        speedAtPeak = result.speedAtPeak,
                                        cornerTimestamp = System.currentTimeMillis()
                                    ))

                                val comparison = PbComparison(
                                    cornerId = result.corner.id,
                                    pbLean = if (result.isNewPb) result.leanAchieved
                                             else result.existingPb?.bestLean ?: result.leanAchieved,
                                    achievedLean = result.leanAchieved,
                                    isNewPb = result.isNewPb
                                )
                                livePbComparison.value = comparison

                                if (result.isNewPb) {
                                    voiceCoach.trigger(VoiceCoach.CoachEvent.NEW_PB, result.leanAchieved)
                                } else {
                                    val pb = result.existingPb
                                    if (pb != null && result.leanAchieved >= pb.bestLean * 0.93f) {
                                        voiceCoach.trigger(VoiceCoach.CoachEvent.NEAR_PB,
                                            pb.bestLean - result.leanAchieved)
                                    }
                                }
                            }
                        }
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
            isUsbGpsConnected.value = true

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

    fun exportAllSessionsToCsv(): File? {
        val sessions = pastSessions.value
        if (sessions.isEmpty()) return null

        val dir = getExternalFilesDir(null)
        val file = File(dir, "AllRides_${System.currentTimeMillis()}.csv")
        FileWriter(file).use { writer ->
            writer.write("SessionID,Timestamp,Latitude,Longitude,SpeedKmh,LeanAngle,PitchAngle\n")
            sessions.forEach { session ->
                session.points.forEach { pt ->
                    writer.write("${session.id},${pt.timestamp},${pt.latitude},${pt.longitude},${pt.speedKmh},${pt.leanAngle},${pt.pitchAngle}\n")
                }
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
            .setContentTitle("RideTracker")
            .setContentText("Sensor telemetry active")
            .setSmallIcon(R.drawable.ic_launcher_fg_white)
            .build()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(101, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(101, notification)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    fun getVoiceCoach(): VoiceCoach = voiceCoach

    override fun onDestroy() {
        usbJob?.cancel()
        serviceScope.cancel()
        voiceCoach.shutdown()
        calibrationToneGen?.release()
        calibrationToneGen = null
        try { usbPort?.close() } catch (e: Exception) {}
        sensorManager.unregisterListener(this)
        try { locationManager.removeUpdates(this) } catch (e: Exception) {}
        super.onDestroy()
    }
}