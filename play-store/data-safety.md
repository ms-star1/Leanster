# Data Safety Form — Answers for Google Play Console

App: **Leanster** (`com.ridetracker.ride`)
Location in console: *App content → Data safety*

> This is a fill-in guide. Enter these answers in the Data safety questionnaire. Answers reflect the app as built: on-device storage only, no analytics/ads, no first-party server, sharing only when the user initiates it.

---

## Section 1 — Data collection and security (overview)

- **Does your app collect or share any of the required user data types?** → **Yes**
  (You collect **Location**. Even though it stays on-device, "collect" in Play's definition includes accessing it in the app.)
- **Is all of the user data collected by your app encrypted in transit?** → **Yes**
  (Map-tile and geocoding requests use HTTPS. You transmit no other user data.)
- **Do you provide a way for users to request that their data is deleted?** → **Yes**
  (Users delete rides in-app and can remove all data by uninstalling. State this.)

## Section 2 — Data types

Declare exactly one data type:

### Location → Precise location
- **Collected:** Yes
- **Shared:** No
  (Map tile providers and the OS geocoder receive coordinates to *provide the map/place-name service* — under Play's rules that "processing on your behalf / for app functionality" is generally **not** counted as *sharing*. Do **not** declare it as shared. If you prefer maximum caution you may declare Shared = Yes with purpose "App functionality," but No is the accurate answer here.)
- **Processed ephemerally?** No (it is stored on the device).
- **Required or optional?** Required for the app's core function (users can decline the permission, but recording won't work).
- **Purposes:** **App functionality** (ride recording, maps, place names). *Not* analytics, *not* advertising, *not* personalization.

> Do **not** declare: personal info (name/email), financial info, contacts, messages, photos, app activity/analytics, device IDs, or advertising ID — the app collects none of these.

## Section 3 — Motion/orientation sensors

Android motion sensors (accelerometer/gyroscope/rotation vector) are **not** one of Play's Data safety data types and are **not** declared here. They never leave the device.

## Section 4 — Security practices to check

- Data is encrypted in transit: **Yes**
- Users can request deletion: **Yes** (in-app delete + uninstall)
- Committed to Play Families Policy: **No** (not a kids app)
- Independent security review: **No** (unless you obtain one)

## One-line rationale to keep on file

> Leanster processes precise location solely on-device for ride telemetry. It has no accounts, analytics, ads, or backend. Location reaches third parties only as HTTPS requests to map-tile (Esri, CARTO) and OS geocoding services strictly to render maps and place names; no personal data is sold or shared for advertising.
