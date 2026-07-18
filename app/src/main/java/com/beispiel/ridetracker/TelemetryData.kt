package com.beispiel.ridetracker

data class TelemetryPoint(
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Double,
    val leanAngle: Float,
    val pitchAngle: Float,
    val distanceDelta: Double
)

data class RideSession(
    val id: String,
    val startTime: Long,
    var endTime: Long = 0,
    val points: List<TelemetryPoint> = emptyList(),
    val corners: List<CornerEvent> = emptyList(),
    val maxLeanLeft: Float = 0f,
    val maxLeanRight: Float = 0f,
    val maxPitch: Float = 0f,
    val stopReason: String? = null,  // null = normal stop; non-null = error/auto-stop label
    var deletedAt: Long? = null      // null = active; non-null = epoch ms when moved to recycle bin
)

data class CornerEvent(
    val id: Int,
    val startIndex: Int,
    var endIndex: Int = -1,
    var maxLeftLean: Float = 0f,
    var maxRightLean: Float = 0f,
    var maxLeanIndex: Int = -1,
    // GPS identity fields — default 0.0 keeps backward-compat with existing JSON files
    val centroidLat: Double = 0.0,
    val centroidLng: Double = 0.0,
    val entryHeading: Float = 0f,
    val dbCornerId: Long = -1L   // -1 = not yet matched to DB
)

enum class RideState {
    STRAIGHT, IN_CORNER
}

/** Live comparison shown on dashboard after each corner. */
data class PbComparison(
    val cornerId: Long,
    val pbLean: Float,
    val achievedLean: Float,
    val isNewPb: Boolean,
    val deltaLean: Float = pbLean - achievedLean
)
