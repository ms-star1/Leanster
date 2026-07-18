package com.bike.leanster

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

/**
 * Interpolates GPS speed between 1 Hz fixes using TYPE_LINEAR_ACCELERATION,
 * publishing updates at ~30 Hz. Uses the calibration matrix to extract the
 * bike's longitudinal (forward) acceleration component for integration.
 *
 * GPS fixes are used as ground-truth corrections — drift cannot compound.
 * If the integrated speed diverges from GPS by more than DRIFT_THRESHOLD_KMH
 * at any fix, interpolation is disabled until the next calibration.
 */
class SpeedInterpolationEngine(
    private val sensorManager: SensorManager,
    private val getMatrix: () -> FloatArray,  // always reads current rDeviceToBike
    private val onSpeedUpdate: (Double) -> Unit  // km/h, called at ~30 Hz
) : SensorEventListener {

    private val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    val isAvailable: Boolean get() = sensor != null

    // Integrator state — @Volatile because onSensorChanged runs on a sensor thread
    @Volatile private var integratedSpeedMps = 0f
    @Volatile private var filteredAccel = 0f
    @Volatile private var lastSensorNs = 0L

    // Drift gate
    @Volatile var isDriftDisabled = false
        private set
    private val driftThresholdMps = 8f / 3.6f  // disable above 8 km/h divergence

    // 30 Hz publish throttle
    private var lastPublishNs = 0L
    private val publishIntervalNs = 1_000_000_000L / 30

    fun start() {
        sensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    /**
     * Called on every GPS fix. Snaps the integrator to GPS truth and checks for drift.
     * If sensor is unavailable or drift-disabled, passes GPS speed through directly.
     */
    fun onGpsFix(speedKmh: Double) {
        val gpsMps = (speedKmh / 3.6).toFloat()

        if (sensor == null || isDriftDisabled) {
            integratedSpeedMps = gpsMps
            onSpeedUpdate(speedKmh)
            return
        }

        val drift = abs(integratedSpeedMps - gpsMps)
        if (drift > driftThresholdMps) {
            isDriftDisabled = true
            integratedSpeedMps = gpsMps
            filteredAccel = 0f
            onSpeedUpdate(speedKmh)
            return
        }

        // Correct integrator to GPS truth and reset filter bias accumulated since last fix
        integratedSpeedMps = gpsMps
        filteredAccel = 0f
    }

    /** Re-enables interpolation after a new calibration. */
    fun onCalibrationComplete() {
        isDriftDisabled = false
        filteredAccel = 0f
        lastSensorNs = 0L
        // integratedSpeedMps will be corrected on the next GPS fix
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || isDriftDisabled) return

        val now = event.timestamp
        if (lastSensorNs == 0L) {
            lastSensorNs = now
            return
        }
        val dt = (now - lastSensorNs) / 1_000_000_000f
        lastSensorNs = now
        if (dt <= 0f || dt > 0.1f) return  // skip stale or overlarge steps

        // Rotate device-frame linear acceleration into bike frame using rDeviceToBike.
        // Bike Y-axis (row 1) is the longitudinal (forward) axis — same convention as
        // the gyroscope mapping where row 1 produces the roll/lean rate.
        val m = getMatrix()
        val r = event.values
        val rawForward = m[3] * r[0] + m[4] * r[1] + m[5] * r[2]

        // Adaptive low-pass: sigmoid biased toward smoothness.
        // At low acceleration (~cruising) alpha≈0.08 → heavy smoothing.
        // At high acceleration (~braking/hard throttle) alpha≈0.50 → responsive but still filtered.
        val mag = abs(rawForward)
        val alpha = (0.08f + 0.42f * (mag / (mag + 1.5f)))
        filteredAccel = alpha * rawForward + (1f - alpha) * filteredAccel

        // Integrate and clamp — bike can't go backwards from this engine's perspective
        integratedSpeedMps = (integratedSpeedMps + filteredAccel * dt).coerceAtLeast(0f)

        // Publish at 30 Hz
        if (now - lastPublishNs >= publishIntervalNs) {
            lastPublishNs = now
            onSpeedUpdate(integratedSpeedMps * 3.6)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
