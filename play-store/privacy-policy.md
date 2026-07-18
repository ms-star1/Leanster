# Privacy Policy — Leanster

**Effective date:** 18 July 2026
**App:** Leanster (package `com.leanster.app`)
**Developer contact:** michasteinauer@gmail.com

> **Live at:** https://ms-star1.github.io/Leanster/ — this is the URL to paste into Google Play Console → *Store presence → Privacy policy*. (Source: `docs/index.html`, served via GitHub Pages.) Replace the contact email if you want to use a dedicated address.

---

## Summary

Leanster is a motorcycle riding-telemetry app. It measures lean angle, pitch, speed and location so you can review your rides. **Your ride data stays on your device.** Leanster has **no user accounts**, runs **no analytics or advertising**, contains **no crash-reporting SDK**, and operates **no server** that receives your data. Data only leaves your device when *you* choose to share it, or through the standard map and geocoding services described below.

## Information the app processes

| Data | Why | Where it goes |
|------|-----|---------------|
| **Precise location (GPS / connected GPS module)** | Track your route, speed, distance, and corner geometry during a ride. | Stored only on your device. Sent to the map and geocoding services below only as needed to draw maps and show place names. |
| **Motion & orientation sensors** (accelerometer, gyroscope, rotation vector) | Estimate lean and pitch angle. | Stored only on your device. Never transmitted. |
| **Ride sessions** (timestamps, coordinates, speed, lean/pitch, corner events) | Let you review and compare past rides. | Stored only on your device as local files and an on-device database. |

Leanster does **not** collect your name, email, contacts, phone identifiers, or advertising ID.

## Where your data is stored

All ride data is stored in the app's private storage on your device (local JSON files and a local database). Uninstalling the app removes this data. If your device has automatic app backup enabled, this local data may be included in your own device/cloud backup controlled by your Android settings.

## When data leaves your device

Leanster itself does not upload your data to any server we operate. Data may reach third parties only in these situations:

1. **Map display.** When a map is shown, map tiles are requested from **Esri ArcGIS Online** and **CartoDB (CARTO)**. These requests reveal the geographic area being displayed. See their policies:
   - Esri: https://www.esri.com/en-us/privacy/overview
   - CARTO: https://carto.com/privacy/
2. **Place names.** To label a ride with a city/place, Leanster uses **Android's built-in geocoding service**. On most devices this is provided by the operating system / Google Play services, so the coordinate being looked up is handled by that system provider under Google's terms: https://policies.google.com/privacy
3. **Sharing you initiate.** If you export a ride as **CSV** or share a **ride card image**, the file is sent only to the app or contact *you* select via the Android share sheet.
4. **Donation links.** Optional "support" buttons open Ko-fi or PayPal in your browser. Those sites have their own privacy policies.

## Data sharing and sale

We do **not** sell your personal data and do **not** share it with third parties for advertising or analytics.

## Permissions and why they are used

- **Location (fine/coarse)** — record ride route, speed, and corners; foreground-service location while recording.
- **Foreground service (location, data sync)** — keep recording reliably while the screen is off or the app is in the background.
- **Notifications** — show the ongoing "recording" notification required for the foreground service.
- **Internet / network state** — download map tiles and perform place-name lookups.
- **USB host** — optionally read data from a connected external GPS module. Not required.

## Children

Leanster is intended for adult motorcyclists and is not directed at children.

## Your choices

- Deny or revoke the location permission in Android settings (recording will be unavailable).
- Delete individual rides in the app, or clear all data by uninstalling.

## Changes to this policy

We may update this policy; the "Effective date" above will change accordingly.

## Contact

Questions about this policy: **michasteinauer@gmail.com**
