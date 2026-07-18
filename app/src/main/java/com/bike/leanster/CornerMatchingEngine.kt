package com.bike.leanster

import com.bike.leanster.database.CornerDao
import com.bike.leanster.database.entities.CornerEntity
import com.bike.leanster.database.entities.CornerPbEntity
import kotlin.math.*

data class CornerMatchResult(
    val corner: CornerEntity,
    val existingPb: CornerPbEntity?,
    val isNewPb: Boolean,
    val leanAchieved: Float,
    val speedAtPeak: Double
)

class CornerMatchingEngine(private val dao: CornerDao) {

    // Fallback coordinates used when GPS is unavailable — skip these points
    private val FALLBACK_LAT = 52.52
    private val HEADING_TOLERANCE_DEG = 35f
    private val EARTH_RADIUS_M = 6371000.0

    suspend fun matchOrCreateCorner(
        corner: CornerEvent,
        points: List<TelemetryPoint>,
        sessionId: String
    ): CornerMatchResult? {
        if (corner.endIndex < 0 || corner.startIndex >= points.size) return null

        val end = minOf(corner.endIndex, points.lastIndex)
        val cornerPoints = points.subList(corner.startIndex, end + 1)
            .filter { it.latitude != FALLBACK_LAT && it.latitude != 0.0 }

        if (cornerPoints.size < 3) return null

        val centroidLat = cornerPoints.map { it.latitude }.average()
        val centroidLng = cornerPoints.map { it.longitude }.average()
        val entryHeading = computeEntryHeading(cornerPoints)

        val maxLean = max(abs(corner.maxLeftLean), corner.maxRightLean)
        val searchRadius = when {
            maxLean > 40f -> 15.0
            maxLean > 20f -> 25.0
            else -> 35.0
        }

        val latDelta = searchRadius / 111320.0
        val lngDelta = searchRadius / (111320.0 * cos(Math.toRadians(centroidLat)))

        val candidates = dao.getCornersInBounds(
            minLat = centroidLat - latDelta, maxLat = centroidLat + latDelta,
            minLng = centroidLng - lngDelta, maxLng = centroidLng + lngDelta
        )

        val matched = candidates
            .filter { headingDiff(it.entryHeading, entryHeading) < HEADING_TOLERANCE_DEG }
            .minByOrNull { haversine(it.centroidLat, it.centroidLng, centroidLat, centroidLng) }
            ?.takeIf { haversine(it.centroidLat, it.centroidLng, centroidLat, centroidLng) < searchRadius }

        val direction = if (corner.maxLeftLean < 0 && abs(corner.maxLeftLean) > corner.maxRightLean) "LEFT" else "RIGHT"
        val category = when {
            maxLean > 40f -> "AGGRESSIVE"
            maxLean > 20f -> "MEDIUM"
            else -> "GENTLE"
        }

        val cornerEntity = if (matched != null) {
            dao.incrementRideCount(matched.id)
            matched
        } else {
            val newCorner = CornerEntity(
                centroidLat = centroidLat,
                centroidLng = centroidLng,
                entryHeading = entryHeading,
                direction = direction,
                leanCategory = category,
                firstSeenAt = System.currentTimeMillis()
            )
            val id = dao.insertCorner(newCorner)
            newCorner.copy(id = id)
        }

        val peakIndex = corner.maxLeanIndex.coerceIn(corner.startIndex, end)
        val speedAtPeak = if (peakIndex < points.size) points[peakIndex].speedKmh else 0.0
        val existingPb = dao.getPbForCorner(cornerEntity.id)
        val isNewPb = existingPb == null || maxLean > existingPb.bestLean

        if (isNewPb) {
            if (existingPb == null) {
                dao.insertPb(CornerPbEntity(
                    cornerId = cornerEntity.id,
                    bestLean = maxLean,
                    bestSpeed = speedAtPeak,
                    bestSessionId = sessionId,
                    achievedAt = System.currentTimeMillis()
                ))
            } else {
                dao.updatePb(cornerEntity.id, maxLean, speedAtPeak, sessionId, System.currentTimeMillis())
            }
        }

        return CornerMatchResult(
            corner = cornerEntity,
            existingPb = existingPb,
            isNewPb = isNewPb,
            leanAchieved = maxLean,
            speedAtPeak = speedAtPeak
        )
    }

    private fun computeEntryHeading(points: List<TelemetryPoint>): Float {
        val ref = points[0]
        val target = points[minOf(4, points.lastIndex)]
        val dLng = target.longitude - ref.longitude
        val dLat = target.latitude - ref.latitude
        val bearing = Math.toDegrees(atan2(dLng, dLat)).toFloat()
        return ((bearing % 360) + 360) % 360
    }

    private fun headingDiff(a: Float, b: Float): Float {
        val diff = abs(a - b) % 360f
        return if (diff > 180f) 360f - diff else diff
    }

    private fun haversine(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return EARTH_RADIUS_M * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
