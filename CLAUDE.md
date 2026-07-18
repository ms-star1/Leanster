# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install debug build on connected device
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run a single unit test class
./gradlew testDebugUnitTest --tests "com.bike.leanster.ExampleUnitTest"

# Clean build
./gradlew clean
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Architecture

**Tech stack**: Kotlin + Jetpack Compose + Foreground Service. No DI framework; no Navigation component; no Room database.

### Core Components

| File | Role |
|------|------|
| `MainActivity.kt` | Single activity; hosts Compose UI; binds to TelemetryService via ServiceConnection; owns tab navigation (0=Dashboard, 1=Settings, 2=Summary, 3=History) |
| `TelemetryService.kt` | Foreground service; owns all sensor/GPS/USB-serial input; exposes all live state as `MutableStateFlow`s that the UI collects |
| `DashboardView.kt` | Main riding UI; a single composable that branches into `DashboardPortraitLayout` / `DashboardLandscapeLayout` — both live in this one file |
| `SessionSummaryView.kt` | Post-ride stats and corner breakdown |
| `MapView.kt` | MapLibre-based route replay |
| `TelemetryData.kt` | Plain data classes: `TelemetryPoint`, `CornerEvent`, `RideSession`, `RideStats` |

### State Flow

All live telemetry (lean, pitch, speed, GPS, recording state, session data) lives as `MutableStateFlow` fields on `TelemetryService`. `MainActivity` exposes the bound service reference as a `State<TelemetryService?>` so all composables can `collectAsState()` directly from the service.

### Persistence

- **SharedPreferences** — all-time max lean/pitch, sensor calibration matrix (9 floats), vibration filtering level, rolling distance target, default launch orientation.
- **JSON files** — completed ride sessions written to `getExternalFilesDir("sessions")`; loaded back by the service on startup. Format: `RideSession(id, startTime, endTime, points[], corners[], stats)`.
- **CSV export** — via `Intent.ACTION_SEND` through a `FileProvider`; headers: Timestamp, Latitude, Longitude, SpeedKmh, LeanAngle, PitchAngle.

### Sensor Fusion

`TelemetryService` fuses three sources for lean/pitch:
1. `TYPE_ROTATION_VECTOR` — absolute orientation reference
2. `TYPE_GYROSCOPE` — high-frequency complementary filter integration
3. GPS / USB-serial NMEA (Adafruit module at 9600 baud, 10 Hz) — speed and location

A configurable **complementary filter alpha** (tuned via vibration-filtering setting) blends gyro integration with the rotation vector reference. A user-recorded **3×3 calibration matrix** transforms device frame → bike frame.

### Corner Detection

Simple two-state machine (`STRAIGHT` ↔ `IN_CORNER`) triggered at ±10° lean entry / ±7° exit. Accumulated as `CornerEvent` objects within the active session.

## Key Dependencies

Managed via `gradle/libs.versions.toml`:

- **Compose BOM** `2026.06.00` — UI, Material3, Icons Extended
- **MapLibre** `13.3.0` — map rendering
- **usb-serial-for-android** `3.10.0` (JitPack) — USB GPS serial communication
- **Gson** `2.14.0` — session JSON serialization
- **Lifecycle Service** `2.11.0` — `LifecycleService` base class for `TelemetryService`

## SDK / Toolchain

- compileSdk / targetSdk: **37**
- minSdk: **26** (Android 8.0)
- Kotlin: **2.4.0**, Java compatibility: **11**
- AGP: **9.1.1**
- ProGuard/R8 minification: **disabled** for all build types

## Design Conventions

**Colors** ("Carbon & Neon" theme): background `0xFF0B0E14`, surface cards `0xFF141A24`, primary accent NeonCyan `0xFF00F5D4`. Brand accent colors for Yamaha/Ducati/Kawasaki are also defined in `Color.kt`.

**Typography**: numeric data uses Rajdhani (falling back to Michroma); UI labels use Inter SansSerif. Custom `sp` scale defined in `Type.kt`.

**Layout**: `DashboardView.kt` handles both orientations in a single file — avoid splitting it. All layout branching is done at the top of the composable with a `LocalConfiguration.current.orientation` check.
