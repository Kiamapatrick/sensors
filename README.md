# Sensor Lab

**App name:** Sensor Lab  
**Package:** com.example.sensors  
**Lab:** ICS 2300 (JKUAT) Lab_05_hm5 — Android sensor inventory and capability analysis  

This app is a **static inventory and analysis tool**. It does NOT register listeners or read live sensor data (that is Lab 06).

## What it does

- Lists all sensors reported by `SensorManager.getSensorList(TYPE_ALL)` with device info, total count, and environment detection (physical vs emulator).
- Shows detailed metadata for any sensor (range, resolution, power, min/max delay, reporting mode, wake-up, FIFO, dynamic, additional info) with correct units and plain-English explanations.
- Compares up to three auto-picked sensors side-by-side, plus a user-driven two-sensor comparison with interpretation.
- Includes a "MavunoLink" idea section justifying one sensor use for a farm-produce app (ambient light for photo lighting checks).
- Checks availability of specific sensor types safely (no crashes on unsupported sensors).
- Handles empty inventory gracefully with a debug toggle (long-press toolbar title or menu item).
- **Synthetic long-name test row**: toggle via long-press on "Total sensors" or the "Test: toggle long-name row" menu item (overflow). Adds a row with a 90+ character name, long vendor, type 0, and an amber "SYNTHETIC TEST ROW" chip. Opens in SensorDetailActivity with a banner and honest "Not reported" values. Total count and "Showing X of N" count only real sensors; a caption "+1 synthetic test row (not counted)" appears when visible.
- Analysis screen with reflection answers and emulator limitations.
- Test log (E5-01 through E5-08) with clipboard copy including device/environment header.
- Rotation-safe everywhere (state saved/restored).

## Screens

| Screen | Activity | Purpose |
|--------|----------|---------|
| Inventory | `MainActivity` | Device card, total count, searchable sensor list (R1, C1, C4) |
| Sensor Detail | `SensorDetailActivity` | Full metadata with units & meanings (R2, C2) |
| Compare | `CompareActivity` | 3-sensor auto-pick + 2-sensor user compare + MavunoLink (R2, R3) |
| Check Sensor | `CheckSensorActivity` | Safe availability check for a type (R4, C3) |
| Analysis | `AnalysisActivity` | Reflection answers, emulator limits, checklist (R3, R4) |
| Test Log | `TestLogActivity` | Evidence log E5-01..E5-08, copy to clipboard |

## How to run

1. Open in Android Studio (Giraffe or later).
2. Build → Run on emulator or physical device (minSdk 24).
3. No special permissions needed.

## Demo checks (C1–C4)

### C1. Inventory populated with device details
- Launch app → observe top device card: Manufacturer, Model, Android version/API, Hardware, Environment chip (green=physical, amber=emulator), Total sensors count.
- Scroll list → each row shows name (wraps to 3 lines), type+constant, vendor, wake-up chip if applicable.

### C2. Metadata labelled with correct units
- Tap any sensor row → SensorDetailActivity opens.
- Every numeric field shows value + unit (m/s², rad/s, µT, lux, cm, hPa, °C, %, unitless, steps, bpm, µs/Hz, mA).
- "Not reported by this device" shown honestly for zeros/missing.
- "What this means" explanation under each field.

### C3. Unsupported sensor handled safely
- Toolbar → "Check a sensor" → pick a type not on device (e.g., Heart Rate on many phones) → press Check.
- Red card appears: "Not available on this device. The app will not offer features that depend on this sensor." No crash.
- Pick an available type → green card with summary + "Open Details" button.

### C4. Readable long names and screen rotation
- **Long-press "Total sensors: N" text** in MainActivity → adds synthetic test row with very long name.
- **Toolbar overflow menu → "Test: toggle long-name row"** does the same.
- **Toolbar overflow menu → "Test: toggle empty inventory"** simulates empty inventory.
- Rotate device/emulator (portrait ↔ landscape) on any screen → state preserved (scroll position, spinner selection, expanded states, synthetic row visibility, empty simulation).
- Names wrap cleanly, no clipping/overflow in either orientation.

## Test Log (E5-01..E5-08) — Logging Rules

| Event | ID | When logged |
|-------|----|-------------|
| Inventory loaded | E5-01 | Fresh launch only, and on explicit **Refresh** menu action. Never on rotation. |
| Device/environment identified | E5-02 | Fresh launch only, and on explicit **Refresh** menu action. Never on rotation. |
| Sensor detail opened | E5-03 | Every time a sensor detail is opened (including synthetic row). |
| Three-sensor comparison shown | E5-04 | Fresh launch of CompareActivity only (`savedInstanceState == null`). Never on rotation. |
| Sensor availability checked | E5-05 | Every check in CheckSensorActivity. Wording: "Sensor availability checked: type=X, result=available" when available; "Unsupported sensor handled safely: type=X, not available" when not. |
| Empty inventory simulated | E5-06 | Every toggle of empty simulation (long-press toolbar title or menu item). |
| Rotation occurred | E5-07 | **Every activity** (Main, Detail, Compare, Check, Analysis, TestLog) when `onCreate` runs with `savedInstanceState != null`. Includes activity name and orientation (e.g., "Rotation occurred in CompareActivity (now landscape)"). |
| Long-name display verified | E5-08 | Only when synthetic row is **added** (not removed). Includes name length: "Long-name test row displayed (name length = N chars)". |

**Copy log** action copies all entries plus a header:
```
Device: <model>
Android: <version> (API <level>)
Environment: <Physical device or Emulator>
Copied: <timestamp>
---
[14:32:15.123] E5-01: Inventory loaded (12 sensors)
...
```

## Architecture

- **Plain Java** (no Kotlin, no Compose, no Navigation component, no third-party libs).
- **AndroidX** only: AppCompat, Material Components, RecyclerView, ConstraintLayout.
- **Helper classes:**
  - `SensorItem` — wraps real Sensor or synthetic test row with display fields.
  - `SensorInfoFormatter` — type names, units, "what this means", honest fallbacks. Uses `Locale.US` for float formatting.
  - `EnvironmentDetector` — heuristics on Build.* for physical vs emulator.
  - `TestLog` — singleton, in-memory, survives rotation, fixed event IDs E5-01..E5-08.
- **All Activities** registered in Manifest with `parentActivityName` for Up navigation.
- **State saved/restored** via `onSaveInstanceState` (RecyclerView position, spinner, search query, empty-simulation flag, synthetic-row flag).
- **Resources:** strings.xml, colors.xml (light/dark), dimens.xml, themes.xml (Material 3 DayNight).
- **No hardcoded strings** in layouts.

## Git

```bash
git init
git branch -M main
git add .
git commit -m "Initial commit: Lab 05 sensor inventory app"
git remote add origin https://github.com/Kiamapatrick/sensors.git
git push -u origin main
```

## Notes

- The emulator sensor list/values are simulated; conclusions about real phones cannot be drawn from it alone (see Analysis screen).
- TestLog resets when the app process dies (static singleton).
- Long-name synthetic test row is labelled "SYNTHETIC TEST ROW" (amber chip) so it can't be mistaken for real data.
- API-dependent fields (`isDynamicSensor` API 24+, `isAdditionalInfoSupported` API 26+) are guarded individually; detail screen shows "Not reported (requires Android N+/O+ API)" when unavailable.