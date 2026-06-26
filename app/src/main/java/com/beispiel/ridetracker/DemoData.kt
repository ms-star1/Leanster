package com.beispiel.ridetracker

import kotlin.math.*

const val DEMO_SESSION_ID = "DEMO_KESSELBERG_V1"

fun createDemoSession(): RideSession {
    // ~3 days ago so it appears in report card history
    val baseTime = System.currentTimeMillis() - 3L * 24 * 3600 * 1000

    // Each waypoint: lat, lng, targetSpeedKmh, peakLeanDeg (+ = right, - = left), durationMs
    data class WP(val lat: Double, val lng: Double, val spd: Double, val lean: Float, val ms: Long)

    // Kesselberg Pass loop (Kochel am See → Walchensee, Bavaria) — outbound + return on parallel road
    val wps = listOf(
        // ── Outbound: climbing northward ──
        WP(47.6022, 11.3487, 82.0,   0f,  7000),  // bottom straight
        WP(47.6068, 11.3508, 56.0,  27f,  5000),  // R moderate
        WP(47.6098, 11.3494, 72.0,   0f,  4000),  // short link
        WP(47.6132, 11.3520, 46.0, -38f,  6500),  // L hairpin
        WP(47.6170, 11.3499, 88.0,   0f,  5500),  // fast uphill
        WP(47.6208, 11.3526, 58.0,  23f,  5000),  // R gentle
        WP(47.6248, 11.3504, 50.0, -33f,  6000),  // L tight
        WP(47.6295, 11.3489, 96.0,   0f,  6000),  // fast section
        WP(47.6330, 11.3516, 42.0, -43f,  7500),  // L famous hairpin
        WP(47.6360, 11.3495, 54.0,  30f,  5500),  // R after hairpin
        WP(47.6395, 11.3524, 90.0,   0f,  6000),  // top plateau
        WP(47.6442, 11.3500, 64.0,  25f,  5000),  // R viewpoint curve
        WP(47.6475, 11.3528, 85.0,   0f,  5500),  // straight near lake
        WP(47.6508, 11.3504, 52.0, -28f,  5500),  // L sweeper

        // ── Return: descending on parallel road ──
        WP(47.6478, 11.3465, 98.0,   0f,  6000),  // fast descent
        WP(47.6445, 11.3442, 60.0,  34f,  5500),  // R descent corner
        WP(47.6408, 11.3464, 46.0, -37f,  6000),  // L tight descent
        WP(47.6368, 11.3445, 110.0,  0f,  6500),  // very fast downhill
        WP(47.6328, 11.3466, 54.0,  29f,  5000),  // R
        WP(47.6288, 11.3444, 50.0, -31f,  5500),  // L
        WP(47.6248, 11.3464, 92.0,   0f,  5500),  // straight
        WP(47.6208, 11.3444, 60.0,  21f,  5000),  // R gentle
        WP(47.6172, 11.3462, 76.0,   0f,  4500),  // link
        WP(47.6138, 11.3444, 50.0, -27f,  5000),  // L
        WP(47.6105, 11.3462, 80.0,   0f,  4000),  // straight
        WP(47.6065, 11.3444, 64.0,  24f,  4500),  // R final
        WP(47.6035, 11.3462, 78.0,   0f,  5000),  // approach
        WP(47.6022, 11.3487, 55.0,   0f,  3000),  // finish
    )

    val points = mutableListOf<TelemetryPoint>()
    var t = baseTime

    for (i in 0 until wps.lastIndex) {
        val from = wps[i]
        val to = wps[i + 1]
        val stepMs = 400L
        val steps = (to.ms / stepMs).toInt().coerceAtLeast(1)

        repeat(steps) { step ->
            val frac = step.toFloat() / steps

            val lat = from.lat + (to.lat - from.lat) * frac
            val lng = from.lng + (to.lng - from.lng) * frac

            // Lean: smooth bell curve within corner, ramp to straight
            val lean = when {
                abs(to.lean) > 8f && abs(from.lean) < 8f ->
                    to.lean * sin(frac * PI).toFloat()            // entry into corner
                abs(from.lean) > 8f && abs(to.lean) < 8f ->
                    from.lean * (1f - sin(frac * PI / 2).toFloat())  // exit corner
                abs(to.lean) > 8f ->
                    from.lean + (to.lean - from.lean) * frac      // corner-to-corner
                else -> from.lean * (1f - frac)                   // straight
            }

            // Speed: decelerate entering corners, accelerate exiting
            val speed = if (abs(to.lean) > 10f)
                from.spd + (to.spd - from.spd) * frac.toDouble().pow(2.0)
            else
                from.spd + (to.spd - from.spd) * sqrt(frac.toDouble())

            val prevPt = points.lastOrNull()
            val dlat = (lat - (prevPt?.latitude ?: lat)) * 111320.0
            val dlng = (lng - (prevPt?.longitude ?: lng)) * 111320.0 * cos(Math.toRadians(lat))
            val distDelta = sqrt(dlat * dlat + dlng * dlng)

            points.add(TelemetryPoint(
                timestamp = t,
                latitude = lat,
                longitude = lng,
                speedKmh = speed.coerceIn(30.0, 130.0),
                leanAngle = lean.coerceIn(-50f, 50f),
                pitchAngle = if (speed > 85 && abs(lean) < 6f) 1.5f else 0f,
                distanceDelta = distDelta
            ))
            t += stepMs
        }
    }

    // Derive corners using the same threshold logic as TelemetryService
    val corners = mutableListOf<CornerEvent>()
    var cId = 0
    var inCorner = false
    var cStart = 0
    var maxAbs = 0f
    var maxIdx = 0

    for (i in points.indices) {
        val absLean = abs(points[i].leanAngle)
        if (!inCorner && absLean > 10f) {
            inCorner = true; cStart = i; maxAbs = absLean; maxIdx = i
        } else if (inCorner) {
            if (absLean > maxAbs) { maxAbs = absLean; maxIdx = i }
            if (absLean < 7f || i == points.lastIndex) {
                val slice = points.subList(cStart, i)
                val isLeft = points[maxIdx].leanAngle < 0
                corners.add(CornerEvent(
                    id = cId++,
                    startIndex = cStart,
                    endIndex = i,
                    maxLeftLean  = if (isLeft) -maxAbs else 0f,
                    maxRightLean = if (!isLeft) maxAbs else 0f,
                    maxLeanIndex = maxIdx,
                    centroidLat = slice.map { it.latitude }.average(),
                    centroidLng = slice.map { it.longitude }.average(),
                    entryHeading = 0f
                ))
                inCorner = false; maxAbs = 0f
            }
        }
    }

    return RideSession(
        id = DEMO_SESSION_ID,
        startTime = baseTime,
        endTime = t,
        points = points,
        corners = corners,
        maxLeanLeft  = points.minOf { it.leanAngle },
        maxLeanRight = points.maxOf { it.leanAngle },
        maxPitch = 2f
    )
}
