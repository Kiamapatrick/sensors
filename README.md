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
- Handles empty inventory gracefully with a debug toggle (long-press toolbar title).
- Analysis screen with reflection answers and emulator limitations.
- Test log (E5-01 through E5-08) with clipboard copy.
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
- Long-press "Total sensors: N" text in MainActivity → adds synthetic test row with very long name.
- Rotate device/emulator (portrait ↔ landscape) on any screen → state preserved (scroll position, spinner selection, expanded states).
- Names wrap cleanly, no clipping/overflow in either orientation.

## Architecture

- **Plain Java** (no Kotlin, no Compose, no Navigation component, no third-party libs).
- **AndroidX** only: AppCompat, Material Components, RecyclerView, ConstraintLayout.
- **Helper classes:**
  - `SensorInfoFormatter` — type names, units, "what this means", honest fallbacks.
  - `EnvironmentDetector` — heuristics on Build.* for physical vs emulator.
  - `TestLog` — singleton, in-memory, survives rotation, fixed event IDs E5-01..E5-08.
- **All Activities** registered in Manifest with `parentActivityName` for Up navigation.
- **State saved/restored** via `onSaveInstanceState` (RecyclerView position, spinner, search query, empty-simulation flag).
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
- Long-name synthetic test row is labelled "SYNTHETIC TEST ROW" so it can't be mistaken for real data.