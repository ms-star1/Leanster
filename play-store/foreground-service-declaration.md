# Foreground Service Declaration — Google Play Console

App: **Leanster** (`com.leanster.app`)
Location in console: *App content → Foreground service permissions*

> Google Play requires you to declare each foreground-service type your app uses, give a justification, and (for most types) attach a short screen recording showing the feature. Leanster declares two FGS types.

---

## Declared foreground service types

The manifest declares:

```xml
<service
    android:name=".TelemetryService"
    android:foregroundServiceType="location|dataSync" ... />
```

…and requests `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, `FOREGROUND_SERVICE_DATA_SYNC`.

### 1. Location (`FOREGROUND_SERVICE_LOCATION`)

**Justification (paste into console):**
> Leanster is a motorcycle ride-telemetry app. While the user is recording a ride, the foreground service continuously reads GPS location to log the route, speed, distance and corner geometry. Recording must continue when the screen is off or the app is backgrounded (the phone is mounted on the motorcycle), so a location foreground service is required. The persistent notification informs the user that recording is active. Location is used only while the user has explicitly started a recording.

**User benefit:** Accurate, uninterrupted ride tracking while the device is mounted and the screen is off.

### 2. Data sync (`FOREGROUND_SERVICE_DATA_SYNC`)

**Justification (paste into console):**
> During an active recording the service continuously fuses and writes telemetry — sensor-derived lean/pitch and, when connected, NMEA data from an external USB GPS module — into the ride session on device. This ongoing data processing/persistence runs for the duration of the ride and is surfaced by the recording notification. It is user-initiated (starts only when the user begins recording) and stops when the ride ends.

**User benefit:** No data loss — the full ride is captured and saved even during long sessions with the screen off.

> Note: `dataSync` is being deprecated/tightened on newer Android versions. If Play pushes back on the `dataSync` justification, the cleanest long-term fix is to fold this work under the `location` service type (the data processing happens alongside location logging) and drop `dataSync`. Flag this to the developer before changing manifest types.

---

## Demo video checklist (required for the location type)

Record a short (~30–60 s) screen capture showing:
1. Launch Leanster and start a **recording**.
2. The **ongoing notification** ("Leanster — Sensor telemetry active") appearing.
3. **Leave the app** (press Home) and/or turn the screen off, then return — show recording is still running and data still accumulating.
4. **Stop** the recording and show the saved session.

Upload the video (unlisted YouTube link or file) where the console requests it.

## Prominent in-app disclosure

Ensure the app tells the user, before or at first recording, that it collects location in the foreground service — your onboarding/disclaimer screen already covers usage; confirm it mentions background/foreground location collection while recording.
