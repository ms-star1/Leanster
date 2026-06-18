package com.beispiel.ridetracker

import android.location.Location

data class TelemetryPoint(
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Double,
    val leanAngle: Float,   // Negative = Left, Positive = Right
    val pitchAngle: Float,  // Wheelie/Endo angle
    val distanceDelta: Double // Distance covered since last point in meters
)

data class RideSession(
    val id: String,
    val startTime: Long,
    var endTime: Long = 0,
    val points: List<TelemetryPoint> = emptyList(),
    val corners: List<CornerEvent> = emptyList(),
    val maxLeanLeft: Float = 0f,
    val maxLeanRight: Float = 0f,
    val maxPitch: Float = 0f
)

data class CornerEvent(
    val id: Int,
    val startIndex: Int,
    var endIndex: Int = -1,
    var maxLeftLean: Float = 0f,
    var maxRightLean: Float = 0f,
    var maxLeanIndex: Int = -1
)

enum class RideState {
    STRAIGHT, IN_CORNER
}