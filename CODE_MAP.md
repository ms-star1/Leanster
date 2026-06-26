# RideTracker Code Map Index

This file serves as a quick lookup map of the RideTracker codebase structure.

## Core App Flow & Navigation
- **`MainActivity.kt`**: Entry point. Service binding (`TelemetryService`), orientation locking, location/notification permission handling, and tab navigation (`currentTab` 0: Dashboard, 1: Settings, 2: Post-Ride Save/Discard, 3: History Menu).

## Telemetry & Services
- **`TelemetryService.kt`**: Foreground Service handling sensors, USB/internal GPS, session recording state, and sensor calibration. Emits flows for live metrics.
- **`TelemetryData.kt`**: Core domain model data classes (`TelemetryPoint`, `CornerEvent`, `RideSession`).
- **`VoiceCoach.kt`**: Text-to-speech engine providing real-time audio cues for corners and performance.
- **`DemoData.kt`**: Mock data generator used for previews and testing.
- `RideTrackerApplication.kt`: Application subclass for global context.

## Views & Layouts
- **`DashboardView.kt`**: Implements the main riding screens:
  - `DashboardPortraitLayout` & `DashboardLandscapeLayout`: The portrait and landscape main UI templates.
  - `DashboardDial`: Custom-drawn canvas meter illustrating live lean angle, horizon, and tick marks.
  - `SettingsScreen`: Brand preset color selections, unit toggles, GPS source selection, and data exports.
  - `CalibrationGuideOverlay` & `CalibrationAlertOverlay`: Multi-step interactive calibration overlay with a bubble-level.
- **`SessionSummaryView.kt`**:
  - `HistoryMenuScreen`: The list of past rides grouped by month.
  - `SessionSummaryScreen`: The pre-save screen displaying ride stats (distance, duration, top speed, max lean), route previews, and a report card.
  - `SessionSummaryOverlay`: Post-ride overlay when inspecting a past ride.
  - `TelemetryGraph`: Generates interactive, scrollable, and zoomable chart lines for speed/lean angles.
- **`CornerBreakdownView.kt`**: Details on each matched corner (T1, T2, etc.) compared to historical bests.
- **`ReportCardView.kt`**: Weekly trend analysis for ride smoothness, symmetry, and average lean.
- **`ShareableCardView.kt`**: Custom offscreen canvas rendering to export a high-res JPG/PNG share card.
- **`MapView.kt`**: Standard MapLibre integration for displaying ride traces.

## Algorithms & Matching
- **`CornerMatchingEngine.kt`**: Matches corner events by proximity (centroid lat/lng within `0.0003` delta) across previous sessions.

## Database & Persistence
- **`AppDatabase.kt`**: Main Room Database instance.
- **`CornerDao.kt`**: Data access object query interface for corners.
- **`CornerEntity.kt`, `CornerPbEntity.kt`, `SessionCornerEntity.kt`**: Persistence entities.

## UI Styling & Theme
- **`Color.kt`**, **`Theme.kt`**, **`Type.kt`**: Corporate color presets (Kawasaki Green, Ducati Red, KTM Orange, etc.) and fonts.
