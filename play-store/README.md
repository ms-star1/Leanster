# Play Store submission docs — Leanster

Draft declarations for publishing **Leanster** (`com.leanster.app`) to Google Play.
All content reflects the app as built: on-device data only, no accounts/analytics/ads/backend.

| File | Console location | Purpose |
|------|------------------|---------|
| [privacy-policy.md](privacy-policy.md) | Store presence → Privacy policy | **Live:** https://ms-star1.github.io/Leanster/ |
| [data-safety.md](data-safety.md) | App content → Data safety | Fill-in answers for the Data safety form. |
| [foreground-service-declaration.md](foreground-service-declaration.md) | App content → Foreground service permissions | Justifications for `location` + `dataSync` FGS, and the demo-video checklist. |
| [content-rating.md](content-rating.md) | App content → Content ratings / Target audience | Questionnaire answers and audience settings. |
| [store-listing.md](store-listing.md) | Store presence → Main store listing | Title, short/full description, what's-new, ASO notes. |

The privacy policy is already hosted at **https://ms-star1.github.io/Leanster/** (source: `docs/index.html`).

**Before submitting:** replace the contact email if desired, record the FGS demo video, and confirm `targetSdk 37` (Android 17, stable since 16 Jun 2026) is what you ship.
